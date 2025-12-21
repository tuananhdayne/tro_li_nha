using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Services.Services;
using WebApp.Utils;

namespace WebApp.Controllers;

public class RoomController : Controller
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

    [Authorize]
    public IActionResult Index(int? houseId)
    {
        var housesWithRooms = new Dictionary<House, IEnumerable<Room>>();

        if (houseId != null)
        {
            var house = _houseService.GetHouseById((int)houseId);
            var rooms = _houseService.GetRooms((int)houseId);
            housesWithRooms[house] = rooms;
            ViewBag.HouseId = houseId;
        }
        else
        {
            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
            foreach (var house in houses)
            {
                var rooms = _houseService.GetRooms(house.ID);
                housesWithRooms[house] = rooms;
            }
        }

        return View(housesWithRooms);
    }

    public IActionResult LoadMoreRoom(int? houseId, int skip, int take)
    {
        var housesWithRooms = new Dictionary<House, IEnumerable<Room>>();
        if (houseId != null)
        {
            var house = _houseService.GetHouseById((int)houseId);
            var rooms = _houseService.GetRooms((int)houseId)
                .Skip(skip)
                .Take(take)
                .ToList();
            housesWithRooms[house] = rooms;
        }
        else
        {
            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
            foreach (var house in houses)
            {
                var rooms = _houseService.GetRooms(house.ID)
                    .Skip(skip)
                    .Take(take)
                    .ToList();
                housesWithRooms[house] = rooms;
            }
        }
        return PartialView("RoomSection", housesWithRooms);
    }

    public IActionResult Search(int? houseId, string keyword)
    {

        var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
        var roomsWithHouse = new Dictionary<House, IEnumerable<Room>>();
        foreach (var house in houses)
        {
            var rooms = _houseService.GetRooms(house.ID)
                .Where(r => StringProcessHelper.RemoveDiacritics(r.Name).ToLower().Contains(StringProcessHelper.RemoveDiacritics(keyword).ToLower()))
                .ToList();
            roomsWithHouse[house] = rooms;
        }

        return PartialView("RoomSection", roomsWithHouse);
    }

    [HttpGet]
    [Authorize]
    public IActionResult Create(int? houseId)
    {
        ViewBag.Houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
        ViewBag.SelectedHouseId = houseId;
        return View();
    }

    [HttpPost]
    public IActionResult Create(int houseId, string name, string detail)
    {
        if (ModelState.IsValid)
        {
            var newRoom = _houseService.AddRoomToHouse(houseId, new Room { Name = name, Detail = detail });
            return RedirectToAction("Index", new { houseId = houseId });
        }

        return View("Index");
    }
    [Authorize]
    public IActionResult Edit(int roomId)
    {
        // check if user is member of the house
        var room = _roomService.GetRoomById(roomId);
        var houseMembers = _houseService.GetHouseMembers((int)room.HouseID!);
        if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
        {
            return RedirectToAction("AccessDenied", "Account");
        }
        return View("Edit", _roomService.GetRoomById(roomId));
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public IActionResult Edit(Room room)
    {
        Console.WriteLine($"[ROOM EDIT] ID: {room.ID}, Name: {room.Name}, IsActive: {room.IsActive}");
        if (ModelState.IsValid)
        {
            _roomService.EditRoom(room);
            return RedirectToAction("Index", new { houseId = room.HouseID });
        }
        return RedirectToAction("Index", new { houseId = room.HouseID });
    }

    [Authorize]
    public IActionResult Delete(int roomId)
    {
        var room = _roomService.GetRoomById(roomId);
        if (room == null)
        {
            return NotFound();
        }
        return View("Delete", room);
    }

    [HttpPost, ActionName("Delete")]
    [ValidateAntiForgeryToken]
    public IActionResult DeleteRoom(int roomId)
    {

        _roomService.DeleteRoom(roomId);
        return RedirectToAction("Index");
    }


    public IActionResult RemoveDeviceFromRoom(int roomId, int deviceId)
    {
        _roomService.RemoveDeviceFromRoom(roomId, deviceId);
        return RedirectToAction("Edit", new { roomId });
    }


    public IActionResult AddDeviceToRoom(int roomId, int deviceId)
    {
        var device = _deviceService.GetDeviceById(deviceId);
        _roomService.AddDeviceToRoom(roomId, device);
        return RedirectToAction("Edit", new { roomId });
    }
}