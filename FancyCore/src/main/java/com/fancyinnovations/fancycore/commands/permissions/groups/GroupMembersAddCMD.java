package com.fancyinnovations.fancycore.commands.permissions.groups;

import com.fancyinnovations.fancycore.api.permissions.Group;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class GroupMembersAddCMD extends CommandBase {

    protected final RequiredArg<Group> groupArg = this.withRequiredArg("group", "name of the group", FancyCoreArgs.GROUP);
    protected final RequiredArg<FancyPlayer> targetArg = this.withRequiredArg("target", "username or uuid of the target player", FancyCoreArgs.PLAYER);

    protected GroupMembersAddCMD() {
        super("add", "Adds a member to a player group");
        requirePermission("fancycore.commands.groups.members.add");
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
        FancyPlayer target = targetArg.get(ctx);

        if (group.getMembers().contains(target.getData().getUUID())) {
            fp.sendMessage("Player " + target.getData().getUsername() + " is already a member of group " + group.getName() + ".");
            return;
        }

        group.addMember(target.getData().getUUID());
        fp.getData().addGroup(group.getName());

        FancyCorePlugin.get().getPermissionStorage().storeGroup(group);

        fp.sendMessage("Player " + target.getData().getUsername() + " has been added to group " + group.getName() + ".");
    }
}
