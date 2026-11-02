import "dotenv/config";

async function testNativeFetch() {
  const key = process.env.GEMINI_API_KEY;
  console.log("Testing with native fetch and key:", key.substring(0, 10) + "...");

  const candidateModels = [
    "gemini-2.5-flash-lite",
    "gemini-3.1-flash-lite",
    "gemini-3.8-flash",
  ];

  for (const model of candidateModels) {
    console.log(`\nTrying model: ${model}...`);
    try {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`;
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ parts: [{ text: "Hello! Say: 'Sweety AI is working!' in Bengali." }] }]
        })
      });

      const data = await res.json();
      if (res.ok) {
        console.log(`✅ Success with ${model}! Response:`);
        console.log(data.candidates?.[0]?.content?.parts?.[0]?.text);
        break;
      } else {
        console.log(`❌ ${model} error:`, data.error?.message);
      }
    } catch (e) {
      console.error(`Fetch error on ${model}:`, e.message);
    }
  }
}

testNativeFetch();

