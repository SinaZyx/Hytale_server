package com.kingc.hytale.duels.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class PipeCommand extends AbstractPlayerCommand {

    private final HytaleDuelsPlugin plugin;

    public PipeCommand(HytaleDuelsPlugin plugin) {
        super("pipe", "Runs a diagnostic pipeline to check for errors");
        this.plugin = plugin;
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be executed by a player."));
            return;
        }

        ctx.sendMessage(Message.raw("&eStarting Diagnostic Pipeline..."));

        try {
            // 1. Check Core Services
            checkServices(ctx);

            // 2. Check UI Loading (Sanity Check)
            checkUIs(ctx, playerRef, store, ref);

            ctx.sendMessage(Message.raw("&aPipeline Completed Successfully! &2[OK]"));
            
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("&cPipeline Failed: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void checkServices(CommandContext ctx) {
        ctx.sendMessage(Message.raw("&7Checking Services..."));
        
        if (plugin.core() == null) throw new RuntimeException("Core plugin not initialized");
        if (plugin.core().kitService() == null) throw new RuntimeException("KitService not initialized");
        int kitCount = plugin.core().kitService().getAllKits().size();
        ctx.sendMessage(Message.raw("&7- KitService active (" + kitCount + " kits loaded)"));
        
        if (plugin.core().matchService() == null) throw new RuntimeException("MatchService not initialized");
        ctx.sendMessage(Message.raw("&7- MatchService active"));

        ctx.sendMessage(Message.raw("&aServices OK"));
    }

    private void checkUIs(CommandContext ctx, PlayerRef playerRef, Store<EntityStore> store, Ref<EntityStore> ref) {
        ctx.sendMessage(Message.raw("&7Checking UIs (Instantiation)..."));
        
        // We verify that we can instantiate the pages, which usually triggers UI parsing
        try {
            new HelpMenuPage(plugin, playerRef);
            ctx.sendMessage(Message.raw("&7- HelpMenuPage: OK"));
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("&c- HelpMenuPage: FAILED (" + e.getMessage() + ")"));
            throw e;
        }

        try {
            new RankingMenuPage(plugin, playerRef);
            ctx.sendMessage(Message.raw("&7- RankingMenuPage: OK"));
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("&c- RankingMenuPage: FAILED (" + e.getMessage() + ")"));
            throw e;
        }

        try {
            new DuelsAdminPage(plugin, playerRef);
            ctx.sendMessage(Message.raw("&7- DuelsAdminPage: OK"));
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("&c- DuelsAdminPage: FAILED (" + e.getMessage() + ")"));
            throw e;
        }
        
        ctx.sendMessage(Message.raw("&aUI Checks OK"));
    }
}
