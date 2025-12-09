import axios from "axios";

export async function getWeather(city = "Hanoi") {
  const key = process.env.WEATHER_KEY;

  const url = `https://api.openweathermap.org/data/2.5/weather?q=${city}&appid=${key}&units=metric&lang=vi`;

  const res = await axios.get(url);
  const d = res.data;

  return `Thời tiết hiện tại ở ${d.name} ${d.weather[0].description}, nhiệt độ ${d.main.temp}°C, độ ẩm ${d.main.humidity}%.`;
}
