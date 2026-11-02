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
        val clean = prompt.trim().lowercase()

        // 0. Wake Word & Friendly Greeting
        if (clean == "hey sweety" || clean == "hi sweety" || clean == "sweety" ||
            clean == "হাই সুইটি" || clean == "হ্যালো সুইটি" || clean == "হেই সুইটি" ||
            clean.startsWith("hey sweety") || clean.startsWith("hi sweety")
        ) {
            val greeting = GreetingHelper.generateWelcomeGreeting("রাব্বি স্যার")
            return Pair(true, greeting)
        }

        // 1. Flashlight on/off
        if (clean.contains("flashlight on") || clean.contains("flash on") ||
            clean.contains("ফ্ল্যাশলাইট অন") || clean.contains("আলো জ্বালাও") ||
            clean.contains("टॉर्च ऑन") || clean.contains("flashlight chalu")
        ) {
            val res = deviceManager.toggleFlashlight(true)
            return Pair(true, res.second)
        }
        if (clean.contains("flashlight off") || clean.contains("flash off") ||
            clean.contains("ফ্ল্যাশলাইট অফ") || clean.contains("আলো নিভিয়ে") ||
            clean.contains("টর্চ বন্ধ") || clean.contains("टॉर्च बंद")
        ) {
            val res = deviceManager.toggleFlashlight(false)
            return Pair(true, res.second)
        }

        // 2. Battery status
        if (clean.contains("battery") || clean.contains("ব্যাটারি") || clean.contains("চার্জ কত") || clean.contains("बैटरी")) {
            val info = deviceManager.getBatteryInfo()
            return Pair(true, info)
        }

        // 3. Screen lock
        if (clean.contains("lock phone") || clean.contains("lock screen") ||
            clean.contains("ফোন লক করো") || clean.contains("ফোনটা লক করো") ||
            clean.contains("स्क्रीन लॉक करो")
        ) {
            val res = deviceManager.lockDevice()
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

            else -> {
                // Unknown or unallowlisted action - drop safely
                onComplete(false, "অজানা বা অনিরাপদ কমান্ড ব্লক করা হয়েছে।")
            }
        }
    }
}

