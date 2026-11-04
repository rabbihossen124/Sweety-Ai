package com.sweety.ai.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.util.Log

class DeviceActionManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var cameraId: String? = null

    init {
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera for torch: ${e.message}")
        }
    }

    /**
     * Toggles device flashlight on or off
     */
    fun toggleFlashlight(state: Boolean): Pair<Boolean, String> {
        val cid = cameraId ?: return Pair(false, "ফ্ল্যাশলাইট ক্যামেরা খুঁজে পাওয়া যায়নি।")
        return try {
            cameraManager?.setTorchMode(cid, state)
            val msg = if (state) "ফ্ল্যাশলাইট অন করা হয়েছে।" else "ফ্ল্যাশলাইট অফ করা হয়েছে।"
            Pair(true, msg)
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight error: ${e.message}")
            Pair(false, "ফ্ল্যাশলাইট চালানো যায়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Finds and opens an installed app by name (e.g., 'YouTube', 'WhatsApp', 'Facebook')
     */
    fun openApp(appName: String, packageName: String? = null): Pair<Boolean, String> {
        val pm = context.packageManager

        // 1. If explicit package name provided and installed
        if (!packageName.isNullOrBlank()) {
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return Pair(true, "$appName ওপেন করছি।")
            }
        }

        // 2. Search installed packages by app label
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val query = appName.trim().lowercase()

            for (appInfo in installedApps) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label.contains(query) || query.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return Pair(true, "${pm.getApplicationLabel(appInfo)} ওপেন করছি।")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "App launch error: ${e.message}")
        }

        // Fallback: search in Google Play Store
        return try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            Pair(true, "$appName ফোনে পাওয়া যায়নি, প্লে-স্টোরে খুঁজছি।")
        } catch (e: Exception) {
            Pair(false, "$appName অ্যাপটি ফোনে খুঁজে পাওয়া যায়নি।")
        }
    }

    /**
     * Opens phone dialer (zero dangerous permissions needed)
     */
    fun makeCall(contactName: String, phoneNumber: String? = null): Pair<Boolean, String> {
        return try {
            val dialIntent = if (!phoneNumber.isNullOrBlank()) {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
            } else {
                Intent(Intent.ACTION_DIAL)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            Pair(true, "$contactName এর জন্য ডায়ালার ওপেন করা হয়েছে।")
        } catch (e: Exception) {
            Pair(false, "ডায়াল করা সম্ভব হয়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Opens SMS app pre-filled (zero dangerous permissions needed)
     */
    fun sendSms(contactName: String, message: String): Pair<Boolean, String> {
        return try {
            val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
            Pair(true, "$contactName এর জন্য মেসেজ প্রস্তুত করা হয়েছে।")
        } catch (e: Exception) {
            Pair(false, "মেসেজ ওপেন করা যায়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Shares a message or file to WhatsApp
     */
    fun shareToWhatsApp(contactName: String, message: String, fileUri: String? = null): Pair<Boolean, String> {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                `package` = "com.whatsapp"
                if (!fileUri.isNullOrBlank()) {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(fileUri))
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
            Pair(true, "WhatsApp এ $contactName এর জন্য মেসেজ প্রস্তুত করা হয়েছে।")
        } catch (e: Exception) {
            Pair(false, "WhatsApp পাওয়া যায়নি অথবা মেসেজ ওপেন করা যায়নি।")
        }
    }

    /**
     * Adjusts system volume
     */
    fun setVolume(levelPercent: Int, streamType: String = "media"): Pair<Boolean, String> {
        val am = audioManager ?: return Pair(false, "অডিও ম্যানেজার পাওয়া যায়নি।")
        val stream = when (streamType.lowercase()) {
            "ring", "ringer" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            else -> AudioManager.STREAM_MUSIC
        }

        val max = am.getStreamMaxVolume(stream)
        val target = ((levelPercent.coerceIn(0, 100) / 100.0) * max).toInt()

        return try {
            am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
            Pair(true, "ভলিউম $levelPercent% এ সেট করা হয়েছে।")
        } catch (e: Exception) {
            Pair(false, "ভলিউম পরিবর্তন করা যায়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Sets an alarm or timer
     */
    fun setAlarm(timeString: String, label: String? = null): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, label ?: "Sweety AI Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "অ্যালার্ম ঘড়িতে সেট করা হচ্ছে: $timeString")
        } catch (e: Exception) {
            Pair(false, "অ্যালার্ম সেট করা যায়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Safe fallback for file queries without sensitive storage permissions
     */
    fun findFiles(query: String): Pair<Boolean, List<String>> {
        return Pair(false, emptyList())
    }

    /**
     * Gets device battery percentage
     */
    fun getBatteryInfo(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (level >= 0) "ফোনের ব্যাটারি এখন $level% চার্জ আছে বস।" else "ব্যাটারি ইনফো পাওয়া যায়নি।"
        } catch (e: Exception) {
            "ব্যাটারি চেক করা যায়নি।"
        }
    }

    /**
     * Screen lock
     */
    fun lockDevice(): Pair<Boolean, String> {
        return Pair(true, "ফোন লক করতে পাওয়ার বাটনে প্রেস করুন।")
    }

    /**
     * Searches and plays a song or video directly on YouTube
     */
    fun playYouTube(query: String): Pair<Boolean, String> {
        return try {
            val cleanQuery = query.trim()
            val uri = Uri.parse("https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(cleanQuery, "UTF-8"))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                Pair(true, "ইউটিউবে \"$cleanQuery\" প্লে করছি।")
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Pair(true, "ব্রাউজারে ইউটিউবে \"$cleanQuery\" প্লে করছি।")
            }
        } catch (e: Exception) {
            Pair(false, "ইউটিউব ওপেন করা যায়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Closes the active app and returns to Android home screen
     */
    fun closeCurrentApp(): Pair<Boolean, String> {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(homeIntent)
            Pair(true, "হোম স্ক্রিনে ফিরে যাচ্ছি এবং অ্যাপ ক্লোজ করছি।")
        } catch (e: Exception) {
            Pair(false, "হোমে যাওয়া সম্ভব হয়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Controls or launches Wi-Fi control panel
     */
    fun toggleWifi(state: Boolean): Pair<Boolean, String> {
        val status = if (state) "অন" else "অফ"
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val panelIntent = Intent(android.provider.Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                Pair(true, "ওয়াইফাই $status করার প্যানেল ওপেন করেছি।")
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                @Suppress("DEPRECATION")
                wifiManager?.isWifiEnabled = state
                Pair(true, "ওয়াইফাই $status করা হয়েছে।")
            }
        } catch (e: Exception) {
            try {
                val settingsIntent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                Pair(true, "ওয়াইফাই সেটিংস ওপেন করেছি।")
            } catch (ex: Exception) {
                Pair(false, "ওয়াইফাই পরিবর্তন করা যায়নি: ${ex.localizedMessage}")
            }
        }
    }

    /**
     * Controls or opens Bluetooth control settings
     */
    fun toggleBluetooth(state: Boolean): Pair<Boolean, String> {
        val status = if (state) "অন" else "অফ"
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "ব্লুটুথ $status করার সেটিংস ওপেন করেছি।")
            } else {
                @Suppress("DEPRECATION")
                val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                @Suppress("DEPRECATION")
                if (state) adapter?.enable() else adapter?.disable()
                Pair(true, "ব্লুটুথ $status করা হয়েছে।")
            }
        } catch (e: Exception) {
            Pair(false, "ব্লুটুথ কন্ট্রোল করা যায়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Opens Cellular / Mobile data settings panel
     */
    fun openMobileDataSettings(): Pair<Boolean, String> {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val intent = Intent(android.provider.Settings.Panel.ACTION_INTERNET).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "মোবাইল ডাটা ও ইন্টারনেট প্যানেল ওপেন করেছি।")
            } else {
                val intent = Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "মোবাইল ডাটা সেটিংস ওপেন করেছি।")
            }
        } catch (e: Exception) {
            Pair(false, "ডাটা সেটিংস ওপেন করা যায়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Opens storage file manager / file picker to share files, audio, video
     */
    fun shareFile(mimeType: String = "*/*"): Pair<Boolean, String> {
        return try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = mimeType
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "ফাইল নির্বাচন করার জন্য স্টোরেজ ওপেন করেছি।")
        } catch (e: Exception) {
            Pair(false, "ফাইল স্টোরেজ ওপেন করা যায়নি: ${e.localizedMessage}")
        }
    }

    companion object {
        private const val TAG = "DeviceActionManager"
    }
}
