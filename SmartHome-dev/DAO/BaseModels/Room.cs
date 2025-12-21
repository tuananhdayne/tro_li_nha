using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Table("Room")]
    public partial class Room
    {
        public Room()
        {
            Devices = new HashSet<Device>();
        }

        [Key]
        public int ID { get; set; }
        [StringLength(255)]
        public string? Name { get; set; }
        [StringLength(255)]
        public string? Detail { get; set; }
        public int? HouseID { get; set; }
        
        // Trạng thái kích hoạt phòng - chỉ khi bật mới điều khiển được thiết bị
        public bool IsActive { get; set; } = false;

        [ForeignKey("HouseID")]
        [InverseProperty("Rooms")]
        public virtual House? House { get; set; }
        [InverseProperty("Room")]
        public virtual ICollection<Device> Devices { get; set; }

        public override string ToString()
        {
            Console.WriteLine("Name: " + Name, "Detail: " + Detail);
            return base.ToString();
        }
    }
}
