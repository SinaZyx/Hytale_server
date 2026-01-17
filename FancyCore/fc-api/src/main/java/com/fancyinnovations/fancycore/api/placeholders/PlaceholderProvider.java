package com.fancyinnovations.fancycore.api.placeholders;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlaceholderProvider {

    String getName();

    String getIdentifier();

    String parse(@Nullable FancyPlayer player, @NotNull String input);
}
