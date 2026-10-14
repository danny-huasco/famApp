import 'dart:async';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../models/vote_tally.dart';
import 'auth_token_store.dart';

enum StompConnectionState { disconnected, connecting, connected }

typedef StompClientFactory = StompClient Function({
  required StompConfig config,
});

class StompVotingService {
  StompVotingService({
    required String apiBaseUrl,
    required AuthTokenStore tokenStore,
    Duration reconnectDelay = const Duration(seconds: 5),
    StompClientFactory clientFactory = StompClient.new,
  }) : this._(
         _toWebSocketUrl(apiBaseUrl),
         tokenStore,
         reconnectDelay,
         clientFactory,
       );

  StompVotingService._(
    this._webSocketUrl,
    this._tokenStore,
    this._reconnectDelay,
    this._clientFactory,
  );

  static final RegExp _uuidPattern = RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
  );

  final String _webSocketUrl;
  final AuthTokenStore _tokenStore;
  final Duration _reconnectDelay;
  final StompClientFactory _clientFactory;
  final Map<String, StreamController<VoteTally>> _voteStreams = {};
  final Map<String, StompUnsubscribe> _subscriptions = {};
  final StreamController<StompConnectionState> _stateController =
      StreamController<StompConnectionState>.broadcast();
  final StreamController<Object> _errorController =
      StreamController<Object>.broadcast();

  StompClient? _client;
  StompConnectionState _state = StompConnectionState.disconnected;
  bool _active = false;
  bool _disposed = false;

  Stream<StompConnectionState> get connectionStates => _stateController.stream;

  Stream<Object> get errors => _errorController.stream;

  StompConnectionState get state => _state;

  Future<void> connect() async {
    if (_disposed) {
      throw StateError('The STOMP voting service has been disposed.');
    }
    if (_active) {
      return;
    }

    final token = await _tokenStore.readToken();
    if (token == null || token.isEmpty) {
      throw StateError('A JWT is required to connect to voting updates.');
    }

    _active = true;
    _setState(StompConnectionState.connecting);
    final connectHeaders = <String, String>{'Authorization': 'Bearer $token'};
    _client = _clientFactory(
      config: StompConfig(
        url: _webSocketUrl,
        reconnectDelay: _reconnectDelay,
        stompConnectHeaders: connectHeaders,
        beforeConnect: () => _refreshConnectHeaders(connectHeaders),
        onConnect: (_) {
          if (!_active) {
            return;
          }
          _setState(StompConnectionState.connected);
          _subscribeToRequestedOutings();
        },
        onStompError: (frame) {
          _errorController.add(
            StateError(frame.body ?? 'The STOMP broker returned an error.'),
          );
        },
        onWebSocketError: (error) {
          _errorController.add(_asObject(error));
          _setState(StompConnectionState.disconnected);
        },
        onWebSocketDone: () {
          _subscriptions.clear();
          _setState(StompConnectionState.disconnected);
        },
        onDisconnect: (_) => _setState(StompConnectionState.disconnected),
      ),
    );
    _client!.activate();
  }

  Stream<VoteTally> subscribeVotes(String outingId) {
    if (_disposed) {
      throw StateError('The STOMP voting service has been disposed.');
    }
    if (!_uuidPattern.hasMatch(outingId)) {
      throw ArgumentError.value(outingId, 'outingId', 'Must be a UUID.');
    }

    final existing = _voteStreams[outingId];
    if (existing != null) {
      return existing.stream;
    }

    late final StreamController<VoteTally> controller;
    controller = StreamController<VoteTally>.broadcast(
      onListen: () => _subscribe(outingId),
      onCancel: () {
        _subscriptions.remove(outingId)?.call();
        _voteStreams.remove(outingId);
        unawaited(controller.close());
      },
    );
    _voteStreams[outingId] = controller;
    _subscribe(outingId);
    return controller.stream;
  }

  Future<void> disconnect() async {
    _active = false;
    _client?.deactivate();
    _client = null;
    _subscriptions.clear();
    _setState(StompConnectionState.disconnected);
  }

  Future<void> dispose() async {
    if (_disposed) {
      return;
    }
    await disconnect();
    _disposed = true;
    for (final controller in _voteStreams.values) {
      await controller.close();
    }
    _voteStreams.clear();
    await _stateController.close();
    await _errorController.close();
  }

  Future<void> _refreshConnectHeaders(Map<String, String> headers) async {
    _setState(StompConnectionState.connecting);
    try {
      final token = await _tokenStore.readToken();
      if (token == null || token.isEmpty) {
        _errorController.add(
          StateError('JWT is unavailable; stopping STOMP reconnection.'),
        );
        _active = false;
        _client?.deactivate();
        _setState(StompConnectionState.disconnected);
        return;
      }
      headers['Authorization'] = 'Bearer $token';
    } catch (error) {
      _errorController.add(_asObject(error));
      _active = false;
      _client?.deactivate();
      _setState(StompConnectionState.disconnected);
    }
  }

  void _subscribeToRequestedOutings() {
    for (final outingId in _voteStreams.keys.toList()) {
      _subscribe(outingId);
    }
  }

  void _subscribe(String outingId) {
    final client = _client;
    final controller = _voteStreams[outingId];
    if (!_active ||
        client == null ||
        !client.connected ||
        controller == null ||
        _subscriptions.containsKey(outingId)) {
      return;
    }

    _subscriptions[outingId] = client.subscribe(
      destination: '/topic/outings/$outingId/votes',
      callback: (frame) {
        final body = frame.body;
        if (body == null) {
          _errorController.add(
            const FormatException('Vote tally message has no body.'),
          );
          return;
        }
        try {
          final tally = VoteTally.fromJson(body);
          if (tally.outingId != outingId) {
            throw const FormatException(
              'Vote tally outing does not match its subscription.',
            );
          }
          controller.add(tally);
        } catch (error) {
          _errorController.add(_asObject(error));
        }
      },
    );
  }

  void _setState(StompConnectionState next) {
    if (_state == next || _stateController.isClosed) {
      return;
    }
    _state = next;
    _stateController.add(next);
  }

  static String _toWebSocketUrl(String apiBaseUrl) {
    final uri = Uri.parse(apiBaseUrl);
    if (!uri.hasAuthority ||
        !const {'http', 'https', 'ws', 'wss'}.contains(uri.scheme)) {
      throw ArgumentError.value(
        apiBaseUrl,
        'apiBaseUrl',
        'Must be an HTTP(S) URL.',
      );
    }
    return uri
        .replace(
          scheme: uri.scheme == 'https' || uri.scheme == 'wss' ? 'wss' : 'ws',
          path: '/ws',
          query: null,
          fragment: null,
        )
        .toString();
  }

  static Object _asObject(Object? error) =>
      error ?? StateError('Unknown WebSocket error.');
}
