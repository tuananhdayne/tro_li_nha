using DAO.BaseModels;

namespace DAO.Models.Devices;

public interface IMotionSensor
{
    object GetMotionStatus();
    object GetLastMotionTime();
}

public class MotionSensor : Device, IMotionSensor
{
    public bool MotionDetected { get; set; }
    public DateTime? LastMotionTime { get; set; }
    
    public object GetMotionStatus() => new { method = "getMotionStatus", @params = new { } };
    
    public object GetLastMotionTime() => new { method = "getLastMotion", @params = new { } };
}
