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

// High-Quality Google Neural Cloud Voice Endpoint
router.get("/tts", async (req, res) => {
  const text = req.query.text;
  const lang = req.query.lang || "bn";
  if (!text) return res.status(400).send("No text provided");

  try {
    const encoded = encodeURIComponent(text.substring(0, 300));
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
    res.send(Buffer.from(arrayBuffer));
  } catch (err) {
    res.status(500).send(err.message);
  }
});

export default router;

