using DAO.Exceptions.UserExceptions;
using DAO.Repositories;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Identity;
using Services.Services;
using System.Security.Claims;
using User = DAO.BaseModels.User;

namespace Services.Services_Impl;

public class UserService : IUserService
{
    private readonly IUserRepository _userRepository;
    private readonly UserManager<User> _userManager;
    private readonly IHttpContextAccessor _httpContextAccessor;
    private readonly SignInManager<User> _signInManager;
    public UserService(IUserRepository userRepository, UserManager<User> userManager, IHttpContextAccessor httpContextAccessor, SignInManager<User> signInManager)
    {
        _userRepository = userRepository;
        _userManager = userManager;
        _httpContextAccessor = httpContextAccessor;
        _signInManager = signInManager;
    }
    public User EditUser(User user)
    {
        var userToUpdate = _userRepository.GetUserByUsername(user.UserName);
        if (userToUpdate == null)
        {
            throw new UserNotFoundException("User not found");
        }
        userToUpdate = user;
        _userRepository.UpdateUser(userToUpdate);
        return userToUpdate;
    }

    public User GetUserByUsername(string username)
    {
        return _userRepository.GetUserByUsername(username);
    }

    public IEnumerable<User> GetUsers()
    {
        return _userRepository.GetAllUsers();
    }

    public string GetCurrentUserId()
    {
        return _httpContextAccessor.HttpContext?.User?.FindFirstValue(ClaimTypes.NameIdentifier);
    }

    public async Task<User> GetCurrentUserAsync()
    {
        return await _userManager.GetUserAsync(_httpContextAccessor.HttpContext?.User);
    }
    public User GetLoggedInUser()
    {
        return GetCurrentUserAsync().GetAwaiter().GetResult();
    }

    public User? GetUserById(string userId)
    {
        return _userRepository.GetUserById(userId);
    }

    public void SetHttpContext(ClaimsPrincipal user)
    {
        var context = new DefaultHttpContext();
        context.User = user;
        _httpContextAccessor.HttpContext = context;
    }

    public async Task Login(string username, string password, bool persistent)
    {
        var user = _userRepository.GetUserByUsername(username);
        if (user == null)
        {
            throw new UserNotFoundException("User not found");
        }
        var result = await _signInManager.PasswordSignInAsync(user, password, persistent, false);
        var claims = new List<Claim>
        {
            new Claim(ClaimTypes.NameIdentifier, user.Id),
            new Claim(ClaimTypes.Name, user.UserName),
            new Claim(ClaimTypes.Email, user.Email)
        };
        var identity = new ClaimsIdentity(claims, "login");
        var principal = new ClaimsPrincipal(identity);
        SetHttpContext(principal);
    }

    public void Logout()
    {
        _signInManager.SignOutAsync().GetAwaiter().GetResult();
    }

    public async Task SignUp(string username, string password, string email, string confirmPassword)
    {
        var user = new User { UserName = username, Email = email };
        if (password != confirmPassword)
        {
            throw new ArgumentException("Passwords do not match");
        }
        var result = await _userManager.CreateAsync(user, password);
        if (result.Succeeded)
        {
            await _userManager.AddToRoleAsync(user, "Member");
            await _signInManager.SignInAsync(user, isPersistent: false);
        }
    }
}