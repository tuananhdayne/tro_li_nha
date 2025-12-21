using Microsoft.AspNetCore.Mvc;
using Services.Services;

namespace WebApp.ViewComponents;

public class HouseMemberViewComponent : ViewComponent
{
    IHouseService _houseService;
    public HouseMemberViewComponent(IHouseService houseService)
    {
        _houseService = houseService;
    }
    public async Task<IViewComponentResult> InvokeAsync(int houseId)
    {
        var houseMembers = _houseService.GetHouseMembers(houseId);
        ViewBag.HouseId = houseId;
        return View("~/Views/Shared/Components/House/RenderHouseMember.cshtml", houseMembers);
    }
}