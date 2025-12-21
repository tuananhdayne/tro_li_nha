using System.Security.Claims;
using User = DAO.BaseModels.User;

namespace Services.Services;

public interface IUserService
{
    User EditUser(User user);
    User GetUserByUsername(string username);
    User GetLoggedInUser();
    User? GetUserById(string userId);
    IEnumerable<User> GetUsers();

    string GetCurrentUserId();
    void SetHttpContext(ClaimsPrincipal user);

    Task Login(string username, string password, bool persistent);
    Task SignUp(string username, string password, string email, string confirmPassword);
    void Logout();
}