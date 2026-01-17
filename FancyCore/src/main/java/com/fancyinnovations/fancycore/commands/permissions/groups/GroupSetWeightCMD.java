package com.fancyinnovations.fancycore.commands.permissions.groups;

import com.fancyinnovations.fancycore.api.permissions.Group;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class GroupSetWeightCMD extends CommandBase {

    protected final RequiredArg<Group> groupArg = this.withRequiredArg("group", "name of the group", FancyCoreArgs.GROUP);
    protected final RequiredArg<Integer> weightArg = this.withRequiredArg("weight", "the new weight for the group", ArgTypes.INTEGER);

    protected GroupSetWeightCMD() {
        super("setweight", "Sets a group's weight");
        requirePermission("fancycore.commands.groups.setweight");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be executed by a player."));
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            ctx.sendMessage(Message.raw("FancyPlayer not found."));
            return;
        }

        Group group = groupArg.get(ctx);
        int weight = weightArg.get(ctx);

        group.setWeight(weight);

        FancyCorePlugin.get().getPermissionStorage().storeGroup(group);

        fp.sendMessage("Set prefix of group " + group.getName() + " to " + weight + ".");
    }
}
