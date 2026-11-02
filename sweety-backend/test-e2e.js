import "dotenv/config";
import { connectDB } from "./src/config/db.js";
import { handleChat } from "./src/controllers/assistantController.js";
import { Conversation } from "./src/models/Conversation.js";

async function testFullPipeline() {
  console.log("1. Connecting to MongoDB...");
  await connectDB();

  console.log("2. Simulating User Request to Sweety AI...");
  const mockReq = {
    body: {
      message: "Sweety, amar priyo rong nil, mone rekho.",
      sessionId: "test_rabbi_session",
      userId: "rabbi_user",
    },
  };

  let responseData = null;
  const mockRes = {
    status: (code) => ({
      json: (data) => {
        responseData = data;
        return data;
      },
    }),
    json: (data) => {
      responseData = data;
      return data;
    },
  };

  await handleChat(mockReq, mockRes);
  console.log("\n✅ AI Response Received:");
  console.log(JSON.stringify(responseData, null, 2));

  console.log("\n3. Verifying MongoDB saved conversation...");
  const savedConvo = await Conversation.findOne({ sessionId: "test_rabbi_session" });
  if (savedConvo) {
    console.log("✅ Conversation verified in MongoDB! Total messages stored:", savedConvo.messages.length);
  } else {
    console.log("⚠️ In-memory store used.");
  }

  process.exit(0);
}

testFullPipeline();

