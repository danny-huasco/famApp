import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:family_outing_mobile/data/hive_service.dart';
import 'package:family_outing_mobile/models/pending_request.dart';
import 'package:family_outing_mobile/network/auth_interceptor.dart';
import 'package:family_outing_mobile/network/auth_token_store.dart';
import 'package:family_outing_mobile/network/offline_mutation_interceptor.dart';
import 'package:family_outing_mobile/network/offline_request_queue.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';

void main() {
  late Directory hiveDirectory;
  late Box<PendingRequest> pendingRequests;
  late TestNetworkStatus networkStatus;
  late RecordingAdapter adapter;
  late Dio dio;
  late OfflineRequestQueue queue;
  late FakeAuthTokenStore tokenStore;
  final replayErrors = <Object>[];

  setUp(() async {
    replayErrors.clear();
    hiveDirectory = await Directory.systemTemp.createTemp(
      'family-app-queue-test-',
    );
    Hive.init(hiveDirectory.path);
    registerHiveAdapters();
    pendingRequests = await Hive.openBox<PendingRequest>(
      'pending-requests-test',
    );
    networkStatus = TestNetworkStatus();
    adapter = RecordingAdapter();
    tokenStore = FakeAuthTokenStore('test-jwt');
    dio = Dio(BaseOptions(baseUrl: 'https://family-app.test'));
    dio.httpClientAdapter = adapter;
    queue = OfflineRequestQueue(
      requests: pendingRequests,
      networkStatus: networkStatus,
      dio: dio,
      onReplayError: (error, _) => replayErrors.add(error),
    );
    dio.interceptors.addAll([
      AuthInterceptor(tokenStore: tokenStore, onUnauthorized: () {}),
      OfflineMutationInterceptor(requestQueue: queue),
    ]);
  });

  tearDown(() async {
    await queue.dispose();
    await networkStatus.close();
    dio.close(force: true);
    await Hive.close();
    await hiveDirectory.delete(recursive: true);
  });

  test('adds the secure JWT as a bearer authorization header', () async {
    await dio.get<dynamic>('/api/protected');

    expect(adapter.requests.single.headers['Authorization'], 'Bearer test-jwt');
  });

  test(
    'clears the token and invokes the unauthorized callback on 401',
    () async {
      adapter.statusCode = 401;
      var unauthorizedCalls = 0;
      dio.interceptors
        ..clear()
        ..add(
          AuthInterceptor(
            tokenStore: tokenStore,
            onUnauthorized: () => unauthorizedCalls++,
          ),
        );

      await expectLater(
        dio.get<dynamic>('/api/protected'),
        throwsA(isA<DioException>()),
      );

      expect(tokenStore.token, isNull);
      expect(unauthorizedCalls, 1);
    },
  );

  test(
    'queues offline mutations without storing authorization credentials',
    () async {
      networkStatus.disconnected = true;

      final response = await dio.post<Map<String, dynamic>>(
        '/api/expenses',
        data: {'amount': '12.34'},
      );

      expect(response.statusCode, 202);
      expect(response.data?['queued'], isTrue);
      expect(adapter.requests, isEmpty);
      expect(pendingRequests.length, 1);
      expect(pendingRequests.values.single.bodyJson, '{"amount":"12.34"}');
    },
  );

  test(
    'rejects a queued idempotency key reused with different input',
    () async {
      networkStatus.disconnected = true;
      const key = '8fefb8e2-b845-4ba2-883a-d38e9d7697c4';
      await dio.post<dynamic>(
        '/api/expenses',
        data: {'amount': '12.34'},
        options: Options(headers: {'Idempotency-Key': key}),
      );

      await expectLater(
        dio.post<dynamic>(
          '/api/expenses',
          data: {'amount': '99.99'},
          options: Options(headers: {'Idempotency-Key': key}),
        ),
        throwsA(isA<DioException>()),
      );

      expect(pendingRequests.values.single.bodyJson, '{"amount":"12.34"}');
    },
  );

  test(
    'replays with the original idempotency key and removes on success',
    () async {
      networkStatus.disconnected = true;
      await dio.post<dynamic>(
        '/api/expenses',
        data: {'amount': '12.34'},
        options: Options(
          headers: {'Idempotency-Key': '8fefb8e2-b845-4ba2-883a-d38e9d7697c4'},
        ),
      );
      networkStatus.disconnected = false;

      await queue.flush();

      expect(
        adapter.requests.single.headers['Idempotency-Key'],
        '8fefb8e2-b845-4ba2-883a-d38e9d7697c4',
      );
      expect(
        adapter.requests.single.headers['Authorization'],
        'Bearer test-jwt',
      );
      expect(pendingRequests.length, 0);
    },
  );

  test('retains failed replay with a safe error description', () async {
    networkStatus.disconnected = true;
    await dio.post<dynamic>(
      '/api/expenses',
      data: {'amount': '12.34'},
      options: Options(
        headers: {'Idempotency-Key': '8fefb8e2-b845-4ba2-883a-d38e9d7697c4'},
      ),
    );
    networkStatus.disconnected = false;
    adapter.statusCode = 500;

    await expectLater(queue.flush(), throwsA(isA<DioException>()));

    final retainedRequest = pendingRequests.values.single;
    expect(retainedRequest.attempts, 1);
    expect(
      retainedRequest.lastError,
      'HTTP 500 while replaying queued request.',
    );
    expect(replayErrors, isEmpty);
  });
}

class FakeAuthTokenStore implements AuthTokenStore {
  FakeAuthTokenStore(this.token);

  String? token;

  @override
  Future<void> clearToken() async {
    token = null;
  }

  @override
  Future<String?> readToken() async => token;

  @override
  Future<void> saveToken(String value) async {
    token = value;
  }
}

class TestNetworkStatus implements NetworkStatus {
  final _connectionChanges = StreamController<bool>.broadcast();
  bool disconnected = false;

  @override
  Stream<bool> get connectionAvailable => _connectionChanges.stream;

  @override
  Future<bool> isDisconnected() async => disconnected;

  Future<void> close() => _connectionChanges.close();
}

class RecordingAdapter implements HttpClientAdapter {
  final requests = <RequestOptions>[];
  int statusCode = 200;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    return ResponseBody.fromString(
      '{"ok":true}',
      statusCode,
      headers: {
        'content-type': ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
