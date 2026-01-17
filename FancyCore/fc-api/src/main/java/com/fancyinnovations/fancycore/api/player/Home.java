package com.fancyinnovations.fancycore.api.player;

import com.fancyinnovations.fancycore.api.teleport.Location;

public record Home(
        String name,
        Location location
) {
}
