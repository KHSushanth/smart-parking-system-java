import java.sql.*;
import java.time.*;
import java.util.*;


public class Database {
    public record Available(int count, List<Integer> slots) {}

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String database;

    public Database(String dbPath) {
        // In this Java port, dbPath is reused as the database name for MySQL.
        this.database = dbPath;
        // MySQL credentials (provided by user)
        this.host = "127.0.0.1";
        this.port = 3306;
        this.user = "root";
        this.password = "KHSushanth@123";
        ensureSchema();
    }

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                user,
                password
        );
    }

    private void ensureSchema() {
        // Create DB if it doesn't exist (connect without selecting DB first)
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                user,
                password
        ); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new RuntimeException("DB init (create database) failed: " + e.getMessage(), e);
        }

        String create = "CREATE TABLE IF NOT EXISTS tickets (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "plate VARCHAR(255) NOT NULL," +
                "vehicle_type VARCHAR(255) NOT NULL," +
                "entry_time DATETIME NOT NULL," +
                "exit_time DATETIME NULL," +
                "entry_floor INT NOT NULL," +
                "entry_slot_number INT NOT NULL," +
                "fee DOUBLE NULL" +
                ");";


        String idx2 = "CREATE INDEX idx_tickets_plate ON tickets(plate, vehicle_type, entry_time);";


        try (Connection c = conn(); Statement st = c.createStatement()) {
            st.executeUpdate(create);
            try {
                st.executeUpdate(idx2);
            } catch (SQLException ignore) {
                // ignore duplicate index name if DB already exists
            }

        } catch (SQLException e) {
            throw new RuntimeException("DB init failed: " + e.getMessage(), e);
        }
    }

    public TicketInfo findActiveTicketByPlate(String plate) {

        String sql = "SELECT id, plate, vehicle_type, entry_time, exit_time, entry_floor, entry_slot_number, fee " +
                "FROM tickets WHERE plate=? AND exit_time IS NULL LIMIT 1";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, plate);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return new TicketInfo(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getInt(6),
                    rs.getInt(7),
                    rs.getObject(8) == null ? null : rs.getDouble(8)
            );
        } catch (SQLException e) {
            throw new RuntimeException("DB query failed: " + e.getMessage(), e);
        }
    }

    public int insertTicket(String plate, String vehicleType, LocalDateTime entryTime, int floor, int slotNumber) {
        String sql = "INSERT INTO tickets(plate, vehicle_type, entry_time, exit_time, entry_floor, entry_slot_number, fee) " +
                "VALUES(?, ?, ?, NULL, ?, ?, NULL)";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plate);
            ps.setString(2, vehicleType);
            ps.setString(3, entryTime.toString());
            ps.setInt(4, floor);
            ps.setInt(5, slotNumber);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Insert failed: " + e.getMessage(), e);
        }
        throw new RuntimeException("Insert failed: no ticket id");
    }

    public void markExitAndComputeFee(int ticketId, LocalDateTime exitTime, double fee) {
        String sql = "UPDATE tickets SET exit_time=?, fee=? WHERE id=? AND exit_time IS NULL";
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, exitTime.toString());
            ps.setDouble(2, fee);
            ps.setInt(3, ticketId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Exit update failed: " + e.getMessage(), e);
        }
    }

    public Available getAvailableSlotsForTypeOnFloor(int floor, String vehicleType, int totalSlotsForType) {




        String sql = "SELECT entry_slot_number FROM tickets WHERE exit_time IS NULL AND entry_floor=? AND vehicle_type=?";
        Set<Integer> occupied = new HashSet<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, floor);
            ps.setString(2, vehicleType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) occupied.add(rs.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Available slots query failed: " + e.getMessage(), e);
        }
        List<Integer> available = new ArrayList<>();
        for (int s = 1; s <= totalSlotsForType; s++) {
            if (!occupied.contains(s)) available.add(s);
        }
        return new Available(available.size(), available);
    }

    public Map<String, Map<String, Integer>> getOccupiedCountByFloorAndType(int floors, List<String> vehicleTypes) {
        Map<String, Map<String, Integer>> data = new LinkedHashMap<>();
        for (int f = 1; f <= floors; f++) {
            Map<String,Integer> m = new LinkedHashMap<>();
            for (String vt : vehicleTypes) m.put(vt, 0);
            data.put("floor_"+f, m);
        }

        String sql = "SELECT entry_floor, vehicle_type, COUNT(*) FROM tickets WHERE exit_time IS NULL GROUP BY entry_floor, vehicle_type";
        try (Connection c = conn(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int fl = rs.getInt(1);
                String vt = rs.getString(2);
                int count = rs.getInt(3);
                if (fl >= 1 && fl <= floors && data.containsKey("floor_"+fl)) {
                    data.get("floor_"+fl).put(vt, count);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Occupied query failed: " + e.getMessage(), e);
        }
        return data;
    }

    public List<TicketInfo> getHistoryByPlate(String plate) {
        String sql = "SELECT id, plate, vehicle_type, entry_time, exit_time, entry_floor, entry_slot_number, fee " +
                "FROM tickets WHERE plate=? ORDER BY entry_time";
        List<TicketInfo> res = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, plate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String exitTime = rs.getString(5);
                Double fee = rs.getObject(8) == null ? null : rs.getDouble(8);
                res.add(new TicketInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), exitTime,
                        rs.getInt(6), rs.getInt(7), fee));
            }
        } catch (SQLException e) {
            throw new RuntimeException("History query failed: " + e.getMessage(), e);
        }
        return res;
    }

    public Map<String,Object> getRevenueReport() {
        Map<String,Object> out = new LinkedHashMap<>();
        double total = 0.0;
        Map<String,Double> breakdown = new LinkedHashMap<>();

        String totalSql = "SELECT COALESCE(SUM(fee), 0) FROM tickets WHERE exit_time IS NOT NULL";
        String breakdownSql = "SELECT vehicle_type, COALESCE(SUM(fee), 0) FROM tickets WHERE exit_time IS NOT NULL GROUP BY vehicle_type";

        try (Connection c = conn(); Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery(totalSql)) {
                if (rs.next()) total = rs.getDouble(1);
            }
            try (ResultSet rs = st.executeQuery(breakdownSql)) {
                while (rs.next()) breakdown.put(rs.getString(1), rs.getDouble(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Revenue query failed: " + e.getMessage(), e);
        }

        out.put("total", total);
        out.put("breakdown", breakdown);
        return out;
    }


}


