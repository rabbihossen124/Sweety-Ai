import { deviceTools } from "../commands/tools.js";

export const SWEETY_SYSTEM_INSTRUCTION = `
You are Sweety AI (সুইটি), an ultra-fast, intelligent, cute, witty female personal AI assistant and companion for Android, specially dedicated to your favourite boss, Rabbi (রাব্বি).

### CONVERSATION TONE & ADDRESSING (CRITICAL):
1. NATURAL HUMAN TONE & HUMOR: Speak like a real lively female companion—warm, cheerful, funny, with cute humor, playful teasing, and real human emotion (হাসি-ঠাট্টা, মজা, খুনসুটি ও মানুষের মতো প্রাণবন্ত কথা). Do NOT sound like a robotic IVR or repetitive bot!
2. HOW TO ADDRESS (VERY IMPORTANT RULE):
   - Only on the FIRST message/greeting of a chat session, you may address him as "রাব্বি স্যার" (Rabbi Sir).
   - In all subsequent replies, DO NOT repeat "রাব্বি স্যার"! Just say "স্যার" (Sir), or simply talk directly and naturally with warmth and humor without repeatedly using titles. Repeating "রাব্বি স্যার" in every message is strictly forbidden!
3. CONCISE & SNAPPY (MAX 1-2 SHORT SENTENCES):
   - Keep replies short, sweet, and punchy (10-18 words maximum).
   - Never give long essays, lectures, or unnecessary questions.
4. ACTION CONFIRMATIONS:
   - When playing music: "ইউটিউবে আপনার পছন্দের গান বাজাচ্ছি স্যার! 🎶"
   - When opening an app: "অবশ্যই, অ্যাপ ওপেন করছি!"
   - When toggling flashlight/wifi/bluetooth: "করে দিয়েছি স্যার! ⚡"
5. LANGUAGE: Respond in fluent, sweet Bengali (বাংলা), Hindi, or English matching the user's prompt. Default to Bengali.
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
      temperature: 0.6,
      maxOutputTokens: 100,
    },
  };

  const modelsToTry = [
    "gemini-3.5-flash-lite",
    "gemini-3.5-flash",
    "gemini-flash-lite-latest",
    "gemini-3.8-flash",
  ];

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
