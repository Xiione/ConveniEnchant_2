package io.github.xiione;

import com.codingforcookies.armorequip.ArmorListener;
import com.codingforcookies.armorequip.DispenserArmorListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

//TODO add alternative level-up/adjustment scheme
//TODO EVEN more permission checks

public class ConveniEnchant extends JavaPlugin {

    private final TableManager tableManager = new TableManager(this);

    @Override
    public void onEnable() {

        final ConveniEnchantClass conveniEnchantClass = new ConveniEnchantClass(this, tableManager);
        this.saveDefaultConfig();
        ConfigUpdater.updateConfig(this);

        this.getCommand("convenienchant").setExecutor(conveniEnchantClass);

        getServer().getPluginManager().registerEvents(conveniEnchantClass, this);
        getServer().getPluginManager().registerEvents(new ArmorListener(), this); //initialize ArmorEquipEvent libraries
        try {
            Class.forName("org.bukkit.event.block.BlockDispenseArmorEvent");
            getServer().getPluginManager().registerEvents(new DispenserArmorListener(), this);
        } catch (Exception ignored) {
        }

        conveniEnchantClass.loadConfigs(false);

        if(conveniEnchantClass.allow_crafting) {
            NamespacedKey key = new NamespacedKey(this, "stock_convenienchant_table");
            ItemStack stack = conveniEnchantClass.createStack(conveniEnchantClass.crafting_output_amount);
            ShapedRecipe stockTableRecipe = new ShapedRecipe(key, stack);
            stockTableRecipe.shape(conveniEnchantClass.recipe_toprow, conveniEnchantClass.recipe_middle, conveniEnchantClass.recipe_bottom);

            for (String ingredient : conveniEnchantClass.recipe_ingredients) {
                String[] ingredients = ingredient.split(",");
                char character = ingredients[0].charAt(0);
                Material material = Material.matchMaterial(ingredients[1]);
                stockTableRecipe.setIngredient(character, material);
            }
            Bukkit.addRecipe(stockTableRecipe);
        }

    }

    @Override
    public void onDisable() {
        tableManager.onDisableProtection(); //attempt closing inventories
    }


}



