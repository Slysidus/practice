package com.oxymore.practice.documents;

import com.mongodb.client.model.Filters;
import com.oxymore.practice.configuration.match.Kit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.conversions.Bson;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KitDocument {
    private UUID owner;
    private String modeId;
    private int slot;

    private String name;
    private Kit kit;

    @BsonIgnore
    public Bson getFilter() {
        return Filters.and(Filters.eq("owner", owner), Filters.eq("modeId", modeId), Filters.eq("slot", slot));
    }

    @BsonIgnore
    public String getDisplayName() {
        return name != null ? name : String.valueOf(slot + 1);
    }
}
