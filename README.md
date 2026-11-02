# 🤖 Sweety AI — Professional Futuristic Android Assistant

**Sweety AI** is a state-of-the-art voice-driven personal assistant for Android devices, built with **Kotlin + Jetpack Compose** and a secure **Node.js + Express** backend powered by **Google Gemini API** (`gemini-3.8-flash`) and **MongoDB**.

---

## 🌟 Key Features

1. **🎨 Premium Sci-Fi Dark UI**:
   - Deep void and dark navy background with electric cyan and neon blue glow.
   - Smooth animated audio wave visualizer that reacts live to your speech volume.
   - Concentric pulsating microphone button with haptic feedback.
   - Translucent frosted-glass chat bubbles and executed action badges.

2. **🗣️ Multilingual Voice Engine (Bangla, Hindi, English)**:
   - Dynamic language switching: speaks and understands in Bengali (বাংলা), Hindi (हिन्दी), and English.
   - Auto script detection for speech output (TTS) with natural cadence.

3. **🔄 Continuous Conversational Loop (No Need to Repeat "Hey Sweety")**:
   - When Sweety finishes an action, the microphone **automatically re-arms for follow-up commands**.
   - Handles chained tasks seamlessly:
     - Turn 1: *"Sweety, open WhatsApp"* ➔ Opens WhatsApp.
     - Turn 2: *"Akta file khujo"* ➔ Searches file.
     - Turn 3: *"File ta Nazneen ke pathao"* ➔ Remembers the file and shares it to Nazneen!
   - 7-second countdown indicator with auto-standby on silence.

4. **👏 2-Time Clap Wake-up & Lock Screen Overlay**:
   - Detects double sharp hand claps via a lightweight background audio service.
   - Wakes up the screen (`turnScreenOn`, `showWhenLocked`) and begins voice listening even when the phone is locked!

5. **📱 Complete Phone Hardware & Intent Control**:
   - **Flashlight**: Instant ON / OFF via `CameraManager`.
   - **App Launching**: Opens any installed app by name via `PackageManager`.
   - **Voice Calling**: Direct calls via `ACTION_CALL` / `ACTION_DIAL`.
   - **SMS & Messaging**: Composes and sends SMS via `SmsManager`.
   - **WhatsApp Sharing**: Shares messages or documents to WhatsApp contacts.
   - **Volume Control**: Adjusts media, ringer, or alarm volume.
   - **Alarms**: Sets alarms via `AlarmClock` API.
   - **Battery Check**: Reads live battery level.
   - **Screen Lock**: Locks screen natively via `SweetyAccessibilityService`.

6. **⚡ Zero-Latency Local Intent Matcher**:
   - Instant 10ms execution for local commands (flashlight, volume, battery, lock) without waiting for cloud round-trips!

7. **🛡️ Safe Allowlisted Command Router**:
   - Zero arbitrary shell code.
   - Consequential action confirmation modal (e.g., verifying before calling or sending SMS).

---

## 📂 Project Architecture

```
AI asisten/
├── sweety-backend/               # Node.js + Express + Gemini + MongoDB
│   ├── src/
│   │   ├── ai/gemini.js          # Google GenAI SDK (gemini-3.8-flash) & Tools
│   │   ├── commands/tools.js     # Structured Function Calling definitions
│   │   ├── config/db.js          # MongoDB connection with in-memory fallback
│   │   ├── controllers/          # Chat, Memory & Session controller
│   │   ├── models/               # Conversation & Memory Mongoose schemas
│   │   ├── routes/               # API endpoints (/chat, /memories, /health)
│   │   └── server.js             # Express entry point
│   ├── .env.example
│   ├── package.json
│   └── test-chat.js
│
└── sweety-android/               # Native Android (Kotlin + Jetpack Compose)
    ├── app/src/main/
    │   ├── AndroidManifest.xml   # Permissions & foreground services
    │   ├── res/                  # Colors, themes, accessibility config
    │   └── java/com/sweety/ai/
    │       ├── data/             # ApiClient, ChatMessage, ActionPayload
    │       ├── device/           # DeviceActionManager & AccessibilityService
    │       ├── domain/           # CommandDispatcher & Local Fast-Intent Matcher
    │       ├── presentation/     # Jetpack Compose UI, Wave Visualizer, Mic Button
    │       ├── sensor/           # ClapDetectorService (2-time clap wake-up)
    │       ├── voice/            # ContinuousSpeechRecognizer & MultilingualTTS
    │       ├── MainActivity.kt   # Lock screen overlay, permission requester
    │       └── SweetyApp.kt      # Notification channel configuration
```

---

## 🚀 How to Run Phase 1

### 1. Start the Node.js Backend:
```bash
cd sweety-backend

# 1. Edit .env and paste your Google AI Studio Gemini API Key:
# GEMINI_API_KEY=your_key_here

# 2. Run the server:
npm start
```
The server will start at: `http://localhost:5000` (and `http://0.0.0.0:5000`).

### 2. Run the Android App:
1. Open Android Studio.
2. Select **Open an existing project** and choose `c:\project\AI asisten\sweety-android`.
3. Let Gradle sync and click **Run (Shift + F10)** on your Android device or emulator.
4. If testing on a physical phone or BlueStacks over WiFi, tap the **Settings (⚙️)** icon in the top right of Sweety AI and enter your PC's IP address (e.g. `http://192.168.1.50:5000`).

