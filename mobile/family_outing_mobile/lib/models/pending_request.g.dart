// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pending_request.dart';

// **************************************************************************
// TypeAdapterGenerator
// **************************************************************************

class PendingRequestAdapter extends TypeAdapter<PendingRequest> {
  @override
  final int typeId = 3;

  @override
  PendingRequest read(BinaryReader reader) {
    final numOfFields = reader.readByte();
    final fields = <int, dynamic>{
      for (int i = 0; i < numOfFields; i++) reader.readByte(): reader.read(),
    };
    return PendingRequest(
      idempotencyKey: fields[0] as String,
      method: fields[1] as String,
      path: fields[2] as String,
      bodyJson: fields[3] as String?,
      queryParametersJson: fields[4] as String,
      contentType: fields[5] as String?,
      queuedAt: fields[6] as DateTime,
      attempts: fields[7] as int,
      lastError: fields[8] as String?,
    );
  }

  @override
  void write(BinaryWriter writer, PendingRequest obj) {
    writer
      ..writeByte(9)
      ..writeByte(0)
      ..write(obj.idempotencyKey)
      ..writeByte(1)
      ..write(obj.method)
      ..writeByte(2)
      ..write(obj.path)
      ..writeByte(3)
      ..write(obj.bodyJson)
      ..writeByte(4)
      ..write(obj.queryParametersJson)
      ..writeByte(5)
      ..write(obj.contentType)
      ..writeByte(6)
      ..write(obj.queuedAt)
      ..writeByte(7)
      ..write(obj.attempts)
      ..writeByte(8)
      ..write(obj.lastError);
  }

  @override
  int get hashCode => typeId.hashCode;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PendingRequestAdapter &&
          runtimeType == other.runtimeType &&
          typeId == other.typeId;
}
