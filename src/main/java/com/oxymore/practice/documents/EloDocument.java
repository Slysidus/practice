package com.oxymore.practice.documents;

import lombok.*;

import java.util.UUID;

@Setter(AccessLevel.NONE)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EloDocument {
    private UUID playerId;
    private String mode;
    private String aux;
    @Setter(AccessLevel.PUBLIC)
    private int elo;
}
