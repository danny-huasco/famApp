import 'package:dio/dio.dart';

import 'auth_token_store.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor({
    required AuthTokenStore tokenStore,
    required void Function() onUnauthorized,
  }) : this._(tokenStore, onUnauthorized);

  AuthInterceptor._(this._tokenStore, this._onUnauthorized);

  final AuthTokenStore _tokenStore;
  final void Function() _onUnauthorized;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    _addAuthorization(options, handler);
  }

  Future<void> _addAuthorization(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    try {
      final token = await _tokenStore.readToken();
      if (token != null && token.isNotEmpty) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    } catch (error) {
      handler.reject(DioException(requestOptions: options, error: error));
    }
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode != 401) {
      handler.next(err);
      return;
    }
    _clearExpiredToken(err, handler);
  }

  Future<void> _clearExpiredToken(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    try {
      await _tokenStore.clearToken();
      _onUnauthorized();
      handler.next(error);
    } catch (storageError) {
      handler.reject(
        DioException(requestOptions: error.requestOptions, error: storageError),
      );
    }
  }
}
