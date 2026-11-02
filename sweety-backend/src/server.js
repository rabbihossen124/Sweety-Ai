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

// Download redirect routes
app.get("/download", (req, res) => {
  res.redirect("https://github.com/rabbihossen124/Sweety-Ai/releases/download/latest/Sweety-AI-Debug.apk");
});

app.get("/release", (req, res) => {
  res.redirect("https://github.com/rabbihossen124/Sweety-Ai/releases/download/latest/Sweety-AI.apk");
});

app.get("/app", (req, res) => {
  res.redirect("https://github.com/rabbihossen124/Sweety-Ai/releases/download/latest/Sweety-AI-Debug.apk");
});

// Root route
app.get("/", (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html lang="bn">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Sweety AI Assistant</title>
      <style>
        body {
          background: #060b14;
          color: #e0f2fe;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          min-height: 100vh;
          margin: 0;
          text-align: center;
          padding: 20px;
        }
        .card {
          background: rgba(15, 23, 42, 0.9);
          border: 1px solid #00E5FF;
          border-radius: 24px;
          padding: 30px;
          max-width: 440px;
          box-shadow: 0 0 30px rgba(0, 229, 255, 0.25);
        }
        h1 { color: #00E5FF; margin-bottom: 8px; font-size: 26px; }
        p { color: #94a3b8; font-size: 14px; margin-bottom: 24px; }
        .btn-primary {
          display: block;
          background: linear-gradient(135deg, #00E5FF, #00B0FF);
          color: #000;
          font-weight: bold;
          font-size: 16px;
          padding: 15px 24px;
          border-radius: 30px;
          text-decoration: none;
          box-shadow: 0 0 20px rgba(0, 229, 255, 0.5);
          margin-bottom: 14px;
        }
        .btn-secondary {
          display: block;
          background: rgba(255, 255, 255, 0.08);
          border: 1px solid rgba(0, 229, 255, 0.4);
          color: #00E5FF;
          font-weight: 600;
          font-size: 14px;
          padding: 12px 20px;
          border-radius: 30px;
          text-decoration: none;
        }
        .tip {
          margin-top: 18px;
          font-size: 12px;
          color: #64748b;
          line-height: 1.5;
        }
        .badge {
          margin-top: 20px;
          font-size: 12px;
          color: #00E5FF;
          background: rgba(0, 229, 255, 0.1);
          padding: 6px 14px;
          border-radius: 12px;
          display: inline-block;
        }
      </style>
    </head>
    <body>
      <div class="card">
        <h1>✨ Sweety AI Official</h1>
        <p>রাব্বি স্যারের পার্সোনাল ভয়েস অ্যাসিস্ট্যান্ট (v1.0.2)</p>
        
        <a class="btn-primary" href="/download">📲 ১ ক্লিকে ইনস্টল (সরাসরি APK)</a>
        <a class="btn-secondary" href="/release">⭐ অফিসিয়াল রিলিজ APK</a>

        <div class="tip">
          💡 ডাউনলোড করার পর ফাইলে ক্লিক করলেই ফোনে ইনস্টল শুরু হয়ে যাবে।
        </div>
        
        <div class="badge">🚀 Backend Status: Active & Connected</div>
      </div>
    </body>
    </html>
  `);
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

