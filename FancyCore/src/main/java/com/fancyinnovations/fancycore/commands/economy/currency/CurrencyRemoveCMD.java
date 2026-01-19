package com.fancyinnovations.fancycore.commands.economy.currency;

import com.fancyinnovations.fancycore.api.economy.Currency;
import com.fancyinnovations.fancycore.api.economy.CurrencyService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class CurrencyRemoveCMD extends CommandBase {

    protected final RequiredArg<Currency> currencyArg = this.withRequiredArg("currency", "name of the currency", FancyCoreArgs.CURRENCY);

    public CurrencyRemoveCMD() {
        super("remove", "Remove a specific currency from the system");
        requirePermission("fancycore.commands.currency.remove");
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

        Currency primaryCurrency = CurrencyService.get().getPrimaryCurrency();
        Currency currency = currencyArg.get(ctx);
        if (currency.name().equals(primaryCurrency.name())) {
            fp.sendMessage("You cannot remove the primary currency.");
            return;
        }

        CurrencyService.get().unregisterCurrency(currency.name());

        fp.sendMessage("Currency " + currency.name() + " has been removed from the system.");
    }
}
