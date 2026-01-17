package com.fancyinnovations.fancycore.player.storage.mongo;

import com.fancyinnovations.fancycore.api.player.FancyPlayerData;
import com.fancyinnovations.fancycore.api.player.FancyPlayerStorage;
import com.fancyinnovations.fancycore.player.storage.json.JsonFancyPlayer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FancyPlayerMongoStorage implements FancyPlayerStorage {

    private final MongoCollection<JsonFancyPlayer> coll;

    public FancyPlayerMongoStorage(MongoDatabase db, String collectionName) {
        this.coll = db.getCollection(collectionName, JsonFancyPlayer.class);
    }

    @Override
    public void savePlayer(FancyPlayerData player) {
        coll.replaceOne(
                new Document("uuid", player.getUUID().toString()),
                JsonFancyPlayer.from(player),
                new ReplaceOptions().upsert(true)
        );
    }

    @Override
    public FancyPlayerData loadPlayer(UUID uuid) {
        JsonFancyPlayer jsonPlayer = coll.find(new Document("uuid", uuid.toString())).first();
        if (jsonPlayer == null) {
            return null;
        }

        return jsonPlayer.toFancyPlayer();
    }

    @Override
    public FancyPlayerData loadPlayerByUsername(String username) {
        JsonFancyPlayer jsonPlayer = coll.find(new Document("username", username)).first();
        if (jsonPlayer == null) {
            return null;
        }

        return jsonPlayer.toFancyPlayer();
    }

    @Override
    public List<FancyPlayerData> loadAllPlayers() {
        return coll.find()
                .map(JsonFancyPlayer::toFancyPlayer)
                .into(new ArrayList<>());
    }

    @Override
    public void deletePlayer(UUID uuid) {
        coll.deleteOne(new Document("uuid", uuid.toString()));
    }

    @Override
    public int countPlayers() {
        return (int) coll.countDocuments();
    }
}
