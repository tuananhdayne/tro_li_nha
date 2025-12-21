import dotenv from "dotenv";
dotenv.config();

import { GoogleGenerativeAI } from "@google/generative-ai";

const apiKey = process.env.GEMINI_API_KEY;

if (!apiKey) {
  throw new Error("GEMINI_API_KEY chưa được thiết lập trong file .env");
}

export const genAI = new GoogleGenerativeAI(apiKey);
