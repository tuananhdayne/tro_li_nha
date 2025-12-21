using DAO.Models.Devices;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Table("Device")]
    public class Device : IDevice
    {
        public Device()
        {
            DeviceConfigs = new HashSet<DeviceConfig>();
            TelemetryData = new HashSet<TelemetryData>();
        }

        [Key]
        public int ID { get; set; }
        [StringLength(255)]
        public string? Name { get; set; }
        [StringLength(50)]
        [Column(TypeName = "nvarchar(50)")]
        public string? Type { get; set; }
        [StringLength(255)]
        public string? DeviceToken { get; set; }
        public string? UserID { get; set; }
        public int? RoomID { get; set; }
        [StringLength(50)]
        public string? Status { get; set; }
        public string? TbDeviceId { get; set; }
        [StringLength(20)]
        public string? MacAddress { get; set; }  // ESP32 MAC address (e.g., "9451DC339E50")

        [ForeignKey("RoomID")]
        [InverseProperty("Devices")]
        public virtual Room? Room { get; set; }
        [ForeignKey("UserID")]
        [InverseProperty("Devices")]
        public virtual User? User { get; set; }
        [InverseProperty("Device")]
        public virtual ICollection<DeviceConfig> DeviceConfigs { get; set; }
        [InverseProperty("Device")]
        public virtual ICollection<TelemetryData> TelemetryData { get; set; }

        public override string ToString()
        {
            Console.WriteLine("Name: " + Name, "Type: " + Type, "DeviceToken: " + DeviceToken, "UserID: " + UserID, "RoomID: " + RoomID, "Status: " + Status);
            return base.ToString();
        }

        public object TurnOn()
        {
            throw new NotImplementedException();
        }

        public object TurnOff()
        {
            throw new NotImplementedException();
        }
    }
}
