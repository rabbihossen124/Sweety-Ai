import { generateSweetyResponse } from "../ai/gemini.js";
import { Conversation } from "../models/Conversation.js";
import { Memory } from "../models/Memory.js";
import { isDbConnected } from "../config/db.js";

// In-memory fallback caches if MongoDB is disconnected
const inMemoryConversations = new Map();
const inMemoryMemories = [];

export const handleChat = async (req, res) => {
  try {
    const { message, sessionId = "session_default", userId = "user_1" } = req.body;

    if (!message || typeof message !== "string" || !message.trim()) {
      return res.status(400).json({
        success: false,
        error: "Message is required and must not be empty.",
      });
    }

    const trimmedMessage = message.trim();

    // 1. Retrieve conversation history
    let history = [];
    if (isDbConnected()) {
      try {
        const convo = await Conversation.findOne({ sessionId });
        if (convo && convo.messages) {
          history = convo.messages.slice(-8);
        }
      } catch (err) {
        console.warn("DB history read error, using empty history:", err.message);
      }
    } else {
      history = inMemoryConversations.get(sessionId) || [];
    }

    // 2. Retrieve user memories
    let memories = [];
    if (isDbConnected()) {
      try {
        memories = await Memory.find({ userId }).limit(20);
      } catch (err) {
        console.warn("DB memory read error:", err.message);
      }
    } else {
      memories = inMemoryMemories;
    }

    // 3. Generate Gemini response
    const geminiResult = await generateSweetyResponse({
      message: trimmedMessage,
      conversationHistory: history,
      userMemories: memories,
    });

    // 4. Check if save_memory action was called
    for (const action of geminiResult.actions) {
      if (action.action === "save_memory" && action.params?.key && action.params?.value) {
        if (isDbConnected()) {
          await Memory.findOneAndUpdate(
            { userId, key: action.params.key },
            { value: action.params.value, updatedAt: new Date() },
            { upsert: true }
          );
        } else {
          inMemoryMemories.push({
            userId,
            key: action.params.key,
            value: action.params.value,
            updatedAt: new Date(),
          });
        }
      }
    }

    // 5. Save turn to conversation history
    const userMsg = { role: "user", content: trimmedMessage, timestamp: new Date() };
    const assistantMsg = {
      role: "assistant",
      content: geminiResult.text,
      actionsTriggered: geminiResult.actions,
      timestamp: new Date(),
    };

    if (isDbConnected()) {
      try {
        await Conversation.findOneAndUpdate(
          { sessionId },
          {
            $push: { messages: { $each: [userMsg, assistantMsg] } },
            lastActive: new Date(),
            userId,
          },
          { upsert: true, new: true }
        );
      } catch (err) {
        console.warn("DB save conversation error:", err.message);
      }
    } else {
      const current = inMemoryConversations.get(sessionId) || [];
      current.push(userMsg, assistantMsg);
      inMemoryConversations.set(sessionId, current.slice(-20));
    }

    // Return response with continueListening: true so Android knows to keep listening
    return res.json({
      success: true,
      text: geminiResult.text,
      actions: geminiResult.actions,
      sessionId,
      continueListening: true, // Keep the listening window active for chained instructions
    });
  } catch (error) {
    console.error("Assistant Controller Error:", error);
    return res.status(500).json({
      success: false,
      error: error.message || "Failed to process message with Sweety AI",
    });
  }
};

export const getMemories = async (req, res) => {
  try {
    const userId = req.query.userId || "user_1";
    if (isDbConnected()) {
      const memories = await Memory.find({ userId }).sort({ updatedAt: -1 });
      return res.json({ success: true, memories });
    } else {
      return res.json({ success: true, memories: inMemoryMemories });
    }
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

export const saveMemory = async (req, res) => {
  try {
    const { key, value, category = "general", userId = "user_1" } = req.body;
    if (!key || !value) {
      return res.status(400).json({ success: false, error: "Key and value are required." });
    }

    if (isDbConnected()) {
      const mem = await Memory.findOneAndUpdate(
        { userId, key },
        { value, category, updatedAt: new Date() },
        { upsert: true, new: true }
      );
      return res.json({ success: true, memory: mem });
    } else {
      const newMem = { userId, key, value, category, updatedAt: new Date() };
      inMemoryMemories.push(newMem);
      return res.json({ success: true, memory: newMem });
    }
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

export const clearHistory = async (req, res) => {
  try {
    const { sessionId } = req.params;
    if (isDbConnected()) {
      await Conversation.deleteOne({ sessionId });
    } else {
      inMemoryConversations.delete(sessionId);
    }
    return res.json({ success: true, message: `History cleared for session ${sessionId}` });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
};

