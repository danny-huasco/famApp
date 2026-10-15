import 'dart:convert';

import 'package:dio/dio.dart';

import '../models/availability_overview.dart';

class AvailabilitySaveResult {
  const AvailabilitySaveResult.queued()
      : queued = true,
        overview = null;

  const AvailabilitySaveResult.saved(this.overview) : queued = false;

  final bool queued;
  final AvailabilityOverview? overview;
}

class CalendarAvailabilityRepository {
  const CalendarAvailabilityRepository({required this._dio});

  final Dio _dio;

  Future<AvailabilityOverview> getOverview(String outingId) async {
    final response = await _dio.get<Map<String, dynamic>>(
      '/api/outings/$outingId/availability',
    );
    final data = response.data;
    if (data == null) {
      throw StateError('Availability response did not contain a body.');
    }
    return AvailabilityOverview.fromJson(_encodeJson(data));
  }

  Future<AvailabilitySaveResult> replaceMyWindows(
    String outingId,
    List<AvailabilityWindow> windows,
  ) async {
    final response = await _dio.put<Map<String, dynamic>>(
      '/api/outings/$outingId/availability',
      data: {
        'windows': windows.map((window) => window.toRequestJson()).toList(),
      },
    );
    final data = response.data;
    if (response.statusCode == 202 && data?['queued'] == true) {
      return const AvailabilitySaveResult.queued();
    }
    if (data == null) {
      throw StateError('Availability update response did not contain a body.');
    }
    return AvailabilitySaveResult.saved(
      AvailabilityOverview.fromJson(_encodeJson(data)),
    );
  }

  String _encodeJson(Map<String, dynamic> json) => jsonEncode(json);
}
