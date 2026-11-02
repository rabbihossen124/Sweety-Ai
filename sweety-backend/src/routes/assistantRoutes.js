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

export default router;

