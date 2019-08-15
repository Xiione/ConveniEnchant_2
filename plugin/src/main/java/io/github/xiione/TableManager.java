package io.github.xiione;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


class TableManager {

    private final ConveniEnchant plugin;
    TableManager(ConveniEnchant plugin) {
        this.plugin = plugin;
    }

    private final HashMap<UUID, Boolean> usingTables = new HashMap<>(); //initialize storage maps for each value
    private final HashMap<UUID, Integer> tableLevels = new HashMap<>();
    private final HashMap<UUID, Integer> tableExps = new HashMap<>();
    private final HashMap<UUID, Integer> tableLapisCounts = new HashMap<>();
    private final HashMap<UUID, Block> tableBlocks = new HashMap<>();

    void setUsingTables(UUID u) {
        usingTables.put(u, true);
    } //setters and getters galore
    boolean isUsingTable(UUID u) {
        return usingTables.getOrDefault(u, false);
    }

    void setTableLevel(UUID u, int i) {
        tableLevels.put(u, i);
    }
    int getTableLevel(UUID u) {
        return tableLevels.getOrDefault(u, 0);
    }

    void setTableExp(UUID u, int i) {
        tableExps.put(u, i);
    }
    int getTableExp(UUID u) {
        return tableExps.getOrDefault(u, 0);
    }

    void setTableLapisCount(UUID u, int i) { tableLapisCounts.put(u, i); }
    int getTableLapisCount(UUID u) { return tableLapisCounts.getOrDefault(u, 0); }

    void setTableBlock(UUID u, Block b) {
        tableBlocks.put(u, b);
    }
    Block getTableBlock(UUID u) {
        return tableBlocks.get(u);
    }

    Player getTableUser(Block b) { //get user of specified table block
        for (Map.Entry<UUID, Block> entry : tableBlocks.entrySet()) {
            if (entry.getValue().equals(b)) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null; //if the block isnt linked to an enchanter
    }

    void clearValues(UUID u) {
        usingTables.remove(u);
        tableLevels.remove(u);
        tableExps.remove(u);
        tableLapisCounts.remove(u);
        tableBlocks.remove(u);
    }


    void onDisableProtection() {
        for (Map.Entry<UUID, Boolean> entry : usingTables.entrySet()) {
            UUID u = entry.getKey();
            Player p = Bukkit.getPlayer(u);
            p.closeInventory();
        }
    }
}
