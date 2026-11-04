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
 * - Generates ultra-expressive human-like conversational voice (Gemini TTS 'Kore')
 * - Strictly locked to Rabbi Sir's provided voice style (sweet female natural voice) across Bengali, Hindi, and English!
 */
router.get("/tts", async (req, res) => {
  const text = req.query.text;
  if (!text) return res.status(400).send("No text provided");

  const cleanText = text.replace(/[*#_`~]/g, "").trim().substring(0, 300);

  // 1. Attempt Gemini Conversational Neural Voice (Kore) across all languages
  if (process.env.GEMINI_API_KEY) {
    const ttsModels = [
      "gemini-2.5-flash-preview-tts",
      "gemini-3.8-flash-tts",
      "gemini-3.1-flash-tts-preview",
    ];
    const apiKey = process.env.GEMINI_API_KEY;

    for (const model of ttsModels) {
      try {
        const ttsModelUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
        const payload = {
          contents: [{ parts: [{ text: cleanText }] }],
          generationConfig: {
            responseModalities: ["AUDIO"],
            speechConfig: {
              voiceConfig: {
                prebuiltVoiceConfig: {
                  voiceName: "Kore", // Rabbi Sir's expressive sweet female voice
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
        console.warn(`TTS Model ${model} failed:`, e.message);
      }
    }
  }

  // 2. High-speed fallback if API key or quota issues arise
  try {
    const isBangla = /[\u0980-\u09FF]/.test(cleanText);
    const isHindi = /[\u0900-\u097F]/.test(cleanText);
    const targetLang = isBangla ? "bn" : isHindi ? "hi" : "en";

    const encoded = encodeURIComponent(cleanText);
    const googleTtsUrl = `https://translate.google.com/translate_tts?ie=UTF-8&tl=${targetLang}&client=tw-ob&q=${encoded}`;
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
