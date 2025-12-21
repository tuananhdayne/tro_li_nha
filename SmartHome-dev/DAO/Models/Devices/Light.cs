using DAO.BaseModels;

namespace DAO.Models.Devices;

public class Light : Device, ILight
{
    public int? Dim { get; set; }

    public object TurnOn() => new { method = "setLedStatus", @params = 1 };

    public object TurnOff() => new { method = "setLedStatus", @params = 0 };

    public object SetDim(int dim)
    {
        Dim = dim;
        return new { method = "setLedDim", @params = dim };
    }
}