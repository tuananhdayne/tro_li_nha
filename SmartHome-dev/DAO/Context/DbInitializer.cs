using DAO.BaseModels;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace DAO.Context;

public class DbInitializer
{
    public static void Initializer(IServiceProvider serviceProvider)
    {
        using (var context = new SmartHomeContext(serviceProvider
                   .GetRequiredService<DbContextOptions<SmartHomeContext>>()))
        {
            context.Database.EnsureCreated();
            if (context.Users.Any())
            {
                return;
            }

            if (context.Roles.Any())
            {
                return;
            }

            var roles = new IdentityRole[]
            {
                new IdentityRole { Id = "ID1", Name = "Admin", NormalizedName = "ADMIN" },
                new IdentityRole { Id = "ID2", Name = "Member", NormalizedName = "MEMBER" }
            };

            foreach (var role in roles)
            {
                context.Roles.Add(role);
            }

            context.SaveChanges();
            // Create admin user
            User adminUser = new User
            {
                Id = "8126108d-3c21-4508-b8fc-2c6720fbffbe",
                UserName = "admin@rangdong.com.vn",
                NormalizedUserName = "ADMIN@RANGDONG.COM.VN",
                Email = "admin@rangdong.com.vn",
                NormalizedEmail = "ADMIN@RANGDONG.COM.VN",
                EmailConfirmed = true,
                SecurityStamp = Guid.NewGuid().ToString(),
                ConcurrencyStamp = Guid.NewGuid().ToString(),
                PhoneNumber = "0123456789",
                PhoneNumberConfirmed = true
            };

            PasswordHasher<User> passwordHasher = new PasswordHasher<User>();
            adminUser.PasswordHash = passwordHasher.HashPassword(adminUser, "admin");
            context.Users.Add(adminUser);
            context.SaveChanges();

            // Add user role with houses
            User houseOwner = new User
            {
                Id = "7fc44614-9f88-4076-84d8-1e439a1943fe",
                UserName = "user@randong.com.vn",
                NormalizedUserName = "USER@RANGDONG.COM.VN",
                Email = "user@rangdong.com.vn",
                NormalizedEmail = "USER@RANGDONG.COM.VN",
                EmailConfirmed = true,
                SecurityStamp = Guid.NewGuid().ToString(),
                ConcurrencyStamp = Guid.NewGuid().ToString(),
                PhoneNumber = "0987654321",
                PhoneNumberConfirmed = true
            };
            houseOwner.PasswordHash = passwordHasher.HashPassword(houseOwner, "user");
            context.Users.Add(houseOwner);
            context.SaveChanges();

            // create guest user
            User gUser = new User
            {
                Id = "935f6194-5165-4dcb-a087-d6d82d25629e",
                UserName = "guest@randong.com.vn",
                NormalizedUserName = "GUEST@RANGDONG.COM.VN",
                Email = "guest@randong.com.vn",
                NormalizedEmail = "GUEST@RANGDONG.COM.VN",
                EmailConfirmed = true,
                SecurityStamp = Guid.NewGuid().ToString(),
                ConcurrencyStamp = Guid.NewGuid().ToString(),
                PhoneNumber = "0987654321",
                PhoneNumberConfirmed = true
            };

            gUser.PasswordHash = passwordHasher.HashPassword(gUser, "guest");
            context.Users.Add(gUser);
            context.SaveChanges();

            // add houses for house owner
            var house = new House
            {
                Name = "House 1",
                Location = "123 Nguyen Luong Bang"
            };

            context.Houses.Add(house);
            context.SaveChanges();

            var houseOwnerHouse = new HouseMember()
            {
                HouseID = house.ID,
                UserID = houseOwner.Id
            };

            var houseOwnerHouse2 = new HouseMember()
            {
                HouseID = house.ID,
                UserID = gUser.Id
            };

            context.HouseMembers.AddRange(houseOwnerHouse, houseOwnerHouse2);
            context.SaveChanges();

            // add rooms for the house
            var room = new Room
            {
                Name = "Phòng khách",
                Detail = "Smart Room Demo",
                HouseID = house.ID
            };

            context.Rooms.Add(room);
            context.SaveChanges();

            // add devices for the smart room
            var light = new Device
            {
                Name = "Đèn trần",
                Type = "Light",
                RoomID = room.ID,
                UserID = houseOwner.Id,
                Status = "Off",
                DeviceToken = "LIGHT001"
            };

            var doorLock = new Device
            {
                Name = "Khóa cửa chính",
                Type = "DoorLock",
                RoomID = room.ID,
                UserID = houseOwner.Id,
                Status = "Locked",
                DeviceToken = "DOOR001"
            };

            var tempSensor = new Device
            {
                Name = "Cảm biến nhiệt độ",
                Type = "TemperatureHumiditySensor",
                RoomID = room.ID,
                UserID = houseOwner.Id,
                Status = "Active",
                DeviceToken = "TEMP001"
            };

            var motionSensor = new Device
            {
                Name = "Cảm biến chuyển động",
                Type = "MotionSensor",
                RoomID = room.ID,
                UserID = houseOwner.Id,
                Status = "Active",
                DeviceToken = "MOTION001"
            };

            context.Devices.AddRange(light, doorLock, tempSensor, motionSensor);
            context.SaveChanges();


            var userRole = new IdentityUserRole<string>
            {
                RoleId = "ID1",
                UserId = "8126108d-3c21-4508-b8fc-2c6720fbffbe"
            };
            var userRole2 = new IdentityUserRole<string>
            {
                RoleId = "ID2",
                UserId = "7fc44614-9f88-4076-84d8-1e439a1943fe"
            };
            var userRole3 = new IdentityUserRole<string>
            {
                RoleId = "ID2",
                UserId = "935f6194-5165-4dcb-a087-d6d82d25629e"
            };

            context.UserRoles.AddRange(userRole, userRole2, userRole3);
            context.SaveChanges();
        }
    }

}