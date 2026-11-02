import "dotenv/config";
import { GoogleGenAI } from "@google/genai";

async function runDebug() {
  console.log("Starting debug test with key length:", process.env.GEMINI_API_KEY?.length);
  const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

  console.time("Gemini Call");
  try {
    const res = await ai.models.generateContent({
      model: "gemini-3.8-flash",
      contents: "Hello Sweety! Say 'I am online and ready to assist Rabbi!' in Bengali."
    });
    console.timeEnd("Gemini Call");
    console.log("Success! Response:\n", res.text);
  } catch (err) {
    console.timeEnd("Gemini Call");
    console.error("Error details:", err);
  }
}

runDebug();

