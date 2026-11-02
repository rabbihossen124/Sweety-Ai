import "dotenv/config";
import mongoose from "mongoose";
import { Memory } from "./src/models/Memory.js";
import { Conversation } from "./src/models/Conversation.js";

async function testDatabase() {
  console.log("Connecting to MongoDB Atlas...");
  const uri = process.env.MONGODB_URI;

  try {
    await mongoose.connect(uri, {
      serverSelectionTimeoutMS: 8000,
    });
    console.log("✅ Successfully connected to MongoDB Atlas!");
    console.log("Database Host:", mongoose.connection.host);
    console.log("Database Name:", mongoose.connection.name);

    // Test saving a sample memory for Sweety AI
    const sampleMemory = await Memory.findOneAndUpdate(
      { userId: "user_owner", key: "assistant_mode" },
      { value: "full_phone_control_active", category: "system", updatedAt: new Date() },
      { upsert: true, new: true }
    );
    console.log("✅ Memory write test passed! Stored doc:", sampleMemory.key, "=>", sampleMemory.value);

    // Test reading it back
    const retrieved = await Memory.findOne({ key: "assistant_mode" });
    console.log("✅ Memory read test passed! Retrieved doc:", retrieved.key, "=>", retrieved.value);

    await mongoose.disconnect();
    console.log("✅ All Database tests completed successfully!");
    process.exit(0);
  } catch (err) {
    console.error("❌ MongoDB Atlas Connection Error:", err.message);
    process.exit(1);
  }
}

testDatabase();

