using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Table("HouseMember")]
    public partial class HouseMember
    {
        [Key]
        public int ID { get; set; }
        public string? UserID { get; set; }
        public int? HouseID { get; set; }
        [StringLength(50)]
        public string? Role { get; set; }

        [ForeignKey("HouseID")]
        [InverseProperty("HouseMembers")]
        public virtual House? House { get; set; }
        [ForeignKey("UserID")]
        [InverseProperty("HouseMembers")]
        public virtual User? User { get; set; }
    }
}
