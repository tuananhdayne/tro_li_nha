using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Table("ActionHistory")]
    public partial class ActionHistory
    {
        [Key]
        public int ID { get; set; }
        public string? UserID { get; set; }
        [StringLength(50)]
        public string? EntityType { get; set; }
        public int? EntityID { get; set; }
        [StringLength(100)]
        public string? Action { get; set; }
        [Column(TypeName = "datetime")]
        public DateTime? Timestamp { get; set; }
        public string? Detail { get; set; }

        [ForeignKey("UserID")]
        [InverseProperty("ActionHistories")]
        public virtual User? User { get; set; }
    }
}
