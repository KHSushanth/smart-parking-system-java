import java.util.*;

public class LotConfig {
    public final List<String> vehicleTypes;
    public final Map<String, Double> hourlyRates;
    public final int floors;
    public final Map<String, Integer> perFloorSlots;
    public final String nearestSlotDirection;

    public LotConfig(List<String> vehicleTypes, Map<String, Double> hourlyRates, int floors,
                      Map<String, Integer> perFloorSlots, String nearestSlotDirection) {
        this.vehicleTypes = vehicleTypes;
        this.hourlyRates = hourlyRates;
        this.floors = floors;
        this.perFloorSlots = perFloorSlots;
        this.nearestSlotDirection = nearestSlotDirection;
    }
}

