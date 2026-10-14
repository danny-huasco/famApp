import 'package:hive/hive.dart';

part 'expense.g.dart';

@HiveType(typeId: 2)
class Expense {
  Expense({
    required this.id,
    required this.outingId,
    required this.description,
    required this.amount,
    required this.payerId,
    required this.createdAt,
    required Map<String, String> shares,
  }) : shares = Map.unmodifiable(shares);

  @HiveField(0)
  final String id;

  @HiveField(1)
  final String outingId;

  @HiveField(2)
  final String description;

  @HiveField(3)
  final String amount;

  @HiveField(4)
  final String payerId;

  @HiveField(5)
  final DateTime createdAt;

  @HiveField(6)
  final Map<String, String> shares;
}
