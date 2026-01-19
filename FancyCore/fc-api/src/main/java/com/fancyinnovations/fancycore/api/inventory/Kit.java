package com.fancyinnovations.fancycore.api.inventory;

import com.google.gson.annotations.SerializedName;

public record Kit(
        String name,
        @SerializedName("display_name") String displayName,
        String description
) {

}
