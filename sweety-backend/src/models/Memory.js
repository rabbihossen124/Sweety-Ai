import mongoose from "mongoose";

const MemorySchema = new mongoose.Schema({
  userId: {
    type: String,
    required: true,
    default: "default_user",
    index: true,
  },
  key: {
    type: String,
    required: true,
  },
  value: {
    type: String,
    required: true,
  },
  category: {
    type: String,
    enum: ["preference", "contact_note", "habit", "system", "general"],
    default: "general",
  },
  updatedAt: {
    type: Date,
    default: Date.now,
  },
});

export const Memory = mongoose.models.Memory || mongoose.model("Memory", MemorySchema);

