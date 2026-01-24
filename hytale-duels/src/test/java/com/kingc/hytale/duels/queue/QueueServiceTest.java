package com.kingc.hytale.duels.queue;

import com.kingc.hytale.duels.match.MatchService;
import com.kingc.hytale.duels.match.MatchType;
import com.kingc.hytale.duels.mock.MockPlayerRef;
import com.kingc.hytale.duels.mock.MockServerAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QueueServiceTest {

    private MockServerAdapter server;
    private MatchService matchService;
    private QueueService queueService;

    @BeforeEach
    void setUp() {
        server = new MockServerAdapter();
        matchService = Mockito.mock(MatchService.class);
        queueService = new QueueService(server, matchService, server::nowEpochMs);
    }

    @Test
    void testJoinAndLeaveQueue() {
        UUID p1Id = UUID.randomUUID();
        MockPlayerRef p1 = new MockPlayerRef(p1Id, "Player1");
        server.addPlayer(p1);

        // Join
        QueueService.Result joinRes = queueService.joinQueue(p1Id, MatchType.DUEL_1V1, "tank");
        assertTrue(joinRes.success());
        assertTrue(queueService.isInQueue(p1Id));

        // Leave
        QueueService.Result leaveRes = queueService.leaveQueue(p1Id);
        assertTrue(leaveRes.success());
        assertFalse(queueService.isInQueue(p1Id));
    }
}
