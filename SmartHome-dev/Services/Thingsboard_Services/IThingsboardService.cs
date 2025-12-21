using DAO.BaseModels;
using Services.Thingsboard_Services.BaseModel;
using System.Text.Json;

namespace Services.Thingsboard_Services;

public interface IThingsboardService
{
    public Token? Login(Account account);
    public Token GetAdminToken();
    public object CreateCustomerAccount(Account account);
    public object? CreateDevice(Device device);
    public object? DeleteDevice(int deviceId);
    public object AssignDeviceToCustomer(string deviceId, string customerId);
    public object? ControlDevice(int deviceId, string command);
    public object? ControlDevice(int deviceId, object command);
    public object? ControlDevice(int deviceId, string command, int? dim = null, int? R = null, int? G = null,
        int? B = null);
    public JsonElement GetLatestTelemetry(string tbDeviceId);
    public object? UpdateDevice(Device device);

}
