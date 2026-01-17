package com.fancyinnovations.fancycore.commands.economy.currency;

import com.fancyinnovations.fancycore.api.economy.Currency;
import com.fancyinnovations.fancycore.api.economy.CurrencyService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class CurrencyListCMD extends CommandBase {

    public CurrencyListCMD() {
        super("list", "List all available currencies");
        requirePermission("fancycore.commands.currency.list");
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

        fp.sendMessage("Available Currencies:");
        for (Currency currency : CurrencyService.get().getAllCurrencies()) {
            boolean isPrimary = currency.name().equals(primaryCurrency.name());
            fp.sendMessage(" - " + currency.name() + " (" + currency.symbol() + ")" + (isPrimary ? " [Primary]" : ""));
        }
    }
}
