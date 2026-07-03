import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Minimal JSON loader for this specific config structure.
// Expects keys: vehicle_types, hourly_rates, lot.floors, lot.nearest_slot_direction, lot.layout.<TYPE>.per_floor_slots
public class Config {
    public static LotConfig load(String configPath) {
        // This Java version intentionally keeps parsing minimal.
        // If parsing fails, defaults are used.
        try {
            String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(configPath)), StandardCharsets.UTF_8);

            List<String> vehicleTypes = extractStringArray(json, "vehicle_types");
            Map<String, Double> hourlyRates = extractNumberMap(json, "hourly_rates");

            int floors = extractInt(json, "\"floors\"\\s*:\\s*(\\d+)");
            String nearest = extractString(json, "\"nearest_slot_direction\"\\s*:\\s*\"([^\"]+)\"");

            Map<String, Integer> perFloorSlots = new LinkedHashMap<>();
            for (String vt : vehicleTypes) {
                String pattern = "\"" + vt + "\"\\s*:\\s*\\{[^}]*\"per_floor_slots\"\\s*:\\s*(\\d+)";
                int slots = extractIntByPattern(json, pattern, 0);

                if (slots > 0) perFloorSlots.put(vt, slots);
            }

            if (vehicleTypes.isEmpty() || hourlyRates.isEmpty() || perFloorSlots.isEmpty()) throw new RuntimeException("Bad config");
            return new LotConfig(vehicleTypes, hourlyRates, floors, perFloorSlots, nearest);
        } catch (Exception e) {
            // Fallback to a sensible default matching typical config.json
            List<String> vehicleTypes = List.of("Bike", "Car", "Truck");
            Map<String, Double> hourlyRates = new LinkedHashMap<>();
            hourlyRates.put("Bike", 20.0);
            hourlyRates.put("Car", 35.0);
            hourlyRates.put("Truck", 60.0);
            Map<String, Integer> perFloorSlots = new LinkedHashMap<>();
            perFloorSlots.put("Bike", 10);
            perFloorSlots.put("Car", 20);
            perFloorSlots.put("Truck", 5);
            return new LotConfig(vehicleTypes, hourlyRates, 3, perFloorSlots, "ascending_slot_number");
        }
    }

    private static List<String> extractStringArray(String json, String key) {
        // Finds: "vehicle_types": ["Bike", "Car", ...]
        String marker = "\"" + key + "\"";
        int idx = json.indexOf(marker);
        if (idx < 0) return List.of();
        int start = json.indexOf('[', idx);
        int end = json.indexOf(']', start);
        if (start < 0 || end < 0) return List.of();
        String arr = json.substring(start + 1, end);
        String[] parts = arr.split(",");
        List<String> res = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (s.startsWith("\"")) s = s.substring(1);
            if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
            if (!s.isEmpty()) res.add(s);
        }
        return res;
    }

    private static Map<String, Double> extractNumberMap(String json, String key) {
        Map<String, Double> res = new LinkedHashMap<>();
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return res;
        int start = json.indexOf('{', idx);
        int end = json.indexOf('}', start);
        if (start < 0 || end < 0) return res;
        String body = json.substring(start + 1, end);
        // naive split by commas at top-level
        String[] parts = body.split(",");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            String k = kv[0].trim();
            if (k.startsWith("\"")) k = k.substring(1);
            if (k.endsWith("\"")) k = k.substring(0, k.length() - 1);
            String v = kv[1].trim();
            try {
                res.put(k, Double.parseDouble(v));
            } catch (Exception ignored) {}
        }
        return res;
    }

    private static int extractInt(String json, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(json);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 3;
    }

    private static String extractString(String json, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(json);
        if (m.find()) return m.group(1);
        return "ascending_slot_number";
    }

    private static int extractIntByPattern(String json, String pattern, int fallback) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignored) {}
        }
        return fallback;
    }
}

