using Microsoft.EntityFrameworkCore;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Index("UserId", Name = "IX_UserClaims_UserId")]
    public partial class UserClaim
    {
        [Key]
        public int Id { get; set; }
        public string UserId { get; set; } = null!;
        public string? ClaimType { get; set; }
        public string? ClaimValue { get; set; }

        [ForeignKey("UserId")]
        [InverseProperty("UserClaims")]
        public virtual User User { get; set; } = null!;
    }
}
