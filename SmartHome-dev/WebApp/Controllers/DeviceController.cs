using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Services.Services;
using Services.Thingsboard_Services;
using System.Text.Json;
using WebApp.Utils;

namespace WebApp.Controllers;

public class DeviceController : Controller
{
    private readonly IDeviceService _deviceService;
    private readonly IRoomService _roomService;
    private readonly IUserService _userService;
    private readonly IHouseService _houseService;
    private readonly IThingsboardService _thingsboardService;
    private readonly ILogger<DeviceController> _logger;

    public DeviceController(IDeviceService deviceService, IRoomService roomService, IUserService userService,
        IHouseService houseService, IThingsboardService thingsboardService, ILogger<DeviceController> logger)
    {
        _deviceService = deviceService;
        _roomService = roomService;
        _userService = userService;
        _houseService = houseService;
        _thingsboardService = thingsboardService;
        _logger = logger;
    }

    [Authorize]
    public IActionResult Index(int? roomId)
    {
        List<Device> deviceList;
        if (roomId != null)
        {
            var room = _roomService.GetRoomById((int)roomId);
            deviceList = _roomService.GetDevicesByRoomId((int)roomId).ToList();

            ViewBag.RoomId = roomId;
            ViewBag.RoomName = room.Name;
        }
        else
        {
            deviceList = _deviceService.GetDevicesByUserId(_userService.GetCurrentUserId()).ToList();
            
            // get devices from joined house's rooms
            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
            foreach (var house in houses)
            {
                var rooms = _houseService.GetRooms(house.ID);
                foreach (var room in rooms)
                {
                    var houseDevices = _roomService.GetDevicesByRoomId(room.ID).ToList();
                    foreach (var device in houseDevices)
                    {
                        if (!deviceList.Any(d => d.ID == device.ID))
                        {
                            deviceList.Add(device);
                        }
                    }
                }
            }
        }

        // Populate rooms for modals
        var currentUserHouses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
        var allUserRooms = new List<Room>();
        foreach (var house in currentUserHouses)
        {
            allUserRooms.AddRange(_houseService.GetRooms(house.ID));
        }
        ViewBag.Rooms = allUserRooms;

        SyncDeviceStatus(deviceList);

        return View(deviceList.Take(10).ToList());
    }

    [Authorize]
    public IActionResult LoadMoreDevices(int? roomId, int skip, int take)
    {
        List<Device> deviceList;
        if (roomId != null)
        {
            var room = _roomService.GetRoomById((int)roomId);
            deviceList = _roomService.GetDevicesByRoomId((int)roomId).ToList();
        }
        else
        {
            deviceList = _deviceService.GetDevicesByUserId(_userService.GetCurrentUserId()).ToList();
            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
            foreach (var house in houses)
            {
                var rooms = _houseService.GetRooms(house.ID);
                foreach (var room in rooms)
                {
                    var houseDevices = _roomService.GetDevicesByRoomId(room.ID).ToList();
                    foreach (var device in houseDevices)
                    {
                        if (!deviceList.Any(d => d.ID == device.ID))
                        {
                            deviceList.Add(device);
                        }
                    }
                }
            }
        }
        
        SyncDeviceStatus(deviceList);
        
        return PartialView("DeviceList", deviceList.Skip(skip).Take(take).ToList());
    }

    [Authorize]
    public IActionResult Search(int? roomId, string keyword)
    {
        List<Device> deviceList;
        if (roomId != null)
        {
            deviceList = _roomService.GetDevicesByRoomId((int)roomId)
                .Where(d => StringProcessHelper.RemoveDiacritics(d.Name).ToLower()
                .Contains(StringProcessHelper.RemoveDiacritics(keyword).ToLower())).ToList();
        }
        else
        {
            deviceList = _deviceService.GetDevicesByUserId(_userService.GetCurrentUserId())
                .Where(d => StringProcessHelper.RemoveDiacritics(d.Name).ToLower()
                .Contains(StringProcessHelper.RemoveDiacritics(keyword).ToLower())).ToList();

            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
            foreach (var house in houses)
            {
                var rooms = _houseService.GetRooms(house.ID);
                foreach (var room in rooms)
                {
                    var houseDevices = _roomService.GetDevicesByRoomId(room.ID)
                        .Where(d => StringProcessHelper.RemoveDiacritics(d.Name).ToLower()
                        .Contains(StringProcessHelper.RemoveDiacritics(keyword).ToLower())).ToList();
                    foreach (var device in houseDevices)
                    {
                        if (!deviceList.Any(d => d.ID == device.ID))
                        {
                            deviceList.Add(device);
                        }
                    }
                }
            }
        }
        
        SyncDeviceStatus(deviceList);
        
        return PartialView("DeviceList", deviceList.Take(10).ToList());
    }

