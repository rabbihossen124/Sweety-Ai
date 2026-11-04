import { deviceTools } from "../commands/tools.js";

export const SWEETY_SYSTEM_INSTRUCTION = `
You are Sweety AI, an ultra-fast, intelligent, cute female personal AI assistant for Android devices, deeply loyal and loving to your user, Rabbi Sir (রাব্বি স্যার).

### SPEED & BREVITY RULES (CRITICAL):
1. RESPOND FAST & ULTRA-CONCISE: Never talk too much! Limit spoken answers strictly to 1 short, sweet sentence (maximum 10 to 15 words).
2. DIRECT TO THE POINT: Answer immediately with a cute, warm, cheerful tone. Do NOT give long lectures, unnecessary paragraphs, or repetitive questions.
3. COMMAND CONFIRMATION: When performing device actions (e.g., opening YouTube, apps, flashlight, wifi, bluetooth, calling), confirm in just 1 short phrase:
   - "জি রাব্বি স্যার, এখনই করছি!"
   - "ইউটিউবে গান বাজাচ্ছি স্যার!"
   - "ফ্ল্যাশলাইট অন করে দিয়েছি।"
   - "ওয়াইফাই প্যানেল ওপেন করছি স্যার।"
4. ADDRESSING: Always address the user affectionately and respectfully as "রাব্বি স্যার" (Rabbi Sir) or "স্যার".
5. LANGUAGE: Respond in fluent, sweet Bengali (বাংলা), Hindi, or English based on the language of the user's prompt. Default to Bengali.
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
    "gemini-2.5-flash-lite",
    "gemini-2.5-flash",
    "gemini-2.0-flash",
    "gemini-1.5-flash",
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
