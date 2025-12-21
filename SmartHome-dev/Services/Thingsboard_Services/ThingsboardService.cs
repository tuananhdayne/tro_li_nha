using Configuration;
using DAO.BaseModels;
using DAO.Models.Devices;
using Microsoft.Extensions.Logging;
using Services.Services;
using Services.Thingsboard_Services.BaseModel;
using System.Text.Json;

namespace Services.Thingsboard_Services;

public class ThingsboardService : IThingsboardService
{
    private Token _adminToken;
    private readonly IDeviceService _deviceService;
    private readonly ILogger<ThingsboardService> _logger;

    public ThingsboardService(IDeviceService deviceService, ILogger<ThingsboardService> logger)
    {
        _deviceService = deviceService;
        _logger = logger;
        _adminToken = GetAdminToken();
    }
    private string GetUrl(string path)
    {
        return SystemConfiguration.ThingsboardServer.TrimEnd('/') + "/" + path.TrimStart('/');
    }

    public Token? Login(Account account)
    {
        return new Request<Token>(GetUrl("api/auth/login"), "{\"username\":\"" + account.Username + "\",\"password\":\"" + account.Password + "\"}", null).Post();
    }

    public Token GetAdminToken()
    {
        if (_adminToken != null) return _adminToken;
        return new Request<Token>(GetUrl("api/auth/login"), "{\"username\":\"" + SystemConfiguration.AdminUsername + "\",\"password\":\"" + SystemConfiguration.AdminPassword + "\"}", null).Post()!;
    }

    public object CreateCustomerAccount(Account account)
    {
        throw new NotImplementedException();
    }

    public object? CreateDevice(Device device)
    {
        var deviceData = new
        {
            device = new
            {
                name = device.Name,
                label = "",
                additionalInfo = new
                {
                    gateway = false,
                    description = "",
                    overwriteActivityTime = false
                }
            },
            credentials = new
            {
                credentialsType = "ACCESS_TOKEN",
                credentialsId = device.DeviceToken
            }
        };
        string jsonData = JsonSerializer.Serialize(deviceData, new JsonSerializerOptions { WriteIndented = true });
        return new Request<object?>(GetUrl("api/device-with-credentials"), jsonData, _adminToken).Post();
    }

    public object? DeleteDevice(int deviceId)
    {
        var temp = _deviceService.GetDeviceById(deviceId);
        if (temp == null) return null;
        try
        {
            return new Request<object?>(GetUrl($"api/device/{temp.TbDeviceId}"), null, _adminToken).Delete();
        }
        catch (HttpRequestException e)
        {
            Console.WriteLine(e);
            throw;
        }
        catch (UnauthorizedAccessException e)
        {
            _logger.LogError(e, "UnauthorizedAccessException: {Message}", e.Message);
            throw new UnauthorizedAccessException("Unauthorized");
        }
        catch (ArgumentException e)
        {
            _logger.LogError(e, "ArgumentException: {Message}", e.Message);
            throw new ArgumentException("Device already registered with this device token");
        }
        catch (TimeoutException e)
        {
            _logger.LogError(e, "TimeoutException: {Message}", e.Message);
            throw new TimeoutException("Device is offline");
        }
        catch (KeyNotFoundException e)
        {
            _logger.LogError(e, "KeyNotFoundException: {Message}", e.Message);
            throw new KeyNotFoundException("Device not found");
        }
    }

    public object AssignDeviceToCustomer(string deviceId, string customerId)
    {
        throw new NotImplementedException();
    }

    public object? ControlDevice(int deviceId, string command)
    {
        var temp = _deviceService.GetDeviceById(deviceId);
        var targetTbDeviceId = temp.TbDeviceId;

        // Routing Logic: Nếu thiết bị có MAC (thuộc bộ kit ESP32), 
        // ta luôn gửi lệnh về "Thiết bị Đèn" của bộ MAC đó vì ESP32 đang lắng nghe qua token của Đèn.
        if (!string.IsNullOrEmpty(temp.MacAddress))
        {
            var kitDevices = _deviceService.GetDevicesByMacAddress(temp.MacAddress);
            var lightDevice = kitDevices.FirstOrDefault(d => d.Type == "Light");
            if (lightDevice != null && !string.IsNullOrEmpty(lightDevice.TbDeviceId))
            {
                targetTbDeviceId = lightDevice.TbDeviceId;
                _logger.LogInformation("Routing command for {Type} (MAC: {Mac}) to Master device (Light) TB-ID: {Id}", 
                    temp.Type, temp.MacAddress, targetTbDeviceId);
            }
        }

