namespace Services.Thingsboard_Services.BaseModel;

public class Response<T>
{
    public Response(T data)
    {
        Data = data;
    }

    public T Data { get; set; }
    public override string ToString()
    {
        return Data.ToString();
    }
}