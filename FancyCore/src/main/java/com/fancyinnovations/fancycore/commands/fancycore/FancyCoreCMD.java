package com.fancyinnovations.fancycore.commands.fancycore;

import com.fancyinnovations.fancycore.commands.FancyCommandBase;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jetbrains.annotations.NotNull;

public class FancyCoreCMD extends FancyCommandBase {

    public FancyCoreCMD() {
        super("fancycore", "Manage the FancyCore plugin");
        addAliases("fc");

        addSubCommand(new FancyCoreVersionCMD());
        addSubCommand(new FancyCoreUpdateCMD());
    }
}
