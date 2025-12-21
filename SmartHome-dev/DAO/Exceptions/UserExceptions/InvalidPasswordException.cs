namespace DAO.Exceptions.UserExceptions;

public class InvalidPasswordException : Exception
{
    public InvalidPasswordException(string? message) : base(message)
    {
    }
}