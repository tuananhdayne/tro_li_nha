using DAO.BaseModels;

namespace WebApp.Utils;

public class DeviceEqualityComparer : IEqualityComparer<Device>
{
    public bool Equals(Device x, Device y)
    {
        if (x == null || y == null)
            return false;

        return x.ID == y.ID;
    }

    public int GetHashCode(Device obj)
    {
        return obj.ID.GetHashCode();
    }
}