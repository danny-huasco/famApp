import 'dart:convert';

class VoteTally {
  VoteTally({
    required this.outingId,
    required this.ballotCount,
    required Map<String, int> firstPreferenceCounts,
  }) : firstPreferenceCounts = Map.unmodifiable(firstPreferenceCounts);

  final String outingId;
  final int ballotCount;
  final Map<String, int> firstPreferenceCounts;

  factory VoteTally.fromJson(String json) {
    final decoded = jsonDecode(json);
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('Vote tally payload must be a JSON object.');
    }

    final outingId = decoded['outingId'];
    final ballotCount = decoded['ballotCount'];
    final rawCounts = decoded['firstPreferenceCounts'];
    if (outingId is! String ||
        !_isUuid(outingId) ||
        ballotCount is! int ||
        ballotCount < 0 ||
        rawCounts is! Map) {
      throw const FormatException('Vote tally payload has invalid fields.');
    }

    final counts = <String, int>{};
    for (final entry in rawCounts.entries) {
      if (entry.key is! String ||
          !_isUuid(entry.key as String) ||
          entry.value is! int ||
          (entry.value as int) < 0) {
        throw const FormatException(
          'Vote tally contains invalid first-preference counts.',
        );
      }
      counts[entry.key as String] = entry.value as int;
    }

    return VoteTally(
      outingId: outingId,
      ballotCount: ballotCount,
      firstPreferenceCounts: counts,
    );
  }

  static bool _isUuid(String value) {
    return RegExp(
      r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
    ).hasMatch(value);
  }
}
