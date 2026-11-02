import express from "express";
import {
  handleChat,
  getMemories,
  saveMemory,
  clearHistory,
} from "../controllers/assistantController.js";

const router = express.Router();

router.post("/chat", handleChat);
router.get("/memories", getMemories);
router.post("/memories", saveMemory);
router.delete("/history/:sessionId", clearHistory);

/**
 * Adds a standard 44-byte RIFF WAV header to 24kHz 16-bit Mono PCM audio
 */
function addWavHeader(pcmBuffer, sampleRate = 24000, numChannels = 1, bitDepth = 16) {
  const header = Buffer.alloc(44);
  const dataLength = pcmBuffer.length;
  const fileLength = dataLength + 36;
  const byteRate = (sampleRate * numChannels * bitDepth) / 8;
  const blockAlign = (numChannels * bitDepth) / 8;

  header.write("RIFF", 0);
  header.writeUInt32LE(fileLength, 4);
  header.write("WAVE", 8);
  header.write("fmt ", 12);
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(numChannels, 22);
  header.writeUInt32LE(sampleRate, 24);
  header.writeUInt32LE(byteRate, 28);
  header.writeUInt16LE(blockAlign, 32);
  header.writeUInt16LE(bitDepth, 34);
  header.write("data", 36);
  header.writeUInt32LE(dataLength, 40);

  return Buffer.concat([header, pcmBuffer]);
}

/**
 * Smart Dual-Language TTS Voice Endpoint:
 * - English: Generates ultra-expressive human-like conversational voice (Gemini 2.5 TTS 'Kore' / Chat Experiences Journey voice)
 * - Bengali / Hindi: Generates natural sweet neural native voice
 */
router.get("/tts", async (req, res) => {
  const text = req.query.text;
  const lang = req.query.lang || "bn";
  if (!text) return res.status(400).send("No text provided");

  const cleanText = text.replace(/[*#_`~]/g, "").trim().substring(0, 300);
  const isEnglish = lang === "en" || !/[\u0980-\u09FF\u0900-\u097F]/.test(cleanText);

  // 1. If English, attempt Gemini's ultra-realistic Conversational Neural Voice (Kore - Chat Experience)
  if (isEnglish && process.env.GEMINI_API_KEY) {
    try {
      const apiKey = process.env.GEMINI_API_KEY;
      const ttsModelUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=${apiKey}`;
      const payload = {
        contents: [{ parts: [{ text: cleanText }] }],
        generationConfig: {
          responseModalities: ["AUDIO"],
          speechConfig: {
            voiceConfig: {
              prebuiltVoiceConfig: {
                voiceName: "Kore", // Expressive, sweet, natural female conversational voice
              },
            },
          },
        },
      };

      const geminiRes = await fetch(ttsModelUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (geminiRes.ok) {
        const geminiData = await geminiRes.json();
        const base64Audio = geminiData.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;
        if (base64Audio) {
          const pcm = Buffer.from(base64Audio, "base64");
          const wavBuffer = addWavHeader(pcm, 24000, 1, 16);
          res.setHeader("Content-Type", "audio/wav");
          res.setHeader("Cache-Control", "public, max-age=86400");
          return res.send(wavBuffer);
        }
      }
    } catch (e) {
      console.warn("Gemini preview-tts error, falling back to Google Neural TTS:", e.message);
    }
  }

  // 2. Natural Bengali / Hindi Neural Voice engine
  try {
    const encoded = encodeURIComponent(cleanText);
    const googleTtsUrl = `https://translate.google.com/translate_tts?ie=UTF-8&tl=${lang}&client=tw-ob&q=${encoded}`;
    const ttsRes = await fetch(googleTtsUrl, {
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
      },
    });

    if (!ttsRes.ok) {
      return res.status(500).send("TTS Service Unavailable");
    }

    res.setHeader("Content-Type", "audio/mpeg");
    res.setHeader("Cache-Control", "public, max-age=86400");
    const arrayBuffer = await ttsRes.arrayBuffer();
    return res.send(Buffer.from(arrayBuffer));
  } catch (err) {
    return res.status(500).send(err.message);
  }
});

export default router;
