using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using System.Diagnostics;
using WebApp.Models;
using Services.Services;
using System.Linq;
using System.Text.Json;

namespace WebApp.Controllers
{
    public class HomeController : Controller
    {
        private readonly ILogger<HomeController> _logger;
        private readonly IHouseService _houseService;
        private readonly IUserService _userService;
        private readonly IDeviceService _deviceService;

        public HomeController(
            ILogger<HomeController> logger, 
            IHouseService houseService, 
            IUserService userService,
            IDeviceService deviceService)
        {
            _logger = logger;
            _houseService = houseService;
            _userService = userService;
            _deviceService = deviceService;
        }

        [Authorize]
        public IActionResult Index()
        {
            var userId = _userService.GetCurrentUserId();
            var houses = _houseService.GetHousesByUserId(userId).ToList();
            
            int totalHouses = houses.Count;
            int totalRooms = houses.Sum(h => h.Rooms?.Count ?? 0);
            
            var devices = _deviceService.GetDevicesByUserId(userId).ToList();
            int activeDevices = devices.Count(d => d.Status?.ToLower() == "on");
            
            // Calculate Average Temperature
            double avgTemp = 0;
            var tempSensors = devices.Where(d => d.Type == "TemperatureHumiditySensor").ToList();
            if (tempSensors.Any())
            {
                var temps = new List<double>();
                foreach (var sensor in tempSensors)
                {
                    var lastTelemetry = _deviceService.GetTelemetryDataByDeviceId(sensor.ID)
                        .OrderByDescending(t => t.Timestamp)
                        .FirstOrDefault();
                    
                    if (lastTelemetry != null && !string.IsNullOrEmpty(lastTelemetry.Body))
                    {
                        try {
                            // Body is usually like {"temperature": 25.5, ...}
                            var json = System.Text.Json.JsonDocument.Parse(lastTelemetry.Body);
                            if (json.RootElement.TryGetProperty("temperature", out var tempProp)) {
                                temps.Add(tempProp.GetDouble());
                            }
                        } catch { }
                    }
                }
                if (temps.Any()) avgTemp = temps.Average();
            }

            ViewBag.TotalHouses = totalHouses;
            ViewBag.TotalRooms = totalRooms;
            ViewBag.ActiveDevices = activeDevices;
            ViewBag.AvgTemp = avgTemp > 0 ? avgTemp.ToString("F1") : "--";

            return View();
        }
        [Authorize]
        public IActionResult Privacy()
        {
            return View();
        }

        [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
        public IActionResult Error()
        {
            return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
        }
    }
}
