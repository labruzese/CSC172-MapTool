package abruzese.console;

import abruzese.gui.MapPanel;
import abruzese.util.StreetMap;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Program Entry Point
 */
public class Console {
    private final StreetMap streetMap;
    private final CommandHandler commandHandler;
    private boolean guiMode = false;
    private MapPanel mapPanel = null;

    public Console(String filename) throws IOException {
        this.streetMap = new StreetMap(filename);
        this.commandHandler = new CommandHandler(this);
    }

    public void setGuiMode(boolean enabled) {
        this.guiMode = enabled;
    }

    public void setMapPanel(MapPanel panel) {
        this.mapPanel = panel;
    }

    public StreetMap getStreetMap() {
        return streetMap;
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public void startInteractiveMode() {
        System.out.println("Street Map Interactive Console");
        System.out.println("Type 'help' for available commands");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {
                System.out.print("> ");
                String input = reader.readLine();

                if (input == null || input.equalsIgnoreCase("exit")) {
                    break;
                }

                processCommand(input);
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
            }
        }
    }

    public void processCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return;

        String command = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        try {
            commandHandler.handleCommand(command, args);
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
        }
    }

    /**
     * Program Entry Point
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Console <map.txt> [--show] [--directions startIntersection endIntersection]");
            return;
        }

        try {
            Console console = new Console(args[0]);
            boolean show = false;
            String startIntersection = null;
            String endIntersection = null;

            // Parse command line arguments
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--show":
                        show = true;
                        break;
                    case "--directions":
                        if (i + 2 < args.length) {
                            startIntersection = args[++i];
                            endIntersection = args[++i];
                        } else {
                            System.err.println("--directions requires two intersection IDs");
                            return;
                        }
                        break;
                }
            }

            // Launch GUI if requested
            if (show) {
                console.setGuiMode(true);
                Frame frame = new Frame("Street Map [" + args[0] + "]");
                frame.setSize(800, 800);
                MapPanel mapPanel = new MapPanel(console.getStreetMap().getStreetGraph(), console.commandHandler);
                console.setMapPanel(mapPanel);
                frame.add(mapPanel);
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                        System.exit(0);
                    }
                });
                frame.setVisible(true);
            }

            // Print directions if exists
            if (startIntersection != null && endIntersection != null) {
                console.processCommand("directions " + startIntersection + " " + endIntersection);
                if (!show) {
                    System.exit(0);
                }
            }

            // Start interactive mode if showing GUI
            if (show) {
                System.out.println();
                console.startInteractiveMode();
            }

        } catch (IOException e) {
            System.err.println("Error initializing map: " + e.getMessage());
        }
    }
}



