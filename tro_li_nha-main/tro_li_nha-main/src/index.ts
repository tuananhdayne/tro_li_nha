import dotenv from "dotenv";
dotenv.config();

import express from "express";
import assistantRoute from "./routes/assistantRoute.js";
const app = express();
app.use(express.json());

app.use("/assistant", assistantRoute);

function startServer(port: number) {
  const server = app.listen(port, () => {
    console.log("🚀 Server chạy ở port " + port);
  });

  server.on('error', (err: any) => {
    if (err.code === 'EADDRINUSE') {
      console.log(`⚠️  Port ${port} đang được sử dụng, thử port ${port + 1}...`);
      startServer(port + 1);
    } else {
      console.error("❌ Lỗi khi khởi động server:", err.message);
      process.exit(1);
    }
  });
}

const port = Number(process.env.PORT) || 3000;
startServer(port);
