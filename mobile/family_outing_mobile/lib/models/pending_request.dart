import 'package:hive/hive.dart';

part 'pending_request.g.dart';

@HiveType(typeId: 3)
class PendingRequest {
  const PendingRequest({
    required this.idempotencyKey,
    required this.method,
    required this.path,
    required this.bodyJson,
    required this.queryParametersJson,
    required this.contentType,
    required this.queuedAt,
    this.attempts = 0,
    this.lastError,
  });

  @HiveField(0)
  final String idempotencyKey;

  @HiveField(1)
  final String method;

  @HiveField(2)
  final String path;

  @HiveField(3)
  final String? bodyJson;

  @HiveField(4)
  final String queryParametersJson;

  @HiveField(5)
  final String? contentType;

  @HiveField(6)
  final DateTime queuedAt;

  @HiveField(7)
  final int attempts;

  @HiveField(8)
  final String? lastError;

  PendingRequest withReplayFailure(String error) => PendingRequest(
        idempotencyKey: idempotencyKey,
        method: method,
        path: path,
        bodyJson: bodyJson,
        queryParametersJson: queryParametersJson,
        contentType: contentType,
        queuedAt: queuedAt,
        attempts: attempts + 1,
        lastError: error,
      );
}
