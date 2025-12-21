using Microsoft.AspNetCore.Mvc;
using Services.Services;

namespace WebApp.ViewComponents;

public class DeviceTelemetryViewComponent : ViewComponent
{
    private IDeviceService _deviceService;

    public DeviceTelemetryViewComponent(IDeviceService deviceService)
    {
        _deviceService = deviceService;
    }

    public async Task<IViewComponentResult> InvokeAsync(int deviceId)
    {
        // take last 15 telemetry data
        var telemetryData = _deviceService.GetTelemetryDataByDeviceId(deviceId).TakeLast(15);
        return View("~/Views/Shared/Components/Device/RenderDeviceTelemetry.cshtml", telemetryData);
    }
}