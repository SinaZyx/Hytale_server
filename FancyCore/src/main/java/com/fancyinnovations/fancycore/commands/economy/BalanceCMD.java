package com.fancyinnovations.fancycore.commands.economy;

import com.fancyinnovations.fancycore.api.economy.Currency;
import com.fancyinnovations.fancycore.api.economy.CurrencyService;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.commands.arguments.FancyCoreArgs;
import com.fancyinnovations.fancycore.utils.NumberUtils;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class BalanceCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final OptionalArg<FancyPlayer> targetArg = this.withOptionalArg("target", "Username or UUID",
            FancyCoreArgs.PLAYER);

    public BalanceCMD() {
        super("balance", "Check your or someone else's balance.");
        addAliases("bal");
        requirePermission("fancycore.commands.balance");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            translator.getMessage("error.command.player_only").sendTo(ctx.sender());
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            translator.getMessage("error.player.not_found").sendTo(ctx.sender());
            return;
        }

        FancyPlayer target = targetArg.provided(ctx) ? targetArg.get(ctx) : fp;

        Currency currency = CurrencyService.get().getPrimaryCurrency();
        if (currency == null) {
            translator.getMessage("economy.currency.no_primary", fp.getLanguage()).sendTo(fp);
            return;
        }

        double balance = target.getData().getBalance(currency);
        String key = target.equals(fp) ? "economy.balance.self" : "economy.balance.other";
        translator.getMessage(key, fp.getLanguage())
                .replace("player", target.getData().getUsername())
                .replace("amount", currency.symbol() + NumberUtils.formatNumber(balance))
                .replace("currency", currency.name())
                .sendTo(fp);
    }
}
