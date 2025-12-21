using DAO.BaseModels;

namespace DAO.Repositories
{
    public interface IDeviceRepository
    {
        Device AddDevice(Device device);
        void DeleteDevice(int deviceId);
        Device UpdateDevice(Device device);
        Device GetDeviceById(int deviceId);
        IEnumerable<Device> GetDevicesByUserId(string userId);
        IEnumerable<Device> GetDevicesByMacAddress(string macAddress);
        TelemetryData AddTelemetryDatum(TelemetryData telemetryData);
        IEnumerable<TelemetryData> GetTelemetryDataByDeviceId(int deviceId);
        DeviceConfig AddDeviceConfig(DeviceConfig deviceConfig);
        DeviceConfig GetDeviceConfigByDeviceId(int deviceId);
        DeviceConfig UpdateDeviceConfig(DeviceConfig deviceConfig);
        bool IsDeviceOwner(string userId, int deviceId);
        IEnumerable<Device> GetAllDevices();
    }
}
