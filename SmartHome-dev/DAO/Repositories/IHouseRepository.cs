using DAO.BaseModels;

namespace DAO.Repositories
{
    public interface IHouseRepository
    {
        House AddHouse(House house);
        void DeleteHouse(int houseId);
        House UpdateHouse(House house);
        IEnumerable<House> GetHouseByUserID(string userId);
        IEnumerable<House> GetAllHouses();
        House GetHouseById(int houseId);
        HouseMember AddHouseMember(string userId, int houseId, string role);
        IEnumerable<HouseMember?> GetHouseMembers(int houseId);

        void RemoveHouseMember(string userId, int houseId);
        Room AddRoomToHouse(int houseId, Room room);
        void RemoveRoomFromHouse(int houseId, int roomId);
        IEnumerable<Room> GetRoomsByHouseId(int houseId);
        public bool IsHouseOwner(string userId, int houseId);
        User GetHouseOwner(int houseId);
    }
}
