using DAO.BaseModels;
using Microsoft.AspNetCore.Mvc;

namespace WebApp.ViewComponents;

public class HouseCreateViewComponent : ViewComponent
{

    public async Task<IViewComponentResult> InvokeAsync()
    {
        House house = new House();
        return View("~/Views/Shared/Components/House/RenderHouseCreate.cshtml", house);
    }

}