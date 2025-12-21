using DAO.BaseModels;
using DAO.Repositories;
using Services.Services;

namespace Services.Services_Impl;

public class DeviceService : IDeviceService
{
    private readonly IDeviceRepository _deviceRepository;

    public DeviceService(IDeviceRepository deviceRepository)
    {
        _deviceRepository = deviceRepository;
    }
    public Device CreateDevice(Device device)
    {
        try
        {
            var deviceCreated = (Device)_deviceRepository.AddDevice(device);


            _deviceRepository.UpdateDevice(deviceCreated);
            return deviceCreated;
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public Device EditDevice(Device device)
    {
        try
        {
            var deviceEdited = _deviceRepository.UpdateDevice(device);
            return deviceEdited;
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public IEnumerable<Device> GetDevicesByHouseId(int houseId)
    {
        throw new NotImplementedException();
    }

    public Device GetDeviceById(int deviceId)
    {
        return _deviceRepository.GetDeviceById(deviceId);
    }

    public IEnumerable<Device> GetDevices()
    {
        return _deviceRepository.GetAllDevices();
    }

    public IEnumerable<Device> GetDevicesByUserId(string userId)
    {
        return _deviceRepository.GetDevicesByUserId(userId);
    }

    public IEnumerable<Device> GetDevicesByMacAddress(string macAddress)
    {
        return _deviceRepository.GetDevicesByMacAddress(macAddress);
    }

    public void DeleteDevice(int deviceId)
    {
        try
        {
            _deviceRepository.DeleteDevice(deviceId);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public TelemetryData AddTelemetryDatum(TelemetryData telemetryData)
    {
        try
        {
            return _deviceRepository.AddTelemetryDatum(telemetryData);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public IEnumerable<TelemetryData> GetTelemetryDataByDeviceId(int deviceId)
    {
        return _deviceRepository.GetTelemetryDataByDeviceId(deviceId);
    }

    public DeviceConfig AddDeviceConfig(DeviceConfig deviceConfig)
    {
        throw new NotImplementedException();
    }

    public DeviceConfig GetDeviceConfigByDeviceId(int deviceId)
    {
        throw new NotImplementedException();
    }

    public DeviceConfig UpdateDeviceConfig(DeviceConfig deviceConfig)
    {
        throw new NotImplementedException();
    }

    public bool IsDeviceOwner(string userId, int deviceId)
    {
        return _deviceRepository.IsDeviceOwner(userId, deviceId);
    }
}