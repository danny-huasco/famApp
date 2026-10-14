import 'package:hive/hive.dart';

part 'user_availability.g.dart';

@HiveType(typeId: 1)
class UserAvailability {
  const UserAvailability({
    required this.id,
    required this.userId,
    this.outingId,
    required this.startTime,
    required this.endTime,
    required this.isTentative,
  });

  @HiveField(0)
  final String id;

  @HiveField(1)
  final String userId;

  @HiveField(2)
  final String? outingId;

  @HiveField(3)
  final DateTime startTime;

  @HiveField(4)
  final DateTime endTime;

  @HiveField(5)
  final bool isTentative;
}
