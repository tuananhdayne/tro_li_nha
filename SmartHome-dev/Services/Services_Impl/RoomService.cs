using DAO.BaseModels;
using DAO.Repositories;
using Services.Services;

namespace Services.Services_Impl;

public class RoomService : IRoomService
{
    private readonly IRoomRepository _roomRepository;
    public RoomService(IRoomRepository roomRepository)
    {
        _roomRepository = roomRepository;
    }

    public Room EditRoom(Room room)
    {
        return _roomRepository.UpdateRoom(room);
    }

    public IEnumerable<Room> GetRoomsByHouseId(int houseId)
    {
        throw new NotImplementedException();
    }

    public Room GetRoomById(int roomId)
    {
        return _roomRepository.GetRoomById(roomId);
    }

    public void DeleteRoom(int roomId)
    {
        _roomRepository.DeleteRoom(roomId);
    }

    public IEnumerable<Device> GetDevicesByRoomId(int roomId)
    {
        return _roomRepository.GetDevicesByRoomId(roomId);
    }

    public Device AddDeviceToRoom(int roomId, Device device)
    {
        return _roomRepository.AddDeviceToRoom(roomId, device);
    }

    public void RemoveDeviceFromRoom(int roomId, int deviceId)
    {
        _roomRepository.RemoveDeviceFromRoom(roomId, deviceId);
    }
}