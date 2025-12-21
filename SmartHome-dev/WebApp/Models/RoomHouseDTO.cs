namespace WebApp.Models
{
    public class RoomDTO
    {
        public int Id { get; set; }
        public string Name { get; set; }
        public int HouseId { get; set; }
    }

    public class HouseDTO
    {
        public int Id { get; set; }
        public string Name { get; set; }
    }
}
