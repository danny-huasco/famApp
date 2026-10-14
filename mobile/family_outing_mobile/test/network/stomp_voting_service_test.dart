import 'dart:async';

import 'package:family_outing_mobile/models/vote_tally.dart';
import 'package:family_outing_mobile/network/auth_token_store.dart';
import 'package:family_outing_mobile/network/stomp_voting_service.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:stomp_dart_client/stomp_dart_client.dart';

void main() {
  const outingId = '8fefb8e2-b845-4ba2-883a-d38e9d7697c4';
  const ideaId = '7a06b3d3-509f-459a-9ec6-a23e6b422758';
  late FakeAuthTokenStore tokenStore;
  late List<FakeStompClient> clients;
  late StompVotingService service;

  setUp(() {
    tokenStore = FakeAuthTokenStore('initial-jwt');
    clients = [];
    service = StompVotingService(
      apiBaseUrl: 'https://api.family-app.test/api',
      tokenStore: tokenStore,
      clientFactory: ({required config}) {
        final client = FakeStompClient(config: config);
        clients.add(client);
        return client;
      },
    );
  });

  tearDown(() async {
    await service.dispose();
  });

  test(
    'connects to /ws with a refreshed bearer token and restores topics',
    () async {
      final tallyStream = service.subscribeVotes(outingId);
      final tallies = <VoteTally>[];
      final subscription = tallyStream.listen(tallies.add);
      final connectionStates = <StompConnectionState>[];
      final stateSubscription = service.connectionStates.listen(
        connectionStates.add,
      );

      await service.connect();
      await pumpEventQueue();

      final client = clients.single;
      expect(client.config.url, 'wss://api.family-app.test/ws');
      expect(
        client.config.stompConnectHeaders?['Authorization'],
        'Bearer initial-jwt',
      );
      expect(client.destinations, ['/topic/outings/$outingId/votes']);

      client.emit(
        '/topic/outings/$outingId/votes',
        '{"outingId":"$outingId","ballotCount":3,'
            '"firstPreferenceCounts":{"$ideaId":2}}',
      );
      await pumpEventQueue();

      expect(tallies.single.outingId, outingId);
      expect(tallies.single.ballotCount, 3);
      expect(tallies.single.firstPreferenceCounts, {ideaId: 2});
      expect(connectionStates, [
        StompConnectionState.connecting,
        StompConnectionState.connected,
      ]);

      tokenStore.token = 'rotated-jwt';
      await client.reconnect();
      expect(
        client.config.stompConnectHeaders?['Authorization'],
        'Bearer rotated-jwt',
      );
      expect(client.destinations, hasLength(2));

      await subscription.cancel();
      await stateSubscription.cancel();
    },
  );

  test(
    'requires a token and stops reconnect when the token is removed',
    () async {
      tokenStore.token = null;

      await expectLater(service.connect(), throwsA(isA<StateError>()));

      tokenStore.token = 'valid-jwt';
      await service.connect();
      await pumpEventQueue();
      tokenStore.token = null;

      await clients.single.reconnect();

      expect(service.state, StompConnectionState.disconnected);
      expect(clients.single.isActive, isFalse);
    },
  );

  test('rejects invalid outing IDs before attempting a subscription', () {
    expect(() => service.subscribeVotes('not-a-uuid'), throwsArgumentError);
  });

  test('parses immutable vote tallies and rejects malformed payloads', () {
    final tally = VoteTally.fromJson(
      '{"outingId":"$outingId","ballotCount":2,'
      '"firstPreferenceCounts":{"$ideaId":2}}',
    );

    expect(tally.ballotCount, 2);
    expect(
      () => tally.firstPreferenceCounts[ideaId] = 5,
      throwsUnsupportedError,
    );
    expect(
      () => VoteTally.fromJson('{"ballotCount":-1}'),
      throwsFormatException,
    );
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

class FakeStompClient extends StompClient {
  FakeStompClient({required super.config});

  final destinations = <String>[];
  final _callbacks = <String, StompFrameCallback>{};
  bool _connected = false;
  bool _active = false;

  @override
  bool get connected => _connected;

  @override
  bool get isActive => _active;

  @override
  void activate() {
    _active = true;
    unawaited(_connect());
  }

  @override
  void deactivate() {
    _active = false;
    _connected = false;
  }

  @override
  StompUnsubscribe subscribe({
    required String destination,
    required StompFrameCallback callback,
    Map<String, String>? headers,
  }) {
    destinations.add(destination);
    _callbacks[destination] = callback;
    return ({Map<String, String>? unsubscribeHeaders}) {
      _callbacks.remove(destination);
    };
  }

  Future<void> reconnect() async {
    _connected = false;
    config.onWebSocketDone();
    await _connect();
  }

  void emit(String destination, String body) {
    _callbacks[destination]?.call(StompFrame(command: 'MESSAGE', body: body));
  }

  Future<void> _connect() async {
    await config.beforeConnect();
    if (_active) {
      _connected = true;
      config.onConnect(StompFrame(command: 'CONNECTED'));
    }
  }
}
