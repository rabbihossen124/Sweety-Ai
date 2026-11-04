/**
 * Gemini Tools (Function Calling definitions) for Sweety AI device actions
 */
export const deviceTools = [
  {
    type: "function",
    name: "open_app",
    description: "Opens an installed application on the user's Android phone.",
    parameters: {
      type: "object",
      properties: {
        app_name: {
          type: "string",
          description: "Name of the app to launch (e.g., 'WhatsApp', 'YouTube', 'Camera', 'Gallery', 'Facebook', 'Chrome', 'Settings').",
        },
        package_name: {
          type: "string",
          description: "Optional package name if known (e.g. 'com.whatsapp', 'com.google.android.youtube').",
        },
      },
      required: ["app_name"],
    },
  },
  {
    type: "function",
    name: "find_file",
    description: "Searches the device storage for a file, photo, document, or audio by name or type.",
    parameters: {
      type: "object",
      properties: {
        file_name: {
          type: "string",
          description: "The name, keyword, or extension of the file to search (e.g., 'invoice.pdf', 'photo', 'screenshot', 'resume').",
        },
        file_type: {
          type: "string",
          description: "Optional category: 'image', 'video', 'audio', 'document', or 'any'.",
        },
      },
      required: ["file_name"],
    },
  },
  {
    type: "function",
    name: "send_whatsapp",
    description: "Sends a WhatsApp message or shares a file with a specified contact.",
    parameters: {
      type: "object",
      properties: {
        contact_name: {
          type: "string",
          description: "Name of the person/contact to send to (e.g., 'Nazneen', 'Mom', 'Abir').",
        },
        message: {
          type: "string",
          description: "The text message content to send.",
        },
        file_name: {
          type: "string",
          description: "Optional file name or uri found in previous turns to attach.",
        },
      },
      required: ["contact_name"],
    },
  },
  {
    type: "function",
    name: "make_call",
    description: "Initiates a phone call to a contact or phone number.",
    parameters: {
      type: "object",
      properties: {
        contact_name: {
          type: "string",
          description: "Name of the contact to call.",
        },
        phone_number: {
          type: "string",
          description: "Optional exact phone number if known.",
        },
      },
      required: ["contact_name"],
    },
  },
  {
    type: "function",
    name: "send_sms",
    description: "Drafts and sends an SMS message to a contact.",
    parameters: {
      type: "object",
      properties: {
        contact_name: {
          type: "string",
          description: "Name of the contact to send SMS to.",
        },
        message: {
          type: "string",
          description: "Text of the SMS.",
        },
      },
      required: ["contact_name", "message"],
    },
  },
  {
    type: "function",
    name: "lock_device",
    description: "Locks the Android phone screen instantly using AccessibilityService.",
    parameters: {
      type: "object",
      properties: {
        confirmation: {
          type: "boolean",
          description: "Set to true to lock the device.",
        },
      },
    },
  },
  {
    type: "function",
    name: "toggle_flashlight",
    description: "Turns the device flashlight on or off.",
    parameters: {
      type: "object",
      properties: {
        state: {
          type: "boolean",
          description: "true to turn flashlight ON, false to turn flashlight OFF.",
        },
      },
      required: ["state"],
    },
  },
  {
    type: "function",
    name: "set_volume",
    description: "Adjusts device media, ringer, or alarm volume level.",
    parameters: {
      type: "object",
      properties: {
        level: {
          type: "number",
          description: "Volume percentage from 0 to 100.",
        },
        type: {
          type: "string",
          enum: ["media", "ring", "alarm", "mute"],
          description: "Volume stream to adjust.",
        },
      },
      required: ["level"],
    },
  },
  {
    type: "function",
    name: "set_alarm",
    description: "Sets an alarm or countdown timer on the Android clock.",
    parameters: {
      type: "object",
      properties: {
        time: {
          type: "string",
          description: "Alarm time e.g., '07:00 AM', '18:30', or 'in 15 minutes'.",
        },
        label: {
          type: "string",
          description: "Label or title for the alarm.",
        },
      },
      required: ["time"],
    },
  },
  {
    type: "function",
    name: "play_youtube",
    description: "Searches and plays a song, music video, or query directly on YouTube.",
    parameters: {
      type: "object",
      properties: {
        query: {
          type: "string",
          description: "Song title, artist, or video search query (e.g., 'Arijit Singh romantic songs', 'Believer Imagine Dragons').",
        },
      },
      required: ["query"],
    },
  },
  {
    type: "function",
    name: "toggle_wifi",
    description: "Turns Wi-Fi on or off, or launches the quick Wi-Fi control panel.",
    parameters: {
      type: "object",
      properties: {
        state: {
          type: "boolean",
          description: "true to enable Wi-Fi, false to disable Wi-Fi.",
        },
      },
      required: ["state"],
    },
  },
  {
    type: "function",
    name: "toggle_bluetooth",
    description: "Turns Bluetooth on or off, or opens the Bluetooth control panel.",
    parameters: {
      type: "object",
      properties: {
        state: {
          type: "boolean",
          description: "true to enable Bluetooth, false to disable Bluetooth.",
        },
      },
      required: ["state"],
    },
  },
  {
    type: "function",
    name: "open_data_settings",
    description: "Opens mobile cellular data connection panel or network settings.",
    parameters: {
      type: "object",
      properties: {
        state: {
          type: "boolean",
          description: "true to turn data ON, false to turn data OFF.",
        },
      },
    },
  },
  {
    type: "function",
    name: "close_app",
    description: "Closes the active app or returns immediately to the Android home screen.",
    parameters: {
      type: "object",
      properties: {
        app_name: {
          type: "string",
          description: "Optional name of the app to close.",
        },
      },
    },
  },
  {
    type: "function",
    name: "save_memory",
    description: "Saves a persistent fact or preference about the user to memory for future recall.",
    parameters: {
      type: "object",
      properties: {
        key: {
          type: "string",
          description: "Short key for the memory (e.g., 'favorite_color', 'boss_name', 'home_address').",
        },
        value: {
          type: "string",
          description: "The information to remember.",
        },
      },
      required: ["key", "value"],
    },
  },
];

