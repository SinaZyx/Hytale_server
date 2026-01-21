package com.kingc.hytale.duels.api.event;

import com.kingc.hytale.duels.match.Match;

public class MatchEndedEvent extends DuelEvent {
    private final Match match;

    public MatchEndedEvent(Match match) {
        this.match = match;
    }

    public Match getMatch() {
        return match;
    }
}
