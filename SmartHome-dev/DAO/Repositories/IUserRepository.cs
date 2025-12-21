using DAO.BaseModels;

namespace DAO.Repositories
{
    public interface IUserRepository
    {
        User AddUser(User user);
        void DeleteUser(string username);
        User UpdateUser(User user);
        User GetUserByUsername(string username);
        User GetLoggedInUser();
        IEnumerable<User> GetAllUsers();
        User? GetUserById(string userId);
    }
}
