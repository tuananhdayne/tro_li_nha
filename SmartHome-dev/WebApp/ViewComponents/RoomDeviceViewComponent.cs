using Microsoft.AspNetCore.Mvc;
using Services.Services;

namespace WebApp.ViewComponents;

public class RoomDeviceViewComponent : ViewComponent
{
    IRoomService _roomService;

    public RoomDeviceViewComponent(IRoomService roomService)
    {
        _roomService = roomService;
    }

    public async Task<IViewComponentResult> InvokeAsync(int roomId)
    {
        var roomDevices = _roomService.GetDevicesByRoomId(roomId);
        ViewBag.RoomId = roomId;
        return View("~/Views/Shared/Components/Room/RenderRoomDevice.cshtml", roomDevices);
    }
}