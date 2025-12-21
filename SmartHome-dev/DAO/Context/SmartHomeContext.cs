using DAO.BaseModels;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace DAO.Context
{
    public partial class SmartHomeContext : IdentityDbContext<User>
    {
        public SmartHomeContext()
        {
        }

        public SmartHomeContext(DbContextOptions<SmartHomeContext> options)
            : base(options)
        {
        }

        public virtual DbSet<ActionHistory> ActionHistories { get; set; } = null!;
        public virtual DbSet<Device> Devices { get; set; } = null!;
        public virtual DbSet<DeviceConfig> DeviceConfigs { get; set; } = null!;
        public virtual DbSet<House> Houses { get; set; } = null!;
        public virtual DbSet<HouseMember> HouseMembers { get; set; } = null!;
        public virtual DbSet<Room> Rooms { get; set; } = null!;
        public virtual DbSet<TelemetryData> TelemetryData { get; set; } = null!;
        public virtual DbSet<UserPreference> UserPreferences { get; set; } = null!;
        

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            if (!optionsBuilder.IsConfigured)
            {
                optionsBuilder.UseSqlServer("Server=(local);Database=SmartHome;Integrated Security=True;MultipleActiveResultSets=True;TrustServerCertificate=True");
            }
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);
            modelBuilder.Entity<ActionHistory>(entity =>
            {
                entity.Property(e => e.Timestamp).HasDefaultValueSql("(getdate())");

                entity.HasOne(d => d.User)
                    .WithMany(p => p.ActionHistories)
                    .HasForeignKey(d => d.UserID)
                    .OnDelete(DeleteBehavior.Cascade)
                    .HasConstraintName("FK__ActionHis__UserI__60A75C0F");
            });

            modelBuilder.Entity<Device>(entity =>
            {
                entity.HasOne(d => d.Room)
                    .WithMany(p => p.Devices)
                    .HasForeignKey(d => d.RoomID)
                    .OnDelete(DeleteBehavior.SetNull)
                    .HasConstraintName("FK__Device__RoomID__5535A963");

                entity.HasOne(d => d.User)
                    .WithMany(p => p.Devices)
                    .HasForeignKey(d => d.UserID)
                    .OnDelete(DeleteBehavior.SetNull)
                    .HasConstraintName("FK__Device__UserID__5441852A");
            });

            modelBuilder.Entity<DeviceConfig>(entity =>
            {
                entity.Property(e => e.LastUpdate).HasDefaultValueSql("(getdate())");

                entity.HasOne(d => d.Device)
                    .WithMany(p => p.DeviceConfigs)
                    .HasForeignKey(d => d.DeviceID)
                    .OnDelete(DeleteBehavior.Cascade)
                    .HasConstraintName("FK__DeviceCon__Devic__5CD6CB2B");
            });

            modelBuilder.Entity<HouseMember>((Action<Microsoft.EntityFrameworkCore.Metadata.Builders.EntityTypeBuilder<HouseMember>>)(entity =>
            {
                entity.HasOne(d => d.House)
                    .WithMany(p => p.HouseMembers)
                    .HasForeignKey((System.Linq.Expressions.Expression<Func<HouseMember, object?>>)(d => (object?)d.HouseID))
                    .OnDelete(DeleteBehavior.Cascade)
                    .HasConstraintName("FK__HouseMemb__House__4E88ABD4");

                entity.HasOne(d => d.User)
                    .WithMany(p => p.HouseMembers)
                    .HasForeignKey(d => d.UserID)
                    .OnDelete(DeleteBehavior.Cascade)
                    .HasConstraintName("FK__HouseMemb__UserI__4D94879B");

            }));

            modelBuilder.Entity<Room>(entity =>
            {
                entity.HasOne(d => d.House)
                    .WithMany(p => p.Rooms)
                    .HasForeignKey(d => d.HouseID)
                    .OnDelete(DeleteBehavior.Cascade)
                    .HasConstraintName("FK__Room__HouseID__5165187F");
            });

            modelBuilder.Entity<TelemetryData>(entity =>
            {
                entity.Property(e => e.Timestamp).HasDefaultValueSql("(getdate())");

                entity.HasOne(d => d.Device)
                    .WithMany(p => p.TelemetryData)
                    .HasForeignKey(d => d.DeviceID)
                    .OnDelete(DeleteBehavior.Cascade)
                    .HasConstraintName("FK__Telemetry__Devic__59063A47");
            });

            modelBuilder.Entity<UserPreference>(entity =>
            {
                entity.HasOne(d => d.User)
                    .WithMany(p => p.UserPreferences)
                    .HasForeignKey(d => d.UserID)
                    .OnDelete(DeleteBehavior.Cascade)
                    .HasConstraintName("FK__UserPrefe__UserI__6383C8BA");
            });

            foreach (var item in modelBuilder.Model.GetEntityTypes())
            {
                var tableName = item.GetTableName();
                if (tableName != null && tableName.StartsWith("AspNet"))
                {
                    item.SetTableName(tableName.Substring(6));
                }
            }

            OnModelCreatingPartial(modelBuilder);
        }

        partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
    }
}
