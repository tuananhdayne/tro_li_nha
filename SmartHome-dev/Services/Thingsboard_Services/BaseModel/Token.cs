using System.Text.Json.Serialization;

namespace Services.Thingsboard_Services.BaseModel;

public class Token
{
    public string token { get; set; }
    public string refreshToken { get; set; }
    [JsonConstructor]
    public Token(string token, string refreshToken)
    {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public override string ToString()
    {
        return token + "\n" + refreshToken;
    }
}