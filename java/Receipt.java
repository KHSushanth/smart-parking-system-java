import java.time.*;

public class Receipt {
    public final String plate;
    public final String vehicleType;
    public final int ticketId;
    public final LocalDateTime entryTime;
    public final LocalDateTime exitTime;
    public final int entryFloor;
    public final int entrySlotNumber;
    public final double parkedDurationHours;
    public final int billableHours;
    public final double hourlyRate;
    public final double fee;

    public Receipt(String plate, String vehicleType, int ticketId,
                   LocalDateTime entryTime, LocalDateTime exitTime,
                   int entryFloor, int entrySlotNumber,
                   double parkedDurationHours, int billableHours,
                   double hourlyRate, double fee) {
        this.plate = plate;
        this.vehicleType = vehicleType;
        this.ticketId = ticketId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.entryFloor = entryFloor;
        this.entrySlotNumber = entrySlotNumber;
        this.parkedDurationHours = parkedDurationHours;
        this.billableHours = billableHours;
        this.hourlyRate = hourlyRate;
        this.fee = fee;
    }
}

