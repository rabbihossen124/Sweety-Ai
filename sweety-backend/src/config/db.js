import mongoose from "mongoose";

let isConnected = false;

export const connectDB = async () => {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    console.warn("⚠️ MONGODB_URI not provided in .env. Running with in-memory persistence fallback.");
    return false;
  }

  try {
    const conn = await mongoose.connect(uri, {
      serverSelectionTimeoutMS: 5000,
    });
    isConnected = true;
    console.log(`✅ MongoDB Connected: ${conn.connection.host}`);
    return true;
  } catch (error) {
    console.error("❌ MongoDB Connection Error:", error.message);
    console.warn("⚠️ Running in in-memory persistence fallback mode to prevent crash.");
    isConnected = false;
    return false;
  }
};

export const isDbConnected = () => isConnected;

