using DAO.BaseModels;
using DAO.Context;
using DAO.Models;
using DAO.Repositories;
using Microsoft.EntityFrameworkCore;

namespace DAO.Reposistories_Impl
{
    public class DeviceRepository : IDeviceRepository
    {
        private readonly SmartHomeContext _context;

        public DeviceRepository(SmartHomeContext context)
        {
            _context = context;
        }

        public Device AddDevice(Device device)
        {
            try
            {
                // check if device already exists
                var deviceInDb = _context.Devices.FirstOrDefault(d => d.ID == device.ID);
                if (deviceInDb != null)
                {
                    throw new Exception("Device already exists");
                }
                _context.Devices.Add(device);
                _context.SaveChanges();
                return device;
            }
            catch (Exception e)
            {
                throw new Exception("Error while adding device");
            }
        }

        public void DeleteDevice(int id)
        {
            try
            {
                var device = _context.Devices.FirstOrDefault(d => d.ID == id);
                if (device == null)
                {
                    throw new Exception("Device not found");
                }

                _context.Devices.Remove(device);
                _context.SaveChanges();
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public Device UpdateDevice(Device device)
        {
            try
            {
                var deviceToUpdate = _context.Devices.FirstOrDefault(d => d.ID == device.ID);
                if (deviceToUpdate == null)
                {
                    throw new Exception("Device not found");
                }

                deviceToUpdate = device;
                _context.SaveChanges();
                return deviceToUpdate;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public Device GetDeviceById(int deviceId)
        {
            try
            {
                var device = _context.Devices.FirstOrDefault(d => d.ID == deviceId);
                if (device == null)
                {
                    throw new Exception("Device not found");
                }

                return device;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public IEnumerable<Device> GetDevicesByUserId(string userId)
        {

            try
            {
                var user = _context.Users.FirstOrDefault(u => u.Id == userId);
                if (user == null)
                {
                    throw new Exception("User not found");
                }

                var devices = _context.Devices.Include(d => d.User).Include(d => d.Room).Where(d => d.UserID == user.Id).ToList();
                var specificDevices = new List<Device>();

                foreach (var device in devices)
                {
                    var specificDevice = DeviceFactory.CreateDevice(device.Type!, device.Name);
                    specificDevice.Room = device.Room;
                    specificDevice.RoomID = device.RoomID;
                    specificDevice.ID = device.ID;
                    specificDevice.UserID = device.UserID;
                    specificDevice.DeviceToken = device.DeviceToken;
                    specificDevice.User = device.User;
                    specificDevices.Add(specificDevice);
                }
                return specificDevices;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public IEnumerable<Device> GetDevicesByMacAddress(string macAddress)
        {
            try
            {
                var devices = _context.Devices
                    .Include(d => d.User)
                    .Include(d => d.Room)
                    .Where(d => d.MacAddress == macAddress)
                    .ToList();

                return devices;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public TelemetryData AddTelemetryDatum(TelemetryData telemetryData)
        {
            // check if device exists
            var device = _context.Devices.FirstOrDefault(d => d.ID == telemetryData.DeviceID);
            if (device == null)
            {
                throw new Exception("Device not found");
            }
            _context.TelemetryData.Add(telemetryData);
            _context.SaveChanges();
            return telemetryData;
        }

        public IEnumerable<TelemetryData> GetTelemetryDataByDeviceId(int deviceId)
        {
            try
            {
                var device = _context.Devices.FirstOrDefault(d => d.ID == deviceId);
                if (device == null)
                {
                    throw new Exception("Device not found");
                }

                return _context.TelemetryData.Where(t => t.DeviceID == device.ID).ToList().TakeLast(15);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public DeviceConfig AddDeviceConfig(DeviceConfig deviceConfig)
        {
            try
            {
                // check if device exists
                var device = _context.Devices.FirstOrDefault(d => d.ID == deviceConfig.DeviceID);
                if (device == null)
                {
                    throw new Exception("Device not found");
                }
                _context.DeviceConfigs.Add(deviceConfig);
                _context.SaveChanges();
                return deviceConfig;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public DeviceConfig GetDeviceConfigByDeviceId(string deviceId)
        {
            throw new NotImplementedException();
        }

        public DeviceConfig GetDeviceConfigByDeviceId(int deviceId)
        {
            try
            {
                var device = _context.Devices.FirstOrDefault(d => d.ID == deviceId);
                if (device == null)
                {
                    throw new Exception("Device not found");
                }

                return _context.DeviceConfigs.FirstOrDefault(dc => dc.DeviceID == device.ID);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }

        public DeviceConfig UpdateDeviceConfig(DeviceConfig deviceConfig)
        {
            try
            {
                var device = _context.Devices.FirstOrDefault(d => d.ID == deviceConfig.DeviceID);
                if (device == null)
                {
                    throw new Exception("Device not found");
                }

                var deviceConfigToUpdate = _context.DeviceConfigs.FirstOrDefault(dc => dc.DeviceID == device.ID);
                if (deviceConfigToUpdate == null)
                {
                    throw new Exception("Device config not found");
                }

                deviceConfigToUpdate = deviceConfig;
                _context.SaveChanges();
                return deviceConfigToUpdate;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }
        }
        public bool IsDeviceOwner(string userId, int deviceId)
        {
            var device = _context.Devices.FirstOrDefault(d => d.ID == deviceId);
            if (device == null)
            {
                throw new Exception("Device not found");
            }

            return device.UserID == userId;
        }

        public IEnumerable<Device> GetAllDevices()
        {
            return _context.Devices.Include(d => d.User).ToList();
        }
    }
}
