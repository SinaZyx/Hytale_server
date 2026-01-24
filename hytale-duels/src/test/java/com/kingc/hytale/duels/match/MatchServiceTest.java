package com.kingc.hytale.duels.match;

import com.kingc.hytale.duels.api.event.DuelEventBus;
import com.kingc.hytale.duels.arena.Arena;
import com.kingc.hytale.duels.arena.ArenaService;
import com.kingc.hytale.duels.kit.KitDefinition;
import com.kingc.hytale.duels.kit.KitService;
import com.kingc.hytale.duels.mock.MockPlayerRef;
import com.kingc.hytale.duels.mock.MockServerAdapter;
import com.kingc.hytale.duels.notifications.NotificationService;
import com.kingc.hytale.duels.ranking.RankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MatchServiceTest {

    private MockServerAdapter server;
    private ArenaService arenaService;
    private KitService kitService;
    private RankingService rankingService;
    private MatchService matchService;
    private DuelEventBus eventBus;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        server = new MockServerAdapter();
        arenaService = Mockito.mock(ArenaService.class);
        kitService = Mockito.mock(KitService.class);
        rankingService = Mockito.mock(RankingService.class);
        eventBus = new DuelEventBus();
        notificationService = new NotificationService(server);

        matchService = new MatchService(server, arenaService, kitService, rankingService, eventBus, notificationService, server::nowEpochMs);
    }

    @Test
    void testDuelRequestFlow() {
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();
        MockPlayerRef p1 = new MockPlayerRef(p1Id, "Player1");
        MockPlayerRef p2 = new MockPlayerRef(p2Id, "Player2");
        server.addPlayer(p1);
        server.addPlayer(p2);

        when(kitService.kitExists("tank")).thenReturn(true);
        when(kitService.getKit("tank")).thenReturn(Optional.of(new KitDefinition("tank", "Tank", Collections.emptyList(), Collections.emptyList(), Collections.emptyMap())));

        Arena mockArena = new Arena("arena1", "Arena 1", Collections.emptyList(), Collections.emptyList());
        when(arenaService.findAvailableArena(2)).thenReturn(Optional.of(mockArena));
        when(arenaService.reserveArena("arena1")).thenReturn(true);

        // 1. Send Request
        MatchService.Result reqResult = matchService.sendDuelRequest(p1Id, p2Id, "tank");
        assertTrue(reqResult.success());

        // 2. Accept Request
        MatchService.Result accResult = matchService.acceptDuel(p2Id);
        assertTrue(accResult.success(), "Match start should succeed: " + accResult.message());

        // 3. Verify match started
        assertTrue(matchService.isInMatch(p1Id));
        assertTrue(matchService.isInMatch(p2Id));
    }
}
