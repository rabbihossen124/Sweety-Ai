import "dotenv/config";
import { generateSweetyResponse } from "./src/ai/gemini.js";

async function testAssistant() {
  console.log("Testing Sweety AI Response with fallback/offline safety...");
  try {
    const res = await generateSweetyResponse({
      message: "Sweety, YouTube kholo",
      conversationHistory: [],
      userMemories: [],
    });
    console.log("Sweety Response Output:", JSON.stringify(res, null, 2));
  } catch (err) {
    console.log("Expected when API key not set or network blocked in sandbox:", err.message);
  }
}

testAssistant();