        try
        {
            string rpcMethod;
            object rpcParams;

            // Choose correct RPC method based on device type and command
            if (temp.Type == "Light")
            {
                rpcMethod = "setLedStatus";
                rpcParams = command.ToLower() == "on" ? 1 : 0;
            }
            else if (temp.Type == "DoorLock")
            {
                rpcMethod = command.ToLower() == "on" ? "lock" : "unlock";
                rpcParams = new { }; // Empty object for lock/unlock
            }
            else
            {
                // For other types, fallback to setState but firmware might not support it
                rpcMethod = "setState";
                rpcParams = command.ToLower() == "on";
            }

            var rpcRequest = new
            {
                method = rpcMethod,
                @params = rpcParams
            };
            // Use clean options to avoid ReferenceHandler.Preserve metadata ($id) that TB rejects
            var cleanOptions = new JsonSerializerOptions();
            string jsonBody = JsonSerializer.Serialize(rpcRequest, cleanOptions);

            _logger.LogInformation("Sending RPC to {Url}: {Body}", GetUrl($"api/rpc/oneway/{targetTbDeviceId}"), jsonBody);

            var response = new Request<object?>(
                GetUrl($"api/rpc/oneway/{targetTbDeviceId}"),
                jsonBody,
                _adminToken).Post();

            TelemetryData telemetryData = new()
            {
                DeviceID = temp.ID,
                Body = command
            };
            _deviceService.AddTelemetryDatum(telemetryData);
            return response;
        }
        catch (UnauthorizedAccessException e)
        {
            _logger.LogError(e, "UnauthorizedAccessException: {Message}", e.Message);
            throw new UnauthorizedAccessException("Unauthorized");
        }
        catch (ArgumentException e)
        {
            _logger.LogError(e, "ArgumentException: {Message}", e.Message);
            throw new ArgumentException("Invalid RPC request or device ID");
        }
        catch (TimeoutException e)
        {
            _logger.LogError(e, "TimeoutException: {Message}", e.Message);
            throw new TimeoutException("Device is offline");
        }
        catch (KeyNotFoundException e)
        {
            _logger.LogError(e, "KeyNotFoundException: {Message}", e.Message);
            throw new KeyNotFoundException("Device not found");
        }
    }

    public object? ControlDevice(int deviceId, object command)
    {
        var temp = _deviceService.GetDeviceById(deviceId);
        try
        {
            var cleanOptions = new JsonSerializerOptions();
            var response = new Request<object?>(
                GetUrl($"api/rpc/oneway/{temp.TbDeviceId}"),
                JsonSerializer.Serialize(command, cleanOptions),
                _adminToken).Post();

            TelemetryData telemetryData = new()
            {
                DeviceID = temp.ID,
                Body = JsonSerializer.Serialize(command, cleanOptions)
            };
            _deviceService.AddTelemetryDatum(telemetryData);
            return response;
        }
        catch (UnauthorizedAccessException e)
        {
            _logger.LogError(e, "UnauthorizedAccessException: {Message}", e.Message);
            throw new UnauthorizedAccessException("Unauthorized");
        }
        catch (ArgumentException e)
        {
            _logger.LogError(e, "ArgumentException: {Message}", e.Message);
            throw new ArgumentException("Device already registered with this device token");
        }
        catch (TimeoutException e)
        {
            _logger.LogError(e, "TimeoutException: {Message}", e.Message);
            throw new TimeoutException("Device is offline");
        }
        catch (KeyNotFoundException e)
        {
            _logger.LogError(e, "KeyNotFoundException: {Message}", e.Message);
            throw new KeyNotFoundException("Device not found");
        }
    }

    private object decodeControlCommand(Device device, string command, int? dim = null, int? R = null, int? G = null, int? B = null)
    {
        switch (command)
        {
            case "turnOn":
                return device switch
                {
                    Light l => l.TurnOn(),
                    DoorLock door => door.Unlock(),
                    _ => null
                };
            case "turnOff":
                return device switch
                {
                    Light l => l.TurnOff(),
                    DoorLock door => door.Lock(),
                    _ => null
                };
            case "setDim":
                return device is Light light ? light.SetDim(dim.Value) : null;
            case "lock":
                return device is DoorLock doorLock ? doorLock.Lock() : null;
            case "unlock":
                return device is DoorLock d ? d.Unlock() : null;
            case "getTemperature":
                return device is TemperatureHumiditySensor temp ? temp.GetTemperature() : null;
            case "getHumidity":
                return device is TemperatureHumiditySensor hum ? hum.GetHumidity() : null;
            case "getMotionStatus":
                return device is MotionSensor motion ? motion.GetMotionStatus() : null;
            default:
                return null;
        }
    }

    public object? ControlDevice(int deviceId, string command, int? dim = null, int? R = null, int? G = null, int? B = null)
    {
        var temp = _deviceService.GetDeviceById(deviceId);
        if (temp == null) return null;
        object controlCommand = decodeControlCommand(temp, command, dim, R, G, B);
        string jsonData = JsonSerializer.Serialize(controlCommand, new JsonSerializerOptions { WriteIndented = true });
        return new Request<object?>(GetUrl($"api/rpc/oneway/{deviceId}"), jsonData,
            _adminToken).Post();
    }
    public JsonElement GetLatestTelemetry(string tbDeviceId)
    {
        try
        {
            string url = GetUrl($"api/plugins/telemetry/DEVICE/{tbDeviceId}/values/timeseries");
            var request = new Request<JsonElement>(url, null, _adminToken);
            var result = request.GetAsync().GetAwaiter().GetResult();
            
            // Log raw response for debugging sync issues
            _logger.LogInformation("Latest telemetry for TB-ID {TbDeviceId}: {Raw}", tbDeviceId, result.ToString());
            
            return result;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting latest telemetry for {TbDeviceId}", tbDeviceId);
            return default;
        }
    }

    public object? UpdateDevice(Device device)
    {
        try
        {
            var deviceData = new
            {
                id = new { id = device.TbDeviceId, entityType = "DEVICE" },
                name = device.Name,
                type = device.Type,
                label = device.Name
            };
            string jsonData = JsonSerializer.Serialize(deviceData);
            return new Request<object?>(GetUrl("api/device"), jsonData, _adminToken).Post();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating device {TbDeviceId} on ThingsBoard", device.TbDeviceId);
            return null;
        }
    }
}