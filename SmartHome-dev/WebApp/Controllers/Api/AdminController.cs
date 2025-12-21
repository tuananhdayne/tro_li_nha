using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Services.Services;

namespace WebApp.Controllers.Api
{
    [Route("api/[controller]")]
    [ApiController]
    [Authorize(AuthenticationSchemes = "Bearer,Identity.Application", Roles = "Admin")]
    public class AdminController : ControllerBase
    {
        private readonly IHouseService _houseService;
        private readonly IDeviceService _deviceService;
        private readonly IUserService _userService;
        private readonly UserManager<User> _userManager;
        private readonly SignInManager<User> _signInManager;
        private readonly RoleManager<IdentityRole> _roleManager;

        public AdminController(
            IHouseService houseService, 
            IDeviceService deviceService, 
            IUserService userService, 
            UserManager<User> userManager, 
            SignInManager<User> signInManager, 
            RoleManager<IdentityRole> roleManager)
        {
            _houseService = houseService;
            _deviceService = deviceService;
            _userService = userService;
            _userManager = userManager;
            _signInManager = signInManager;
            _roleManager = roleManager;
        }

        [HttpGet("users")]
        public IActionResult GetUsers()
        {
            try
            {
                var users = _userService.GetUsers();
                return Ok(new { users = users });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("users/{id}")]
        public async Task<IActionResult> GetUser(string id)
        {
            try
            {
                var user = await _userManager.FindByIdAsync(id);
                if (user == null)
                    return NotFound(new { message = "User not found" });

                var roles = await _userManager.GetRolesAsync(user);
                
                return Ok(new
                {
                    user = new
                    {
                        id = user.Id,
                        email = user.Email,
                        displayName = user.DisplayName,
                        phoneNumber = user.PhoneNumber,
                        roles = roles
                    }
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPut("users/{id}")]
        public async Task<IActionResult> UpdateUser(string id, [FromBody] UpdateUserRequest request)
        {
            try
            {
                var user = await _userManager.FindByIdAsync(id);
                if (user == null)
                    return NotFound(new { message = "User not found" });

                // Update user information
                user.Email = request.Email;
                user.DisplayName = request.DisplayName;
                user.PhoneNumber = request.PhoneNumber;
                
                var updateResult = await _userManager.UpdateAsync(user);
                if (!updateResult.Succeeded)
                {
                    return BadRequest(new { errors = updateResult.Errors.Select(e => e.Description) });
                }

                // Check if the role exists, if not, create it
                if (!await _roleManager.RoleExistsAsync(request.Role))
                {
                    var roleResult = await _roleManager.CreateAsync(new IdentityRole(request.Role));
                    if (!roleResult.Succeeded)
                    {
                        return BadRequest(new { errors = roleResult.Errors.Select(e => e.Description) });
                    }
                }

                // Clear user roles and add new role
                var userRoles = await _userManager.GetRolesAsync(user);
                await _userManager.RemoveFromRolesAsync(user, userRoles);
                await _userManager.AddToRoleAsync(user, request.Role);

                return Ok(new { message = "User updated successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("users/{id}")]
        public async Task<IActionResult> DeleteUser(string id)
        {
            try
            {
                var user = await _userManager.FindByIdAsync(id);
                if (user == null)
                    return NotFound(new { message = "User not found" });

                var result = await _userManager.DeleteAsync(user);
                if (!result.Succeeded)
                {
                    return BadRequest(new { errors = result.Errors.Select(e => e.Description) });
                }

                return Ok(new { message = "User deleted successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("houses")]
        public IActionResult GetHouses()
        {
            try
            {
                var houses = _houseService.GetHouses();
                return Ok(new { houses = houses });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("houses/{id}")]
        public IActionResult GetHouse(int id)
        {
            try
            {
                var house = _houseService.GetHouseById(id);
                if (house == null)
                    return NotFound(new { message = "House not found" });

                var members = _houseService.GetHouseMembers(id);
                return Ok(new
                {
                    house = house,
                    members = members
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("houses/{id}")]
        public IActionResult DeleteHouse(int id)
        {
            try
            {
                var house = _houseService.GetHouseById(id);
                if (house == null)
                    return NotFound(new { message = "House not found" });

                _houseService.DeleteHouse(id);
                return Ok(new { message = "House deleted successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("devices")]
        public IActionResult GetDevices()
        {
            try
            {
                var devices = _deviceService.GetDevices();
                return Ok(new { devices = devices });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("devices/{id}")]
        public IActionResult GetDevice(int id)
        {
            try
            {
                var device = _deviceService.GetDeviceById(id);
                if (device == null)
                    return NotFound(new { message = "Device not found" });

                return Ok(new { device = device });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("devices/{id}")]
        public IActionResult DeleteDevice(int id)
        {
            try
            {
                var device = _deviceService.GetDeviceById(id);
                if (device == null)
                    return NotFound(new { message = "Device not found" });

                _deviceService.DeleteDevice(id);
                return Ok(new { message = "Device deleted successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpGet("roles")]
        public IActionResult GetRoles()
        {
            try
            {
                var roles = _roleManager.Roles.Select(r => r.Name).ToList();
                return Ok(new { roles = roles });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPost("roles")]
        public async Task<IActionResult> CreateRole([FromBody] CreateRoleRequest request)
        {
            try
            {
                if (await _roleManager.RoleExistsAsync(request.Name))
                {
                    return BadRequest(new { message = "Role already exists" });
                }

                var result = await _roleManager.CreateAsync(new IdentityRole(request.Name));
                if (!result.Succeeded)
                {
                    return BadRequest(new { errors = result.Errors.Select(e => e.Description) });
                }

                return Ok(new { message = "Role created successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpDelete("roles/{name}")]
        public async Task<IActionResult> DeleteRole(string name)
        {
            try
            {
                var role = await _roleManager.FindByNameAsync(name);
                if (role == null)
                    return NotFound(new { message = "Role not found" });

                var result = await _roleManager.DeleteAsync(role);
                if (!result.Succeeded)
                {
                    return BadRequest(new { errors = result.Errors.Select(e => e.Description) });
                }

                return Ok(new { message = "Role deleted successfully" });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }

        [HttpPost("users/{id}/login-as")]
        public async Task<IActionResult> LoginAsUser(string id)
        {
            try
            {
                var user = await _userManager.FindByIdAsync(id);
                if (user == null)
                    return NotFound(new { message = "User not found" });

                await _signInManager.SignOutAsync();
                await _signInManager.SignInAsync(user, isPersistent: false);

                var roles = await _userManager.GetRolesAsync(user);
                
                return Ok(new
                {
                    message = "Logged in as user successfully",
                    user = new
                    {
                        id = user.Id,
                        email = user.Email,
                        displayName = user.DisplayName,
                        roles = roles
                    }
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { message = "Internal server error" });
            }
        }
    }

    public class UpdateUserRequest
    {
        public string Email { get; set; }
        public string DisplayName { get; set; }
        public string PhoneNumber { get; set; }
        public string Role { get; set; }
    }

    public class CreateRoleRequest
    {
        public string Name { get; set; }
    }
} 