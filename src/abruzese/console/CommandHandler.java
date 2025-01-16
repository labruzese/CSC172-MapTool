package abruzese.console;

import abruzese.graph.edges.Road;
import abruzese.graph.vertices.Intersection;
import abruzese.hashtable.HashTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CommandHandler {
    private final Console console;
    private List<DirectionStep> lastDirections = null;
    private static final double WALKING_SPEED_MPS = 1.4; // meters per second
    private static final double DRIVING_SPEED_MPS = 13.4; // ~30 mph in meters per second

    public CommandHandler(Console console) {
        this.console = console;
    }

    public void handleCommand(String command, String[] args) {
        switch (command) {
            case "help":
                showHelp();
                break;
            case "search":
                handleSearch(args);
                break;
            case "directions":
                handleDirections(args);
                break;
            case "highlight":
                handleHighlight(args);
                break;
            default:
                System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  help                          - Show this help message");
        System.out.println("  search <intersection>         - Show information about an intersection");
        System.out.println("  directions <int1> <int2> ...  - Show directions between multiple intersections");
        System.out.println("  highlight <step>              - Highlight a specific direction step after directions command");
        System.out.println("  highlight <start>-<end>       - Highlight a range of direction steps after directions command");
        System.out.println("  highlight clear               - Clear all highlights");
        System.out.println("  exit                          - Exit the program");
    }

    private void handleSearch(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: search <intersection> or search road <roadID>");
            return;
        }

        if (args[0].equals("road")) {
            if (args.length < 2) {
                System.out.println("Usage: search road <roadID>");
                return;
            }
            searchRoad(args[1]);
            return;
        }

        searchIntersection(args[0]);
    }

    private void searchRoad(String roadId) {
        boolean found = false;

        for (HashTable.Entry<Intersection, Intersection> edge : console.getStreetMap().getStreetGraph().getEdges()) {
            Road road = console.getStreetMap().getStreetGraph().get(edge.getKey(), edge.getValue());
            if (road.roadID.equals(roadId)) {
                System.out.println("ID: " + road.roadID);
                System.out.printf("Distance: %s\n", formatDistance(road.getDistance()));
                System.out.println("Connects:");
                System.out.println("  - " + edge.getKey().intersectionID());
                System.out.println("  - " + edge.getValue().intersectionID());

                // Highlight on map if in GUI mode
                if (console.getMapPanel() != null) {
                    // Create a single-step direction to highlight this road
                    List<DirectionStep> steps = new ArrayList<>();
                    steps.add(new DirectionStep(
                            edge.getKey(),
                            edge.getValue(),
                            road,
                            "Road " + road.roadID,
                            road.getDistance(),
                            1
                    ));
                    console.getMapPanel().highlightSteps(steps);
                }

                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Road not found: " + roadId);
            if (console.getMapPanel() != null) {
                console.getMapPanel().clearHighlights();
            }
        }
    }

    private void searchIntersection(String intersectionId) {

        boolean found = false;

        for (Intersection intersection : console.getStreetMap().getStreetGraph()) {
            if (intersection.intersectionID().equals(intersectionId)) {
                System.out.println("ID: " + intersection.intersectionID());
                System.out.println("Latitude: " + intersection.latitude());
                System.out.println("Longitude: " + intersection.longitude());

                // Count and list connected intersections
                Collection<Intersection> neighbors = console.getStreetMap().getStreetGraph().neighbors(intersection);
                System.out.println("Connected to " + neighbors.size() + " other intersection(s):");
                for (Intersection neighbor : neighbors) {
                    Road road = console.getStreetMap().getStreetGraph().get(intersection, neighbor);
                    System.out.printf("  - %s (Road: %s, Distance: %.2f meters)\n",
                            neighbor.intersectionID(), road.roadID, road.getDistance());
                }

                // Highlight on map if in GUI mode
                if (console.getMapPanel() != null) {
                    console.getMapPanel().highlightIntersection(intersection);
                }

                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Intersection not found: " + intersectionId);
            if (console.getMapPanel() != null) {
                console.getMapPanel().clearHighlights();
            }
        }
    }

    private void handleDirections(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: directions <intersection1> <intersection2> [intersection3...]");
            return;
        }

        try {
            List<DirectionStep> allSteps = new ArrayList<>();
            double totalDistance = 0;
            int stepNumber = 1;
            double currentAngle = 0;

            // Calculate paths between consecutive waypoints
            for (int i = 0; i < args.length - 1; i++) {
                List<Intersection> pathSegment = console.getStreetMap().findPath(args[i], args[i + 1]);

                if (pathSegment.isEmpty()) {
                    System.out.printf("No path found between %s and %s\n", args[i], args[i + 1]);
                    return;
                }

                DirectionStep currentCombinedStep = null;

                // Calculate steps for this segment
                for (int j = 0; j < pathSegment.size() - 1; j++) {
                    Intersection current = pathSegment.get(j);
                    Intersection next = pathSegment.get(j + 1);
                    Road road = console.getStreetMap().getStreetGraph().get(current, next);
                    double distance = road.getDistance();
                    totalDistance += distance;

                    // Calculate turn instruction
                    double nextAngle = calculateAngle(current, next);
                    String turnInstruction;

                    if (stepNumber == 1) {
                        turnInstruction = "Start at " + current.intersectionID() + ", facing " + getCardinalDirection(nextAngle);
                        currentCombinedStep = new DirectionStep(current, next, road, turnInstruction, distance, stepNumber++);
                        allSteps.add(currentCombinedStep);
                    } else {
                        double angleDiff = normalizeDegrees(nextAngle - currentAngle);
                        turnInstruction = getTurnInstruction(angleDiff);

                        if (turnInstruction.equals("Continue straight") && currentCombinedStep != null &&
                                currentCombinedStep.instruction.equals("Continue straight")) {
                            // Update the existing straight step with new distance and destination
                            currentCombinedStep = new DirectionStep(
                                    currentCombinedStep.from,
                                    next,
                                    road,
                                    "Continue straight",
                                    currentCombinedStep.distance + distance,
                                    currentCombinedStep.stepNumber
                            );
                            // Replace the last step with the updated combined step
                            allSteps.set(allSteps.size() - 1, currentCombinedStep);
                        } else {
                            // New direction (turn or first straight)
                            currentCombinedStep = new DirectionStep(current, next, road, turnInstruction, distance, stepNumber++);
                            allSteps.add(currentCombinedStep);
                        }
                    }
                    currentAngle = nextAngle;
                }
            }

            // Store directions for later highlighting
            lastDirections = allSteps;

            // Print directions header
            System.out.printf("\nDirections from %s via %s to %s:\n",
                    args[0],
                    args.length > 3 ? (args.length - 2) + " waypoints" : args[1],
                    args[args.length - 1]);
            System.out.println("=====================================");

            // Print each step
            for (DirectionStep step : allSteps) {
                System.out.printf("%d. %s\n",
                        step.stepNumber,
                        step.instruction);
                System.out.printf("   Continue on %s for %s to %s\n",
                        step.road.roadID,
                        formatDistance(step.distance),
                        step.to.intersectionID());
            }

            // Calculate time estimates
            double walkingMinutes = (totalDistance / WALKING_SPEED_MPS) / 60;
            double drivingMinutes = (totalDistance / DRIVING_SPEED_MPS) / 60;

            System.out.println("\nSummary:");
            System.out.println("--------");
            System.out.printf("Total distance: %s\n", formatDistance(totalDistance));
            System.out.printf("Estimated walking time: %s\n", formatTime(walkingMinutes));
            System.out.printf("Estimated driving time: %s\n", formatTime(drivingMinutes));
            System.out.println("\nCommands available:");
            System.out.println("- Type 'highlight <all|clear>' to clear/highlight all steps (e.g., 'highlight clear')");
            System.out.println("- Type 'highlight <step>' to highlight a specific step (e.g., 'highlight 3')");
            System.out.println("- Type 'highlight <start>-<end>' to highlight a range of steps (e.g., 'highlight 2-4')");

            if (console.getMapPanel() != null) {
                console.getMapPanel().highlightSteps(allSteps);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleHighlight(String[] args) {
        if (lastDirections == null) {
            System.out.println("No directions available. Please run 'directions' command first.");
            return;
        }

        if (args.length != 1) {
            System.out.println("Usage: highlight <step> or highlight <start>-<end> or highlight all or highlight clear");
            return;
        }

        if (args[0].equals("clear")) {
            if (console.getMapPanel() != null) {
                console.getMapPanel().clearHighlights();
                System.out.println("Cleared all highlights");
            }
            return;
        }

        if (args[0].equals("all")) {
            args[0] = "1-" + lastDirections.size();
        }

        try {
            if (args[0].contains("-")) {
                // Handle range highlight
                String[] range = args[0].split("-");
                if (range.length != 2) {
                    System.out.println("Invalid range format. Use: highlight <start>-<end>");
                    return;
                }

                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);

                if (start < 1 || end > lastDirections.size() || start > end) {
                    System.out.println("Invalid step range. Available steps: 1-" + lastDirections.size());
                    return;
                }

                highlightSteps(start, end);
            } else {
                // Handle single step highlight
                int step = Integer.parseInt(args[0]);
                if (step < 1 || step > lastDirections.size()) {
                    System.out.println("Invalid step number. Available steps: 1-" + lastDirections.size());
                    return;
                }

                highlightSteps(step, step);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid step number format. Please use numbers only.");
        }
    }

    private void highlightSteps(int start, int end) {
        List<DirectionStep> stepsToHighlight = lastDirections.subList(start - 1, end);
        console.getMapPanel().highlightSteps(stepsToHighlight);
        System.out.printf("Highlighting step%s %d%s\n",
                start == end ? "" : "s",
                start,
                start == end ? "" : "-" + end);
    }

    // Formatting and calculation helper methods
    private String formatDistance(double meters) {
        if (meters >= 1000) {
            if (meters >= 10000) {
                return String.format("%d km", Math.round(meters/1000));
            }
            return String.format("%.1f km", meters/1000);
        }
        return String.format("%.0f meters", meters);
    }

    private String formatTime(double minutes) {
        if (minutes < 1) {
            return "less than a minute";
        } else if (minutes < 60) {
            return String.format("%.0f minutes", minutes);
        } else {
            int hours = (int) (minutes / 60);
            int mins = (int) (minutes % 60);
            if (mins == 0) {
                return String.format("%d hour%s", hours, hours > 1 ? "s" : "");
            }
            return String.format("%d hour%s %d minute%s",
                    hours, hours > 1 ? "s" : "",
                    mins, mins > 1 ? "s" : "");
        }
    }

    private double calculateAngle(Intersection from, Intersection to) {
        double lat1 = Math.toRadians(from.latitude());
        double lon1 = Math.toRadians(from.longitude());
        double lat2 = Math.toRadians(to.latitude());
        double lon2 = Math.toRadians(to.longitude());

        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return normalizeDegrees(bearing);
    }

    private double normalizeDegrees(double degrees) {
        degrees = degrees % 360;
        if (degrees < 0) {
            degrees += 360;
        }
        return degrees;
    }

    private String getCardinalDirection(double angle) {
        String[] directions = {"north", "northeast", "east", "southeast",
                "south", "southwest", "west", "northwest"};
        int index = (int) Math.round(angle / 45) % 8;
        return directions[index];
    }

    private String getTurnInstruction(double angleDiff) {
        if (angleDiff < 10 || angleDiff > 350) {
            return "Continue straight";
        } else if (angleDiff < 45) {
            return "Turn slight right";
        } else if (angleDiff < 135) {
            return "Turn right";
        } else if (angleDiff < 225) {
            return "Make a U-turn";
        } else if (angleDiff < 315) {
            return "Turn left";
        } else {
            return "Turn slight left";
        }
    }

    public void echo(String message) {
        System.out.println(message);
        System.out.print("> ");
    }
}