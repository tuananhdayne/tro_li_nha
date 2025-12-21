using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Services.Services;
using WebApp.Utils;

namespace WebApp.Controllers.Api
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
    public class HouseController : ControllerBase
    {
        private readonly IHouseService _houseService;
        private readonly IUserService _userService;
        private readonly IRoomService _roomService;

        public HouseController(IHouseService houseService, IUserService userService, IRoomService roomService)
        {
            _houseService = houseService;
            _userService = userService;
            _roomService = roomService;
        }

        [HttpGet]
        public IActionResult GetHouses(int skip = 0, int take = 10)
        {
            try
            {
                var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId())
                    .Skip(skip)
                    .Take(take)
                    .ToList();

                return Ok(new
                {
                    houses = houses,
                    total = _houseService.GetHousesByUserId(_userService.GetCurrentUserId()).Count(),
                    skip = skip,
                    take = take
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("search")]
        public IActionResult SearchHouses(string keyword)
        {
            try
            {
                var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
                if (!string.IsNullOrEmpty(keyword))
                {
                    houses = houses.Where(h => 
                        StringProcessHelper.RemoveDiacritics(h.Name).Contains(keyword, StringComparison.OrdinalIgnoreCase) || 
                        StringProcessHelper.RemoveDiacritics(h.Location).Contains(keyword, StringComparison.OrdinalIgnoreCase))
                        .ToList();
                }

                return Ok(new { houses = houses });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("{id}")]
        public IActionResult GetHouse(int id)
        {
            try
            {
                var house = _houseService.GetHouseById(id);
                if (house == null)
                    return NotFound(new { message = "House not found" });

                // Check if user has access to this house
                var houseMembers = _houseService.GetHouseMembers(id);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                return Ok(house);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPost]
        public IActionResult CreateHouse([FromBody] CreateHouseRequest request)
        {
            try
            {
                if (!ModelState.IsValid)
                    return BadRequest(ModelState);

                var newHouse = _houseService.CreateHouse(request.Name, request.Location);
                _houseService.AddHouseMember(_userService.GetCurrentUserId(), newHouse.ID, "Owner");

                return CreatedAtAction(nameof(GetHouse), new { id = newHouse.ID }, newHouse);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPut("{id}")]
        public IActionResult UpdateHouse(int id, [FromBody] House house)
        {
            try
            {
                if (!ModelState.IsValid)
                    return BadRequest(ModelState);

                if (!_houseService.IsHouseOwner(_userService.GetCurrentUserId(), id))
                    return Forbid();

                house.ID = id;
                var houseToUpdate = _houseService.GetHouseById(id);
                if (houseToUpdate == null)
                    return NotFound(new { message = "House not found" });
                
                if(!string.IsNullOrEmpty(house.Name))
                    houseToUpdate.Name = house.Name;
                if(!string.IsNullOrEmpty(house.Location))
                    houseToUpdate.Location = house.Location;
                
                _houseService.EditHouse(houseToUpdate);

                return Ok(houseToUpdate);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("{id}")]
        public IActionResult DeleteHouse(int id)
        {
            try
            {
                var houseMembers = _houseService.GetHouseMembers(id);
                var userRole = houseMembers.FirstOrDefault(hm => hm.UserID == _userService.GetCurrentUserId())?.Role;
                
                if (userRole == "Owner")
                {
                    _houseService.DeleteHouse(id);
                    return Ok(new { message = "House deleted successfully" });
                }
                else
                {
                    _houseService.RemoveHouseMember(_userService.GetCurrentUserId(), id);
                    return Ok(new { message = "Removed from house successfully" });
                }
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("{id}/members")]
        public IActionResult GetHouseMembers(int id)
        {
            try
            {
                // Check if user has access to this house
                var houseMembers = _houseService.GetHouseMembers(id);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                return Ok(new { members = houseMembers });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPost("{id}/members")]
        public IActionResult AddHouseMember(int id, [FromBody] AddMemberRequest request)
        {
            try
            {
                if (!_houseService.IsHouseOwner(_userService.GetCurrentUserId(), id))
                    return Forbid();

                _houseService.AddHouseMember(request.UserId, id, request.Role ?? "Member");
                return Ok(new { message = "Member added successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("{id}/members/{userId}")]
        public IActionResult RemoveHouseMember(int id, string userId)
        {
            try
            {
                if (!_houseService.IsHouseOwner(_userService.GetCurrentUserId(), id))
                    return Forbid();

                _houseService.RemoveHouseMember(userId, id);
                return Ok(new { message = "Member removed successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPost("join")]
        public IActionResult JoinHouse([FromBody] JoinHouseRequest request)
        {
            try
            {
                if (!_houseService.IsHouseOwner(request.OwnerId, request.HouseId))
                    return BadRequest(new { message = "Invalid house owner" });

                _houseService.AddHouseMember(_userService.GetCurrentUserId(), request.HouseId, "Member");
                return Ok(new { message = "Joined house successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("{id}/rooms")]
        public IActionResult GetRoomsByHouse(int id)
        {
            
            try
            {
                var rooms = _houseService.GetRooms(id);
                // if (house == null)
                //     return NotFound(new { message = "House not found" });

                // Check if user has access to this house
                // var houseMembers = _houseService.GetHouseMembers(id);
                // if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                //     return Forbid();

                // var rooms = _roomService.GetRoomsByHouseId(id);
                return Ok(new { rooms = rooms });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        /// <summary>
        /// Toggle kích hoạt nhà ON/OFF
        /// </summary>
        [HttpPut("{id}/toggle")]
        public IActionResult ToggleHouseActive(int id, [FromBody] ToggleActiveRequest request)
        {
            try
            {
                var house = _houseService.GetHouseById(id);
                if (house == null)
                    return NotFound(new { message = "House not found" });

                // Check if user has access to this house
                var houseMembers = _houseService.GetHouseMembers(id);
                if (!houseMembers.Any(hm => hm.UserID == _userService.GetCurrentUserId()))
                    return Forbid();

                house.IsActive = request.IsActive;
                _houseService.EditHouse(house);

                return Ok(new { 
                    message = request.IsActive ? "Nhà đã được kích hoạt" : "Nhà đã tắt kích hoạt",
                    isActive = house.IsActive 
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }
    }

    public class CreateHouseRequest
    {
        public string Name { get; set; }
        public string Location { get; set; }
    }

    public class AddMemberRequest
    {
        public string UserId { get; set; }
        public string Role { get; set; }
    }

    public class JoinHouseRequest
    {
        public string OwnerId { get; set; }
        public int HouseId { get; set; }
    }

    public class ToggleActiveRequest
    {
        public bool IsActive { get; set; }
    }
}
