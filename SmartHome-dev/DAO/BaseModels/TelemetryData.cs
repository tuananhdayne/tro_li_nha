using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    public partial class TelemetryData
    {
        [Key]
        public int ID { get; set; }
        public int? DeviceID { get; set; }
        public string? Body { get; set; }
        [Column(TypeName = "datetime")]
        public DateTime? Timestamp { get; set; }

        [ForeignKey("DeviceID")]
        [InverseProperty("TelemetryData")]
        public virtual Device? Device { get; set; }

        public override string ToString()
        {
            return $"Device ID: {DeviceID}, Body: {Body}";
        }
    }
}
