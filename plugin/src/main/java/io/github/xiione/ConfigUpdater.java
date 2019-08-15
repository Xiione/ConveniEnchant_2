package io.github.xiione;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class ConfigUpdater {

    static void updateConfig(ConveniEnchant plugin) {
        InputStream customClassStream = plugin.getClass().getResourceAsStream("/config.yml");
        InputStreamReader strR = new InputStreamReader(customClassStream);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(strR);
        try {
            if(new File(plugin.getDataFolder() + "/config.yml").exists()) {
                boolean changesMade = false;
                YamlConfiguration tmp = new YamlConfiguration();
                tmp.load(plugin.getDataFolder() + "/config.yml");
                for(String str : cfg.getKeys(true)) {
                    if(!tmp.getKeys(true).contains(str)) {
                        tmp.set(str, cfg.get(str));
                        changesMade = true;
                    }
                }
                if(changesMade)
                    tmp.save(plugin.getDataFolder() + "/config.yml");
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}
