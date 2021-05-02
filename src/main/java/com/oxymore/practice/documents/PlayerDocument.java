package com.oxymore.practice.documents;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDocument {
    @Setter(AccessLevel.NONE)
    private UUID playerId;
    private int kills, deaths;
}
