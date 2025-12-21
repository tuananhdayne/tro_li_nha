using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Mscc.GenerativeAI;
using System.Text.RegularExpressions;
using Services.Services;
using Services.Thingsboard_Services;
using DAO.BaseModels;
using System.Text.Json;

namespace WebApp.Controllers;

/// <summary>
/// AI Assistant Controller - Gemini Chatbot for voice commands with device state awareness
/// </summary>
[ApiController]
[Route("api/[controller]")]
public class AssistantController : ControllerBase
{
    private readonly IConfiguration _configuration;
    private readonly ILogger<AssistantController> _logger;
    private readonly IDeviceService _deviceService;
    private readonly IThingsboardService _thingsboardService;
    private readonly IRoomService _roomService;
    private readonly IHouseService _houseService;

    public AssistantController(
        IConfiguration configuration, 
        ILogger<AssistantController> logger,
        IDeviceService deviceService,
        IThingsboardService thingsboardService,
        IRoomService roomService,
        IHouseService houseService)
    {
        _configuration = configuration;
        _logger = logger;
        _deviceService = deviceService;
        _thingsboardService = thingsboardService;
        _roomService = roomService;
        _houseService = houseService;
    }

    [HttpPost]
    [AllowAnonymous]
    public async Task<IActionResult> Ask([FromBody] AssistantRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Text))
        {
            return BadRequest(new { error = "Text is required" });
        }

        try
        {
            _logger.LogInformation("Processing Assistant Request: {Text}, User: {UserId}", request.Text, request.UserId);
            var result = await HandleGeminiWithTools(request.Text, request.UserId);
            
            return Ok(new
            {
                question = request.Text,
                assistant = result
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing assistant request");
            return StatusCode(500, new { error = ex.Message });
        }
    }

    private async Task<object> HandleGeminiWithTools(string text, string? userId)
    {
        var apiKey = _configuration["GeminiApiKey"];
        if (string.IsNullOrEmpty(apiKey))
        {
            return new { message = "Xin lỗi, hệ thống AI chưa được cấu hình khóa API." };
        }

        using var httpClient = new HttpClient();
        // Using gemini-2.5-flash as it has separate quota available in late 2025
        var url = $"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={apiKey}";
        // Using a long timeout for the 2.5 models as they might involve thinking/complex processing
        httpClient.Timeout = TimeSpan.FromSeconds(30);

        // 1. Define Tools
        var tools = new object[]
        {
            new
            {
                function_declarations = new object[]
                {
                    new
                    {
                        name = "control_device",
                        description = "Điều khiển thiết bị bật/tắt hoặc đóng/mở (Đèn, Khóa cửa, Quạt).",
                        parameters = new
                        {
                            type = "object",
                            properties = new
                            {
                                deviceType = new { type = "string", @enum = new[] { "Light", "DoorLock", "Fan" }, description = "Loại thiết bị" },
                                action = new { type = "string", @enum = new[] { "on", "off" }, description = "Hành động (on/off)" }
                            },
                            required = new[] { "deviceType", "action" }
                        }
                    },
                    new
                    {
                        name = "query_sensor",
                        description = "Hỏi thông tin từ cảm biến (Nhiệt độ, Độ ẩm, Chuyển động).",
                        parameters = new
                        {
                            type = "object",
                            properties = new
                            {
                                sensorType = new { type = "string", @enum = new[] { "Temperature", "Humidity", "PIR" }, description = "Loại cảm biến" }
                            },
                            required = new[] { "sensorType" }
                        }
                    }
                }
            }
        };

        // 2. Prepare Request with System Instruction
        var requestBody = new
        {
            contents = new[] { new { parts = new[] { new { text = text } } } },
            tools = tools,
            tool_config = new { function_calling_config = new { mode = "AUTO" } },
            system_instruction = new
            {
                parts = new[]
                {
                    new
                    {
                        text = "Bạn là một trợ lý ảo thông minh toàn diện. Bạn có thể giúp người dùng điều khiển nhà thông minh (Smarthome) thông qua các công cụ được cung cấp, ĐỒNG THỜI bạn cũng là một chuyên gia kiến thức có thể trả lời mọi câu hỏi về đời sống, khoa học, xã hội, toán học, văn hóa... Đừng bao giờ nói rằng bạn chỉ giới hạn ở việc điều khiển thiết bị. Hãy trả lời thân thiện bằng tiếng Việt."
                    }
                }
            }
        };

        var response = await httpClient.PostAsJsonAsync(url, requestBody);
        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync();
            _logger.LogError("Gemini API Error: {StatusCode} - {Error}", response.StatusCode, errorBody);
            return new { message = "Xin lỗi, hiện tại tôi không thể kết nối tới máy chủ AI." };
        }

        var root = await response.Content.ReadFromJsonAsync<JsonElement>();
        var candidates = root.GetProperty("candidates");
        if (candidates.GetArrayLength() == 0) return new { message = "AI không phản hồi." };
        
        var parts = candidates[0].GetProperty("content").GetProperty("parts");

        // 3. Handle Response (Text or Tool Call)
        foreach (var part in parts.EnumerateArray())
        {
            if (part.TryGetProperty("functionCall", out var functionCall))
            {
                var functionName = functionCall.GetProperty("name").GetString();
                var args = functionCall.GetProperty("args");

                if (functionName == "control_device")
                {
                    string dType = args.GetProperty("deviceType").GetString();
                    string action = args.GetProperty("action").GetString();
                    return await ExecuteAction(dType, action, userId);
                }
                else if (functionName == "query_sensor")
                {
                    string sType = args.GetProperty("sensorType").GetString();
                    return await ExecuteQuery(sType, userId);
                }
            }
            
            if (part.TryGetProperty("text", out var responseText))
            {
                return new { message = responseText.GetString() };
            }
        }

        return new { message = "Tôi chưa hiểu ý bạn, hãy thử lại nhé!" };
    }

    private async Task<object> ExecuteAction(string deviceType, string action, string? userId)
    {
        var device = FindDeviceByType(deviceType, userId);
        if (device == null) return new { message = $"Không tìm thấy thiết bị {deviceType}." };

        var activationError = CheckDeviceActivation(device);
        if (activationError != null) return new { message = activationError };

        bool targetState = action == "on";
        
        // State awareness check
        if (deviceType == "Light") {
            bool currentState = GetLightState(device);
            if (currentState == targetState) {
                return new { message = targetState ? "Đèn đã bật rồi." : "Đèn đã tắt rồi." };
            }
        }

        string commandMethod = deviceType == "DoorLock" ? (targetState ? "unlock" : "lock") : "setLedStatus";
        object commandParams = deviceType == "DoorLock" ? new { } : (targetState ? 1 : 0);

        try
        {
            var command = JsonSerializer.Serialize(new { method = commandMethod, @params = commandParams });
            _thingsboardService.ControlDevice(device.ID, command);

            string msg = deviceType switch
            {
                "Light" => targetState ? "Đèn đã được bật." : "Đèn đã được tắt.",
                "DoorLock" => targetState ? "Cửa đã được mở khóa." : "Cửa đã được khóa lại.",
                _ => "Yêu cầu đã được thực hiện."
            };

            return new
            {
                action = action,
                deviceType = deviceType,
                message = msg,
                command = new { method = commandMethod, @params = commandParams }
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error executing action {Action} on {Type}", action, deviceType);
            return new { message = "Có lỗi xảy ra khi điều khiển thiết bị." };
        }
    }

    private async Task<object> ExecuteQuery(string sensorType, string? userId)
    {
        string tbType = sensorType == "PIR" ? "MotionSensor" : "TemperatureHumiditySensor";
        var device = FindDeviceByType(tbType, userId);
        if (device == null) return new { message = $"Không tìm thấy cảm biến {sensorType}." };

        string key = sensorType switch
        {
            "Temperature" => "temperature",
            "Humidity" => "humidity",
            "PIR" => "motion",
            _ => "status"
        };

        var val = GetLatestTelemetryValue(device.ID, key);
        if (!val.HasValue) return new { message = "Cảm biến chưa gửi dữ liệu." };

        string msg = sensorType switch
        {
            "Temperature" => $"Nhiệt độ hiện tại là {val:F1}°C.",
            "Humidity" => $"Độ ẩm hiện tại là {val:F1}%.",
            "PIR" => val > 0 ? "Phát hiện có người!" : "Hiện không có ai.",
            _ => "Dữ liệu cảm biến không khả dụng."
        };

        return new { message = msg };
    }

    /// <summary>
    /// Find first device by type for the user
    /// </summary>
    private Device? FindDeviceByType(string deviceType, string? userId)
    {
        try
        {
            IEnumerable<Device> devices;
            
            if (!string.IsNullOrEmpty(userId))
            {
                devices = _deviceService.GetDevicesByUserId(userId);
            }
            else
            {
                devices = _deviceService.GetDevices();
            }

            return devices.FirstOrDefault(d => d.Type != null && d.Type.Equals(deviceType, StringComparison.OrdinalIgnoreCase));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error finding device by type {DeviceType}", deviceType);
            return null;
        }
    }

    /// <summary>
    /// Get light on/off state from latest telemetry or status field
    /// </summary>
    private bool GetLightState(Device device)
    {
        try
        {
            // Try to get from telemetry first
            var ledStatus = GetLatestTelemetryValue(device.ID, "ledStatus");
            if (ledStatus.HasValue)
            {
                return ledStatus.Value > 0;
            }

            // Fallback to device Status field if exists
            if (!string.IsNullOrEmpty(device.Status))
            {
                return device.Status.Equals("on", StringComparison.OrdinalIgnoreCase) || 
                       device.Status.Equals("1", StringComparison.OrdinalIgnoreCase);
            }

            // Default to off if no data
            return false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting light state for device {DeviceId}", device.ID);
            return false;
        }
    }

    /// <summary>
    /// Get latest telemetry value for a specific key from JSON Body
    /// </summary>
    private double? GetLatestTelemetryValue(int deviceId, string key)
    {
        try
        {
            var telemetryData = _deviceService.GetTelemetryDataByDeviceId(deviceId);
            var latest = telemetryData
                .OrderByDescending(t => t.Timestamp)
                .FirstOrDefault();

            if (latest == null || string.IsNullOrEmpty(latest.Body))
                return null;

            // Parse JSON Body
            var jsonDoc = JsonDocument.Parse(latest.Body);
            if (jsonDoc.RootElement.TryGetProperty(key, out var property))
            {
                if (property.ValueKind == JsonValueKind.Number)
                {
                    return property.GetDouble();
                }
                else if (property.ValueKind == JsonValueKind.String && double.TryParse(property.GetString(), out var value))
                {
                    return value;
                }
            }

            return null;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting telemetry for device {DeviceId}, key {Key}", deviceId, key);
            return null;
        }
    }

    /// <summary>
    /// Kiểm tra xem device có thuộc house/room đã được kích hoạt không
    /// Returns null nếu OK, hoặc message lỗi nếu chưa kích hoạt
    /// </summary>
    private string? CheckDeviceActivation(Device device)
    {
        try
        {
            // Nếu device không thuộc room nào thì allow
            if (device.RoomID == null)
                return null;

            // Lấy thông tin Room
            var room = _roomService.GetRoomById((int)device.RoomID);
            if (room == null)
                return null;

            // Kiểm tra Room có được kích hoạt không
            if (!room.IsActive)
            {
                return $"Phòng {room.Name ?? "này"} chưa được kích hoạt. Vui lòng bật phòng trước.";
            }

            // Kiểm tra House có được kích hoạt không
            if (room.HouseID.HasValue)
            {
                var house = _houseService.GetHouseById(room.HouseID.Value);
                if (house != null && !house.IsActive)
                {
                    return $"Nhà {house.Name ?? "này"} chưa được kích hoạt. Vui lòng bật nhà trước.";
                }
            }

            return null; // OK, đã kích hoạt
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking device activation for device {DeviceId}", device.ID);
            return null; // Cho phép nếu không check được
        }
    }
}

public class AssistantRequest
{
    public string Text { get; set; } = "";
    public string? UserId { get; set; }
}
