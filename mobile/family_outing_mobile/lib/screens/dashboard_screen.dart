import 'package:flutter/material.dart';
import 'package:hive_flutter/hive_flutter.dart';

import '../data/hive_service.dart';
import '../models/expense.dart';
import '../models/outing.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Family plans')),
      body: ValueListenableBuilder<Box<Outing>>(
        valueListenable: HiveService.outings.listenable(),
        builder: (context, outingsBox, _) {
          return ValueListenableBuilder<Box<Expense>>(
            valueListenable: HiveService.expenses.listenable(),
            builder: (context, expensesBox, _) {
              final now = DateTime.now();
              final upcomingOutings =
                  outingsBox.values
                      .where(
                        (outing) =>
                            outing.startTime.isAfter(now) &&
                            outing.status.toUpperCase() != 'COMPLETED',
                      )
                      .toList()
                    ..sort((first, second) {
                      return first.startTime.compareTo(second.startTime);
                    });
              final unsettledCents = _calculateUnsettledCents(
                expensesBox.values,
              );

              return Center(
                child: ConstrainedBox(
                  constraints: const BoxConstraints(maxWidth: 880),
                  child: ListView(
                    padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
                    children: [
                      Text(
                        'Make time together',
                        style: Theme.of(context).textTheme.headlineMedium
                            ?.copyWith(fontWeight: FontWeight.w700),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Your family’s week at a glance',
                        style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 24),
                      _WeekOverview(outings: upcomingOutings),
                      const SizedBox(height: 24),
                      _BalanceSummary(unsettledCents: unsettledCents),
                      const SizedBox(height: 28),
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              'Upcoming outings',
                              style: Theme.of(context).textTheme.titleLarge
                                  ?.copyWith(fontWeight: FontWeight.w700),
                            ),
                          ),
                          Text(
                            '${upcomingOutings.length}',
                            style: Theme.of(context).textTheme.labelLarge,
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      if (upcomingOutings.isEmpty)
                        const _EmptyOutings()
                      else
                        for (final outing in upcomingOutings.take(5))
                          _OutingCard(outing: outing),
                    ],
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }

  static int _calculateUnsettledCents(Iterable<Expense> expenses) {
    final netBalances = <String, int>{};
    for (final expense in expenses) {
      final amount = _parseCents(expense.amount);
      netBalances[expense.payerId] =
          (netBalances[expense.payerId] ?? 0) + amount;
      for (final share in expense.shares.entries) {
        netBalances[share.key] =
            (netBalances[share.key] ?? 0) - _parseCents(share.value);
      }
    }
    return netBalances.values
        .where((balance) => balance > 0)
        .fold(0, (total, balance) => total + balance);
  }

  static int _parseCents(String amount) {
    final match = RegExp(r'^(\d+)(?:\.(\d{1,2}))?$').firstMatch(amount);
    if (match == null) {
      throw FormatException('Invalid cached monetary amount: $amount');
    }
    final wholeUnits = int.parse(match.group(1)!);
    final fractionalUnits = int.parse((match.group(2) ?? '').padRight(2, '0'));
    return wholeUnits * 100 + fractionalUnits;
  }
}

class _WeekOverview extends StatelessWidget {
  const _WeekOverview({required this.outings});

  final List<Outing> outings;

  @override
  Widget build(BuildContext context) {
    final today = DateTime.now();
    final localOutingDates = outings.map((outing) {
      final localStart = outing.startTime.toLocal();
      return DateTime(localStart.year, localStart.month, localStart.day);
    }).toSet();

    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Next 7 days',
              style: Theme.of(context).textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 16),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: List.generate(7, (index) {
                  final date = DateTime(
                    today.year,
                    today.month,
                    today.day + index,
                  );
                  final isToday = index == 0;
                  final hasOuting = localOutingDates.contains(date);
                  final colors = Theme.of(context).colorScheme;
                  return Container(
                    width: 52,
                    margin: EdgeInsets.only(right: index == 6 ? 0 : 8),
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    decoration: BoxDecoration(
                      color: isToday ? colors.primaryContainer : colors.surface,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: isToday ? colors.primary : colors.outlineVariant,
                      ),
                    ),
                    child: Column(
                      children: [
                        Text(
                          MaterialLocalizations.of(context)
                              .narrowWeekdays[date.weekday % 7],
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                        const SizedBox(height: 6),
                        Text(
                          '${date.day}',
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 6),
                        Icon(
                          hasOuting ? Icons.circle : Icons.circle_outlined,
                          size: 8,
                          color: hasOuting ? colors.primary : colors.surface,
                        ),
                      ],
                    ),
                  );
                }),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _BalanceSummary extends StatelessWidget {
  const _BalanceSummary({required this.unsettledCents});

  final int unsettledCents;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      color: colors.primaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Row(
          children: [
            Icon(Icons.account_balance_wallet_outlined, color: colors.primary),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Family balance',
                    style: Theme.of(context).textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.w700),
                  ),
                  Text(
                    'Unsettled from cached expenses',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
            Text(
              _formatMoney(unsettledCents),
              style: Theme.of(context).textTheme.titleLarge
                  ?.copyWith(fontWeight: FontWeight.w700),
            ),
          ],
        ),
      ),
    );
  }

  static String _formatMoney(int cents) {
    final units = cents ~/ 100;
    final fractionalUnits = (cents % 100).toString().padLeft(2, '0');
    return '\$$units.$fractionalUnits';
  }
}

class _OutingCard extends StatelessWidget {
  const _OutingCard({required this.outing});

  final Outing outing;

  @override
  Widget build(BuildContext context) {
    final localStart = outing.startTime.toLocal();
    final localEnd = outing.endTime.toLocal();
    final localizations = MaterialLocalizations.of(context);
    final date = localizations.formatMediumDate(localStart);
    final startTime = localizations.formatTimeOfDay(
      TimeOfDay.fromDateTime(localStart),
    );
    final endTime = localizations.formatTimeOfDay(
      TimeOfDay.fromDateTime(localEnd),
    );

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Theme.of(context).colorScheme.secondaryContainer,
          child: const Icon(Icons.event_available_outlined),
        ),
        title: Text(outing.title),
        subtitle: Text('$date · $startTime–$endTime'),
        isThreeLine: false,
      ),
    );
  }
}

class _EmptyOutings extends StatelessWidget {
  const _EmptyOutings();

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const Icon(Icons.event_note_outlined, size: 32),
            const SizedBox(height: 8),
            Text(
              'Nothing planned yet',
              style: Theme.of(context).textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 4),
            const Text('Your saved outings will show up here.'),
          ],
        ),
      ),
    );
  }
}
