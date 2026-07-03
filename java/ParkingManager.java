import java.time.*;
import java.util.*;

public class ParkingManager {
    private final Database db;
    private final LotConfig config;

    public ParkingManager(String dbPath, String configPath) {
        this.db = new Database(dbPath);
        this.config = Config.load(configPath);
    }

    private void validateVehicleType(String vehicleType) {
        if (!config.vehicleTypes.contains(vehicleType)) {
            throw new InvalidVehicleTypeError("Invalid vehicle type '" + vehicleType + "'. Allowed: " + config.vehicleTypes);
        }
    }

    private static int ceilHours(double durationHours) {
        if (durationHours <= 0) return 1;
        int c = (int) Math.ceil(durationHours);
        return Math.max(1, c);
    }

    private FeeCalc calcFee(LocalDateTime entryTime, LocalDateTime exitTime, String vehicleType) {
        double durationSeconds = Duration.between(entryTime, exitTime).getSeconds();
        double durationHours = durationSeconds / 3600.0;
        int billableHours = ceilHours(durationHours);
        double hourlyRate = config.hourlyRates.get(vehicleType);
        double fee = billableHours * hourlyRate;
        return new FeeCalc(durationHours, billableHours, fee);
    }

    public Receipt park(String vehicleType, String plate) {
        vehicleType = vehicleType.trim();
        plate = plate.trim().toUpperCase(Locale.ROOT);
        validateVehicleType(vehicleType);

        if (db.findActiveTicketByPlate(plate) != null) {
            throw new VehicleAlreadyParkedError("Vehicle '" + plate + "' is already parked");
        }

        LocalDateTime entryTime = LocalDateTime.now();

        String direction = config.nearestSlotDirection;
        int totalSlotsForType = config.perFloorSlots.get(vehicleType);

        for (int floor = 1; floor <= config.floors; floor++) {
            Database.Available avail = db.getAvailableSlotsForTypeOnFloor(floor, vehicleType, totalSlotsForType);

            if (avail.count() <= 0) continue;

            List<Integer> slots = avail.slots();




            boolean descending = direction.equals("descending_slot_number");
            slots.sort(descending ? Comparator.reverseOrder() : Comparator.naturalOrder());
            int chosenSlot = slots.get(0);

            int ticketId = db.insertTicket(plate, vehicleType, entryTime, floor, chosenSlot);
            return new Receipt(plate, vehicleType, ticketId, entryTime, entryTime, floor, chosenSlot,
                    0.0, 0, config.hourlyRates.get(vehicleType), 0.0);
        }

        throw new LotFullError("No available slots for " + vehicleType + " in the entire lot");
    }

    public Receipt exit(String plate) {
        plate = plate.trim().toUpperCase(Locale.ROOT);
        LocalDateTime exitTime = LocalDateTime.now();

        TicketInfo active = db.findActiveTicketByPlate(plate);
        if (active == null) {
            throw new VehicleNotParkedError("Vehicle '" + plate + "' is not currently parked");
        }

        LocalDateTime entryTime;
        // handle legacy/DB datetime string parsing

        try {
            // stored as MySQL DATETIME string like: yyyy-MM-dd HH:mm:ss
            entryTime = LocalDateTime.parse(active.entryTime);
        } catch (java.time.format.DateTimeParseException ex) {

            // fallback: normalize to the expected format
            String s = active.entryTime.trim().replace('T', ' ');
            entryTime = LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        FeeCalc fc = calcFee(entryTime, exitTime, active.vehicleType);

        db.markExitAndComputeFee(active.ticketId, exitTime, fc.fee);

        return new Receipt(active.plate, active.vehicleType, active.ticketId, entryTime, exitTime,
                active.floor, active.slotNumber, fc.durationHours, fc.billableHours,
                config.hourlyRates.get(active.vehicleType), fc.fee);
    }

    public Map<String, Map<String, Map<String, Integer>>> getLiveStatus() {
        Map<String, Map<String, Integer>> occupied = db.getOccupiedCountByFloorAndType(config.floors, config.vehicleTypes);
        Map<String, Map<String, Map<String, Integer>>> status = new LinkedHashMap<>();
        for (int f = 1; f <= config.floors; f++) {
            String floorKey = "floor_" + f;
            status.put(floorKey, new LinkedHashMap<>());
            for (String vt : config.vehicleTypes) {
                int total = config.perFloorSlots.get(vt);
                int occ = occupied.getOrDefault(floorKey, Collections.emptyMap()).getOrDefault(vt, 0);
                Map<String, Integer> st = new LinkedHashMap<>();
                st.put("occupied", occ);
                st.put("available", total - occ);
                st.put("total", total);
                status.get(floorKey).put(vt, st);
            }
        }
        return status;
    }

    public List<TicketInfo> searchHistory(String plate) {
        return db.getHistoryByPlate(plate.trim().toUpperCase(Locale.ROOT));
    }

    public Map<String,Object> getRevenueReport() {
        return db.getRevenueReport();
    }








    private record FeeCalc(double durationHours, int billableHours, double fee) {}


}

