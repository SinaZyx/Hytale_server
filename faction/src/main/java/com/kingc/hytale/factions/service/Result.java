package com.kingc.hytale.factions.service;

import java.util.Collections;
import java.util.Map;

public final class Result<T> {
    private final boolean ok;
    private final String message;
    private final T value;
    private final Map<String, String> args;

    private Result(boolean ok, String message, T value, Map<String, String> args) {
        this.ok = ok;
        this.message = message;
        this.value = value;
        this.args = args != null ? args : Collections.emptyMap();
    }

    public static <T> Result<T> ok(String message, T value) {
        return new Result<>(true, message, value, null);
    }

    public static <T> Result<T> ok(String message, T value, Map<String, String> args) {
        return new Result<>(true, message, value, args);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null, null);
    }

    public static <T> Result<T> error(String message, Map<String, String> args) {
        return new Result<>(false, message, null, args);
    }

    public boolean ok() {
        return ok;
    }

    public String message() {
        return message;
    }

    public T value() {
        return value;
    }

    public Map<String, String> args() {
        return args;
    }
}
