import 'dart:async';

import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';

import 'offline_request_queue.dart';

class OfflineMutationInterceptor extends Interceptor {
  OfflineMutationInterceptor({
    required OfflineRequestQueue requestQueue,
    Uuid uuid = const Uuid(),
  }) : this._(requestQueue, uuid);

  OfflineMutationInterceptor._(this._requestQueue, this._uuid);

  static const String _idempotencyHeader = 'Idempotency-Key';
  static const String _skipQueueExtra = 'skipOfflineQueue';

  final OfflineRequestQueue _requestQueue;
  final Uuid _uuid;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    if (!_isMutation(options) ||
        options.extra[_skipQueueExtra] == true ||
        options.headers['Authorization'] == null) {
      handler.next(options);
      return;
    }

    final key = options.headers.putIfAbsent(_idempotencyHeader, _uuid.v4);
    _checkConnectivity(options, key.toString(), handler);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (!_isMutation(err.requestOptions) ||
        err.requestOptions.extra[_skipQueueExtra] == true ||
        err.requestOptions.headers['Authorization'] == null ||
        err.response != null ||
        !_isUncertainTransportFailure(err.type)) {
      handler.next(err);
      return;
    }

    _queueAfterTransportFailure(err, handler);
  }

  void _checkConnectivity(
    RequestOptions options,
    String idempotencyKey,
    RequestInterceptorHandler handler,
  ) {
    unawaited(_resolveOfflineRequest(options, idempotencyKey, handler));
  }

  Future<void> _resolveOfflineRequest(
    RequestOptions options,
    String idempotencyKey,
    RequestInterceptorHandler handler,
  ) async {
    try {
      if (!await _requestQueue.isDisconnected()) {
        handler.next(options);
        return;
      }
      await _requestQueue.enqueue(options, idempotencyKey);
      handler.resolve(_queuedResponse(options, idempotencyKey));
    } catch (error) {
      handler.reject(DioException(requestOptions: options, error: error));
    }
  }

  void _queueAfterTransportFailure(
    DioException error,
    ErrorInterceptorHandler handler,
  ) {
    unawaited(_saveAndResolve(error, handler));
  }

  Future<void> _saveAndResolve(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final options = error.requestOptions;
    final key = options.headers[_idempotencyHeader]?.toString();
    if (key == null || key.isEmpty) {
      handler.next(error);
      return;
    }
    try {
      await _requestQueue.enqueue(options, key);
      handler.resolve(_queuedResponse(options, key));
    } catch (queueError) {
      handler.reject(DioException(requestOptions: options, error: queueError));
    }
  }

  Response<Map<String, Object>> _queuedResponse(
    RequestOptions options,
    String idempotencyKey,
  ) {
    return Response<Map<String, Object>>(
      requestOptions: options,
      statusCode: 202,
      data: {'queued': true, 'idempotencyKey': idempotencyKey},
    );
  }

  bool _isMutation(RequestOptions options) {
    return const {'POST', 'PUT', 'PATCH', 'DELETE'}.contains(options.method);
  }

  bool _isUncertainTransportFailure(DioExceptionType type) {
    return const {
      DioExceptionType.connectionError,
      DioExceptionType.connectionTimeout,
      DioExceptionType.sendTimeout,
      DioExceptionType.receiveTimeout,
    }.contains(type);
  }
}
