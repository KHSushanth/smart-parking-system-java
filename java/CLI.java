import java.time.*;
import java.util.*;

public class CLI {
    public static void run(String dbPath, String configPath) {
        ParkingManager pm = new ParkingManager(dbPath, configPath);
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Smart Parking Management (CLI) ===");
            System.out.println("1) Park vehicle");
            System.out.println("2) Exit vehicle");
            System.out.println("3) Live status");
            System.out.println("4) Search history by plate");
            System.out.println("5) Revenue report");
            System.out.println("6) Exit");

            String choice = sc.nextLine().trim();
            try {
                if (!Set.of("1","2","3","4","5","6").contains(choice)) {
                    System.out.println("Invalid option. Enter a number from 1 to 6.");
                    continue;
                }

                switch (choice) {
                    case "1" -> {
                        System.out.print("Vehicle type (Bike/Car/Truck): ");
                        String vt = sc.nextLine().trim();
                        System.out.print("Plate: ");
                        String plate = sc.nextLine().trim().toUpperCase(Locale.ROOT);

                        // Validate format: first 2 letters, next 2 digits, next 2 letters, last 4 digits
                        // Example: KA20MC7362
                        if (!plate.matches("^[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}$")) {
                            System.out.println("Error: Invalid plate format. Expected like KA20MC7362.");
                            break;
                        }


                        Receipt r = pm.park(vt, plate);
                        System.out.println("\nParked successfully. Ticket ID: " + r.ticketId);
                        System.out.println("Slot: Floor " + r.entryFloor + ", Slot " + r.entrySlotNumber + " (" + r.vehicleType + ")");
                    }
                    case "2" -> {
                        System.out.print("Plate: ");
                        String plate = sc.nextLine().trim();
                        Receipt r = pm.exit(plate);
                        System.out.println("\nExit successful. Receipt");
                        System.out.println("  Ticket ID: " + r.ticketId);
                        System.out.println("  Vehicle: " + r.vehicleType + " | Plate: " + r.plate);
                        System.out.println("  Entry: " + r.entryTime);
                        System.out.println("  Exit:  " + r.exitTime);
                        System.out.println("  Duration: " + String.format(Locale.US, "%.2f", r.parkedDurationHours) + " hours");
                        System.out.println("  Billable (rounded): " + r.billableHours + " hours");
                        System.out.println("  Hourly rate: " + r.hourlyRate);
                        System.out.println("  TOTAL FEE: " + r.fee);
                    }
                    case "3" -> {
                        Map<String, Map<String, Map<String, Integer>>> status = pm.getLiveStatus();
                        List<String> floors = new ArrayList<>(status.keySet());
                        floors.sort(Comparator.comparingInt(s -> Integer.parseInt(s.split("_")[1])));
                        for (String floorKey : floors) {
                            System.out.println("\n" + floorKey.toUpperCase());
                            for (String vt : status.get(floorKey).keySet()) {
                                Map<String,Integer> st = status.get(floorKey).get(vt);
                                System.out.println("  " + vt + ": occupied=" + st.get("occupied") + " available=" + st.get("available") + " total=" + st.get("total"));
                            }
                        }
                    }
                    case "4" -> {
                        System.out.print("Plate: ");
                        String plate = sc.nextLine().trim().toUpperCase(Locale.ROOT);
                        List<TicketInfo> hist = pm.searchHistory(plate);
                        if (hist.isEmpty()) {
                            System.out.println("No history found.");
                        } else {
                            System.out.println("\nParking history:");
                            for (TicketInfo t : hist) {
                                System.out.println("- Ticket " + t.ticketId + " | " + t.vehicleType + " | Floor " + t.floor + " Slot " + t.slotNumber + " | Entry " + t.entryTime + " | Exit " + t.exitTime + " | Fee " + t.fee);
                            }
                        }
                    }
                    case "5" -> {
                        Map<String,Object> report = pm.getRevenueReport();
                        System.out.println("\nRevenue report");
                        System.out.println("Total: " + report.get("total"));
                        System.out.println("Breakdown:");
                        @SuppressWarnings("unchecked")
                        Map<String,Double> bd = (Map<String,Double>) report.get("breakdown");
                        List<String> keys = new ArrayList<>(bd.keySet());
                        keys.sort(String::compareTo);
                        for (String vt : keys) {
                            System.out.println("  " + vt + ": " + bd.get(vt));
                        }
                    }
                    case "6" -> {
                        System.out.println("Goodbye");
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}

