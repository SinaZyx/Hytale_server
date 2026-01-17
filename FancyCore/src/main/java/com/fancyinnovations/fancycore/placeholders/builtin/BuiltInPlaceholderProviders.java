package com.fancyinnovations.fancycore.placeholders.builtin;

import com.fancyinnovations.fancycore.api.placeholders.PlaceholderService;
import com.fancyinnovations.fancycore.placeholders.builtin.player.*;

public class BuiltInPlaceholderProviders {

    public static void registerAll() {
        // Player placeholders
        PlaceholderService.get().registerProvider(PlayerBalanceFormattedPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerBalanceRawPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerChatColorPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerFirstTimeJoinedPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerFirstTimeJoinedRawPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerGroupPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerGroupPrefixPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerGroupSuffixPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerNamePlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerNickNamePlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerPlayTimeFormattedPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerPlayTimeMsPlaceholder.INSTANCE);
        PlaceholderService.get().registerProvider(PlayerUuidPlaceholder.INSTANCE);
    }

}
