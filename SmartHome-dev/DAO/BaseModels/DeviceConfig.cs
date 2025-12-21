using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Table("DeviceConfig")]
    public partial class DeviceConfig
    {
        [Key]
        public int ID { get; set; }
        public int? DeviceID { get; set; }
        public string? Detail { get; set; }
        [Column(TypeName = "datetime")]
        public DateTime? LastUpdate { get; set; }

        [ForeignKey("DeviceID")]
        [InverseProperty("DeviceConfigs")]
        public virtual Device? Device { get; set; }
    }
}
