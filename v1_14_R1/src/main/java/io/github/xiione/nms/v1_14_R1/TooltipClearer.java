package io.github.xiione.nms.v1_14_R1;

import net.minecraft.server.v1_14_R1.ContainerEnchantTable;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftInventoryView;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TooltipClearer implements io.github.xiione.api.TooltipClearer {
    @Override
    public void clearTooltips(JavaPlugin plugin, PrepareItemEnchantEvent e) {
        CraftInventoryView view = (CraftInventoryView) e.getView();
        ContainerEnchantTable table = (ContainerEnchantTable) view.getHandle();
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> { //stolen from oldEnchanting
            for (int i = 0; i < table.enchantments.length; i++)
                table.enchantments[i] = -1;
        }, 1);
    }
}
