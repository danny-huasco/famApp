// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user_availability.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class UserAvailabilityAdapter extends TypeAdapter<UserAvailability> {
  @override
  final int typeId = 1;

  @override
  UserAvailability read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return UserAvailability(
      id: fields[0] as String,
      userId: fields[1] as String,
      outingId: fields[2] as String?,
      startTime: fields[3] as DateTime,
      endTime: fields[4] as DateTime,
      isTentative: fields[5] as bool,
    );
  }

  @override
  void write(BinaryWriter writer, UserAvailability obj) {
    writer
      ..writeByte(6)
      ..writeByte(0)
      ..write(obj.id)
      ..writeByte(1)
      ..write(obj.userId)
      ..writeByte(2)
      ..write(obj.outingId)
      ..writeByte(3)
      ..write(obj.startTime)
      ..writeByte(4)
      ..write(obj.endTime)
      ..writeByte(5)
      ..write(obj.isTentative);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is UserAvailabilityAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
