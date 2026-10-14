import 'dart:io';

import 'package:family_outing_mobile/data/hive_service.dart';
import 'package:family_outing_mobile/main.dart';
import 'package:family_outing_mobile/models/expense.dart';
import 'package:family_outing_mobile/models/outing.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';

void main() {
  late Directory hiveDirectory;
  late Box<Outing> outings;
  late Box<Expense> expenses;

  setUp(() async {
    hiveDirectory = await Directory.systemTemp.createTemp(
      'family-app-dashboard-',
    );
    Hive.init(hiveDirectory.path);
    registerHiveAdapters();
    outings = await Hive.openBox<Outing>(outingsBoxName);
    await Hive.openBox(userAvailabilityBoxName);
    expenses = await Hive.openBox<Expense>(expensesBoxName);
    await Hive.openBox(pendingRequestsBoxName);
  });

  tearDown(() async {
    await Hive.close();
    await hiveDirectory.delete(recursive: true);
  });

  testWidgets('renders upcoming outings and family balance from Hive offline', (
    tester,
  ) async {
    final startTime = DateTime.now().add(const Duration(days: 2));
    final outing = Outing(
      id: 'outing-id',
      title: 'Botanical garden',
      status: 'OPEN',
      startTime: startTime,
      endTime: startTime.add(const Duration(hours: 2)),
      attendeeIds: const ['user-a', 'user-b'],
    );
    final expense = Expense(
      id: 'expense-id',
      outingId: outing.id,
      description: 'Tickets',
      amount: '20.00',
      payerId: 'user-a',
      createdAt: DateTime.now(),
      shares: const {'user-a': '10.00', 'user-b': '10.00'},
    );
    await outings.put(outing.id, outing);
    await expenses.put(expense.id, expense);

    await tester.pumpWidget(const MyApp());

    expect(find.text('Family plans'), findsOneWidget);
    expect(find.text('Next 7 days'), findsOneWidget);
    expect(find.text('Family balance'), findsOneWidget);
    expect(find.text('Unsettled from cached expenses'), findsOneWidget);
    expect(find.text('Upcoming outings'), findsOneWidget);
    expect(find.text('Botanical garden'), findsOneWidget);
    expect(find.text('\$10.00'), findsOneWidget);
  });
}
