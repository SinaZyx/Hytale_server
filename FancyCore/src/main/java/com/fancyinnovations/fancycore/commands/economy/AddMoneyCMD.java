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
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public class AddMoneyCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<FancyPlayer> targetArg = this.withRequiredArg("target", "Username or UUID",
            FancyCoreArgs.PLAYER);
    protected final RequiredArg<Double> amountArg = this.withRequiredArg("target", "amount you want to add",
            ArgTypes.DOUBLE);

    public AddMoneyCMD() {
        super("addmoney", "Add a specific amount of money to another player");
        requirePermission("fancycore.commands.addmoney");
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

        FancyPlayer target = targetArg.get(ctx);
        double amount = amountArg.get(ctx);
        if (amount <= 0) {
            translator.getMessage("economy.pay.invalid_amount", fp.getLanguage()).sendTo(fp);
            return;
        }

        Currency currency = CurrencyService.get().getPrimaryCurrency();
        if (currency == null) {
            translator.getMessage("economy.currency.no_primary", fp.getLanguage()).sendTo(fp);
            return;
        }

        target.getData().addBalance(currency, amount);
        String formattedAmount = NumberUtils.formatNumber(amount);
        translator.getMessage("economy.add.success", fp.getLanguage())
                .replace("amount", currency.symbol() + formattedAmount)
                .replace("player", target.getData().getUsername())
                .replace("currency", currency.name())
                .sendTo(fp);
    }
}
