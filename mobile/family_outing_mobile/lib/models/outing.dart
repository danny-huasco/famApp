import 'package:hive/hive.dart';

part 'outing.g.dart';

@HiveType(typeId: 0)
class Outing {
  Outing({
    required this.id,
    required this.title,
    this.description,
    required this.status,
    required this.startTime,
    required this.endTime,
    required List<String> attendeeIds,
  }) : attendeeIds = List.unmodifiable(attendeeIds);

  @HiveField(0)
  final String id;

  @HiveField(1)
  final String title;

  @HiveField(2)
  final String? description;

  @HiveField(3)
  final String status;

  @HiveField(4)
  final DateTime startTime;

  @HiveField(5)
  final DateTime endTime;

  @HiveField(6)
  final List<String> attendeeIds;
}
