import mongoose from "mongoose";

const MessageSchema = new mongoose.Schema({
  role: {
    type: String,
    enum: ["user", "assistant", "system"],
    required: true,
  },
  content: {
    type: String,
    required: true,
  },
  language: {
    type: String,
    default: "auto",
  },
  actionsTriggered: [
    {
      action: String,
      params: mongoose.Schema.Types.Mixed,
      status: {
        type: String,
        enum: ["pending", "confirmed", "executed", "failed"],
        default: "executed",
      },
    },
  ],
  timestamp: {
    type: Date,
    default: Date.now,
  },
});

const ConversationSchema = new mongoose.Schema({
  sessionId: {
    type: String,
    required: true,
    index: true,
  },
  userId: {
    type: String,
    default: "default_user",
  },
  messages: [MessageSchema],
  lastActive: {
    type: Date,
    default: Date.now,
  },
});

export const Conversation = mongoose.models.Conversation || mongoose.model("Conversation", ConversationSchema);