    private void SyncDeviceStatus(IEnumerable<Device> devices)
    {
        foreach (var device in devices)
        {
            if (device.Type == "Light" || device.Type == "DoorLock")
            {
                try {
                    var telemetry = _thingsboardService.GetLatestTelemetry(device.TbDeviceId);
                    if (telemetry.ValueKind != JsonValueKind.Undefined) {
                        string key = device.Type == "Light" ? "ledStatus" : "isLocked";
                        if (telemetry.TryGetProperty(key, out var prop) && prop.ValueKind == JsonValueKind.Array && prop.GetArrayLength() > 0) {
                            var val = prop[0].GetProperty("value");
                            bool isOn = false;
                            
                            if (val.ValueKind == JsonValueKind.True) isOn = true;
                            else if (val.ValueKind == JsonValueKind.String) {
                                var s = val.GetString()?.ToLower();
                                isOn = (s == "true" || s == "1" || s == "on");
                            }
                            else if (val.ValueKind == JsonValueKind.Number) isOn = val.GetDouble() > 0;

                            if (device.Type == "DoorLock") isOn = !isOn; 

                            device.Status = isOn ? "on" : "off";
                            _logger.LogInformation("Synced {Name} ({Type}) to {Status}. MAC: {Mac}", 
                                device.Name, device.Type, device.Status, device.MacAddress);
                        }
                    }
                } catch (Exception ex) { 
                    _logger.LogWarning("Failed to sync status for {Name}: {Msg}", device.Name, ex.Message);
                }
            }
        }
    }

    [HttpPost]
    public IActionResult Create(Device device)
    {
        var tempDevice = device;
        tempDevice.Name = StringProcessHelper.RemoveDiacritics(device.Name);
        try
        {
            var tbDevice = _thingsboardService.CreateDevice(tempDevice);
            Console.WriteLine("\u001b[32m" + tbDevice.ToString() + "\u001b[0m");
            var root = JsonDocument.Parse(tbDevice.ToString()).RootElement;
            device.TbDeviceId = root.GetProperty("id").GetProperty("id").GetString();
            var deviceCreated = _deviceService.CreateDevice(device);
        }
        catch (Exception e)
        {
            ModelState.AddModelError("Error", e.Message);
            _logger.LogError(e, "Error while creating device");
            return StatusCode(400, new { message = "Error while creating device", details = e.Message });
        }
        return RedirectToAction("Index");
    }

    [Authorize]
    public IActionResult Edit(int id)
    {
        var device = _deviceService.GetDeviceById(id);
        if (!_deviceService.IsDeviceOwner(_userService.GetCurrentUserId(), id))
        {
            return RedirectToAction("AccessDenied", "Account");
        }
        return View(device);
    }

    [HttpPost]
    public IActionResult Edit(Device device)
    {
        var existing = _deviceService.GetDeviceById(device.ID);
        if (existing != null) {
            existing.Name = device.Name;
            if (device.RoomID != null)
                existing.RoomID = device.RoomID;
                
            _deviceService.EditDevice(existing);
            _thingsboardService.UpdateDevice(existing);
        }
        return RedirectToAction("Index");
    }

    public IActionResult Delete(int id)
    {
        if (!_deviceService.IsDeviceOwner(_userService.GetCurrentUserId(), id))
        {
            return RedirectToAction("AccessDenied", "Account");
        }
        
        try {
            _thingsboardService.DeleteDevice(id);
        } catch { /* Ignore TB deletion errors if already deleted */ }
        
        _deviceService.DeleteDevice(id);
        return RedirectToAction("Index");
    }

    [HttpPost]
    public IActionResult Control(int id, string command)
    {
        try
        {
            _thingsboardService.ControlDevice(id, command);
        }
        catch (UnauthorizedAccessException e)
        {
            _logger.LogError(e, "UnauthorizedAccessException: {Message}", e.Message);
            return StatusCode(401, new { message = "Unauthorized access while controlling device", details = e.Message });
        }
        catch (ArgumentException e)
        {
            _logger.LogError(e, "ArgumentException: {Message}", e.Message);
            return StatusCode(400, new { message = "Invalid argument while controlling device", details = e.Message });
        }
        catch (TimeoutException e)
        {
            _logger.LogError(e, "TimeoutException: {Message}", e.Message);
            return StatusCode(504, new { message = "Request timed out while controlling device", details = e.Message });
        }
        catch (KeyNotFoundException e)
        {
            _logger.LogError(e, "KeyNotFoundException: {Message}", e.Message);
            return StatusCode(404, new { message = "Device not registered", details = e.Message });
        }
        catch (Exception e)
        {
            _logger.LogError(e, "An unexpected error occurred: {Message}", e.Message);
            return StatusCode(500, new { message = "An unexpected error occurred while controlling device", details = "Please check the server logs for more details." });
        }
        return Ok();
    }
}