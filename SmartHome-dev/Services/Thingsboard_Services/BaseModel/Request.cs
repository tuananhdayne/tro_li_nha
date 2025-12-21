using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
namespace Services.Thingsboard_Services.BaseModel;

public class Request<T>
{
    public string Url { get; set; }
    public string Body { get; set; }
    public Token? Token { get; set; }

    public Request(string url, string body, Token? token)
    {
        Url = url;
        Body = body;
        Token = token;
    }

    public T? Post()
    {
        using HttpClient client = new HttpClient();

        if (Token != null)
        {
            // TB standard header
            client.DefaultRequestHeaders.Add("X-Authorization", "Bearer " + Token.token);
            // Standard OAuth2 header for robustness
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", Token.token);
        }

        var content = new StringContent(Body, Encoding.UTF8, "application/json");

        Console.WriteLine($"\u001b[34m[Request] Posting to: {Url}\u001b[0m");
        Console.WriteLine($"\u001b[34m[Request] Body: {Body}\u001b[0m");

        var responseTask = client.PostAsync(Url, content);
        responseTask.Wait();
        var response = responseTask.Result;

        // Kiểm tra trạng thái phản hồi

        // Check response status
        if (!response.IsSuccessStatusCode)
        {
            var errorMessage = response.Content.ReadAsStringAsync().Result;
            switch (response.StatusCode)
            {
                case HttpStatusCode.Unauthorized:
                    throw new UnauthorizedAccessException();
                case HttpStatusCode.BadRequest:
                    throw new ArgumentException();
                case HttpStatusCode.NotFound:
                    throw new KeyNotFoundException();
                case HttpStatusCode.GatewayTimeout:
                    throw new TimeoutException();

            }
        }


        var responseString = response.Content.ReadAsStringAsync().Result;
        try
        {
            var result = JsonSerializer.Deserialize<T>(responseString);
            // Deserialize JSON thành đối tượng loại T
            return result;
        }
        catch (Exception e)
        {
            Console.WriteLine(e);
            return default;
        }
    }

    public async Task<T?> PostAsync()
    {
        using HttpClient client = new HttpClient();

        if (Token != null)
        {
            client.DefaultRequestHeaders.Add("X-Authorization", "Bearer " + Token.token);
        }

        var content = new StringContent(Body, Encoding.UTF8, "application/json");

        try
        {
            var response = await client.PostAsync(Url, content);
            Console.WriteLine($"\u001b[32mRequest URL: {Url}\u001b[0m");
            Console.WriteLine($"\u001b[32mRequest Content: {Body}\u001b[0m");

            if (!response.IsSuccessStatusCode)
            {
                var errorMessage = await response.Content.ReadAsStringAsync();
                switch (response.StatusCode)
                {
                    case HttpStatusCode.Unauthorized:
                        throw new UnauthorizedAccessException();
                    case HttpStatusCode.BadRequest:
                        throw new ArgumentException();
                    case HttpStatusCode.NotFound:
                        throw new KeyNotFoundException();
                    case HttpStatusCode.GatewayTimeout:
                        throw new TimeoutException();
                }
            }

            var responseString = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<T>(responseString);
        }
        catch (Exception ex)
        {
            Console.WriteLine(ex.Message);
            throw;
        }
    }


    public async Task<T?> GetAsync()
    {
        using HttpClient client = new HttpClient();

        if (Token != null)
        {
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", Token.token);
        }

        client.DefaultRequestHeaders.Add("Accept", "application/json");

        try
        {
            var response = await client.GetAsync(Url);
            response.EnsureSuccessStatusCode(); // Kiểm tra mã trạng thái HTTP

            var jsonResponse = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<T>(jsonResponse);
        }
        catch (HttpRequestException ex)
        {
            // Xử lý lỗi kết nối tại đây
            Console.WriteLine($"Request error: {ex.Message}");
            return default; // Hoặc ném ngoại lệ tùy theo nhu cầu của bạn
        }
    }

    public object? Delete()
    {
        using HttpClient client = new HttpClient();

        if (Token != null)
        {
            client.DefaultRequestHeaders.Add("X-Authorization", "Bearer " + Token.token);
        }

        var request = client.DeleteAsync(Url);
        request.Wait();
        // log request in console color green
        Console.WriteLine($"\u001b[32mRequest URL: {Url}\u001b[0m");

        var response = request.Result;

        // Kiểm tra trạng thái phản hồi

        // Check response status
        if (!response.IsSuccessStatusCode)
        {
            var errorMessage = response.Content.ReadAsStringAsync().Result;
            switch (response.StatusCode)
            {
                case HttpStatusCode.Unauthorized:
                    throw new UnauthorizedAccessException();
                case HttpStatusCode.BadRequest:
                    throw new ArgumentException();
                case HttpStatusCode.NotFound:
                    throw new KeyNotFoundException();
                case HttpStatusCode.GatewayTimeout:
                    throw new TimeoutException();

            }
        }

        return null;
    }
}