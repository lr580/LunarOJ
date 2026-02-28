package com.lunaroj.migration.cli;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConsoleIO {

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private final PrintStream out = System.out;

    public void println(String message) {
        out.println(message);
    }

    public void print(String message) {
        out.print(message);
    }

    public String prompt(String label) {
        while (true) {
            try {
                print(label + ": ");
                String line = reader.readLine();
                if (line == null) {
                    throw new IllegalStateException("input stream closed");
                }
                line = line.trim();
                if (StringUtils.hasText(line)) {
                    return line;
                }
            } catch (IOException ex) {
                throw new IllegalStateException("failed to read input", ex);
            }
            println("Input cannot be empty.");
        }
    }

    public String promptWithDefault(String label, String defaultValue) {
        try {
            print(label + " (default: " + defaultValue + "): ");
            String line = reader.readLine();
            if (!StringUtils.hasText(line)) {
                return defaultValue;
            }
            return line.trim();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read input", ex);
        }
    }

    public int promptIntWithDefault(String label, int defaultValue, int minValue) {
        while (true) {
            String value = promptWithDefault(label, String.valueOf(defaultValue));
            try {
                int parsed = Integer.parseInt(value);
                if (parsed < minValue) {
                    println("Value must be >= " + minValue);
                    continue;
                }
                return parsed;
            } catch (NumberFormatException ex) {
                println("Invalid integer: " + value);
            }
        }
    }

    public boolean promptYesNo(String label, boolean defaultYes) {
        String defaultText = defaultYes ? "Y/n" : "y/N";
        while (true) {
            String value = promptWithDefault(label + " (" + defaultText + ")", defaultYes ? "y" : "n")
                    .toLowerCase();
            if ("y".equals(value) || "yes".equals(value)) {
                return true;
            }
            if ("n".equals(value) || "no".equals(value)) {
                return false;
            }
            println("Please input y or n.");
        }
    }

    public String promptPassword(String label) {
        java.io.Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword("%s: ", label);
            if (chars == null || chars.length == 0) {
                throw new IllegalStateException("password cannot be empty");
            }
            return new String(chars);
        }
        return prompt(label);
    }

    public List<Integer> promptCsvIntegersWithDefault(String label, List<Integer> defaultValues) {
        String defaultText = defaultValues.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        while (true) {
            String value = promptWithDefault(label + " (comma separated)", defaultText);
            try {
                return Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            } catch (NumberFormatException ex) {
                println("Invalid integer list: " + value);
            }
        }
    }
}

