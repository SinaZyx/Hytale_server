package com.fancyinnovations.fancycore.inventory.storage.json;

import com.hypixel.hytale.codec.EmptyExtraInfo;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.json.JsonWriterSettings;

import java.util.ArrayList;
import java.util.List;

public class ItemStackJsonStorageHelper {

    public static String toJson(List<ItemStack> items) {
        BsonArray array = new BsonArray();
        for (ItemStack item : items) {
            BsonDocument doc = ItemStack.CODEC.encode(item, EmptyExtraInfo.EMPTY);
            array.add(doc);
        }

        return new BsonDocument("items", array).toJson(
                JsonWriterSettings.builder()
                        .indent(true)
                        .build()
        );
    }

    public static List<ItemStack> fromJson(String json) {
        List<ItemStack> items = new ArrayList<>();

        BsonDocument document = BsonDocument.parse(json);
        BsonArray array = document.getArray("items");

        for (BsonValue value : array.getValues()) {
            ItemStack item = ItemStack.CODEC.decode(value.asDocument(), EmptyExtraInfo.EMPTY);
            items.add(item);
        }

        return items;
    }

}
