using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Services.Services;
using WebApp.Utils;

namespace WebApp.Controllers.Api
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
    public class RoomController : ControllerBase
    {
        private readonly IRoomService _roomService;
        private readonly IHouseService _houseService;
        private readonly IUserService _userService;
        private readonly IDeviceService _deviceService;

        public RoomController(IRoomService roomService, IHouseService houseService, IUserService userService, IDeviceService deviceService)
        {
            _roomService = roomService;
            _houseService = houseService;
            _userService = userService;
            _deviceService = deviceService;
        }

        [HttpGet]
        [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
        public IActionResult GetRooms(int? houseId, int skip = 0, int take = 100)
        {
            try
            {
                var userId = _userService.GetCurrentUserId();
                var housesWithRooms = new Dictionary<int, IEnumerable<Room>>();

                if (houseId != null)
                {
                    var house = _houseService.GetHouseById((int)houseId);
                    if (house == null)
                        return NotFound(new { message = "House not found" });

                    // Check if user has access to this house
                    var houseMembers = _houseService.GetHouseMembers((int)houseId);
                    if (!houseMembers.Any(hm => hm.UserID == userId))
                        return Forbid();

                    var rooms = _houseService.GetRooms((int)houseId)
                        .Skip(skip)
                        .Take(take)
                        .ToList();
                    housesWithRooms[house.ID] = rooms;
                }
                else
                {
                    var houses = _houseService.GetHousesByUserId(userId);
                    foreach (var house in houses)
                    {
                        var rooms = _houseService.GetRooms(house.ID)
                            .Skip(skip)
                            .Take(take)
                            .ToList();
                        housesWithRooms[house.ID] = rooms;
                    }
                }

                return Ok(new
                {
                    housesWithRooms = housesWithRooms,
                    skip = skip,
                    take = take
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error", error = ex.Message });
            }
        }


        [HttpGet("search")]
        public IActionResult SearchRooms(int? houseId, string keyword)
        {
            try
            {
                var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
                var roomsWithHouse = new Dictionary<int, IEnumerable<Room>>();
                
                foreach (var house in houses)
                {
                    var rooms = _houseService.GetRooms(house.ID)
                        .Where(r => StringProcessHelper.RemoveDiacritics(r.Name).ToLower()
                            .Contains(StringProcessHelper.RemoveDiacritics(keyword).ToLower()))
                        .ToList();
                    roomsWithHouse[house.ID] = rooms;
                }

                return Ok(new { roomsWithHouse = roomsWithHouse });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("{id}")]
        public IActionResult GetRoom(int id)
        {
            try
            {
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Check if user has access to this room's house
                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                return Ok(room);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPost]
        public IActionResult CreateRoom([FromBody] CreateRoomRequest request)
        {
            try
            {
                if (!ModelState.IsValid)
                    return BadRequest(ModelState);

                // Check if user is owner of the house
                var houseMembers = _houseService.GetHouseMembers(request.HouseId);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId() && hm.Role == "Owner"))
                    return Forbid();

                var newRoom = _houseService.AddRoomToHouse(request.HouseId, new Room { Name = request.Name, Detail = request.Detail });
                
                return CreatedAtAction(nameof(GetRoom), new { id = newRoom.ID }, newRoom);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPut("{id}")]
        public IActionResult UpdateRoom(int id, [FromBody] Room room)
        {
            try
            {
                if (!ModelState.IsValid)
                    return BadRequest(ModelState);

                // Check if user is owner of the house
                var existingRoom = _roomService.GetRoomById(id);
                if (existingRoom == null)
                    return NotFound(new { message = "Room not found" });

                var houseMembers = _houseService.GetHouseMembers((int)existingRoom.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId() && hm.Role == "Owner"))
                    return Forbid();

                room.ID = id;
                _roomService.EditRoom(room);

                return Ok(room);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("{id}")]
        public IActionResult DeleteRoom(int id)
        {
            try
            {
                // Check if user is owner of the house
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId() && hm.Role == "Owner"))
                    return Forbid();

                _roomService.DeleteRoom(id);
                return Ok(new { message = "Room deleted successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("{id}/devices")]
        public IActionResult GetRoomDevices(int id)
        {
            try
            {
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Check if user has access to this room's house
                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                var devices = _roomService.GetDevicesByRoomId(id);
                return Ok(new { devices = devices });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPost("{id}/devices/{deviceId}")]
        public IActionResult AddDeviceToRoom(int id, int deviceId)
        {
            try
            {
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Check if user is owner of the house
                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId() && hm.Role == "Owner"))
                    return Forbid();

                var device = _deviceService.GetDeviceById(deviceId);
                if (device == null)
                    return NotFound(new { message = "Device not found" });

                _roomService.AddDeviceToRoom(id, device);
                return Ok(new { message = "Device added to room successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("{id}/devices/{deviceId}")]
        public IActionResult RemoveDeviceFromRoom(int id, int deviceId)
        {
            try
            {
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Check if user is owner of the house
                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId() && hm.Role == "Owner"))
                    return Forbid();

                _roomService.RemoveDeviceFromRoom(id, deviceId);
                return Ok(new { message = "Device removed from room successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        /// <summary>
        /// Lấy dữ liệu sensors trong phòng (nhiệt độ, độ ẩm, chuyển động)
        /// </summary>
        [HttpGet("{id}/sensors")]
        public IActionResult GetRoomSensors(int id)
        {
            try
            {
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Check if user has access to this room's house
                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                // Lấy devices trong room
                var devices = _roomService.GetDevicesByRoomId(id);

                // Tìm sensors
                var tempSensor = devices.FirstOrDefault(d => d.Type == "TemperatureHumiditySensor");
                var motionSensor = devices.FirstOrDefault(d => d.Type == "MotionSensor");

                // Lấy telemetry mới nhất
                double? temperature = null;
                double? humidity = null;
                bool? motion = null;

                if (tempSensor != null)
                {
                    var tempTelemetry = _deviceService.GetTelemetryDataByDeviceId(tempSensor.ID).LastOrDefault();
                    if (tempTelemetry != null && !string.IsNullOrEmpty(tempTelemetry.Body))
                    {
                        try
                        {
                            var json = System.Text.Json.JsonDocument.Parse(tempTelemetry.Body);
                            if (json.RootElement.TryGetProperty("temperature", out var tempValue))
                                temperature = tempValue.GetDouble();
                            if (json.RootElement.TryGetProperty("humidity", out var humValue))
                                humidity = humValue.GetDouble();
                        }
                        catch { }
                    }
                }

                if (motionSensor != null)
                {
                    var motionTelemetry = _deviceService.GetTelemetryDataByDeviceId(motionSensor.ID).LastOrDefault();
                    if (motionTelemetry != null && !string.IsNullOrEmpty(motionTelemetry.Body))
                    {
                        try
                        {
                            var json = System.Text.Json.JsonDocument.Parse(motionTelemetry.Body);
                            if (json.RootElement.TryGetProperty("motion", out var motionValue))
                                motion = motionValue.GetBoolean();
                        }
                        catch { }
                    }
                }

                return Ok(new
                {
                    roomId = id,
                    roomName = room.Name,
                    temperature = temperature,
                    humidity = humidity,
                    motion = motion,
                    hasSensors = tempSensor != null || motionSensor != null
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        /// <summary>
        /// Toggle kích hoạt phòng ON/OFF
        /// </summary>
        [HttpPut("{id}/toggle")]
        public IActionResult ToggleRoomActive(int id, [FromBody] RoomToggleRequest request)
        {
            try
            {
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Check if user has access to this room's house
                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                room.IsActive = request.IsActive;
                _roomService.EditRoom(room);

                return Ok(new { 
                    message = request.IsActive ? "Phòng đã được kích hoạt" : "Phòng đã tắt kích hoạt",
                    isActive = room.IsActive 
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        /// <summary>
        /// Lấy lịch sử sensors trong 24h qua
        /// </summary>
        [HttpGet("{id}/sensors/history")]
        public IActionResult GetRoomSensorHistory(int id, int hours = 24)
        {
            try
            {
                var room = _roomService.GetRoomById(id);
                if (room == null)
                    return NotFound(new { message = "Room not found" });

                // Check if user has access to this room's house
                var houseMembers = _houseService.GetHouseMembers((int)room.HouseID);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                // Lấy devices trong room
                var devices = _roomService.GetDevicesByRoomId(id);
                var tempSensor = devices.FirstOrDefault(d => d.Type == "TemperatureHumiditySensor");

                var historyData = new List<object>();

                if (tempSensor != null)
                {
                    var cutoff = DateTime.UtcNow.AddHours(-hours);
                    var telemetryList = _deviceService.GetTelemetryDataByDeviceId(tempSensor.ID)
                        .Where(t => t.Timestamp >= cutoff)
                        .OrderBy(t => t.Timestamp)
                        .ToList();

                    foreach (var telemetry in telemetryList)
                    {
                        if (!string.IsNullOrEmpty(telemetry.Body))
                        {
                            try
                            {
                                var json = System.Text.Json.JsonDocument.Parse(telemetry.Body);
                                double? temp = null, hum = null;
                                
                                if (json.RootElement.TryGetProperty("temperature", out var tempValue))
                                    temp = tempValue.GetDouble();
                                if (json.RootElement.TryGetProperty("humidity", out var humValue))
                                    hum = humValue.GetDouble();

                                if (temp.HasValue || hum.HasValue)
                                {
                                    historyData.Add(new
                                    {
                                        timestamp = telemetry.Timestamp?.ToString("yyyy-MM-dd HH:mm"),
                                        temperature = temp,
                                        humidity = hum
                                    });
                                }
                            }
                            catch { }
                        }
                    }
                }

                return Ok(new
                {
                    roomId = id,
                    roomName = room.Name,
                    hours = hours,
                    data = historyData
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }
    }

    public class CreateRoomRequest
    {
        public int HouseId { get; set; }
        public string Name { get; set; }
        public string Detail { get; set; }
    }

    public class RoomToggleRequest
    {
        public bool IsActive { get; set; }
    }
}
