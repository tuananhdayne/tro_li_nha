import express from "express";
import { runAssistant } from "../assistant.js";

const router = express.Router();

router.post("/", async (req, res) => {
  const text = req.body.text;
  const reply = await runAssistant(text);

  res.json({
    question: text,
    assistant: reply,
  });
});

export default router;
