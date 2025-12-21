namespace DAO.Models.Devices;

public interface ILight : IDevice
{
    object SetDim(int dim);
}