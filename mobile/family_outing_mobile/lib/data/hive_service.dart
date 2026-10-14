import 'package:hive_flutter/hive_flutter.dart';

import '../models/expense.dart';
import '../models/outing.dart';
import '../models/user_availability.dart';
import '../models/pending_request.dart';

const String outingsBoxName = 'outings';
const String userAvailabilityBoxName = 'user_availability';
const String expensesBoxName = 'expenses';
const String pendingRequestsBoxName = 'pending_requests';

void registerHiveAdapters() {
  if (!Hive.isAdapterRegistered(0)) {
    Hive.registerAdapter(OutingAdapter());
  }
  if (!Hive.isAdapterRegistered(1)) {
    Hive.registerAdapter(UserAvailabilityAdapter());
  }
  if (!Hive.isAdapterRegistered(2)) {
    Hive.registerAdapter(ExpenseAdapter());
  }
  if (!Hive.isAdapterRegistered(3)) {
    Hive.registerAdapter(PendingRequestAdapter());
  }
}

class HiveService {
  static Future<void> initialize() async {
    await Hive.initFlutter();
    registerHiveAdapters();
    await Hive.openBox<Outing>(outingsBoxName);
    await Hive.openBox<UserAvailability>(userAvailabilityBoxName);
    await Hive.openBox<Expense>(expensesBoxName);
    await Hive.openBox<PendingRequest>(pendingRequestsBoxName);
  }

  static Box<Outing> get outings => Hive.box<Outing>(outingsBoxName);

  static Box<UserAvailability> get availabilities =>
      Hive.box<UserAvailability>(userAvailabilityBoxName);

  static Box<Expense> get expenses => Hive.box<Expense>(expensesBoxName);

  static Box<PendingRequest> get pendingRequests =>
      Hive.box<PendingRequest>(pendingRequestsBoxName);
}
