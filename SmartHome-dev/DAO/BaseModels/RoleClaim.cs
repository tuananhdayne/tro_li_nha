using Microsoft.EntityFrameworkCore;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Index("RoleId", Name = "IX_RoleClaims_RoleId")]
    public partial class RoleClaim
    {
        [Key]
        public int Id { get; set; }
        public string RoleId { get; set; } = null!;
        public string? ClaimType { get; set; }
        public string? ClaimValue { get; set; }

        [ForeignKey("RoleId")]
        [InverseProperty("RoleClaims")]
        public virtual Role Role { get; set; } = null!;
    }
}
