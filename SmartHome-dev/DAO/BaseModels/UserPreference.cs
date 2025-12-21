using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Table("UserPreference")]
    public partial class UserPreference
    {
        [Key]
        public int ID { get; set; }
        public string? UserID { get; set; }
        public string? Detail { get; set; }

        [ForeignKey("UserID")]
        [InverseProperty("UserPreferences")]
        public virtual User? User { get; set; }
    }
}
