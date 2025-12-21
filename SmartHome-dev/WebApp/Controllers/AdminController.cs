using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Services.Services;

namespace WebApp.Controllers;
[Authorize(Roles = "Admin")]
public class AdminController : Controller
{
    private readonly IHouseService _houseService;
    private readonly IDeviceService _deviceService;
    private readonly IUserService _userService;
    private readonly UserManager<User> _userManager;
    private readonly SignInManager<User> _signInManager;
    private readonly RoleManager<IdentityRole> _roleManager;

    public AdminController(IHouseService houseService, IDeviceService deviceService, IUserService userService, UserManager<User> userManager, SignInManager<User> signInManager, RoleManager<IdentityRole> roleManager)
    {
        _houseService = houseService;
        _deviceService = deviceService;
        _userService = userService;
        _userManager = userManager;
        _signInManager = signInManager;
        _roleManager = roleManager;
    }


    public IActionResult Index()
    {
        return View();
    }

    public IActionResult ManageUsers()
    {
        var users = _userService.GetUsers();
        return View(users);
    }


    public IActionResult ManageHouses()
    {
        var houses = _houseService.GetHouses();
        return View(houses);
    }


    public IActionResult ManageDevices()
    {
        var devices = _deviceService.GetDevices();
        return View(devices);
    }

    public async Task<IActionResult> LoginAsUser(string id)
    {
        var user = await _userManager.FindByIdAsync(id);
        if (user == null)
        {
            return NotFound();
        }

        await _signInManager.SignOutAsync();
        await _signInManager.SignInAsync(user, isPersistent: false);

        return RedirectToAction("Index", "House");
    }

    public async Task<IActionResult> EditUser(string id)
    {
        var user = await _userManager.FindByIdAsync(id);
        if (user == null)
        {
            return NotFound();
        }
        // SelectList roles = new SelectList(_roleManager.Roles.ToList(), "Name", "Name");
        // Get all roles
        var roles = _roleManager.Roles.Select(r => new SelectListItem
        {
            Text = r.Name,
            Value = r.Name
        }).ToList();
        ViewBag.Roles = roles;
        return View(user);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> UpdateUserAndAddRole(string userId, string newDisplayName, string newPhoneNumber, string newEmail, string roleName)
    {
        var user = await _userManager.FindByIdAsync(userId);
        if (user == null)
        {
            return NotFound();
        }

        // Update user information
        user.Email = newEmail;
        user.DisplayName = newDisplayName;
        user.PhoneNumber = newPhoneNumber;
        var updateResult = await _userManager.UpdateAsync(user);
        if (!updateResult.Succeeded)
        {
            return BadRequest(updateResult.Errors);
        }

        // Check if the role exists, if not, create it
        if (!await _roleManager.RoleExistsAsync(roleName))
        {
            var roleResult = await _roleManager.CreateAsync(new IdentityRole(roleName));
            if (!roleResult.Succeeded)
            {
                return BadRequest(roleResult.Errors);
            }
        }

        // clear user roles
        var userRoles = await _userManager.GetRolesAsync(user);
        var removeRoleResult = await _userManager.RemoveFromRolesAsync(user, userRoles);


        // add new role
        var addRoleResult = await _userManager.AddToRoleAsync(user, roleName);

        return RedirectToAction("ManageUsers");
    }

}