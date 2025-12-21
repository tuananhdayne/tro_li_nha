using DAO.BaseModels;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Services.Services;
using System.Security.Claims;
using WebApp.Models;

namespace WebApp.Controllers
{
    [Route("Account/")]
    public class AccountController : Controller
    {
        private readonly SignInManager<User> _signInManager;
        private readonly UserManager<User> _userManager;
        private readonly IUserService _userService;
        private ILogger<AccountController> _logger;

        public AccountController(UserManager<User> userManager, SignInManager<User> signInManager,
            IUserService userService, ILogger<AccountController> logger)
        {
            _userManager = userManager;
            _signInManager = signInManager;
            _userService = userService;
            _logger = logger;
        }

        [HttpGet("login")]
        [AllowAnonymous]
        public IActionResult Login()
        {
            _logger.LogInformation(User.Identity.Name, "User logged in.");
            if (User.Identity.IsAuthenticated)
            {
                return RedirectToAction("Index", "Home");
            }

            return View();
        }

        [HttpPost("login")]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Login(AccountViewModel model)
        {
            if (!ModelState.IsValid)
            {
                foreach (var state in ModelState)
                {
                    foreach (var error in state.Value.Errors)
                    {
                        Console.WriteLine($"Key: {state.Key}, Error: {error.ErrorMessage}");
                    }
                }
            }

            if (ModelState.IsValid)
            {
                var result = await _signInManager.PasswordSignInAsync(model.LoginModel.Email, model.LoginModel.Password,
                    model.LoginModel.RememberMe, lockoutOnFailure: false);
                if (result.Succeeded)
                {
                    // add claims
                    var user = await _userManager.FindByNameAsync(model.LoginModel.Email);
                    // var roles = await _userManager.GetRolesAsync(user);
                    var claims = new List<Claim>
                    {
                        new Claim(ClaimTypes.NameIdentifier, user.Id),
                        new Claim(ClaimTypes.Name, user.UserName),
                        new Claim(ClaimTypes.Email, user.Email)
                    };
                    // Add role claims
                    // claims.AddRange(roles.Select(role => new Claim(ClaimTypes.Role, role)));

                    var claimsIdentity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
                    var authProperties = new AuthenticationProperties
                    {
                        IsPersistent = model.LoginModel.RememberMe
                    };
                    await HttpContext.SignInAsync(new ClaimsPrincipal(claimsIdentity), authProperties);
                    return RedirectToAction("Index", "House");
                }

                ModelState.AddModelError("", "Invalid login attempt.");
            }

            return View(model);
        }

        [HttpGet("register")]
        [AllowAnonymous]
        public IActionResult Register()
        {
            return View("Register", new AccountViewModel());
        }

        [HttpPost("register")]
        [AllowAnonymous]
        [ValidateAntiForgeryToken]
        public async Task<IActionResult> Register(AccountViewModel model)
        {
            if (!ModelState.IsValid)
            {
                foreach (var state in ModelState)
                {
                    foreach (var error in state.Value.Errors)
                    {
                        Console.WriteLine($"Key: {state.Key}, Error: {error.ErrorMessage}");
                    }
                }
            }

            if (ModelState.IsValid)
            {
                var user = new User { UserName = model.RegisterModel.Email, Email = model.RegisterModel.Email };
                var result = await _userManager.CreateAsync(user, model.RegisterModel.Password);
                if (result.Succeeded)
                {
                    await _userManager.AddToRoleAsync(user, "Member");
                    await _signInManager.SignInAsync(user, isPersistent: false);
                    return RedirectToAction("Index", "House");
                }

                foreach (var error in result.Errors)
                {
                    ModelState.AddModelError("", error.Description);
                }
            }

            return View("Login", model);
        }

        [HttpGet("logout")]
        // [ValidateAntiForgeryToken]
        public async Task<IActionResult> Logout()
        {
            await _signInManager.SignOutAsync();
            return RedirectToAction("Index", "House");
        }

        [HttpGet("accessdenied")]
        public IActionResult AccessDenied()
        {
            return View();
        }

        [HttpGet("changepassword")]
        [Authorize]
        public IActionResult ChangePassword()
        {
            return View(new ChangePasswordModel());
        }

        [HttpPost("changepassword")]
        [ValidateAntiForgeryToken]
        public IActionResult ChangePassword(ChangePasswordModel model)
        {
            if (!ModelState.IsValid)
            {
                foreach (var state in ModelState)
                {
                    foreach (var error in state.Value.Errors)
                    {
                        Console.WriteLine($"Key: {state.Key}, Error: {error.ErrorMessage}");
                    }
                }
            }

            if (ModelState.IsValid)
            {
                var user = _userService.GetLoggedInUser();
                var result = _userManager.ChangePasswordAsync(user, model.OldPassword, model.NewPassword)
                    .GetAwaiter().GetResult();
                if (result.Succeeded)
                {
                    return RedirectToAction("Index", "House");
                }

                foreach (var error in result.Errors)
                {
                    ModelState.AddModelError("", error.Description);
                }
            }

            return View(model);
        }

        [HttpGet("settings")]
        [Authorize]
        public IActionResult Settings()
        {
            return View();
        }

        [HttpGet("accountdetails")]
        [Authorize]
        public IActionResult Index()
        {
            return View(_userService.GetLoggedInUser());
        }

        [HttpPost("accountdetails")]
        [ValidateAntiForgeryToken]
        public IActionResult Edit(User user)
        {
            if (!ModelState.IsValid)
            {
                foreach (var state in ModelState)
                {
                    foreach (var error in state.Value.Errors)
                    {
                        Console.WriteLine($"Key: {state.Key}, Error: {error.ErrorMessage}");
                    }
                }
                return View("Index", user);
            }

            var updatedUser = _userService.EditUser(user);
            if (updatedUser != null)
            {
                return RedirectToAction("Index");
            }

            ModelState.AddModelError("", "Failed to update user information.");
            return View("Index", user);
        }

        [HttpGet("forgotpassword")]
        [AllowAnonymous]
        public IActionResult ForgotPassword()
        {
            return View(_userService.GetLoggedInUser());
        }
    }
}