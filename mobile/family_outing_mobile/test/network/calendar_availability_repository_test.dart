import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:family_outing_mobile/models/availability_overview.dart';
import 'package:family_outing_mobile/network/calendar_availability_repository.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const outingId = '8fefb8e2-b845-4ba2-883a-d38e9d7697c4';
  const attendeeId = '7a06b3d3-509f-459a-9ec6-a23e6b422758';
  final commonResponse = {
    'outingId': outingId,
    'myWindows': [
      {
        'id': 'window-id',
        'userId': attendeeId,
        'startTime': '2026-10-15T09:00:00Z',
        'endTime': '2026-10-15T10:00:00Z',
        'tentative': false,
      },
    ],
    'attendees': [
      {
        'userId': attendeeId,
        'windows': [
          {
            'id': 'window-id',
            'userId': attendeeId,
            'startTime': '2026-10-15T09:00:00Z',
            'endTime': '2026-10-15T10:00:00Z',
            'tentative': false,
          },
        ],
      },
    ],
    'commonFreeSlots': [
      {
        'startTime': '2026-10-15T09:00:00Z',
        'endTime': '2026-10-15T10:00:00Z',
        'tentative': false,
      },
    ],
  };
  late Dio dio;
  late ScriptedAdapter adapter;
  late CalendarAvailabilityRepository repository;
  Object? interceptedBody;

  setUp(() {
    interceptedBody = null;
    adapter = ScriptedAdapter(body: commonResponse);
    dio = Dio(BaseOptions(baseUrl: 'https://family-app.test'));
    dio.httpClientAdapter = adapter;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          interceptedBody = options.data;
          handler.next(options);
        },
      ),
    );
    repository = CalendarAvailabilityRepository(dio: dio);
  });

  tearDown(() => dio.close(force: true));

  test('loads the caller windows, attendee windows, and common overlaps',
      () async {
    final overview = await repository.getOverview(outingId);

    expect(adapter.requests.single.path, '/api/outings/$outingId/availability');
    expect(overview.myWindows.single.userId, attendeeId);
    expect(overview.attendees.single.windows.single.tentative, isFalse);
    expect(overview.commonFreeSlots.single.startTime, DateTime.utc(2026, 10, 15, 9));
  });

  test('replaces availability using a UTC payload and parses the response',
      () async {
    final result = await repository.replaceMyWindows(
      outingId,
      [
        AvailabilityWindow(
          startTime: DateTime.utc(2026, 10, 15, 9),
          endTime: DateTime.utc(2026, 10, 15, 10),
          tentative: true,
        ),
      ],
    );

    expect(result.queued, isFalse);
    expect(result.overview?.outingId, outingId);
    expect(adapter.requests.single.method, 'PUT');
    expect(
      interceptedBody,
      {
        'windows': [
          {
            'startTime': '2026-10-15T09:00:00.000Z',
            'endTime': '2026-10-15T10:00:00.000Z',
            'tentative': true,
          },
        ],
      },
    );
  });

  test('recognizes a locally queued offline update', () async {
    adapter.statusCode = 202;
    adapter.body = {'queued': true, 'idempotencyKey': 'queued-key'};

    final result = await repository.replaceMyWindows(outingId, const []);

    expect(result.queued, isTrue);
    expect(result.overview, isNull);
  });

  test('rejects invalid time ranges while parsing overview payloads', () {
    expect(
      () => AvailabilityOverview.fromJson(
        jsonEncode({
          ...commonResponse,
          'myWindows': [
            {
              'startTime': '2026-10-15T10:00:00Z',
              'endTime': '2026-10-15T09:00:00Z',
              'tentative': false,
            },
          ],
        }),
      ),
      throwsFormatException,
    );
  });
}

class ScriptedAdapter implements HttpClientAdapter {
  ScriptedAdapter({required this.body});

  final requests = <RequestOptions>[];
  Object? body;
  int statusCode = 200;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    return ResponseBody.fromString(
      jsonEncode(body),
      statusCode,
      headers: {'content-type': ['application/json']},
    );
  }

  @override
  void close({bool force = false}) {}
}
