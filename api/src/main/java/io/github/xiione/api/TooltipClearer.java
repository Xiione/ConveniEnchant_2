package io.github.xiione.api;

import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.plugin.java.JavaPlugin;

public interface TooltipClearer {
    public void clearTooltips(JavaPlugin plugin, PrepareItemEnchantEvent e);
}
