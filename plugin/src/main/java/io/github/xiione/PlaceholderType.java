package io.github.xiione;

import java.util.HashMap;

public enum PlaceholderType {
    LEVEL("%level%"),
    LAPIS("%lapis%"),
    EXP_TOTAL("%exp_total%"),
    EXP_REMAINING("%exp_remaining%"),
    EXP_PROGRESS("%exp_progress%");

    private String key;
    PlaceholderType(String key) { this.key = key; }

    public static HashMap<String, PlaceholderType> getMap() {
        HashMap<String, PlaceholderType> map = new HashMap<>();
        for(PlaceholderType type : PlaceholderType.values()) {
            map.put(type.key, type);
        }
        return map;
    }
}
