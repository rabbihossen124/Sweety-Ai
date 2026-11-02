import "dotenv/config";
import express from "express";
import cors from "cors";
import { connectDB, isDbConnected } from "./config/db.js";
import assistantRoutes from "./routes/assistantRoutes.js";

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// Health check endpoint
app.get("/api/health", (req, res) => {
  res.json({
    status: "ok",
    service: "Sweety AI Backend",
    database: isDbConnected() ? "connected" : "in-memory-fallback",
    geminiConfigured: !!process.env.GEMINI_API_KEY,
    timestamp: new Date().toISOString(),
  });
});

// Assistant endpoints
app.use("/api/assistant", assistantRoutes);

// Root route
app.get("/", (req, res) => {
  res.send({
    message: "🚀 Sweety AI Android Backend is active and running!",
    endpoints: {
      health: "/api/health",
      chat: "POST /api/assistant/chat",
      memories: "GET, POST /api/assistant/memories",
    },
  });
});

// Start server locally (if not running in Vercel serverless environment)
if (!process.env.VERCEL) {
  connectDB();
  app.listen(PORT, "0.0.0.0", () => {
    console.log(`\n==============================================`);
    console.log(`🤖 SWEETY AI BACKEND SERVER RUNNING`);
    console.log(`📡 Local URL:   http://localhost:${PORT}`);
    console.log(`🌐 Network URL: http://0.0.0.0:${PORT}`);
    console.log(`==============================================\n`);
  });
} else {
  connectDB();
}

export default app;

