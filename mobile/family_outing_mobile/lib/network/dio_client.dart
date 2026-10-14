import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:hive/hive.dart';

import '../models/pending_request.dart';
import 'auth_interceptor.dart';
import 'auth_token_store.dart';
import 'offline_mutation_interceptor.dart';
import 'offline_request_queue.dart';

class DioClient {
  DioClient({
    required String baseUrl,
    required AuthTokenStore tokenStore,
    required Box<PendingRequest> pendingRequests,
    required NetworkStatus networkStatus,
    required VoidCallback onUnauthorized,
    required void Function(Object error, StackTrace stackTrace) onReplayError,
  }) : dio = Dio(BaseOptions(baseUrl: baseUrl)) {
    offlineQueue = OfflineRequestQueue(
      requests: pendingRequests,
      networkStatus: networkStatus,
      dio: dio,
      onReplayError: onReplayError,
    );
    dio.interceptors.addAll([
      AuthInterceptor(tokenStore: tokenStore, onUnauthorized: onUnauthorized),
      OfflineMutationInterceptor(requestQueue: offlineQueue),
    ]);
  }

  final Dio dio;
  late final OfflineRequestQueue offlineQueue;

  Future<void> dispose() async {
    await offlineQueue.dispose();
    dio.close(force: true);
  }
}
