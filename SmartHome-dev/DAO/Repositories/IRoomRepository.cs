using DAO.BaseModels;

namespace DAO.Repositories
{
    public interface IRoomRepository
    {
        Room AddRoom(Room room);
        void DeleteRoom(int roomId);
        Room UpdateRoom(Room room);
        Room GetRoomById(int roomId);
        Device AddDeviceToRoom(int roomId, Device device);
        void RemoveDeviceFromRoom(int roomId, int deviceId);
        IEnumerable<Device> GetDevicesByRoomId(int roomId);

    }
}
