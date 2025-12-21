using DAO.BaseModels;

namespace DAO.Models.Devices;

public interface ITemperatureHumiditySensor
{
    object GetTemperature();
    object GetHumidity();
    object GetBothReadings();
}

public class TemperatureHumiditySensor : Device, ITemperatureHumiditySensor
{
    public double? Temperature { get; set; }  // Celsius
    public double? Humidity { get; set; }     // Percentage
    public DateTime? LastReading { get; set; }
    
    public object GetTemperature() => new { method = "getTemperature", @params = new { } };
    
    public object GetHumidity() => new { method = "getHumidity", @params = new { } };
    
    public object GetBothReadings() => new { method = "getSensorData", @params = new { } };
}
