public class TicketInfo {
    public final int ticketId;
    public final String plate;
    public final String vehicleType;
    public final String entryTime;
    public final String exitTime;
    public final int floor;
    public final int slotNumber;
    public final Double fee;

    public TicketInfo(int ticketId, String plate, String vehicleType, String entryTime, String exitTime,
                       int floor, int slotNumber, Double fee) {
        this.ticketId = ticketId;
        this.plate = plate;
        this.vehicleType = vehicleType;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.floor = floor;
        this.slotNumber = slotNumber;
        this.fee = fee;
    }
}

