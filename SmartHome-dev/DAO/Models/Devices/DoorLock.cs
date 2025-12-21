using DAO.BaseModels;

namespace DAO.Models.Devices;

public interface IDoorLock
{
    object Lock();
    object Unlock();
    object GetLockStatus();
}

public class DoorLock : Device, IDoorLock
{
    public bool IsLocked { get; set; }
    
    public object Lock() => new { method = "lock", @params = true };
    
    public object Unlock() => new { method = "unlock", @params = false };
    
    public object GetLockStatus() => new { method = "getLockStatus", @params = new { } };
    
    public new object TurnOn() => Lock();
    
    public new object TurnOff() => Unlock();
}
