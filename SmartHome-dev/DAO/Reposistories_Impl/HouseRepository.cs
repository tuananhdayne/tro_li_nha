using DAO.BaseModels;
using DAO.Context;
using DAO.Repositories;
using Microsoft.EntityFrameworkCore;

namespace DAO.Reposistories_Impl
{
    public class HouseRepository : IHouseRepository
    {
        private readonly SmartHomeContext _context;
        private readonly IRoomRepository _roomRepository;

        public HouseRepository(SmartHomeContext context, IRoomRepository roomRepository)
        {
            _context = context;
            _roomRepository = roomRepository;
        }

        public House AddHouse(House house)
        {
            try
            {
                _context.Houses.Add(house);
                _context.SaveChanges();
                return house;
            }
            catch (Exception e)
            {
                throw new Exception("Error while adding house");
            }
        }

        public House GetHouseById(int houseId)
        {
            var house = _context.Houses.FirstOrDefault(h => h.ID == houseId);
            if (house == null)
            {
                throw new Exception("House not found");
            }
            return house;
        }

        public HouseMember AddHouseMember(string userId, int houseId, string role)
        {
            try
            {
                var house = _context.Houses.FirstOrDefault(h => h.ID == houseId);
                if (house == null)
                {
                    throw new Exception("House not found");
                }

                var user = _context.Users.FirstOrDefault(u => u.Id == userId);
                if (user == null)
                {
                    throw new Exception("User not found");
                }

                // Check if the user is already a member of the house
                var houseMember = _context.HouseMembers.FirstOrDefault(hm => hm.UserID == userId && hm.HouseID == houseId);
                if (houseMember != null)
                {
                    throw new Exception("User is already a member of the house");
                }

                houseMember = new HouseMember()
                {
                    UserID = userId,
                    HouseID = houseId,
                    Role = role,
                    User = user,
                    House = house
                };
                _context.HouseMembers.Add(houseMember);
                _context.SaveChanges();
                return houseMember;
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw new Exception("Error while adding house member");
            }
        }

        public IEnumerable<HouseMember?> GetHouseMembers(int houseId)
        {
            // Get all house members
            var houseMembers = _context.HouseMembers.Include(hm => hm.User).Where(hm => hm.HouseID == houseId).ToList();
            return houseMembers;
        }

        public Room AddRoomToHouse(int houseId, Room room)
        {
            try
            {
                var house = _context.Houses.FirstOrDefault(h => h.ID == houseId);
                if (house == null)
                {
                    throw new Exception("House not found");
                }
                // Check if the room is already in the house
                var roomInHouse = house.Rooms.FirstOrDefault(r => r.ID == room.ID);
                if (roomInHouse != null)
                {
                    throw new Exception("Room is already in the house");
                }
                house.Rooms.Add(room);
                _context.SaveChanges();
                return room;
            }
            catch (Exception e)
            {
                throw new Exception("Error while adding room to house");
            }
        }

        public void DeleteHouse(int houseId)
        {
            var house = _context.Houses.Include(h => h.Rooms).FirstOrDefault(h => h.ID == houseId);
            if (house == null)
            {
                throw new Exception("House not found");
            }
            // remove house's rooms
            foreach (var room in house.Rooms)
            {
                _roomRepository.DeleteRoom(room.ID);
            }
            _context.Houses.Remove(house);
            _context.SaveChanges();
        }

        public IEnumerable<House> GetAllHouses()
        {
            return _context.Houses.Include(h => h.HouseMembers).ToList();
        }

        public IEnumerable<House> GetHouseByUserID(string userId)
        {
            var houses = _context.HouseMembers.Where(hm => hm.UserID == userId).ToList();
            List<House> result = new List<House>();
            foreach (var house in houses)
            {
                result.Add(_context.Houses.Include(h => h.Rooms).FirstOrDefault(h => h.ID == house.HouseID));
            }
            return result;
        }

        public IEnumerable<Room> GetRoomsByHouseId(int houseId)
        {
            var house = _context.Houses
                .Include(h => h.Rooms)
                    .ThenInclude(r => r.Devices)
                .FirstOrDefault(h => h.ID == houseId);
            if (house == null)
            {
                throw new Exception("House not found");
            }
            return house.Rooms;
        }

        public void RemoveHouseMember(string userId, int houseId)
        {
            var houseMember = _context.HouseMembers.FirstOrDefault(hm => hm.UserID == userId && hm.HouseID == houseId);
            if (houseMember == null)
            {
                throw new Exception("House member not found");
            }
            _context.HouseMembers.Remove(houseMember);
            _context.SaveChanges();
        }

        public void RemoveRoomFromHouse(int houseId, int roomId)
        {
            var house = _context.Houses
                .Include(h => h.Rooms)
                .FirstOrDefault(h => h.ID == houseId);
            if (house == null)
            {
                throw new Exception("House not found");
            }
            var room = house.Rooms.FirstOrDefault(r => r.ID == roomId);
            if (room == null)
            {
                throw new Exception("Room not found");
            }
            // Change the room's house to null
            room.HouseID = null;
            _context.Rooms.Update(room);
            _context.SaveChanges();
        }

        public House UpdateHouse(House house)
        {
            var houseToUpdate = _context.Houses.FirstOrDefault(h => h.ID == house.ID);
            if (houseToUpdate == null)
            {
                throw new Exception("House not found!");
            }
            
            houseToUpdate.Name = house.Name;
            houseToUpdate.Location = house.Location;
            houseToUpdate.IsActive = house.IsActive;
            
            _context.SaveChanges();
            return houseToUpdate;
        }

        public bool IsHouseOwner(string userId, int houseId)
        {
            var houseMember = _context.HouseMembers.FirstOrDefault(hm => hm.UserID == userId && hm.HouseID == houseId);
            if (houseMember == null)
            {
                throw new Exception("House member not found");
            }
            return houseMember.Role == "Owner";
        }

        public User GetHouseOwner(int houseId)
        {
            var houseMember = _context.HouseMembers.Include(hm => hm.User).FirstOrDefault(hm => hm.HouseID == houseId && hm.Role == "Owner");
            if (houseMember == null)
            {
                throw new Exception("House owner not found");
            }
            return houseMember.User!;
        }
    }
}
