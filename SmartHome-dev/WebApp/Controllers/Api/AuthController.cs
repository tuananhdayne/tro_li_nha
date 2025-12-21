using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Services.Services;
using WebApp.Models;

namespace WebApp.Controllers.Api
{
    [Route("api/[controller]")]
    [ApiController]
    public class AuthController : ControllerBase
    {
        private readonly SignInManager<User> _signInManager;
        private readonly UserManager<User> _userManager;
        private readonly IUserService _userService;
        private readonly IConfiguration _configuration;

        public AuthController(
            UserManager<User> userManager, 
            SignInManager<User> signInManager,
            IUserService userService,
            IConfiguration configuration)
        {
            _userManager = userManager;
            _signInManager = signInManager;
            _userService = userService;
            _configuration = configuration;
        }

        [HttpPost("login")]
        [AllowAnonymous]
        public async Task<IActionResult> Login([FromBody] LoginModel model)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var result = await _signInManager.PasswordSignInAsync(model.Email, model.Password, false, lockoutOnFailure: false);
            if (result.Succeeded)
            {
                var user = await _userManager.FindByNameAsync(model.Email);
                var roles = await _userManager.GetRolesAsync(user);
                
                var token = GenerateJwtToken(user, roles);
                
                return Ok(new
                {
                    token = token,
                    user = new
                    {
                        id = user.Id,
                        email = user.Email,
                        displayName = user.DisplayName,
                        roles = roles
                    }
                });
            }

            return Unauthorized(new { message = "Invalid login attempt." });
        }

        [HttpPost("register")]
        [AllowAnonymous]
        public async Task<IActionResult> Register([FromBody] RegisterModel model)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var user = new User { UserName = model.Email, Email = model.Email };
            var result = await _userManager.CreateAsync(user, model.Password);
            
            if (result.Succeeded)
            {
                await _userManager.AddToRoleAsync(user, "Member");
                
                var roles = await _userManager.GetRolesAsync(user);
                var token = GenerateJwtToken(user, roles);
                
                return Ok(new
                {
                    token = token,
                    user = new
                    {
                        id = user.Id,
                        email = user.Email,
                        displayName = user.DisplayName,
                        roles = roles
                    }
                });
            }   

            return BadRequest(new { errors = result.Errors.Select(e => e.Description) });
        }

        [HttpPost("logout")]
        [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
        public async Task<IActionResult> Logout()
        {
            await _signInManager.SignOutAsync();
            return Ok(new { message = "Logged out successfully" });
        }

        [HttpPost("change-password")]
        [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
        public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordModel model)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var user = _userService.GetLoggedInUser();
            var result = await _userManager.ChangePasswordAsync(user, model.OldPassword, model.NewPassword);
            
            if (result.Succeeded)
            {
                return Ok(new { message = "Password changed successfully" });
            }

            return BadRequest(new { errors = result.Errors.Select(e => e.Description) });
        }

        [HttpGet("profile")]
        [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
        public IActionResult GetProfile()
        {
            var user = _userService.GetLoggedInUser();
            return Ok(new
            {
                id = user.Id,
                email = user.Email,
                displayName = user.DisplayName,
                phoneNumber = user.PhoneNumber
            });
        }

        [HttpPut("profile")]
        [Authorize(AuthenticationSchemes = "Bearer,Identity.Application")]
        public IActionResult UpdateProfile([FromBody] User userUpdate)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }
            
            // get current user
            var currentUser = _userService.GetLoggedInUser();
            if (currentUser == null || currentUser.Id != userUpdate.Id)
            {
                return Unauthorized(new { message = "You can only update your own profile." });
            }
            currentUser.DisplayName = userUpdate.DisplayName;
            currentUser.PhoneNumber = userUpdate.PhoneNumber;
            
            
            var updatedUser = _userService.EditUser(currentUser);
            if (updatedUser != null)
            {
                return Ok(new
                {
                    id = updatedUser.Id,
                    email = updatedUser.Email,
                    displayName = updatedUser.DisplayName,
                    phoneNumber = updatedUser.PhoneNumber
                });
            }

            return BadRequest(new { message = "Failed to update user information." });
        }

        private string GenerateJwtToken(User user, IList<string> roles)
        {
            var claims = new List<Claim>
            {
                new Claim(ClaimTypes.NameIdentifier, user.Id),
                new Claim(ClaimTypes.Name, user.UserName),
                new Claim(ClaimTypes.Email, user.Email)
            };

            // Add role claims
            claims.AddRange(roles.Select(role => new Claim(ClaimTypes.Role, role)));

            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_configuration["Jwt:Secret"]));
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var token = new JwtSecurityToken(
                issuer: _configuration["Jwt:Issuer"],
                audience: _configuration["Jwt:Audience"],
                claims: claims,
                expires: DateTime.Now.AddDays(7),
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
} 