namespace Services.Thingsboard_Services.BaseModel;

public class Account
{
    public string Id { get; set; }
    public string Username { get; set; }
    public string Password { get; set; }
    public Account(string username, string password, string id)
    {
        Username = username;
        Password = password;
        Id = id;
    }
}