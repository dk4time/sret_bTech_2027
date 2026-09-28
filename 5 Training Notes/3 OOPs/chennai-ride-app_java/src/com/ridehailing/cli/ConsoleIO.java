package com.ridehailing.cli;

import com.ridehailing.model.common.ChennaiPlaces;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

/**
 * Every bit of console input and output for the interactive mode lives here, so menus stay
 * short and no menu ever crashes on bad input.
 *
 * At ANY prompt: "b" goes back (GoBackException), "q" quits after a confirmation (QuitException).
 * End of input also quits cleanly, so a replayed session file can never loop forever.
 */
public class ConsoleIO {

    private final BufferedReader in;
    private final PrintStream out;
    private final boolean echoInput;
    private boolean placeListShown;

    /**
     * @param echoInput true when input comes from a file: each answer is printed after its prompt,
     *                  so a replayed session reads like a live one.
     */
    public ConsoleIO(InputStream input, PrintStream output, boolean echoInput) {
        this.in = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        this.out = output;
        this.echoInput = echoInput;
    }

    // ================================================================== reading

    /** Reads one trimmed line. Handles "b", "q" and end of input for every other read method. */
    public String readLine(String prompt) {
        while (true) {
            String line = rawRead("  › " + prompt + " ");
            if (line.equalsIgnoreCase("b")) {
                throw new GoBackException();
            }
            if (line.equalsIgnoreCase("q")) {
                if (confirmQuit()) {
                    throw new QuitException("Goodbye!");
                }
                continue;
            }
            return line;
        }
    }

    private String rawRead(String prompt) {
        out.print(prompt);
        out.flush();
        String line;
        while (true) {
            try {
                line = in.readLine();
            } catch (IOException e) {
                line = null;
            }
            if (line == null) {
                out.println();
                throw new QuitException("End of input - leaving the app.");
            }
            line = line.trim();
            if (!line.startsWith("#")) {
                break;
            }
            // "# ..." lines are comments in walkthrough files: shown as narration when replaying, never answered.
            if (echoInput) {
                out.println();
                out.println("  " + line);
                out.print(prompt);
            }
        }
        if (echoInput) {
            out.println(line);
        }
        return line;
    }

