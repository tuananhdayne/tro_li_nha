using DAO.BaseModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Services.Services;
using WebApp.Utils;

namespace WebApp.Controllers
{
    public class HouseController : Controller
    {
        private readonly IHouseService _houseService;
        private readonly IUserService _userService;

        public HouseController(IHouseService houseService, IUserService userService)
        {
            _houseService = houseService;
            _userService = userService;
        }

        [Authorize]
        public IActionResult Index()
        {
            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId())
                .Take(10) // Load only the first 10 items initially
                .ToList();
            return View(houses);
        }

        public IActionResult Search(string keyword)
        {
            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId());
            if (!string.IsNullOrEmpty(keyword))
            {
                houses = houses.Where(h => StringProcessHelper.RemoveDiacritics(h.Name).Contains(keyword, StringComparison.OrdinalIgnoreCase) || StringProcessHelper.RemoveDiacritics(h.Location).Contains(keyword, StringComparison.OrdinalIgnoreCase)).ToList();
            }

            return PartialView("HouseSection", houses);
        }

        public IActionResult LoadMoreHouses(int skip, int take)
        {
            var houses = _houseService.GetHousesByUserId(_userService.GetCurrentUserId())
                .Skip(skip)
                .Take(take)
                .ToList();
            return PartialView("HouseSection", houses);
        }

        [HttpGet]
        [Authorize]
        public IActionResult Create()
        {
            return View();
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult Create(string name, string location)
        {
            if (ModelState.IsValid)
            {
                var newHouse = _houseService.CreateHouse(name, location);
                _houseService.AddHouseMember(_userService.GetCurrentUserId(), newHouse.ID, "Owner");
                return RedirectToAction("Index");
            }

            return PartialView("Create");
        }

        [Authorize]
        [HttpGet]
        public IActionResult Edit(int houseId)
        {
            Console.WriteLine("House ID: " + houseId + " User ID: " + _userService.GetCurrentUserId());
            if (!_houseService.IsHouseOwner(_userService.GetCurrentUserId(), houseId))
                return RedirectToAction("AccessDenied", "Account");

            return View("Edit", _houseService.GetHouseById(houseId));
        }

        [HttpPost]
        [ValidateAntiForgeryToken]
        public IActionResult Edit(House house)
        {
            Console.WriteLine($"[HOUSE EDIT] ID: {house.ID}, Name: {house.Name}, IsActive: {house.IsActive}");
            if (ModelState.IsValid)
            {
                _houseService.EditHouse(house);
                return RedirectToAction("Index");
            }

            return RedirectToAction("Index");
        }

        public IActionResult Delete(int houseId)
        {
            if (!_houseService.IsHouseOwner(_userService.GetCurrentUserId(), houseId))
                return RedirectToAction("AccessDenied", "Account");

            return View(_houseService.GetHouseById(houseId));
        }


        [HttpPost, ActionName("Delete")]
        [ValidateAntiForgeryToken]
        public IActionResult DeleteHouse(int houseId)
        {
            var houseMembers = _houseService.GetHouseMembers(houseId);
            Console.WriteLine("House ID: " + houseId + " User ID: " + _userService.GetCurrentUserId());
            // get the current user role in the house
            var userRole = houseMembers.FirstOrDefault(hm => hm.UserID == _userService.GetCurrentUserId())?.Role;
            if (userRole == "Owner")
            {
                _houseService.DeleteHouse(houseId);
            }
            else
            {
                _houseService.RemoveHouseMember(_userService.GetCurrentUserId(), houseId);
            }

            return RedirectToAction("Index");
        }

        public IActionResult RemoveHouseMember(int houseId, string userId)
        {
            if (!_houseService.IsHouseOwner(_userService.GetCurrentUserId(), houseId))
                return RedirectToAction("AccessDenied", "Account");
            Console.WriteLine("House ID: " + houseId + " User ID: " + userId);
            _houseService.RemoveHouseMember(userId, houseId);
            return RedirectToAction("Edit", new { houseId });
        }

        [HttpGet, ActionName("JoinHouse")]
        public IActionResult AddUserToHouse(string ownerid, int houseid)
        {
            if (!_houseService.IsHouseOwner(_userService.GetUserById(ownerid)!.Id, houseid))
                return RedirectToAction("AccessDenied", "Account");

            _houseService.AddHouseMember(_userService.GetCurrentUserId(), houseid, "Member");
            return RedirectToAction("Index");
        }
    }
}