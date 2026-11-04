package com.sweety.ai.domain

import com.sweety.ai.data.ActionPayload
import com.sweety.ai.data.PendingConfirmation
import com.sweety.ai.device.DeviceActionManager

class CommandDispatcher(
    private val deviceManager: DeviceActionManager,
    private val onConfirmationRequired: (PendingConfirmation) -> Unit
) {

    /**
     * Attempts fast-path local pattern matching (zero network latency, ~10ms execution)
     * Returns Pair(handled: Boolean, replyText: String?)
     */
    fun tryFastLocalIntent(prompt: String): Pair<Boolean, String?> {
        var clean = prompt.trim().lowercase()

        // 0. Strip wake word prefix if user said "Sweety, play a song" or similar
        val wakePrefixes = listOf(
            "hey sweety", "hi sweety", "sweety", "sweetie",
            "হাই সুইটি", "হ্যালো সুইটি", "হেই সুইটি", "সুইটি ", "এই সুইটি ", "সুইটি শোনো", "সুইটি আছো"
        )
        for (prefix in wakePrefixes) {
            if (clean == prefix || clean == prefix.trim()) {
                // Standalone wake word called! Fast & friendly greeting
                return Pair(true, "জি স্যার! শুনছি, বলুন?")
            }
            if (clean.startsWith(prefix)) {
                clean = clean.removePrefix(prefix).trim().removePrefix(",").trim()
            }
        }

        // If after stripping only wake word existed
        if (clean.isBlank() || clean == "সুইটি" || clean == "sweetie") {
            return Pair(true, "জি স্যার! শুনছি, বলুন?")
        }

        // 1. Flashlight on/off
        if (clean.contains("flashlight on") || clean.contains("flash on") ||
            clean.contains("ফ্ল্যাশলাইট অন") || clean.contains("আলো জ্বালাও") ||
            clean.contains("টর্চ অন") || clean.contains("flashlight chalu")
        ) {
            val res = deviceManager.toggleFlashlight(true)
            return Pair(true, res.second)
        }
        if (clean.contains("flashlight off") || clean.contains("flash off") ||
            clean.contains("ফ্ল্যাশলাইট অফ") || clean.contains("আলো নিভিয়ে") ||
            clean.contains("টর্চ বন্ধ") || clean.contains("টর্চ অফ")
        ) {
            val res = deviceManager.toggleFlashlight(false)
            return Pair(true, res.second)
        }

        // 2. Battery status
        if (clean.contains("battery") || clean.contains("ব্যাটারি") || clean.contains("চার্জ কত")) {
            val info = deviceManager.getBatteryInfo()
            return Pair(true, info)
        }

        // 3. Screen lock
        if (clean.contains("lock phone") || clean.contains("lock screen") ||
            clean.contains("ফোন লক") || clean.contains("লক করো") || clean.contains("স্ক্রিন লক")
        ) {
            val res = deviceManager.lockDevice()
            return Pair(true, res.second)
        }

        // 4. Wi-Fi control
        if (clean.contains("wifi on") || clean.contains("ওয়াইফাই অন") || clean.contains("ওয়াইফাই অন") || clean.contains("wifi chalu")) {
            val res = deviceManager.toggleWifi(true)
            return Pair(true, res.second)
        }
        if (clean.contains("wifi off") || clean.contains("ওয়াইফাই অফ") || clean.contains("ওয়াইফাই অফ") || clean.contains("ওয়াইফাই ক্লোজ") || clean.contains("wifi bandho")) {
            val res = deviceManager.toggleWifi(false)
            return Pair(true, res.second)
        }

        // 5. Bluetooth control
        if (clean.contains("bluetooth on") || clean.contains("ব্লুটুথ অন") || clean.contains("bluetooth chalu")) {
            val res = deviceManager.toggleBluetooth(true)
            return Pair(true, res.second)
        }
        if (clean.contains("bluetooth off") || clean.contains("ব্লুটুথ অফ") || clean.contains("bluetooth bandho")) {
            val res = deviceManager.toggleBluetooth(false)
            return Pair(true, res.second)
        }

        // 6. Mobile Data panel
        if (clean.contains("data on") || clean.contains("mobile data") || clean.contains("ডাটা অন") || clean.contains("মোবাইল ডাটা") || clean.contains("নেট অন")) {
            val res = deviceManager.openMobileDataSettings()
            return Pair(true, res.second)
        }

        // 7. Close Chat / Close App / Go Home
        if (clean.contains("close") || clean.contains("clos") || clean.contains("বন্ধ") ||
            clean.contains("ক্লোজ") || clean.contains("হোমে যাও") || clean.contains("go home") ||
            clean.contains("exit") || clean.contains("বাহির")
        ) {
            val res = deviceManager.closeCurrentApp()
            return Pair(true, res.second)
        }

        // 8. YouTube Quick Song/Music search (Plays directly into YouTube app, no Play Store!)
        if (clean.contains("play") || clean.contains("গান") || clean.contains("song") ||
            clean.contains("music") || clean.contains("মিউজিক") || clean.contains("ইউটিউব") || clean.contains("youtube")
        ) {
            var songQuery = clean
                .replace("play", "")
                .replace("on youtube", "")
                .replace("in youtube", "")
                .replace("youtube", "")
                .replace("ইউটিউবে", "")
                .replace("ইউটিউব", "")
                .replace("গান চালাও", "")
                .replace("গান বাজাও", "")
                .replace("গান শোনাও", "")
                .replace("চালিয়ে দাও", "")
                .replace("বাজিয়ে দাও", "")
                .replace("চালিয়ে দাও", "")
                .replace("শোনাও", "")
                .replace("গান", "")
                .replace("song", "")
                .replace("music", "")
                .trim()
            if (songQuery.isBlank()) songQuery = "Bangla new hit songs"
            val res = deviceManager.playYouTube(songQuery)
            return Pair(true, res.second)
        }

        return Pair(false, null)
    }

    /**
     * Executes allowlisted actions returned from Gemini backend safely
     */
    fun dispatchAction(
        action: ActionPayload,
        onComplete: (success: Boolean, feedback: String) -> Unit
    ) {
        when (action.action) {
            "open_app" -> {
                val appName = action.params["app_name"] as? String ?: "App"
                val pkg = action.params["package_name"] as? String
                val res = deviceManager.openApp(appName, pkg)
                onComplete(res.first, res.second)
            }

            "toggle_flashlight" -> {
                val state = action.params["state"] as? Boolean ?: true
                val res = deviceManager.toggleFlashlight(state)
                onComplete(res.first, res.second)
            }

            "make_call" -> {
                val contact = action.params["contact_name"] as? String ?: "Contact"
                val number = action.params["phone_number"] as? String

                // Consequential action: Confirm before dialing
                val confirm = PendingConfirmation(
                    action = "make_call",
                    description = "$contact কে কল করতে চান?",
                    onConfirm = {
                        val res = deviceManager.makeCall(contact, number)
                        onComplete(res.first, res.second)
                    },
                    onCancel = {
                        onComplete(false, "কল বাতিল করা হয়েছে।")
                    }
                )
                onConfirmationRequired(confirm)
            }

            "send_sms" -> {
                val contact = action.params["contact_name"] as? String ?: "Contact"
                val msg = action.params["message"] as? String ?: ""

                // Consequential action: Confirm message content before sending
                val confirm = PendingConfirmation(
                    action = "send_sms",
                    description = "$contact কে এই মেসেজটি পাঠাবেন: \"$msg\"?",
                    onConfirm = {
                        val res = deviceManager.sendSms(contact, msg)
                        onComplete(res.first, res.second)
                    },
                    onCancel = {
                        onComplete(false, "মেসেজ পাঠানো বাতিল করা হয়েছে।")
                    }
                )
                onConfirmationRequired(confirm)
            }

            "send_whatsapp" -> {
                val contact = action.params["contact_name"] as? String ?: "Contact"
                val msg = action.params["message"] as? String ?: ""
                val file = action.params["file_name"] as? String
                val res = deviceManager.shareToWhatsApp(contact, msg, file)
                onComplete(res.first, res.second)
            }

            "find_file" -> {
                val query = action.params["file_name"] as? String ?: ""
                val res = deviceManager.findFiles(query)
                if (res.first && res.second.isNotEmpty()) {
                    val filesList = res.second.joinToString(", ")
                    onComplete(true, "ফাইল পেয়েছি: $filesList")
                } else {
                    onComplete(false, "\"$query\" নামের কোনো ফাইল পাওয়া যায়নি।")
                }
            }

            "set_volume" -> {
                val level = (action.params["level"] as? Number)?.toInt() ?: 50
                val type = action.params["type"] as? String ?: "media"
                val res = deviceManager.setVolume(level, type)
                onComplete(res.first, res.second)
            }

            "set_alarm" -> {
                val time = action.params["time"] as? String ?: "07:00 AM"
                val label = action.params["label"] as? String
                val res = deviceManager.setAlarm(time, label)
                onComplete(res.first, res.second)
            }

            "lock_device" -> {
                val res = deviceManager.lockDevice()
                onComplete(res.first, res.second)
            }

            "play_youtube" -> {
                val query = action.params["query"] as? String ?: "Bangla songs"
                val res = deviceManager.playYouTube(query)
                onComplete(res.first, res.second)
            }

            "toggle_wifi" -> {
                val state = action.params["state"] as? Boolean ?: true
                val res = deviceManager.toggleWifi(state)
                onComplete(res.first, res.second)
            }

            "toggle_bluetooth" -> {
                val state = action.params["state"] as? Boolean ?: true
                val res = deviceManager.toggleBluetooth(state)
                onComplete(res.first, res.second)
            }

            "open_data_settings" -> {
                val res = deviceManager.openMobileDataSettings()
                onComplete(res.first, res.second)
            }

            "close_app" -> {
                val res = deviceManager.closeCurrentApp()
                onComplete(res.first, res.second)
            }

            "share_file" -> {
                val mime = action.params["mime_type"] as? String ?: "*/*"
                val res = deviceManager.shareFile(mime)
                onComplete(res.first, res.second)
            }

            else -> {
                // Unknown or unallowlisted action - drop safely
                onComplete(false, "অজানা বা অনিরাপদ কমান্ড ব্লক করা হয়েছে।")
            }
        }
    }
}

