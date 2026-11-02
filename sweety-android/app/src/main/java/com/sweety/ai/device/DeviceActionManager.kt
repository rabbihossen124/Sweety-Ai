package com.sweety.ai.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telephony.SmsManager
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
     * Makes a phone call directly or opens dialer
     */
    fun makeCall(contactName: String, phoneNumber: String? = null): Pair<Boolean, String> {
        var targetNumber = phoneNumber

        if (targetNumber.isNullOrBlank()) {
            targetNumber = findPhoneNumberByContact(contactName)
        }

        if (targetNumber.isNullOrBlank()) {
            return Pair(false, "$contactName এর ফোন নাম্বার কন্টাক্টসে পাওয়া যায়নি।")
        }

        return try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$targetNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            Pair(true, "$contactName কে কল দেওয়া হচ্ছে...")
        } catch (e: SecurityException) {
            // If CALL_PHONE permission not granted, fallback safely to ACTION_DIAL (never crash!)
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$targetNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            Pair(true, "$contactName এর নাম্বার ডায়ালারে ওপেন করা হয়েছে।")
        } catch (e: Exception) {
            Pair(false, "কল করা সম্ভব হয়নি: ${e.localizedMessage}")
        }
    }

    /**
     * Sends an SMS message to a contact
     */
    fun sendSms(contactName: String, message: String): Pair<Boolean, String> {
        val number = findPhoneNumberByContact(contactName)
        if (number.isNullOrBlank()) {
            return Pair(false, "$contactName এর নাম্বার পাওয়া যায়নি।")
        }

        return try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
            Pair(true, "$contactName কে মেসেজ পাঠিয়ে দেওয়া হয়েছে: \"$message\"")
        } catch (e: SecurityException) {
            // Fallback to SMS Intent
            val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$number")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
            Pair(true, "মেসেজ পাঠানোর স্ক্রিন ওপেন করা হয়েছে।")
        } catch (e: Exception) {
            Pair(false, "মেসেজ পাঠানো যায়নি: ${e.localizedMessage}")
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
     * Finds files in media store matching keyword
     */
    fun findFiles(query: String): Pair<Boolean, List<String>> {
        val results = mutableListOf<String>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns._ID
        )

        try {
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")
            val cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                while (it.moveToNext() && results.size < 5) {
                    val name = it.getString(nameIdx)
                    results.add(name)
                }
            }
            return Pair(true, results)
        } catch (e: Exception) {
            Log.e(TAG, "File search error: ${e.message}")
            return Pair(false, emptyList())
        }
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
     * Locks the device screen via Accessibility Service
     */
    fun lockDevice(): Pair<Boolean, String> {
        val success = SweetyAccessibilityService.lockDevice()
        return if (success) {
            Pair(true, "ফোন স্ক্রিন লক করা হয়েছে।")
        } else {
            Pair(false, "ফোন লক করতে অ্যাক্সেসিবিলিটি সেটিংসে Sweety AI চালু করুন।")
        }
    }

    /**
     * Helper to lookup contact number from name
     */
    private fun findPhoneNumberByContact(name: String): String? {
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val cursor = context.contentResolver.query(
                uri,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$name%"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx != -1) return it.getString(numIdx)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Contact lookup error: ${e.message}")
        }
        return null
    }

    companion object {
        private const val TAG = "DeviceActionManager"
    }
}

