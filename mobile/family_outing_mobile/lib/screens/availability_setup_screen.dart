import 'package:dio/dio.dart';
import 'package:flutter/material.dart';

import '../models/availability_overview.dart';
import '../network/calendar_availability_repository.dart';

class AvailabilitySetupScreen extends StatefulWidget {
  const AvailabilitySetupScreen({
    required this.outingId,
    required this.outingTitle,
    required this.repository,
    super.key,
  });

  final String outingId;
  final String outingTitle;
  final CalendarAvailabilityRepository repository;

  @override
  State<AvailabilitySetupScreen> createState() =>
      _AvailabilitySetupScreenState();
}

class _AvailabilitySetupScreenState extends State<AvailabilitySetupScreen> {
  AvailabilityOverview? _overview;
  List<AvailabilityWindow> _draftWindows = [];
  bool _loading = true;
  bool _saving = false;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.outingTitle)),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _loadError != null
              ? _ErrorPanel(error: _loadError!, onRetry: _load)
              : _buildAvailability(context),
    );
  }

  Widget _buildAvailability(BuildContext context) {
    final overview = _overview!;
    final colors = Theme.of(context).colorScheme;
    final submittedCount =
        overview.attendees.where((attendee) => attendee.windows.isNotEmpty).length;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
      children: [
        Text(
          'When are you available?',
          style: Theme.of(context)
              .textTheme
              .headlineSmall
              ?.copyWith(fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 6),
        Text(
          'Add the times that work for you. Your family’s shared free times '
          'update when everyone has added availability.',
          style: Theme.of(context).textTheme.bodyMedium,
        ),
        const SizedBox(height: 24),
        Row(
          children: [
            Expanded(
              child: Text(
                'Your time windows',
                style: Theme.of(context)
                    .textTheme
                    .titleLarge
                    ?.copyWith(fontWeight: FontWeight.w700),
              ),
            ),
            TextButton.icon(
              onPressed: _saving ? null : _addWindow,
              icon: const Icon(Icons.add),
              label: const Text('Add time'),
            ),
          ],
        ),
        if (_draftWindows.isEmpty)
          const Card(
            child: Padding(
              padding: EdgeInsets.all(18),
              child: Text('No availability added yet.'),
            ),
          )
        else
          for (var index = 0; index < _draftWindows.length; index++)
            Card(
              child: ListTile(
                leading: Icon(
                  _draftWindows[index].tentative
                      ? Icons.help_outline
                      : Icons.check_circle_outline,
                ),
                title: Text(
                  _formatRange(
                    context,
                    _draftWindows[index].startTime,
                    _draftWindows[index].endTime,
                  ),
                ),
                subtitle: Text(
                  _draftWindows[index].tentative ? 'Tentative' : 'Available',
                ),
                trailing: IconButton(
                  tooltip: 'Remove time window',
                  onPressed: _saving ? null : () => _removeWindow(index),
                  icon: const Icon(Icons.close),
                ),
              ),
            ),
        const SizedBox(height: 12),
        FilledButton.icon(
          onPressed: _saving || !_hasChanges ? null : _save,
          icon: _saving
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.cloud_upload_outlined),
          label: Text(_saving ? 'Saving…' : 'Save availability'),
        ),
        const SizedBox(height: 32),
        Text(
          'Group overlap',
          style: Theme.of(context)
              .textTheme
              .titleLarge
              ?.copyWith(fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 4),
        Text(
          '$submittedCount of ${overview.attendees.length} attendees have '
          'shared times. Shared slots appear when all attendees have '
          'availability on record.',
          style: Theme.of(context).textTheme.bodySmall,
        ),
        const SizedBox(height: 12),
        if (overview.commonFreeSlots.isEmpty)
          const Card(
            child: Padding(
              padding: EdgeInsets.all(18),
              child: Text(
                'No shared free times yet. Add availability for each attendee '
                'to find an overlap.',
              ),
            ),
          )
        else
          for (final slot in overview.commonFreeSlots)
            Card(
              color: slot.tentative ? colors.tertiaryContainer : null,
              child: ListTile(
                leading: Icon(
                  slot.tentative
                      ? Icons.pending_outlined
                      : Icons.groups_outlined,
                ),
                title: Text(_formatRange(context, slot.startTime, slot.endTime)),
                subtitle: Text(
                  slot.tentative
                      ? 'Shared time includes tentative availability'
                      : 'All attendees are available',
                ),
              ),
            ),
      ],
    );
  }

  bool get _hasChanges {
    final original = _overview?.myWindows ?? const <AvailabilityWindow>[];
    if (original.length != _draftWindows.length) {
      return true;
    }
    for (var index = 0; index < original.length; index++) {
      final first = original[index];
      final second = _draftWindows[index];
      if (first.startTime != second.startTime ||
          first.endTime != second.endTime ||
          first.tentative != second.tentative) {
        return true;
      }
    }
    return false;
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _loadError = null;
    });
    try {
      final overview =
          await widget.repository.getOverview(widget.outingId);
      if (!mounted) {
        return;
      }
      setState(() {
        _overview = overview;
        _draftWindows = List.of(overview.myWindows);
        _loading = false;
      });
    } on DioException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _loadError = error;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _loadError = error;
        _loading = false;
      });
    }
  }

  Future<void> _addWindow() async {
    final now = DateTime.now();
    final selectedDate = await showDatePicker(
      context: context,
      initialDate: DateTime(now.year, now.month, now.day),
      firstDate: DateTime(now.year, now.month, now.day),
      lastDate: DateTime(now.year + 2, now.month, now.day),
    );
    if (selectedDate == null || !mounted) {
      return;
    }
    final startTime = await showTimePicker(
      context: context,
      initialTime: const TimeOfDay(hour: 9, minute: 0),
      helpText: 'Choose when you become available',
    );
    if (startTime == null || !mounted) {
      return;
    }
    final endTime = await showTimePicker(
      context: context,
      initialTime: TimeOfDay(
        hour: (startTime.hour + 1) % 24,
        minute: startTime.minute,
      ),
      helpText: 'Choose when your availability ends',
    );
    if (endTime == null || !mounted) {
      return;
    }

    final start = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      startTime.hour,
      startTime.minute,
    );
    var end = DateTime(
      selectedDate.year,
      selectedDate.month,
      selectedDate.day,
      endTime.hour,
      endTime.minute,
    );
    if (!end.isAfter(start)) {
      end = end.add(const Duration(days: 1));
    }

    final tentative = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Availability confidence'),
        content: const Text(
          'Should your family treat this time as tentative?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('No, confirmed'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Yes, tentative'),
          ),
        ],
      ),
    );
    if (tentative == null || !mounted) {
      return;
    }
    setState(() {
      _draftWindows.add(
        AvailabilityWindow(
          startTime: start,
          endTime: end,
          tentative: tentative,
        ),
      );
      _draftWindows.sort(
        (first, second) => first.startTime.compareTo(second.startTime),
      );
    });
  }

  void _removeWindow(int index) {
    setState(() => _draftWindows.removeAt(index));
  }

  Future<void> _save() async {
    setState(() => _saving = true);
    try {
      final result = await widget.repository.replaceMyWindows(
        widget.outingId,
        _draftWindows,
      );
      if (!mounted) {
        return;
      }
      if (result.queued) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Saved on this device and queued to sync.'),
          ),
        );
      } else {
        final overview = result.overview!;
        setState(() {
          _overview = overview;
          _draftWindows = List.of(overview.myWindows);
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Availability updated.')),
        );
      }
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Could not save availability: $error')),
      );
    } finally {
      if (mounted) {
        setState(() => _saving = false);
      }
    }
  }

  String _formatRange(BuildContext context, DateTime startTime, DateTime endTime) {
    final localizations = MaterialLocalizations.of(context);
    final start = startTime.toLocal();
    final end = endTime.toLocal();
    final date = localizations.formatMediumDate(start);
    final formattedStartTime = localizations.formatTimeOfDay(
      TimeOfDay.fromDateTime(start),
    );
    final endDate = DateTime(start.year, start.month, start.day) ==
            DateTime(end.year, end.month, end.day)
        ? ''
        : '${localizations.formatMediumDate(end)} ';
    final formattedEndTime =
        localizations.formatTimeOfDay(TimeOfDay.fromDateTime(end));
    return '$date · $formattedStartTime–$endDate$formattedEndTime';
  }
}

class _ErrorPanel extends StatelessWidget {
  const _ErrorPanel({required this.error, required this.onRetry});

  final Object error;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_outlined, size: 40),
            const SizedBox(height: 12),
            const Text(
              'Could not load availability',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            Text(
              '$error',
              textAlign: TextAlign.center,
              maxLines: 3,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Try again'),
            ),
          ],
        ),
      ),
    );
  }
}
