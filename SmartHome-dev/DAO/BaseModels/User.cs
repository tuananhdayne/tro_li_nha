using Microsoft.AspNetCore.Identity;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace DAO.BaseModels
{
    [Table("User")]
    public partial class User : IdentityUser
    {
        public User()
        {
            ActionHistories = new HashSet<ActionHistory>();
            Devices = new HashSet<Device>();
            HouseMembers = new HashSet<HouseMember>();
            UserPreferences = new HashSet<UserPreference>();
        }

        [StringLength(50)]
        [Column(TypeName = "nvarchar")]
        public string? DisplayName { get; set; }

        [InverseProperty("User")]
        public virtual ICollection<ActionHistory> ActionHistories { get; set; }

        [InverseProperty("User")]
        public virtual ICollection<Device> Devices { get; set; }

        [InverseProperty("User")]
        public virtual ICollection<HouseMember> HouseMembers { get; set; }

        [InverseProperty("User")]
        public virtual ICollection<UserPreference> UserPreferences { get; set; }

        public override string ToString()
        {
            Console.WriteLine("Name: " + DisplayName, "Email: " + Email);
            return base.ToString();
        }
    }
}
