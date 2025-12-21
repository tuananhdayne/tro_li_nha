using Microsoft.EntityFrameworkCore;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Index("UserId", Name = "IX_UserLogins_UserId")]
    public partial class UserLogin
    {
        [Key]
        public string LoginProvider { get; set; } = null!;
        [Key]
        public string ProviderKey { get; set; } = null!;
        public string? ProviderDisplayName { get; set; }
        public string UserId { get; set; } = null!;

        [ForeignKey("UserId")]
        [InverseProperty("UserLogins")]
        public virtual User User { get; set; } = null!;
    }
}
