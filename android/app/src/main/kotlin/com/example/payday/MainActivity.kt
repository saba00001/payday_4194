package com.example.payday

import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * MainActivity — Flutter host activity and MethodChannel bridge.
 *
 * MethodChannel: "com.example.payday/alarm"
 *
 * All handlers:
 * - Validate arguments before use (missing args use safe defaults).
 * - Catch all exceptions and return result.error() or safe fallback.
 * - Never crash on permission denial.
 * - Return accurate success/failure state.
 */
class MainActivity : FlutterFragmentActivity() {

    private val CHANNEL = "com.example.payday/alarm"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                try {
                    when (call.method) {

                        "scheduleAlarm" -> {
                            // Validate arguments — use safe defaults if missing.
                            val hour = call.argument<Int>("hour")?.coerceIn(0, 23) ?: 9
                            val minute = call.argument<Int>("minute")?.coerceIn(0, 59) ?: 0
                            AlarmScheduler.scheduleAlarm(this, hour, minute)
                            result.success(true)
                        }

                        "cancelAlarm" -> {
                            AlarmScheduler.cancelAlarm(this)
                            result.success(true)
                        }

                        "scheduleTestAlarm" -> {
                            AlarmScheduler.scheduleImmediateTest(this)
                            result.success(true)
                        }

                        "canScheduleExactAlarms" -> {
                            val canSchedule = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager
                                    alarmManager?.canScheduleExactAlarms() ?: false
                                } else {
                                    // Below Android 12: exact alarms always available.
                                    true
                                }
                            } catch (_: Exception) {
                                false
                            }
                            result.success(canSchedule)
                        }

                        "canUseFullScreenIntent" -> {
                            val canUse = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    val nm = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
                                    nm?.canUseFullScreenIntent() ?: false
                                } else {
                                    // Below Android 14: full-screen intent always available.
                                    true
                                }
                            } catch (_: Exception) {
                                false
                            }
                            result.success(canUse)
                        }

                        "openExactAlarmSettings" -> {
                            val opened = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                    ).apply {
                                        data = android.net.Uri.parse("package:$packageName")
                                    }
                                    startActivity(intent)
                                    true
                                } else {
                                    // Not needed below Android 12.
                                    false
                                }
                            } catch (_: Exception) {
                                false
                            }
                            result.success(opened)
                        }

                        "openFullScreenIntentSettings" -> {
                            val opened = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
                                    ).apply {
                                        data = android.net.Uri.parse("package:$packageName")
                                    }
                                    startActivity(intent)
                                    true
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val intent = Intent(
                                        android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    ).apply {
                                        putExtra(
                                            android.provider.Settings.EXTRA_APP_PACKAGE,
                                            packageName
                                        )
                                    }
                                    startActivity(intent)
                                    true
                                } else {
                                    false
                                }
                            } catch (_: Exception) {
                                false
                            }
                            result.success(opened)
                        }

                        "openNotificationSettings" -> {
                            val opened = try {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    Intent(
                                        android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    ).apply {
                                        putExtra(
                                            android.provider.Settings.EXTRA_APP_PACKAGE,
                                            packageName
                                        )
                                    }
                                } else {
                                    Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = android.net.Uri.parse("package:$packageName")
                                    }
                                }
                                startActivity(intent)
                                true
                            } catch (_: Exception) {
                                false
                            }
                            result.success(opened)
                        }

                        "getReminderActionResult" -> {
                            // Read and clear the pending action result stored by
                            // WorkActionReceiver or ReminderActivity.
                            // Returns "worked", "not_worked", or null.
                            val actionResult = try {
                                val prefs = getSharedPreferences(
                                    "FlutterSharedPreferences", MODE_PRIVATE
                                )
                                val value = prefs.getString(
                                    "flutter.payday_reminder_action_result", null
                                )
                                if (value != null) {
                                    // Clear after reading — consume once.
                                    prefs.edit()
                                        .remove("flutter.payday_reminder_action_result")
                                        .apply()
                                }
                                value
                            } catch (_: Exception) {
                                null
                            }
                            result.success(actionResult)
                        }

                        else -> result.notImplemented()
                    }
                } catch (e: Exception) {
                    // Top-level catch: no MethodChannel call should ever crash the app.
                    result.error("UNEXPECTED_ERROR", e.message ?: "Unknown error", null)
                }
            }
    }
}
