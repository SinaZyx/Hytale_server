package com.fancyinnovations.uihelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a parsed UI action from button clicks.
 * Handles patterns like "Edit:hologramName" or "Delete:5".
 *
 * Example usage:
 * <pre>
 * UIAction action = UIAction.parse(data.button);
 * if (action.is("Edit")) {
 *     String name = action.arg();
 *     // handle edit
 * } else if (action.is("Delete")) {
 *     int index = action.argInt();
 *     // handle delete
 * }
 * </pre>
 */
public class UIAction {

    @Nonnull
    private final String name;

    @Nonnull
    private final String[] arguments;

    private UIAction(@Nonnull String name, @Nonnull String[] arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    /**
     * Parses an action string like "Edit:name" or "Delete:5" or "Move:id:x:y".
     * Supports multiple colon-separated arguments.
     */
    @Nonnull
    public static UIAction parse(@Nullable String action) {
        if (action == null || action.isEmpty()) {
            return new UIAction("", new String[0]);
        }

        int colonIndex = action.indexOf(':');
        if (colonIndex == -1) {
            return new UIAction(action, new String[0]);
        }

        String name = action.substring(0, colonIndex);
        String argsStr = action.substring(colonIndex + 1);
        if (argsStr.isEmpty()) {
            return new UIAction(name, new String[0]);
        }

        String[] args = argsStr.split(":");
        return new UIAction(name, args);
    }

    /**
     * Checks if this action matches the given name (case-insensitive).
     */
    public boolean is(@Nonnull String actionName) {
        return name.equalsIgnoreCase(actionName);
    }

    /**
     * Checks if this action starts with the given prefix.
     */
    public boolean startsWith(@Nonnull String prefix) {
        return name.toLowerCase().startsWith(prefix.toLowerCase());
    }

    /**
     * Gets the action name.
     */
    @Nonnull
    public String name() {
        return name;
    }

    /**
     * Gets the first argument, or null if none.
     */
    @Nullable
    public String arg() {
        return arguments.length > 0 ? arguments[0] : null;
    }

    /**
     * Gets the first argument, or a default value if none.
     */
    @Nonnull
    public String arg(@Nonnull String defaultValue) {
        return arguments.length > 0 ? arguments[0] : defaultValue;
    }

    /**
     * Gets argument at index, or null if not present.
     */
    @Nullable
    public String arg(int index) {
        return index >= 0 && index < arguments.length ? arguments[index] : null;
    }

    /**
     * Gets argument at index, or default if not present.
     */
    @Nonnull
    public String arg(int index, @Nonnull String defaultValue) {
        return index >= 0 && index < arguments.length ? arguments[index] : defaultValue;
    }

    /**
     * Gets all arguments as array.
     */
    @Nonnull
    public String[] args() {
        return arguments.clone();
    }

    /**
     * Gets the number of arguments.
     */
    public int argCount() {
        return arguments.length;
    }

    /**
     * Gets the first argument as an integer.
     * @throws IllegalStateException if no argument
     * @throws NumberFormatException if not a valid integer
     */
    public int argInt() {
        if (arguments.length == 0) {
            throw new IllegalStateException("Action has no argument");
        }
        return Integer.parseInt(arguments[0]);
    }

    /**
     * Gets the first argument as an integer, or default if missing/invalid.
     */
    public int argInt(int defaultValue) {
        if (arguments.length == 0) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(arguments[0]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets argument at index as integer.
     */
    public int argInt(int index, int defaultValue) {
        if (index < 0 || index >= arguments.length) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(arguments[index]);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the first argument as a long.
     */
    public long argLong() {
        if (arguments.length == 0) {
            throw new IllegalStateException("Action has no argument");
        }
        return Long.parseLong(arguments[0]);
    }

    /**
     * Gets the first argument as a double.
     */
    public double argDouble() {
        if (arguments.length == 0) {
            throw new IllegalStateException("Action has no argument");
        }
        return Double.parseDouble(arguments[0]);
    }

    /**
     * Checks if this action has at least one argument.
     */
    public boolean hasArg() {
        return arguments.length > 0;
    }

    /**
     * Checks if this action has argument at index.
     */
    public boolean hasArg(int index) {
        return index >= 0 && index < arguments.length;
    }

    /**
     * Checks if this action is empty (no name).
     */
    public boolean isEmpty() {
        return name.isEmpty();
    }

    @Override
    public String toString() {
        if (arguments.length == 0) {
            return name;
        }
        return name + ":" + String.join(":", arguments);
    }
}