    private boolean confirmQuit() {
        while (true) {
            String answer = rawRead("  › Quit the app? (y/n) ");
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return false;
            }
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt + " [" + min + "-" + max + "]:");
            Integer value = parseInt(line);
            if (value != null && value >= min && value <= max) {
                return value;
            }
            printError("Please enter a whole number from " + min + " to " + max + " (b = back, q = quit).");
        }
    }

    /** Like readInt, but Enter accepts the default value. */
    public int readInt(String prompt, int min, int max, int defaultValue) {
        while (true) {
            String line = readLine(prompt + " [" + min + "-" + max + ", Enter = " + defaultValue + "]:");
            if (line.isEmpty()) {
                return defaultValue;
            }
            Integer value = parseInt(line);
            if (value != null && value >= min && value <= max) {
                return value;
            }
            printError("Please enter a whole number from " + min + " to " + max + ".");
        }
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Shows numbered options and returns the chosen index (0-based). */
    public int readChoice(String prompt, String[] options) {
        for (int i = 0; i < options.length; i++) {
            out.println(String.format("     %d. %s", i + 1, options[i]));
        }
        return readInt(prompt, 1, options.length) - 1;
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String line = readLine(prompt + " (y/n):");
            if (line.equalsIgnoreCase("y") || line.equalsIgnoreCase("yes")) {
                return true;
            }
            if (line.equalsIgnoreCase("n") || line.equalsIgnoreCase("no")) {
                return false;
            }
            printError("Please answer y or n.");
        }
    }

    /** Free text. When allowEmpty is false, keeps asking until something is typed. */
    public String readText(String prompt, boolean allowEmpty) {
        while (true) {
            String line = readLine(prompt);
            if (!line.isEmpty() || allowEmpty) {
                return line;
            }
            printError("Please type something (b = back).");
        }
    }

    /** A positive rupee amount such as 500 or 250.50. */
    public Money readMoney(String prompt) {
        while (true) {
            String line = readLine(prompt + " ₹");
            try {
                Money amount = Money.of(line.replace(",", "").replace("₹", ""));
                if (!amount.isZero()) {
                    return amount;
                }
            } catch (IllegalArgumentException e) {
                // not a number, or negative: fall through to the friendly message
            }
            printError("Please enter a positive amount, e.g. 500 or 250.50.");
        }
    }

    /** A 24-hour time such as 23:30 or 9:05. */
    public LocalTime readTime(String prompt) {
        while (true) {
            String line = readLine(prompt + " (HH:MM, 24-hour):");
            try {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    return LocalTime.of(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                }
            } catch (NumberFormatException | DateTimeException e) {
                // fall through
            }
            printError("Please enter a time like 23:30.");
        }
    }

    // ================================================================== places

    /** Every place you can pick: all named Chennai areas, then a few just outside the service area. */
    public static List<Location> selectablePlaces() {
        List<Location> places = new ArrayList<>(ChennaiPlaces.namedAreas());
        places.addAll(ChennaiPlaces.outsideServiceAreaExamples());
        return places;
    }

    private static String placeLabel(Location place) {
        if (ChennaiPlaces.outsideServiceAreaExamples().contains(place)) {
            return place.getName() + " (outside service area)";
        }
        return place.getName();
    }

    public void printPlaceList() {
        List<Location> places = selectablePlaces();
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < places.size(); i++) {
            String cell = String.format("%2d %-38s", i + 1, placeLabel(places.get(i)));
            row.append(cell);
            if ((i + 1) % 3 == 0 || i == places.size() - 1) {
                out.println("    " + row.toString().stripTrailing());
                row.setLength(0);
            }
        }
        placeListShown = true;
    }

    /**
     * A place by number or by part of its name ("tam" -> Tambaram, "cent" -> Chennai Central).
     * Enter accepts defaultPlace when one is given. "?" shows the list again.
     */
    public Location readPlace(String prompt, Location defaultPlace) {
        if (!placeListShown) {
            printPlaceList();
        }
        List<Location> places = selectablePlaces();
        String hint = defaultPlace == null ? "" : ", Enter = " + defaultPlace.getName();
        while (true) {
            String line = readLine(prompt + " (number or name, ? = list" + hint + "):");
            if (line.isEmpty()) {
                if (defaultPlace != null) {
                    return defaultPlace;
                }
                printError("Please choose a place.");
                continue;
            }
            if (line.equals("?")) {
                printPlaceList();
                continue;
            }
            Integer number = parseInt(line);
            if (number != null) {
                if (number >= 1 && number <= places.size()) {
                    return places.get(number - 1);
                }
                printError("There is no place number " + number + ".");
                continue;
            }
            List<Location> matches = new ArrayList<>();
            for (Location place : places) {
                if (place.getName().toLowerCase().contains(line.toLowerCase())) {
                    matches.add(place);
                }
            }
            if (matches.size() == 1) {
                out.println("     -> " + placeLabel(matches.get(0)));
                return matches.get(0);
            }
            if (matches.isEmpty()) {
                printError("No place matches \"" + line + "\". Type ? to see the list.");
                continue;
            }
            out.println("     \"" + line + "\" matches several places:");
            String[] labels = new String[matches.size()];
            for (int i = 0; i < matches.size(); i++) {
                labels[i] = placeLabel(matches.get(i));
            }
            return matches.get(readChoice("Which one?", labels));
        }
    }

    // ================================================================== printing

    public void printHeader(String title) {
        out.println();
        out.println("  ═══ " + title + " " + "═".repeat(Math.max(3, 70 - title.length())));
    }

    public void printStatus(String statusLine) {
        out.println();
        out.println("  " + statusLine);
    }

    public void printError(String message) {
        out.println("  ✘ " + message);
    }

    public void printSuccess(String message) {
        out.println("  ✔ " + message);
    }

    public void printInfo(String message) {
        out.println("  " + message);
    }

    public void println(String text) {
        out.println(text);
    }

    /** Prints rows under headers with every column padded to its widest cell. */
    public void printTable(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int c = 0; c < headers.length; c++) {
            widths[c] = headers[c].length();
        }
        for (String[] row : rows) {
            for (int c = 0; c < headers.length && c < row.length; c++) {
                widths[c] = Math.max(widths[c], row[c] == null ? 0 : row[c].length());
            }
        }
        out.println("    " + formatRow(headers, widths));
        StringBuilder rule = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            rule.append("─".repeat(widths[c])).append(c < widths.length - 1 ? "  " : "");
        }
        out.println("    " + rule);
        for (String[] row : rows) {
            out.println("    " + formatRow(row, widths));
        }
        if (rows.isEmpty()) {
            out.println("    (none)");
        }
    }

    private static String formatRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            String cell = c < cells.length && cells[c] != null ? cells[c] : "";
            sb.append(String.format("%-" + widths[c] + "s", cell));
            if (c < widths.length - 1) {
                sb.append("  ");
            }
        }
        return sb.toString().stripTrailing();
    }
}
