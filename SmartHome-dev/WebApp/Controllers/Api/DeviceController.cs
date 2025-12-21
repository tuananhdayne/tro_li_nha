using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Services.Services;
using Services.Thingsboard_Services;
using System.Text.Json;
using WebApp.Utils;
using WebApp.Models;

namespace WebApp.Controllers.Api
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
    public class DeviceController : ControllerBase
    {
        private readonly IDeviceService _deviceService;
        private readonly IRoomService _roomService;
        private readonly IUserService _userService;
        private readonly IHouseService _houseService;
        private readonly IThingsboardService _thingsboardService;
        private readonly ILogger<DeviceController> _logger;

        public DeviceController(
            IDeviceService deviceService, 
            IRoomService roomService, 
            IUserService userService,
            IHouseService houseService, 
            IThingsboardService thingsboardService, 
            ILogger<DeviceController> logger)
        {
            _deviceService = deviceService;
            _roomService = roomService;
            _userService = userService;
            _houseService = houseService;
            _thingsboardService = thingsboardService;
            _logger = logger;
        }

        [HttpGet]
        public IActionResult GetDevices(int? roomId, int skip = 0, int take = 10)
        {
            try
            {
                var userId = _userService.GetCurrentUserId();
                List<Device> deviceList = new List<Device>();
                Dictionary<int, bool> deviceMap = new Dictionary<int, bool>();

                if (roomId != null)
                {
                    deviceList = _roomService.GetDevicesByRoomId((int)roomId).ToList();
                }
                else
                {
                    // User's own devices
                    var ownDevices = _deviceService.GetDevicesByUserId(userId).ToList();
                    foreach (var d in ownDevices) {
                        if (deviceMap.TryAdd(d.ID, true)) deviceList.Add(d);
                    }

                    // Devices from joined houses
                    var houses = _houseService.GetHousesByUserId(userId);
                    foreach (var house in houses)
                    {
                        var rooms = _houseService.GetRooms(house.ID);
                        foreach (var room in rooms)
                        {
                            var houseDevices = _roomService.GetDevicesByRoomId(room.ID);
                            foreach (var d in houseDevices)
                            {
                                if (deviceMap.TryAdd(d.ID, true)) deviceList.Add(d);
                            }
                        }
                    }
                }

                // Sync status before returning
                SyncDeviceStatus(deviceList);

                var result = deviceList.Skip(skip).Take(take)
                    .Select(d => new DeviceDTO
                    {
                        Id = d.ID,
                        Name = d.Name,
                        UserId = d.UserID,
                        Type = d.Type,
                        Status = d.Status,
                        MacAddress = d.MacAddress
                    })
                    .ToList();

                return Ok(new
                {
                    devices = result,
                    total = deviceList.Count,
                    skip = skip,
                    take = take
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error getting devices");
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("search")]
        public IActionResult SearchDevices(int? roomId, string keyword, int skip = 0, int take = 10)
        {
            try
            {
                var userId = _userService.GetCurrentUserId();
                List<Device> deviceList = new List<Device>();
                Dictionary<int, bool> deviceMap = new Dictionary<int, bool>();
                string kw = StringProcessHelper.RemoveDiacritics(keyword ?? "").ToLower();

                if (roomId != null)
                {
                    deviceList = _roomService.GetDevicesByRoomId((int)roomId)
                        .Where(d => StringProcessHelper.RemoveDiacritics(d.Name).ToLower().Contains(kw))
                        .ToList();
                }
                else
                {
                    var allDevices = _deviceService.GetDevicesByUserId(userId).ToList();
                    
                    // Houses
                    var houses = _houseService.GetHousesByUserId(userId);
                    foreach (var h in houses) {
                        foreach (var r in _houseService.GetRooms(h.ID)) {
                            allDevices.AddRange(_roomService.GetDevicesByRoomId(r.ID));
                        }
                    }

                    foreach (var d in allDevices) {
                        if (StringProcessHelper.RemoveDiacritics(d.Name).ToLower().Contains(kw)) {
                            if (deviceMap.TryAdd(d.ID, true)) deviceList.Add(d);
                        }
                    }
                }

                SyncDeviceStatus(deviceList);

                var result = deviceList.Skip(skip).Take(take)
                    .Select(d => new DeviceDTO
                    {
                        Id = d.ID,
                        Name = d.Name,
                        UserId = d.UserID,
                        Type = d.Type,
                        Status = d.Status,
                        MacAddress = d.MacAddress
                    })
                    .ToList();

                return Ok(new { devices = result, total = deviceList.Count, skip = skip, take = take });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error searching devices");
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPut("{id}")]
        public IActionResult UpdateDevice(int id, [FromBody] Device device)
        {
            try
            {
                if (!_deviceService.IsDeviceOwner(_userService.GetCurrentUserId(), id))
                    return Forbid();

                var existing = _deviceService.GetDeviceById(id);
                if (existing == null) return NotFound();

                if (!string.IsNullOrWhiteSpace(device.Name))
                    existing.Name = device.Name;
                
                if (device.RoomID != null)
                    existing.RoomID = device.RoomID;

                _deviceService.EditDevice(existing);
                _thingsboardService.UpdateDevice(existing);
                
                return Ok(existing);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating device {DeviceId}", id);
                return StatusCode(500, new { message = "Internal server error" });
            }
        }
        
        [HttpPost("anonymous")]
        [AllowAnonymous]
        public IActionResult AddDevAno(int id, [FromBody] Device device)
        {
            try
            {
                _logger.LogInformation("Adding anonymous device: {@Device}", device.ToString());
                var deviceCreated = _deviceService.CreateDevice(device);
                return Ok(deviceCreated);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating device");
                return StatusCode(500, new { message = "Error while creating device", details = ex.Message });
            }
        }

        /// <summary>
        /// Auto-create 4 ESP32 devices (Light, DoorLock, TempSensor, Motion) with same MAC address
        /// </summary>
        [HttpPost("create-esp32-kit")]
        public IActionResult CreateEsp32Kit([FromBody] CreateEsp32KitRequest request)
        {
            try
            {
                if (string.IsNullOrEmpty(request.MacAddress))
                    return BadRequest(new { message = "MAC Address is required" });

                if (request.RoomId <= 0)
                    return BadRequest(new { message = "Room ID is required" });

                // Check if room exists
                var room = _roomService.GetRoomById(request.RoomId);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Normalize MAC: remove colons and uppercase
                var normalizedMac = request.MacAddress.Replace(":", "").ToUpper();
                request.MacAddress = normalizedMac;

                // Check if devices with this MAC already exist
                var existingDevices = _deviceService.GetDevicesByMacAddress(normalizedMac);
                if (existingDevices.Any())
                    return BadRequest(new { message = $"Devices with MAC {request.MacAddress} already exist. Delete them first." });

                var userId = _userService.GetCurrentUserId();
                var deviceTypes = new[] { "Light", "DoorLock", "TemperatureHumiditySensor", "MotionSensor" };
                var createdDevices = new List<CreatedDeviceInfo>();
                var namePrefix = string.IsNullOrEmpty(request.NamePrefix) ? "ESP32" : request.NamePrefix;
                _logger.LogInformation("Creating ESP32 Kit: MAC={Mac}, Room={RoomId}, NamePrefix={Prefix}", 
                    normalizedMac, request.RoomId, namePrefix);

                foreach (var deviceType in deviceTypes)
                {
                    _logger.LogInformation("Creating device type: {Type} for MAC: {Mac}", deviceType, request.MacAddress);
                    
                    // Generate unique token
                    var token = Guid.NewGuid().ToString("N").Substring(0, 20);
                    
                    // Create device object
                    var device = new Device
                    {
                        Name = $"{namePrefix}_{request.MacAddress}_{deviceType}",
                        Type = deviceType,
                        DeviceToken = token,
                        MacAddress = request.MacAddress,
                        UserID = userId,
                        RoomID = request.RoomId,
                        Status = "Active"
                    };

                    // Create on ThingsBoard first
                    try
                    {
                        _logger.LogInformation("Contacting ThingsBoard for device: {Name}", device.Name);
                        var tbResponse = _thingsboardService.CreateDevice(device);
                        if (tbResponse != null)
                        {
                            // Parse TbDeviceId from response
                            var jsonString = tbResponse.ToString();
                            using var doc = JsonDocument.Parse(jsonString);
                            if (doc.RootElement.TryGetProperty("id", out var idElement) &&
                                idElement.TryGetProperty("id", out var tbIdElement))
                            {
                                device.TbDeviceId = tbIdElement.GetString();
                                _logger.LogInformation("ThingsBoard device created: {TbId}", device.TbDeviceId);
                            }
                        }
                        else
                        {
                            _logger.LogWarning("ThingsBoard returned null for device: {Name}", device.Name);
                        }
                    }
                    catch (Exception tbEx)
                    {
                        _logger.LogError(tbEx, "ThingsBoard error for device {Type} (MAC: {Mac})", deviceType, request.MacAddress);
                    }

                    // Save to database
                    _logger.LogInformation("Saving device to local database: {Name}", device.Name);
                    var createdDevice = _deviceService.CreateDevice(device);

                    // Add to room
                    _logger.LogInformation("Adding device {Id} to room {RoomId}", createdDevice.ID, request.RoomId);
                    _roomService.AddDeviceToRoom(request.RoomId, createdDevice);

                    createdDevices.Add(new CreatedDeviceInfo
                    {
                        Id = createdDevice.ID,
                        Name = createdDevice.Name,
                        Type = createdDevice.Type,
                        DeviceToken = createdDevice.DeviceToken,
                        TbDeviceId = createdDevice.TbDeviceId
                    });
                }

                _logger.LogInformation("Created ESP32 Kit with MAC {Mac}: {Count} devices", 
                    request.MacAddress, createdDevices.Count);

                return Ok(new CreateEsp32KitResponse
                {
                    Success = true,
                    Message = $"Successfully created {createdDevices.Count} devices for MAC {request.MacAddress}",
                    Devices = createdDevices
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating ESP32 kit");
                return StatusCode(500, new { message = "Error creating ESP32 kit", details = ex.Message });
            }
        }

        /// <summary>
        /// ESP32 Manual Provisioning: ESP32 gửi MAC address, server tìm devices đã tạo sẵn và trả về tokens
        /// </summary>
        [HttpPost("esp32/provision")]
        [AllowAnonymous]
        public IActionResult Esp32Provision([FromBody] Esp32ProvisionRequest request)
        {
            try
            {
                // Normalize MAC: remove colons and uppercase
                var normalizedMac = request.MacAddress.Replace(":", "").ToUpper();
                _logger.LogInformation("ESP32 Provisioning request from MAC: {OriginalMac} -> Normalized: {NormalizedMac}", 
                    request.MacAddress, normalizedMac);

                // Tìm devices theo MacAddress
                var devices = _deviceService.GetDevicesByMacAddress(normalizedMac).ToList();
                
                if (!devices.Any())
                {
                    _logger.LogWarning("No devices found for MAC: {MacAddress}", request.MacAddress);
                    return NotFound(new 
                    { 
                        success = false, 
                        message = "Devices not found. Please create devices in WebApp first.",
                        macAddress = request.MacAddress
                    });
                }

                // Validate có đủ 4 device types
                var requiredTypes = new[] { "Light", "DoorLock", "TemperatureHumiditySensor", "MotionSensor" };
                var foundTypes = devices.Select(d => d.Type).ToHashSet();
                var missingTypes = requiredTypes.Where(t => !foundTypes.Contains(t)).ToList();

                if (missingTypes.Any())
                {
                    _logger.LogWarning("Missing device types for MAC {MacAddress}: {MissingTypes}", 
                        request.MacAddress, string.Join(", ", missingTypes));
                    return BadRequest(new 
                    { 
                        success = false, 
                        message = $"Missing device types: {string.Join(", ", missingTypes)}. Please create all 4 device types in WebApp.",
                        macAddress = request.MacAddress,
                        missingTypes = missingTypes
                    });
                }

                // Validate TbDeviceId không NULL
                var devicesWithoutTbId = devices.Where(d => string.IsNullOrEmpty(d.TbDeviceId)).ToList();
                if (devicesWithoutTbId.Any())
                {
                    _logger.LogError("Devices without TbDeviceId for MAC {MacAddress}: {DeviceNames}", 
                        request.MacAddress, string.Join(", ", devicesWithoutTbId.Select(d => d.Name)));
                    return StatusCode(500, new 
                    { 
                        success = false, 
                        message = "Devices not registered on ThingsBoard. Please recreate devices in WebApp.",
                        macAddress = request.MacAddress,
                        devicesWithoutTbId = devicesWithoutTbId.Select(d => d.Name).ToList()
                    });
                }

                // Trả về tokens
                var response = new Esp32ProvisionResponse
                {
                    Success = true,
                    Message = "Provisioned successfully",
                    MacAddress = request.MacAddress,
                    Devices = devices.Select(d => new Esp32DeviceInfo
                    {
                        Type = d.Type,
                        DeviceToken = d.DeviceToken,
                        DeviceId = d.ID
                    }).ToList()
                };

                _logger.LogInformation("ESP32 {MacAddress} provisioned with {DeviceCount} devices", 
                    request.MacAddress, devices.Count);

                // Dùng clean options để ESP32 không bị dính $id/$values từ ReferenceHandler.Preserve của hệ thống
                var cleanOptions = new System.Text.Json.JsonSerializerOptions 
                { 
                    PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase,
                    ReferenceHandler = null 
                };
                string jsonString = System.Text.Json.JsonSerializer.Serialize(response, cleanOptions);
                
                _logger.LogInformation("Sending provisioning response to {Mac}: {Json}", request.MacAddress, jsonString);
                return Content(jsonString, "application/json");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "ESP32 Provisioning failed");
                return StatusCode(500, new { success = false, message = "Provisioning failed", details = ex.Message });
            }
        }

        private string GenerateDeviceToken()
        {
            var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            var random = new Random();
            return new string(Enumerable.Repeat(chars, 12)
                .Select(s => s[random.Next(s.Length)]).ToArray());
        }

        [HttpDelete("{id}")]
        public IActionResult DeleteDevice(int id)
        {
            try
            {
                if (!_deviceService.IsDeviceOwner(_userService.GetCurrentUserId(), id))
                    return Forbid();

                _thingsboardService.DeleteDevice(id);
                _deviceService.DeleteDevice(id);
                return Ok(new { message = "Device deleted successfully" });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error deleting device {DeviceId}", id);
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        private void SyncDeviceStatus(IEnumerable<Device> devices)
        {
            foreach (var device in devices)
            {
                if (device.Type == "Light" || device.Type == "DoorLock")
                {
                    try
                    {
                        var telemetry = _thingsboardService.GetLatestTelemetry(device.TbDeviceId);
                        if (telemetry.ValueKind != JsonValueKind.Undefined)
                        {
                            string key = device.Type == "Light" ? "ledStatus" : "isLocked";
                            if (telemetry.TryGetProperty(key, out var prop) && prop.ValueKind == JsonValueKind.Array && prop.GetArrayLength() > 0)
                            {
                                var val = prop[0].GetProperty("value");
                                bool isOn = false;

                                if (val.ValueKind == JsonValueKind.True) isOn = true;
                                else if (val.ValueKind == JsonValueKind.String)
                                {
                                    var s = val.GetString()?.ToLower();
                                    isOn = (s == "true" || s == "1" || s == "on");
                                }
                                else if (val.ValueKind == JsonValueKind.Number) isOn = val.GetDouble() > 0;

                                if (device.Type == "DoorLock") isOn = !isOn; // true => false (khóa => off), false => true (mở => on)

                                device.Status = isOn ? "on" : "off";
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning("Failed to sync status for {Name}: {Msg}", device.Name, ex.Message);
                    }
                }
            }
        }

        [HttpPost("{id}/control")]
        public IActionResult ControlDevice(int id, [FromBody] DeviceControlRequest request)
        {
            try
            {
                if (!_deviceService.IsDeviceOwner(_userService.GetCurrentUserId(), id))
                    return Forbid();

                _thingsboardService.ControlDevice(id, request.Command);
                return Ok(new { message = "Device control command sent successfully" });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error controlling device {DeviceId}", id);
                return StatusCode(500, new { message = "Error while controlling device", details = ex.Message });
            }
        }
    }

    public class DeviceControlRequest
    {
        public string Command { get; set; }
    }

    public class Esp32ProvisionRequest
    {
        public string MacAddress { get; set; }
        public string? FirmwareVersion { get; set; }
    }

    public class Esp32ProvisionResponse
    {
        public bool Success { get; set; }
        public string Message { get; set; }
        public string MacAddress { get; set; }
        public List<Esp32DeviceInfo> Devices { get; set; }
    }

    public class Esp32DeviceInfo
    {
        public string Type { get; set; }
        public string DeviceToken { get; set; }
        public int DeviceId { get; set; }
    }

    public class CreateEsp32KitRequest
    {
        public string MacAddress { get; set; }
        public int RoomId { get; set; }
        public string? NamePrefix { get; set; } = "ESP32";
    }

    public class CreateEsp32KitResponse
    {
        public bool Success { get; set; }
        public string Message { get; set; }
        public List<CreatedDeviceInfo> Devices { get; set; }
    }

    public class CreatedDeviceInfo
    {
        public int Id { get; set; }
        public string Name { get; set; }
        public string Type { get; set; }
        public string DeviceToken { get; set; }
        public string TbDeviceId { get; set; }
    }
}
