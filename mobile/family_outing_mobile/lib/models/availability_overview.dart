import 'dart:convert';

class AvailabilityWindow {
  const AvailabilityWindow({
    this.id,
    this.userId,
    required this.startTime,
    required this.endTime,
    required this.tentative,
  });

  final String? id;
  final String? userId;
  final DateTime startTime;
  final DateTime endTime;
  final bool tentative;

  factory AvailabilityWindow.fromJson(Map<String, dynamic> json) {
    final id = json['id'];
    final userId = json['userId'];
    final startTime = json['startTime'];
    final endTime = json['endTime'];
    final tentative = json['tentative'];
    if ((id != null && id is! String) ||
        (userId != null && userId is! String) ||
        startTime is! String ||
        endTime is! String ||
        tentative is! bool) {
      throw const FormatException('Availability window has invalid fields.');
    }
    final parsedStart = DateTime.parse(startTime);
    final parsedEnd = DateTime.parse(endTime);
    if (!parsedEnd.isAfter(parsedStart)) {
      throw const FormatException('Availability window end must follow start.');
    }
    return AvailabilityWindow(
      id: id as String?,
      userId: userId as String?,
      startTime: parsedStart,
      endTime: parsedEnd,
      tentative: tentative,
    );
  }

  Map<String, Object> toRequestJson() => {
        'startTime': startTime.toUtc().toIso8601String(),
        'endTime': endTime.toUtc().toIso8601String(),
        'tentative': tentative,
      };
}

class AttendeeAvailability {
  AttendeeAvailability({
    required this.userId,
    required List<AvailabilityWindow> windows,
  }) : windows = List.unmodifiable(windows);

  final String userId;
  final List<AvailabilityWindow> windows;

  factory AttendeeAvailability.fromJson(Map<String, dynamic> json) {
    final userId = json['userId'];
    final windowsJson = json['windows'];
    if (userId is! String || windowsJson is! List) {
      throw const FormatException('Attendee availability has invalid fields.');
    }
    return AttendeeAvailability(
      userId: userId,
      windows: windowsJson
          .map(
            (window) => AvailabilityWindow.fromJson(
              _asJsonObject(window, 'availability window'),
            ),
          )
          .toList(),
    );
  }
}

class CommonFreeSlot {
  const CommonFreeSlot({
    required this.startTime,
    required this.endTime,
    required this.tentative,
  });

  final DateTime startTime;
  final DateTime endTime;
  final bool tentative;

  factory CommonFreeSlot.fromJson(Map<String, dynamic> json) {
    final startTime = json['startTime'];
    final endTime = json['endTime'];
    final tentative = json['tentative'];
    if (startTime is! String || endTime is! String || tentative is! bool) {
      throw const FormatException('Common free slot has invalid fields.');
    }
    final parsedStart = DateTime.parse(startTime);
    final parsedEnd = DateTime.parse(endTime);
    if (!parsedEnd.isAfter(parsedStart)) {
      throw const FormatException('Common free slot end must follow start.');
    }
    return CommonFreeSlot(
      startTime: parsedStart,
      endTime: parsedEnd,
      tentative: tentative,
    );
  }
}

class AvailabilityOverview {
  AvailabilityOverview({
    required this.outingId,
    required List<AvailabilityWindow> myWindows,
    required List<AttendeeAvailability> attendees,
    required List<CommonFreeSlot> commonFreeSlots,
  })  : myWindows = List.unmodifiable(myWindows),
        attendees = List.unmodifiable(attendees),
        commonFreeSlots = List.unmodifiable(commonFreeSlots);

  final String outingId;
  final List<AvailabilityWindow> myWindows;
  final List<AttendeeAvailability> attendees;
  final List<CommonFreeSlot> commonFreeSlots;

  factory AvailabilityOverview.fromJson(String json) {
    final decoded = jsonDecode(json);
    if (decoded is! Map<String, dynamic>) {
      throw const FormatException('Availability overview must be an object.');
    }
    final outingId = decoded['outingId'];
    final myWindowsJson = decoded['myWindows'];
    final attendeesJson = decoded['attendees'];
    final commonSlotsJson = decoded['commonFreeSlots'];
    if (outingId is! String ||
        myWindowsJson is! List ||
        attendeesJson is! List ||
        commonSlotsJson is! List) {
      throw const FormatException('Availability overview has invalid fields.');
    }
    return AvailabilityOverview(
      outingId: outingId,
      myWindows: myWindowsJson
          .map(
            (window) => AvailabilityWindow.fromJson(
              _asJsonObject(window, 'availability window'),
            ),
          )
          .toList(),
      attendees: attendeesJson
          .map(
            (attendee) => AttendeeAvailability.fromJson(
              _asJsonObject(attendee, 'attendee availability'),
            ),
          )
          .toList(),
      commonFreeSlots: commonSlotsJson
          .map(
            (slot) => CommonFreeSlot.fromJson(
              _asJsonObject(slot, 'common free slot'),
            ),
          )
          .toList(),
    );
  }
}

Map<String, dynamic> _asJsonObject(Object? value, String fieldName) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  throw FormatException('$fieldName must be a JSON object.');
}
