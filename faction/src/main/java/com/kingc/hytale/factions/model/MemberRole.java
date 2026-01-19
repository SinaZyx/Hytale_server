package com.kingc.hytale.factions.model;

public enum MemberRole {
    LEADER(3),
    OFFICER(2),
    MEMBER(1),
    RECRUIT(0);

    private final int rank;

    MemberRole(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }

    public boolean atLeast(MemberRole other) {
        return rank >= other.rank;
    }

    public MemberRole promote() {
        if (this == RECRUIT) {
            return MEMBER;
        }
        if (this == MEMBER) {
            return OFFICER;
        }
        return this;
    }

    public MemberRole demote() {
        if (this == OFFICER) {
            return MEMBER;
        }
        if (this == MEMBER) {
            return RECRUIT;
        }
        return this;
    }

    public static MemberRole fromString(String raw) {
        for (MemberRole role : values()) {
            if (role.name().equalsIgnoreCase(raw)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + raw);
    }
}
