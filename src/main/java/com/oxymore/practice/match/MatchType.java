package com.oxymore.practice.match;

public enum MatchType {
    UNRANKED_1v1,
    UNRANKED_2v2,
    RANKED_1v1,
    RANKED_2v2,
    DUEL,
    SPLIT,
    FFA,
    ;

    public boolean isSingle() {
        return this == MatchType.UNRANKED_1v1 || this == MatchType.RANKED_1v1;
    }

    public boolean isDouble() {
        return this == MatchType.UNRANKED_2v2 || this == MatchType.RANKED_2v2;
    }

    public boolean isUnranked() {
        return !isRanked();
    }

    public boolean isRanked() {
        return this == MatchType.RANKED_1v1 || this == MatchType.RANKED_2v2;
    }

    public boolean isJoinable() {
        return isSingle() || isDouble();
    }

    public String getAux() {
        if (isSingle()) {
            return "1v1";
        } else if (isDouble()) {
            return "2v2";
        } else if (this == MatchType.FFA) {
            return "FFA";
        } else if (this == MatchType.SPLIT) {
            return "Split";
        } else {
            return this.toString().toLowerCase();
        }
    }
}
