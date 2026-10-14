import 'dart:io';

import 'package:family_outing_mobile/data/hive_service.dart';
import 'package:family_outing_mobile/models/expense.dart';
import 'package:family_outing_mobile/models/outing.dart';
import 'package:family_outing_mobile/models/user_availability.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';

void main() {
  late Directory hiveDirectory;

  setUp(() async {
    hiveDirectory = await Directory.systemTemp.createTemp('family-app-hive-test-');
    Hive.init(hiveDirectory.path);
    registerHiveAdapters();
  });

  tearDown(() async {
    await Hive.close();
    await hiveDirectory.delete(recursive: true);
  });

  test('stores and reads outings, availability, and exact decimal expense values', () async {
    final startTime = DateTime.utc(2026, 10, 13, 10);
    final outing = Outing(
      id: 'outing-id',
      title: 'Family outing',
      status: 'OPEN',
      startTime: startTime,
      endTime: startTime.add(const Duration(hours: 2)),
      attendeeIds: const ['user-a', 'user-b'],
    );
    final availability = UserAvailability(
      id: 'availability-id',
      userId: 'user-a',
      outingId: outing.id,
      startTime: startTime,
      endTime: startTime.add(const Duration(hours: 1)),
      isTentative: true,
    );
    final expense = Expense(
      id: 'expense-id',
      outingId: outing.id,
      description: 'Tickets',
      amount: '12.34',
      payerId: 'user-a',
      createdAt: startTime,
      shares: const {'user-a': '6.17', 'user-b': '6.17'},
    );

    final outingsBox = await Hive.openBox<Outing>('outings-test');
    final availabilityBox =
        await Hive.openBox<UserAvailability>('availability-test');
    final expensesBox = await Hive.openBox<Expense>('expenses-test');
    await outingsBox.put(outing.id, outing);
    await availabilityBox.put(availability.id, availability);
    await expensesBox.put(expense.id, expense);

    final restoredOuting = outingsBox.get(outing.id)!;
    final restoredAvailability = availabilityBox.get(availability.id)!;
    final restoredExpense = expensesBox.get(expense.id)!;

    expect(restoredOuting.attendeeIds, ['user-a', 'user-b']);
    expect(restoredOuting.startTime, startTime);
    expect(restoredAvailability.isTentative, isTrue);
    expect(restoredAvailability.outingId, outing.id);
    expect(restoredExpense.amount, '12.34');
    expect(restoredExpense.shares, {'user-a': '6.17', 'user-b': '6.17'});
  });
}
