package com.kingc.hytale.factions.service;

public final class Result<T> {
    private final boolean ok;
    private final String message;
    private final T value;

    private Result(boolean ok, String message, T value) {
        this.ok = ok;
        this.message = message;
        this.value = value;
    }

    public static <T> Result<T> ok(String message, T value) {
        return new Result<>(true, message, value);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null);
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
}
