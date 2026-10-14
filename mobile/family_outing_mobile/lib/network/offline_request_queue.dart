import 'dart:async';
import 'dart:convert';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';
import 'package:hive/hive.dart';

import '../models/pending_request.dart';

abstract interface class NetworkStatus {
  Future<bool> isDisconnected();

  Stream<bool> get connectionAvailable;
}

class ConnectivityNetworkStatus implements NetworkStatus {
  ConnectivityNetworkStatus({Connectivity? connectivity})
    : _connectivity = connectivity ?? Connectivity();

  final Connectivity _connectivity;

  @override
  Future<bool> isDisconnected() async {
    final results = await _connectivity.checkConnectivity();
    return results.every((result) => result == ConnectivityResult.none);
  }

  @override
  Stream<bool> get connectionAvailable =>
      _connectivity.onConnectivityChanged.map((results) {
        return results.any((result) => result != ConnectivityResult.none);
      });
}

class OfflineRequestQueue {
  OfflineRequestQueue({
    required Box<PendingRequest> requests,
    required NetworkStatus networkStatus,
    required Dio dio,
    required void Function(Object error, StackTrace stackTrace) onReplayError,
  }) : this._(requests, networkStatus, dio, onReplayError);

  OfflineRequestQueue._(
    this._requests,
    this._networkStatus,
    this._dio,
    this._onReplayError,
  ) {
    _connectivitySubscription = _networkStatus.connectionAvailable.listen(
      (connected) {
        if (connected) {
          unawaited(_flushAndReport());
        }
      },
      onError: (Object error, StackTrace stackTrace) {
        _onReplayError(error, stackTrace);
      },
    );
  }

  final Box<PendingRequest> _requests;
  final NetworkStatus _networkStatus;
  final Dio _dio;
  final void Function(Object error, StackTrace stackTrace) _onReplayError;
  late final StreamSubscription<bool> _connectivitySubscription;
  bool _isFlushing = false;

  List<PendingRequest> get pendingRequests {
    final requests = _requests.values.toList()
      ..sort((first, second) => first.queuedAt.compareTo(second.queuedAt));
    return List.unmodifiable(requests);
  }

  Future<bool> isDisconnected() => _networkStatus.isDisconnected();

  Future<void> enqueue(RequestOptions options, String idempotencyKey) async {
    final data = options.data;
    final bodyJson = data == null ? null : jsonEncode(data);
    final queryParametersJson = jsonEncode(options.queryParameters);
    final existing = _requests.get(idempotencyKey);
    if (existing != null &&
        (existing.method != options.method ||
            existing.path != options.path ||
            existing.bodyJson != bodyJson ||
            existing.queryParametersJson != queryParametersJson ||
            existing.contentType != options.contentType)) {
      throw StateError(
        'Idempotency key is already queued for another request.',
      );
    }
    if (existing != null) {
      return;
    }
    await _requests.put(
      idempotencyKey,
      PendingRequest(
        idempotencyKey: idempotencyKey,
        method: options.method,
        path: options.path,
        bodyJson: bodyJson,
        queryParametersJson: queryParametersJson,
        contentType: options.contentType,
        queuedAt: DateTime.now().toUtc(),
      ),
    );
  }

  Future<void> flush() async {
    if (_isFlushing) {
      return;
    }
    _isFlushing = true;
    try {
      for (final request in pendingRequests) {
        await _replay(request);
      }
    } finally {
      _isFlushing = false;
    }
  }

  Future<void> discard(String idempotencyKey) =>
      _requests.delete(idempotencyKey);

  Future<void> dispose() => _connectivitySubscription.cancel();

  Future<void> _replay(PendingRequest request) async {
    try {
      final response = await _dio.request<dynamic>(
        request.path,
        data: request.bodyJson == null ? null : jsonDecode(request.bodyJson!),
        queryParameters:
            jsonDecode(request.queryParametersJson) as Map<String, dynamic>,
        options: Options(
          method: request.method,
          contentType: request.contentType,
          headers: {'Idempotency-Key': request.idempotencyKey},
          extra: const {'skipOfflineQueue': true},
        ),
      );
      final statusCode = response.statusCode;
      if (statusCode == null || statusCode < 200 || statusCode >= 300) {
        throw StateError('Queued request returned an invalid HTTP status.');
      }
      await _requests.delete(request.idempotencyKey);
    } catch (error, stackTrace) {
      await _requests.put(
        request.idempotencyKey,
        request.withReplayFailure(_safeFailureDescription(error)),
      );
      Error.throwWithStackTrace(error, stackTrace);
    }
  }

  Future<void> _flushAndReport() async {
    try {
      await flush();
    } catch (error, stackTrace) {
      _onReplayError(error, stackTrace);
    }
  }

  String _safeFailureDescription(Object error) {
    if (error is DioException) {
      final statusCode = error.response?.statusCode;
      return statusCode == null
          ? 'Transport failure (${error.type.name}).'
          : 'HTTP $statusCode while replaying queued request.';
    }
    return 'Replay failed (${error.runtimeType}).';
  }
}
