package com.kingc.hytale.duels.command;

import com.kingc.hytale.duels.api.PlayerRef;
import com.kingc.hytale.duels.kit.KitService;
import com.kingc.hytale.duels.match.MatchService;
import com.kingc.hytale.duels.match.MatchType;
import com.kingc.hytale.duels.mock.MockCommandSource;
import com.kingc.hytale.duels.mock.MockPlayerRef;
import com.kingc.hytale.duels.mock.MockServerAdapter;
import com.kingc.hytale.duels.queue.QueueService;
import com.kingc.hytale.duels.ranking.PlayerStats;
import com.kingc.hytale.duels.ranking.Rank;
import com.kingc.hytale.duels.ranking.RankingService;
import com.kingc.hytale.duels.translations.DuelsTranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommandDispatcherTest {

    private MockServerAdapter server;
    private MatchService matchService;
    private QueueService queueService;
    private KitService kitService;
    private RankingService rankingService;
    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        DuelsTranslationService.init(); // Ensure translations are loaded
        server = new MockServerAdapter();
        matchService = Mockito.mock(MatchService.class);
        queueService = Mockito.mock(QueueService.class);
        kitService = Mockito.mock(KitService.class);
        rankingService = Mockito.mock(RankingService.class);

        dispatcher = new CommandDispatcher(matchService, queueService, kitService, rankingService, server);
    }

    @Test
    void testDuelCommand_SendRequest() {
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();
        MockCommandSource source = new MockCommandSource(p1Id, "Player1");

        // Setup mock target player in server
        MockPlayerRef target = new MockPlayerRef(p2Id, "Player2");
        server.addPlayer(target);

        // Mock match service response
        when(matchService.sendDuelRequest(eq(p1Id), eq(p2Id), any())).thenReturn(MatchService.Result.success("Request sent"));

        // Execute command
        boolean result = dispatcher.handle(source, "duel Player2");

        assertTrue(result);
        verify(matchService).sendDuelRequest(eq(p1Id), eq(p2Id), eq("tank")); // Default kit
        assertTrue(source.messages.stream().anyMatch(m -> m.contains("Request sent")));
    }

    @Test
    void testQueueCommand_Join() {
        UUID p1Id = UUID.randomUUID();
        MockCommandSource source = new MockCommandSource(p1Id, "Player1");

        when(queueService.joinQueue(eq(p1Id), eq(MatchType.DUEL_1V1), any())).thenReturn(QueueService.Result.success("Joined queue"));

        boolean result = dispatcher.handle(source, "queue 1v1");

        assertTrue(result);
        verify(queueService).joinQueue(eq(p1Id), eq(MatchType.DUEL_1V1), eq("tank"));
    }

    @Test
    void testStatsCommand_Self() {
        UUID p1Id = UUID.randomUUID();
        MockCommandSource source = new MockCommandSource(p1Id, "Player1");

        PlayerStats mockStats = new PlayerStats(p1Id, "Player1", 1200, 10, 5, 2, 5, System.currentTimeMillis());
        when(rankingService.getStats(p1Id)).thenReturn(Optional.of(mockStats));
        when(rankingService.getPlayerRank(p1Id)).thenReturn(1);

        boolean result = dispatcher.handle(source, "stats");

        assertTrue(result);
        assertTrue(source.messages.stream().anyMatch(m -> m.contains("1200 ELO")));
        assertTrue(source.messages.stream().anyMatch(m -> m.contains("Player1")));
    }
}
