package com.sweety.ai.domain

import java.util.Calendar

object GreetingHelper {

    fun generateWelcomeGreeting(userName: String = "রাব্বি স্যার"): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 5..11 -> {
                "আসসালামু আলাইকুম $userName! শুভ সকাল! ঘুম কেমন হলো আপনার? আজ আপনাকে কীভাবে সাহায্য করতে পারি বলুন তো?"
            }
            in 12..16 -> {
                "আসসালামু আলাইকুম $userName! শুভ দুপুর! দুপুরের খাবার খেয়েছেন তো? বলুন আপনার জন্য কী কাজ করে দেব?"
            }
            in 17..19 -> {
                "আসসালামু আলাইকুম $userName! শুভ সন্ধ্যা! সারা দিন কেমন কাটল আপনার? চা বা কফি কি খাওয়া হয়েছে? আজ কী কাজ আছে বলুন।"
            }
            in 20..23 -> {
                "আসসালামু আলাইকুম $userName! শুভ রাত্রি! এত রাতেও কাজ করছেন? বেশি রাত জাগা কিন্তু ঠিক না! বলুন দ্রুত কী কাজ গুছিয়ে দেব?"
            }
            else -> {
                "আসসালামু আলাইকুম $userName! এত গভীর রাতেও আপনি জেগে আছেন? কোনো জরুরি সাহায্য লাগবে কি? আমি সবসময় আপনার সাথে আছি!"
            }
        }
    }
}

