package com.fancyinnovations.fancycore.commands.permissions.player;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import org.jetbrains.annotations.NotNull;

public class PermissionsCheckCMD extends CommandBase {

    protected final RequiredArg<String> permissionArg = this.withRequiredArg("permission", "the permission to test", ArgTypes.STRING);
    protected final OptionalArg<FancyPlayer> targetArg = this.withOptionalArg("target", "the target player", FancyCoreArgs.PLAYER);

    public PermissionsCheckCMD() {
        super("check", "Checks if you/someone has a permission");
        addAliases("test");

        requirePermission("fancycore.commands.permissions.check");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!targetArg.provided(ctx) && !ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("You must specify a target player when executing this command from the console."));
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());

        FancyPlayer target = targetArg.provided(ctx) ? targetArg.get(ctx) : fp;

        String permission = permissionArg.get(ctx);

        boolean success = PermissionsModule.get().hasPermission(target.getData().getUUID(), permission);

        if (ctx.isPlayer()) {
            fp.sendMessage("Player " + target.getData().getUsername() + (success ? " has " : " does not have ") + "the permission " + permission + ".");
        } else {
            ctx.sendMessage(Message.raw("Player " + target.getData().getUsername() + (success ? " has " : " does not have ") + "the permission " + permission + "."));
        }
    }
}
