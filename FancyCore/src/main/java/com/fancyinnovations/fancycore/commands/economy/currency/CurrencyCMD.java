package com.fancyinnovations.fancycore.commands.economy.currency;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class CurrencyCMD extends AbstractCommandCollection {

    public CurrencyCMD() {
        super("currency", "Manage currencies");
        requirePermission("fancycore.commands.currency");

        addSubCommand(new CurrencyListCMD());
        addSubCommand(new CurrencyInfoCMD());
        addSubCommand(new CurrencyCreateCMD());
        addSubCommand(new CurrencyRemoveCMD());
    }
}
