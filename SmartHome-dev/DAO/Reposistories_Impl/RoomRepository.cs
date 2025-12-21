using DAO.BaseModels;
using DAO.Context;
using DAO.Models;
using DAO.Repositories;
using Microsoft.EntityFrameworkCore;

namespace DAO.Reposistories_Impl
{
    public class RoomRepository : IRoomRepository
    {
        private readonly SmartHomeContext _context;
        private readonly IDeviceRepository _deviceRepository;

        public RoomRepository(SmartHomeContext context, IDeviceRepository deviceRepository)
        {
            _context = context;
            _deviceRepository = deviceRepository;
        }

        public Device AddDeviceToRoom(int roomId, Device device)
        {
            // check if device already exists
            var deviceInDb = _context.Devices.FirstOrDefault(d => d.ID == device.ID);
            var room = _context.Rooms.FirstOrDefault(r => r.ID == roomId);
            if (room == null)
            {
                throw new Exception("Room not found");
            }

            if (deviceInDb != null)
            {
                deviceInDb.RoomID = roomId;
                deviceInDb.Room = room;
                // deviceInDb.Name = device.Type.ToString() + " " + room.Name;
                deviceInDb.Type = device.Type;
                _deviceRepository.UpdateDevice(deviceInDb);
                _context.SaveChanges();
                return deviceInDb;
            }

            deviceInDb = (Device)_deviceRepository.AddDevice(device);
            deviceInDb.RoomID = roomId;
            deviceInDb.Room = room;
            // Only set a generic name if it doesn't already have one
            if (string.IsNullOrEmpty(deviceInDb.Name))
            {
                deviceInDb.Name = device.Type.ToString() + " " + room.Name;
            }
            _context.SaveChanges();
            return deviceInDb;
        }

        public Room AddRoom(Room room)
        {
            try
            {
                // check if room already exists
                var roomInDb = _context.Rooms.FirstOrDefault(r => r.Name == room.Name);
                if (roomInDb != null)
                {
                    throw new Exception("Room already exists");
                }

                _context.Rooms.Add(room);
                _context.SaveChanges();
                return room;
            }
            catch (Exception e)
            {
                throw new Exception("Error while adding room");
            }
        }

        public void DeleteRoom(int roomId)
        {
            try
            {
                var room = _context.Rooms.Include(r => r.Devices).FirstOrDefault(r => r.ID == roomId);
                if (room == null)
                {
                    throw new Exception("Room not found");
                }

                foreach (var roomDevice in room.Devices)
                {
                    roomDevice.RoomID = null;
                    roomDevice.Room = null;
                }
                _context.Rooms.Remove(room);
                _context.SaveChanges();
            }
            catch (Exception e)
            {
                throw new Exception("Error while deleting room");
            }
        }

        public IEnumerable<Device> GetDevicesByRoomId(int roomId)
        {
            var devices = _context.Devices.Include(d => d.User).Where(d => d.RoomID == roomId).ToList();
            var specificDevices = new List<Device>();

            foreach (var device in devices)
            {
                var specificDevice = DeviceFactory.CreateDevice(device.Type!, device.Name);
                specificDevice.User = device.User;
                specificDevice.ID = device.ID;
                specificDevice.RoomID = device.RoomID;
                specificDevice.DeviceToken = device.DeviceToken;
                specificDevices.Add(specificDevice);
            }

            return specificDevices;
        }

        public Room GetRoomById(int roomId)
        {
            var room = _context.Rooms
                .Include(r => r.House)
                .Include(r => r.Devices)
                .FirstOrDefault(r => r.ID == roomId);
            return room;
        }

        public void RemoveDeviceFromRoom(int roomId, int deviceId)
        {
            try
            {
                var device = _context.Devices.FirstOrDefault(d => d.ID == deviceId);
                if (device == null)
                {
                    throw new Exception("Device not found");
                }

                device.RoomID = null;
                device.Room = null;
                _context.SaveChanges();
            }
            catch (Exception e)
            {
                throw new Exception("Error while removing device from room");
            }
        }

        public Room UpdateRoom(Room room)
        {
            try
            {
                var roomToUpdate = _context.Rooms.FirstOrDefault(r => r.ID == room.ID);
                if (roomToUpdate == null)
                {
                    throw new Exception("Room not found");
                }

                roomToUpdate.Name = room.Name;
                roomToUpdate.Detail = room.Detail;
                roomToUpdate.IsActive = room.IsActive; // Added this line
                _context.Rooms.Update(roomToUpdate);
                _context.SaveChanges();
                return roomToUpdate;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw new Exception("Error while updating room");
            }
        }
    }
}
