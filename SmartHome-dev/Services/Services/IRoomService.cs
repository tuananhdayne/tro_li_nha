using DAO.BaseModels;

namespace Services.Services;

public interface IRoomService
{
    public Room EditRoom(Room room);
    public IEnumerable<Room> GetRoomsByHouseId(int houseId);
    public Room GetRoomById(int roomId);
    public void DeleteRoom(int roomId);
    public IEnumerable<Device> GetDevicesByRoomId(int roomId);
    public Device AddDeviceToRoom(int roomId, Device device);
    public void RemoveDeviceFromRoom(int roomId, int deviceId);
}