package com.fancyinnovations.fancycore.commands.economy.currency;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.fancyinnovations.fancycore.api.economy.Currency;
import com.fancyinnovations.fancycore.api.economy.CurrencyService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class CurrencyCreateCMD extends CommandBase {

    protected final RequiredArg<String> nameArg = this.withRequiredArg("name", "name of the new currency", ArgTypes.STRING);
    protected final OptionalArg<String> symbolArg = this.withOptionalArg("symbol", "symbol of the new currency", ArgTypes.STRING);
    protected final OptionalArg<Boolean> serverboundArg = this.withOptionalArg("serverbound", "whether the currency should be bound to this server only", ArgTypes.BOOLEAN);

    public CurrencyCreateCMD() {
        super("create", "Create a new currency in the system");
        requirePermission("fancycore.commands.currency.create");
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

        String name = nameArg.get(ctx);
        String symbol = symbolArg.provided(ctx) ? symbolArg.get(ctx) : "";
        String server = serverboundArg.provided(ctx) && serverboundArg.get(ctx) ?
                FancyCore.get().getConfig().getServerName() :
                "global";

        if (CurrencyService.get().getCurrency(name) != null) {
            fp.sendMessage("A currency with the name " + name + " already exists.");
            return;
        }

        Currency newCurrency = new Currency(name, symbol, 2, server);
        CurrencyService.get().registerCurrency(newCurrency);

        fp.sendMessage("Currency " + name + " with symbol " + symbol + " has been created.");
    }
}
