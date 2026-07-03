import java.util.*;

public class Main {
    public static void main(String[] args) {
        boolean cli = false;
        boolean gui = false;
        String dbPath = "parking.sqlite3";
        String configPath = "config.json";

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--cli")) cli = true;
            else if (a.equals("--gui")) gui = true;
            else if (a.equals("--db") && i + 1 < args.length) dbPath = args[++i];
            else if (a.equals("--config") && i + 1 < args.length) configPath = args[++i];
        }

        if (cli && gui) {
            System.err.println("Choose only one: --cli or --gui");
            System.exit(1);
        }

        if (cli) {
            CLI.run(dbPath, configPath);
        } else {
            // GUI placeholder (not implemented)
            // If you want a Swing GUI, we can add it.
            System.out.println("GUI not implemented in this Java version. Use --cli.");
            CLI.run(dbPath, configPath);
        }
    }
}

