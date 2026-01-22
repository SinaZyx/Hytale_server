package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingc.hytale.duels.api.Location;
import com.kingc.hytale.duels.arena.Arena;
import com.kingc.hytale.duels.arena.ArenaService;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

public class ArenaCommand extends CommandBase {
    private final HytaleDuelsPlugin plugin;

    public ArenaCommand(HytaleDuelsPlugin plugin) {
        super("arena", "Manage arenas");
        this.plugin = plugin;
        this.setPermissionGroup(GameMode.Creative);
        this.setAllowsExtraArguments(true);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String input = ctx.getInputString();
        String argsStr = "";

        if (input != null) {
            String[] split = input.trim().split("\\s+", 2);
            if (split.length > 1) {
                argsStr = split[1];
            }
        }

        String[] args = argsStr.isEmpty() ? new String[0] : argsStr.split("\\s+");

        if (args.length == 0) {
            sendHelp(ctx);
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String name = args.length > 1 ? args[1] : null;

        ArenaService service = plugin.core().arenaService();

        switch (action) {
            case "create" -> {
                if (name == null) {
                    ctx.sendMessage(Message.raw("&cUsage: /arena create <name>"));
                    return;
                }
                if (service.getArena(name).isPresent()) {
                    ctx.sendMessage(Message.raw("&cArena '" + name + "' already exists."));
                    return;
                }
                Arena arena = Arena.builder(name)
                        .displayName(name)
                        .build();
                service.addArena(arena);
                try {
                    service.save();
                    ctx.sendMessage(Message.raw("&aArena '" + name + "' created!"));
                } catch (IOException e) {
                    ctx.sendMessage(Message.raw("&cError saving arena: " + e.getMessage()));
                }
            }
            case "delete" -> {
                if (name == null) {
                    ctx.sendMessage(Message.raw("&cUsage: /arena delete <name>"));
                    return;
                }
                if (service.removeArena(name)) {
                    try {
                        service.save();
                        ctx.sendMessage(Message.raw("&aArena '" + name + "' deleted!"));
                    } catch (IOException e) {
                        ctx.sendMessage(Message.raw("&cError saving deletion: " + e.getMessage()));
                    }
                } else {
                    ctx.sendMessage(Message.raw("&cCould not delete arena (not found or in use)."));
                }
            }
            case "list" -> {
                ctx.sendMessage(Message.raw("&eArenas:"));
                for (Arena a : service.getAllArenas()) {
                    ctx.sendMessage(Message.raw(" - " + a.displayName() + " (" + a.id() + ")"));
                }
            }
            case "setspawn1" -> handleSetSpawn(ctx, name, service, 1);
            case "setspawn2" -> handleSetSpawn(ctx, name, service, 2);
            case "setspectator" -> handleSetSpawn(ctx, name, service, 3);
            default -> sendHelp(ctx);
        }
    }

    private void handleSetSpawn(CommandContext ctx, String name, ArenaService service, int type) {
        if (name == null) {
            ctx.sendMessage(Message.raw("&cUsage: /arena setspawn1/2 <name>"));
            return;
        }
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("&cMust be a player."));
            return;
        }

        Optional<Arena> arenaOpt = service.getArena(name);
        if (arenaOpt.isEmpty()) {
            ctx.sendMessage(Message.raw("&cArena '" + name + "' not found."));
            return;
        }

        PlayerRef playerRef = Universe.get().getPlayer(ctx.sender().getUuid());
        if (playerRef == null)
            return;

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null)
            return;

        world.execute(() -> {
            EntityStore entityStore = world.getEntityStore();
            if (entityStore == null)
                return;

            Store<EntityStore> store = entityStore.getStore();
            Ref<EntityStore> ref = entityStore.getRefFromUUID(playerRef.getUuid());

            if (store != null && ref != null) {
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                HeadRotation headRotation = store.getComponent(ref, HeadRotation.getComponentType());

                if (transform != null) {
                    Vector3d pos = transform.getPosition();
                    Vector3f rot = headRotation != null ? headRotation.getRotation() : new Vector3f(0f, 0f, 0f);

                    Location loc = new Location(
                            world.getName(),
                            pos.getX(), pos.getY(), pos.getZ(),
                            rot.getYaw(), rot.getPitch());

                    Arena oldArena = arenaOpt.get();
                    Arena.Builder builder = Arena.builder(oldArena.id())
                            .displayName(oldArena.displayName())
                            .maxPlayers(oldArena.maxPlayers());

                    List<Location> team1 = new ArrayList<>(oldArena.team1Spawns());
                    List<Location> team2 = new ArrayList<>(oldArena.team2Spawns());
                    Location spec = oldArena.spectatorSpawn();

                    if (type == 1) {
                        team1.add(loc);
                        ctx.sendMessage(Message.raw("&aAdded spawn to Team 1 for arena " + name));
                    } else if (type == 2) {
                        team2.add(loc);
                        ctx.sendMessage(Message.raw("&aAdded spawn to Team 2 for arena " + name));
                    } else {
                        spec = loc;
                        ctx.sendMessage(Message.raw("&aSet spectator spawn for arena " + name));
                    }

                    builder.team1Spawns(team1)
                            .team2Spawns(team2)
                            .spectatorSpawn(spec);

                    service.addArena(builder.build());
                    try {
                        service.save();
                    } catch (IOException e) {
                        ctx.sendMessage(Message.raw("&cError saving arena: " + e.getMessage()));
                    }
                }
            }
        });
    }

    private void sendHelp(CommandContext ctx) {
        ctx.sendMessage(Message.raw("&6=== Arena Help ==="));
        ctx.sendMessage(Message.raw("&e/arena create <name>"));
        ctx.sendMessage(Message.raw("&e/arena delete <name>"));
        ctx.sendMessage(Message.raw("&e/arena list"));
        ctx.sendMessage(Message.raw("&e/arena setspawn1 <name>"));
        ctx.sendMessage(Message.raw("&e/arena setspawn2 <name>"));
        ctx.sendMessage(Message.raw("&e/arena setspectator <name>"));
    }
}
