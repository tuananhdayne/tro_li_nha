using DAO.BaseModels;
using Microsoft.AspNetCore.Mvc;

namespace WebApp.ViewComponents;

public class DeviceCreateViewComponent : ViewComponent
{
    public async Task<IViewComponentResult> InvokeAsync()
    {
        Device device = new Device();
        return View("~/Views/Shared/Components/Device/RenderDeviceCreate.cshtml", device);
    }
}