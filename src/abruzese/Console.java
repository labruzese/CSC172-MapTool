package abruzese;

public class StreetMapConsole {
    private final StreetMap map;
    private final MapDisplay display; // New class for visualization
    private final BufferedReader consoleReader;
    private volatile boolean running = true;

    public StreetMapConsole(String filename) throws IOException {
        this.map = new StreetMap(filename);
        this.display = new MapDisplay(map);
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
    }

    public void startInteractiveMode() {
        System.out.println("Interactive console mode started. Type 'help' for commands.");

        while (running) {
            try {
                String input = consoleReader.readLine().trim();
                processCommand(input);
            } catch (IOException e) {
                System.out.println("Error reading command: " + e.getMessage());
            }
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        if (parts.length == 0) return;

        switch (parts[0].toLowerCase()) {
            case "help":
                printHelp();
                break;
            case "search":
                if (parts.length < 2) {
                    System.out.println("Usage: search <intersectionID>");
                    return;
                }
                searchIntersection(parts[1]);
                break;
            case "directions":
                if (parts.length < 3) {
                    System.out.println("Usage: directions <startIntersection> <endIntersection>");
                    return;
                }
                showDirections(parts[1], parts[2]);
                break;
            case "exit":
                running = false;
                break;
            default:
                System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }
}