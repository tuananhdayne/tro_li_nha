using DAO.BaseModels;
using Services.Services;

namespace Test;

public class TestHouse
{
    private IHouseService _houseService;
    public TestHouse(IHouseService houseService)
    {
        _houseService = houseService;
    }

    public void TestCreateHouse()
    {
        House house = new House()
        {
            Name = "House 1",
            Location = "123 Nguyen Trai"
        };
        try
        {
            var createdHouse = _houseService.CreateHouse(house.Name, house.Location);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }
    
    public void TestAddHouseMember()
    {
        string userId = "0f775766-9969-4beb-a179-6c9466f170a0";
        int houseId = 1;
        try
        {
            _houseService.AddHouseMember(userId, houseId);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public void TestEditHouse()
    {
        House house = new House()
        {
            ID = 1,
            Name = "House 1",
            Location = "123 Vo Van Ngan"
        };
        try
        {
            var editedHouse = _houseService.EditHouse(house);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public void TestGetHousesByUserId()
    {
        string userId = "1";
        try
        {
            var houses = _houseService.GetHousesByUserId(userId);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public void TestGetRooms()
    {
        int houseId = 1;
        try
        {
            var rooms = _houseService.GetRooms(houseId);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public void TestGetRoom()
    {
        int roomId = 1;
        try
        {
            var room = _houseService.GetRoom(roomId);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public void TestAddRoomToHouse()
    {
        int houseId = 1;
        Room room = new Room()
        {
            Name = "Room 1",
        };
        try
        {
            var addedRoom = _houseService.AddRoomToHouse(houseId, room);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }
    }

    public void TestRemoveRoomFromHouse()
    {
        int houseId = 1;
        int roomId = 1;
        try
        {
            _houseService.RemoveRoomFromHouse(houseId, roomId);
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            throw;
        }

    }
}