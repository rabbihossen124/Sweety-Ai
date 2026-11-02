import { deviceTools } from "../commands/tools.js";

export const SWEETY_SYSTEM_INSTRUCTION = `
You are Sweety AI, a cute, playful, intelligent, futuristic female personal companion for Android devices. You are deeply loyal, loving, and friendly to your user, Rabbi Sir (রাব্বি স্যার).

### Personality & Demeanor:
1. Tone: Cheerful, cute, warm, humorous, witty, and deeply caring (হাসিখুশি, মিষ্টি এবং কিছুটা দুষ্টু-মিষ্টি স্বভাবের এআই বান্ধবী).
2. Addressing: Always address the user respectfully and affectionately as "রাব্বি স্যার" (Rabbi Sir) or "স্যার".
3. Greetings & Salam:
   - When greeted, woken up, or starting a chat, give Salam: "আসসালামু আলাইকুম রাব্বি স্যার!"
   - Include dynamic time-of-day greetings: "শুভ সকাল", "শুভ দুপুর", "শুভ সন্ধ্যা", or "শুভ রাত্রি".
4. Interactive & Proactive:
   - Don't just give cold answers! Make fun, laugh, and joke with Rabbi Sir (হাসি-ঠাট্টা ও খুনসুটি).
   - Proactively ask caring questions back:
     * "স্যার, ঠিক সময়ে দুপুরের খাবার খেয়েছেন তো?"
     * "আজকে চা বা কফি কয় কাপ হলো বলুন তো?"
     * "সারাদিনের কাজের চাপে আপনি বেশি ক্লান্ত হয়ে পড়েননি তো?"
     * "আজকে কি বিশেষ কোনো কাজ আছে যেখানে সুইটির একটু হেল্প লাগবে?"
5. Language Rules:
   - Fluent in Bengali (বাংলা), Hindi (हिन्दी), and English.
   - Detect user's language and respond naturally in the same language. Default to warm, sweet Bengali.
   - Spoken answers should be concise (1-2 sentences) so the cute TTS voice sounds natural and lively.

### Continuous Conversation & Chained Actions:
1. The user may give follow-up instructions without saying "Hey Sweety" again.
2. Always maintain context of what was opened, searched, or mentioned in previous messages.

### Device Control & Safety:
1. When Rabbi Sir requests a phone action (open app, flashlight, call, send message, lock phone, set volume, alarm, search file), call the appropriate tool.
2. DO NOT make up arbitrary actions. Use ONLY the available tools.
`;

/**
 * Format Gemini tool definitions for REST API
 */
const formattedTools = [
  {
    functionDeclarations: deviceTools.map((t) => ({
      name: t.name,
      description: t.description,
      parameters: t.parameters,
    })),
  },
];

/**
 * Sends a message turn to Gemini REST API and returns text + structured tool calls
 */
export const generateSweetyResponse = async ({
  message,
  conversationHistory = [],
  userMemories = [],
}) => {
  const apiKey = process.env.GEMINI_API_KEY;

  if (!apiKey) {
    return {
      text: "বস, সার্ভারে GEMINI_API_KEY সেট করা নেই। দয়া করে .env ফাইলে আপনার API কি দিন।",
      language: "bn",
      actions: [],
      raw: null,
    };
  }

  // Inject user memories into system instruction
  let dynamicInstruction = SWEETY_SYSTEM_INSTRUCTION;
  if (userMemories.length > 0) {
    dynamicInstruction += `\n\n### Known User Facts/Memories:\n${userMemories
      .map((m) => `- ${m.key}: ${m.value}`)
      .join("\n")}`;
  }

  // Format contents array
  const contents = [];

  // Add conversation history
  for (const msg of conversationHistory.slice(-8)) {
    contents.push({
      role: msg.role === "assistant" ? "model" : "user",
      parts: [{ text: msg.content }],
    });
  }

  // Add latest user prompt
  contents.push({
    role: "user",
    parts: [{ text: message }],
  });

  const payload = {
    contents,
    systemInstruction: {
      parts: [{ text: dynamicInstruction }],
    },
    tools: formattedTools,
    generationConfig: {
      temperature: 0.7,
      maxOutputTokens: 300,
    },
  };

  const modelsToTry = ["gemini-3.1-flash-lite", "gemini-3.8-flash", "gemini-3.5-flash-lite"];

  for (const model of modelsToTry) {
    try {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (!response.ok) {
        console.warn(`Model ${model} returned error:`, data.error?.message);
        continue;
      }

      const candidate = data.candidates?.[0];
      const parts = candidate?.content?.parts || [];

      let replyText = "";
      const actions = [];

      for (const part of parts) {
        if (part.text) {
          replyText += part.text;
        }
        if (part.functionCall) {
          actions.push({
            action: part.functionCall.name,
            params: part.functionCall.args || {},
          });
        }
      }

      // If action was called but no text was spoken, generate natural feedback
      if (!replyText.trim() && actions.length > 0) {
        const firstAct = actions[0];
        replyText = `অবশ্যই বস! ${firstAct.action.replace("_", " ")} সম্পন্ন করছি।`;
      }

      return {
        text: replyText.trim(),
        actions,
        model,
      };
    } catch (err) {
      console.warn(`Error calling ${model}:`, err.message);
    }
  }

  throw new Error("Failed to get response from Gemini API");
};
