import 'package:dio/dio.dart';
import 'package:family_outing_mobile/network/calendar_availability_repository.dart';
import 'package:family_outing_mobile/screens/availability_setup_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

const outingId = '8fefb8e2-b845-4ba2-883a-d38e9d7697c4';
const attendeeId = '7a06b3d3-509f-459a-9ec6-a23e6b422758';

void main() {
  testWidgets('shows attendee availability and server-calculated overlap', (
    tester,
  ) async {
    final dio = _createDio(_overview(hasWindow: true));
    await tester.pumpWidget(
      MaterialApp(
        home: AvailabilitySetupScreen(
          outingId: outingId,
          outingTitle: 'Family picnic',
          repository: CalendarAvailabilityRepository(dio: dio),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('When are you available?'), findsOneWidget);
    expect(find.text('Your time windows'), findsOneWidget);
    expect(find.text('Group overlap'), findsOneWidget);
    expect(find.text('All attendees are available'), findsOneWidget);
    expect(find.text('Save availability'), findsNothing);

    dio.close(force: true);
  });

  testWidgets('clears saved windows through the replacement endpoint', (
    tester,
  ) async {
    final dio = Dio(BaseOptions(baseUrl: 'https://family-app.test'));
    final requests = <RequestOptions>[];
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options);
          final data = options.method == 'PUT'
              ? _overview(hasWindow: false)
              : _overview(hasWindow: true);
          handler.resolve(
            Response<Map<String, dynamic>>(
              requestOptions: options,
              statusCode: 200,
              data: data,
            ),
          );
        },
      ),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: AvailabilitySetupScreen(
          outingId: outingId,
          outingTitle: 'Family picnic',
          repository: CalendarAvailabilityRepository(dio: dio),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Remove time window'));
    await tester.pump();
    await tester.tap(find.text('Save availability'));
    await tester.pumpAndSettle();

    expect(requests.map((request) => request.method), ['GET', 'PUT']);
    expect(requests.last.data, {'windows': <Object>[]});
    expect(find.text('Availability updated.'), findsOneWidget);
    expect(find.text('No availability added yet.'), findsOneWidget);

    dio.close(force: true);
  });
}

Dio _createDio(Map<String, dynamic> responseBody) {
  final dio = Dio(BaseOptions(baseUrl: 'https://family-app.test'));
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) {
        handler.resolve(
          Response<Map<String, dynamic>>(
            requestOptions: options,
            statusCode: 200,
            data: responseBody,
          ),
        );
      },
    ),
  );
  return dio;
}

Map<String, dynamic> _overview({required bool hasWindow}) {
  final window = {
    'id': 'window-id',
    'userId': attendeeId,
    'startTime': '2026-10-15T09:00:00Z',
    'endTime': '2026-10-15T10:00:00Z',
    'tentative': false,
  };
  return {
    'outingId': outingId,
    'myWindows': hasWindow ? [window] : <Object>[],
    'attendees': [
      {
        'userId': attendeeId,
        'windows': hasWindow ? [window] : <Object>[],
      },
    ],
    'commonFreeSlots': hasWindow
        ? [
            {
              'startTime': '2026-10-15T09:00:00Z',
              'endTime': '2026-10-15T10:00:00Z',
              'tentative': false,
            },
          ]
        : <Object>[],
  };
}
