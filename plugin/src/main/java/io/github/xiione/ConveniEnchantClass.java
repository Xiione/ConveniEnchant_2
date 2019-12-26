package io.github.xiione;

import com.codingforcookies.armorequip.ArmorEquipEvent;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTListCompound;
import io.github.xiione.api.TooltipClearer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ConveniEnchantClass implements Listener, CommandExecutor, TabCompleter {

    ConveniEnchantClass(ConveniEnchant plugin, TableManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private final ConveniEnchant plugin;
    private final TableManager manager;
    private final double PLUGIN_VERSION = 2.0;


    private int item_level_default, item_exp_default, item_lapis_default, level_cap_default, exp_gained_modifier, levels_gained_cap;
    int crafting_output_amount;
    private boolean notify_update, place_table_block, level_up_playsound, hide_enchant_hints, allow_lapis_storage, return_table_item, item_allow_equip;
    boolean allow_crafting;
    private String item_display_name, item_head_uuid, item_head_texture_string;
    String recipe_toprow, recipe_middle, recipe_bottom;
    private float level_up_sound_volume, level_up_sound_pitch;
    private List<String> item_lore;
    List<String> recipe_ingredients;
    private Sound level_up_sound;
    private Material item_material;

    void loadConfigs(boolean reload) { //load config values
        FileConfiguration config = plugin.getConfig();
        plugin.saveDefaultConfig(); //create the config if it does not exist
        if (reload) {
            plugin.reloadConfig(); //if being issued via command, reload config values
        }
        config = plugin.getConfig();
        item_level_default = config.getInt("item-level-default");
        item_exp_default = config.getInt("item-exp-default");
        item_lapis_default = config.getInt("item-lapis-default");
        level_cap_default = config.getInt("level-cap-default");
        exp_gained_modifier = config.getInt("exp-gained-modifier");
        levels_gained_cap = config.getInt("levels-gained-cap");
        crafting_output_amount = config.getInt("crafting-output-amount");

        notify_update = config.getBoolean("notify-update");
        place_table_block = config.getBoolean("place-table-block");
        level_up_playsound = config.getBoolean("level-up-playsound");
        hide_enchant_hints = config.getBoolean("hide-enchant-hints");
        allow_lapis_storage = config.getBoolean("allow-lapis-storage");
        return_table_item = config.getBoolean("return-table-item");
        allow_crafting = config.getBoolean("allow-crafting");
        item_allow_equip = config.getBoolean("item-allow-equip");

        item_display_name = ChatColor.translateAlternateColorCodes('&', config.getString("item-display-name"));
        item_head_uuid = config.getString("item-head-uuid");
        item_head_texture_string = config.getString("item-head-texture-string");
        recipe_toprow = config.getString("recipe-toprow");
        recipe_middle = config.getString("recipe-middle");
        recipe_bottom = config.getString("recipe-bottom");

        level_up_sound_volume = (float) config.getDouble("level-up-sound-volume");
        level_up_sound_pitch = (float) config.getDouble("level-up-sound-pitch");
        item_lore = config.getStringList("item-lore");
        recipe_ingredients = config.getStringList("recipe-ingredients");

        level_up_sound = Sound.valueOf(config.getString("level-up-sound"));
        item_material = Material.valueOf(config.getString("item-material"));
    }

    @EventHandler
    public void armorEquip(ArmorEquipEvent e) {
        if (!item_allow_equip && e.getPlayer().getGameMode() != GameMode.CREATIVE && e.getNewArmorPiece().getType() == item_material) { //Prevent table item from being equipped as headgear
            ItemStack i = e.getNewArmorPiece(); //May be worth writing own handlers to reduce size TODO Write own handlers for head equip
            NBTItem n = new NBTItem(i);
            if (n.hasKey("CONVENIENCHANT_EXP")) e.setCancelled(true);
        }
    }

    @EventHandler
    public void blockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.ENCHANTING_TABLE) {
            Block b = e.getBlock();
            Player p = e.getPlayer();
            Player u = manager.getTableUser(b);
            World w = p.getWorld();
            if (u != null) { //if the table broken is linked to an active enchanter...
                b.setType(Material.AIR);
                e.setCancelled(true);
                u.closeInventory(); //force close enchanter's gui
            }
        }
    }

    @EventHandler
    public void blockExplode(BlockExplodeEvent e) { //actually impossible, but worth including for failsafe purposes
        for (Block b : e.blockList()) { //for each block affected by the explosion

            Player u = manager.getTableUser(b);

            if (b.getType() == Material.ENCHANTING_TABLE) {
                if (u != null) {
                    b.setType(Material.AIR);
                    e.setCancelled(true); //does this prevent the explosion from damaging blocks entirely?? whatever
                    u.closeInventory(); //force close enchanter's gui
                }
            }
        }
    }

    @EventHandler
    public void blockPlace(BlockPlaceEvent e) {
        ItemStack i = e.getItemInHand();
        NBTItem n = new NBTItem(i);
        Player p = e.getPlayer();
        if (p.hasPermission("covenienchant.use") && n.hasKey("CONVENIENCHANT_EXP")) { //if held item is a valid table...

            UUID u = p.getUniqueId();
            Block b = e.getBlockPlaced();
            Material eyeblock = p.getEyeLocation().getBlock().getType();

            e.setCancelled(true); //cancel actual placing
            int lapisCount = n.getInteger("CONVENIENCHANT_LAPIS_COUNT");

            if (place_table_block) { //if a table block is to be placed at the place location
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> { //zero(?) ticks later, change placed location to enchanting table block
                    b.setType(Material.ENCHANTING_TABLE);
                    InventoryView v = p.openEnchanting(b.getLocation(), true); //open enchant gui, casting table location at location of placed table
                    if (lapisCount > 0) {
                        ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI, lapisCount); //put stored amount of lapis in lapis slot of gui
                        v.setItem(1, lapis);
                    }
                }, 0L);
                manager.setTableBlock(u, b); //mark the block as being used
            } else {
                try {
                    InventoryView v = p.openEnchanting(p.getEyeLocation(), true);
                    if (lapisCount > 0) {
                        ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI, lapisCount); //put stored amount of lapis in lapis slot of gui
                        v.setItem(1, lapis);
                    }
                } catch (ClassCastException exception) { //if the block occupying the location of the player's head can't be cast, simply do nothing
                    return;
                }
            }

            manager.setUsingTables(u); //initialize various temporary values for storage while table is out of ItemStack form
            manager.setTableLevel(u, n.getInteger("CONVENIENCHANT_LEVEL"));
            manager.setTableExp(u, n.getInteger("CONVENIENCHANT_EXP"));
            if (e.getHand() == EquipmentSlot.HAND) { //take corresponding ItemStack from hand
                ItemStack hand = p.getInventory().getItemInMainHand();
                hand.setAmount(hand.getAmount() - 1);
            } else {
                ItemStack hand = p.getInventory().getItemInOffHand();
                hand.setAmount(hand.getAmount() - 1);
            }
        }
    }


    @EventHandler     //TODO add alternative level-up scheme
    public void enchantItem(EnchantItemEvent e) {
        UUID u = e.getEnchanter().getUniqueId();
        if (manager.isUsingTable(u)) { //if the enchant was performed in a ConveniEnchant gui...

            int expGained = (exp_gained_modifier * (e.whichButton() + 1)); //calculate experience gained
            Player p = e.getEnchanter();
            World w = p.getWorld();

            manager.setTableExp(u, manager.getTableExp(u) + expGained); //add to temporary exp storage value

            int totalExpReqNextLevel;

            for (int i = 1; i <= levels_gained_cap; i++) { //for as many levels as the config will allow...
                totalExpReqNextLevel = (int) ((Math.pow(manager.getTableLevel(u), 2) + (6 * manager.getTableLevel(u)))); //calculate the exp needed till next level is reached
                if (manager.getTableExp(u) > totalExpReqNextLevel && manager.getTableLevel(u) < level_cap_default) { //if the level is below the level cap, and
                    manager.setTableLevel(u, manager.getTableLevel(u) + 1); //level up!                                  //the amount of exp is greater than the amount needed to reach the next level...
                    if (level_up_playsound)
                        w.playSound(p.getLocation(), level_up_sound, level_up_sound_volume, level_up_sound_pitch); //play the configured level up sound
                } else { //otherwise no need to continue looping
                    break;
                }
            }
        }
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent e) {
        if (e.getInventory() instanceof EnchantingInventory && manager.isUsingTable(e.getPlayer().getUniqueId()) && e.getPlayer() instanceof Player) {

            Player p = (Player) e.getPlayer(); //shorthands for days
            UUID u = p.getUniqueId();
            World w = p.getWorld();
            Block b = manager.getTableBlock(u);
            int lapiscount;

            if (e.getInventory().getItem(1) != null && e.getInventory().getItem(1).getType() == Material.LAPIS_LAZULI) {  //get amount of lapis on gui close
                lapiscount = e.getInventory().getItem(1).getAmount();
            } else {
                lapiscount = 0;
            }

            if (allow_lapis_storage) {
                e.getInventory().setItem(1, null); //clear lapis slot on close so it isn't returned to the player
                manager.setTableLapisCount(u, lapiscount); //set temp lapis count storage
            }
            if (place_table_block && manager.getTableBlock(u) != null) {
                b.setType(Material.AIR); //get rid of table and play break effects
                w.playEffect(b.getLocation(), Effect.STEP_SOUND, Material.ENCHANTING_TABLE);
            }

            dropPlacedTable(p); //return table to enchanter with updated values
            manager.clearValues(u); //clear all temp values under enchanter's UUID/Name
        }
    }

    @EventHandler
    public void playerInteract(PlayerInteractEvent e) {
        if ((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)) { //if the interaction is a right click
            ItemStack i = e.getItem();
            if ((!e.isBlockInHand() && i != null) || (!place_table_block && e.isCancelled())) { //let blockplace handle block place interactions,
                NBTItem n = new NBTItem(i);                                                     //if table block placing is disabled and the material happens to be a block, let air clicks work too
                Player p = e.getPlayer();
                if (p.hasPermission("covenienchant.use") && n.hasKey("CONVENIENCHANT_EXP")) { //if held item is a valid table...
                    UUID u = p.getUniqueId();
                    Material eyeblock = p.getEyeLocation().getBlock().getType();
                    int lapisCount = n.getInteger("CONVENIENCHANT_LAPIS_COUNT");

                    try {
                        InventoryView v = p.openEnchanting(p.getEyeLocation(), true);
                        if (lapisCount > 0) {
                            ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI, lapisCount); //put stored amount of lapis in lapis slot of gui
                            v.setItem(1, lapis);
                        }
                    } catch (ClassCastException exception) { //if the block occupying the location of the player's head can't be cast, simply do nothing
                        return;
                    }
                    manager.setUsingTables(u); //initialize various temporary values for storage while table is out of ItemStack form
                    manager.setTableLevel(u, n.getInteger("CONVENIENCHANT_LEVEL"));
                    manager.setTableExp(u, n.getInteger("CONVENIENCHANT_EXP"));
                    if (e.getHand() == EquipmentSlot.HAND) { //take corresponding ItemStack from hand
                        ItemStack hand = p.getInventory().getItemInMainHand();
                        hand.setAmount(hand.getAmount() - 1);
                    } else {
                        ItemStack hand = p.getInventory().getItemInOffHand();
                        hand.setAmount(hand.getAmount() - 1);
                    }
                }
            }
        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (notify_update && p.hasPermission("convenienchant.notifyupdate")) {
            UpdateCheck
                    .of(plugin)
                    .resourceId(69428)
                    .handleResponse((versionResponse, version) -> {
                        switch (versionResponse) {
                            case FOUND_NEW:
                                p.sendMessage(ChatColor.RED + "A new version of ConveniEnchant is available!" + ChatColor.GRAY + " (" + ChatColor.GRAY + version + ChatColor.GRAY + ")");
                                p.sendMessage(ChatColor.GRAY + "You can find it here: " + ChatColor.RED + "https://www.spigotmc.org/resources/convenienchant.69428/");
                                break;
                            case LATEST:
                                break;
                            case UNAVAILABLE:
                                p.sendMessage(ChatColor.RED + "Unable to perform a version check for Convenienchant.");
                        }
                    }).check();
        }
    }

    @EventHandler
    public void prepareItemEnchant(PrepareItemEnchantEvent e) {
        UUID u = e.getEnchanter().getUniqueId();

        if (manager.isUsingTable(u)) {
            int tableLevel = manager.getTableLevel(u);
            calculateLevels(tableLevel, e);
            if (hide_enchant_hints) { //if hiding tooltips is enabled
                TooltipClearer clearer = this.getTooltipClearer();  //load correct NMS methods
                if(clearer != null)
                    clearer.clearTooltips(plugin, e);
            }
        }

    }

    private TooltipClearer getTooltipClearer() {
        try {
            final Class<?> aClass = Class.forName("io.github.xiione.nms." + (plugin.version + ".TooltipClearer"));
            // Check if we have a NMSHandler class at that location.
            if (TooltipClearer.class.isAssignableFrom(aClass)) { // Make sure it actually implements NMS
                return (TooltipClearer) aClass.getConstructor().newInstance(); // Set our handler
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    //Begin cool methods

    private List<String> createLore(UUID uuid) { //create lore from already stored values
        List<String> lores = new ArrayList<>();

        int level, lapis, exp;
        level = manager.getTableLevel(uuid);
        lapis = manager.getTableLapisCount(uuid);
        exp = manager.getTableExp(uuid);
        for (String line : item_lore) {
            for (Map.Entry<String, PlaceholderType> entry : PlaceholderType.getMap().entrySet()) {
                if (line.contains(entry.getKey())) {
                    switch (entry.getValue()) {
                        case LEVEL:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(level));
                            break;
                        case EXP_TOTAL:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(exp));
                            break;
                        case LAPIS:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(lapis));
                            break;
                        case EXP_REMAINING:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(calculateRemainingExp(level, exp)));
                            break;
                        case EXP_PROGRESS:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(calculateExpProgress(level, exp)));
                            break;
                    }
                }
            }

            line = ChatColor.translateAlternateColorCodes('&', line);
            lores.add(line);
        }
        return lores;
    }

    private List<String> createLore(int level, int exp, int lapis) { //create lore from passed values
        List<String> lores = new ArrayList<>();

        for (String line : item_lore) {
            for (Map.Entry<String, PlaceholderType> entry : PlaceholderType.getMap().entrySet()) {
                if (line.contains(entry.getKey())) {
                    switch (entry.getValue()) {
                        case LEVEL:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(level));
                            break;
                        case EXP_TOTAL:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(exp));
                            break;
                        case LAPIS:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(lapis));
                            break;
                        case EXP_REMAINING:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(calculateRemainingExp(level, exp)));
                            break;
                        case EXP_PROGRESS:
                            line = line.replace(entry.getKey(),
                                    Integer.toString(calculateExpProgress(level, exp)));
                            break;
                    }
                }
            }

            line = ChatColor.translateAlternateColorCodes('&', line);
            lores.add(line);
        }
        return lores;
    }


    ItemStack createStack(UUID uuid) { //create a stack from already stored values by uuid
        ItemStack table = new ItemStack(item_material);
        NBTItem tableNBT = new NBTItem(table);

        int exp, level, lapis;
        UUID id;
        exp = manager.getTableExp(uuid);
        level = manager.getTableLevel(uuid);
        lapis = manager.getTableLapisCount(uuid);
        id = UUID.randomUUID();

        if (item_material == Material.PLAYER_HEAD) {
            NBTCompound skull = tableNBT.addCompound("SkullOwner");
            skull.setString("Name", "CONVENIENCHANT_TABLE");
            skull.setString("Id", item_head_uuid);
            NBTListCompound texture = skull.addCompound("Properties").getCompoundList("textures").addCompound();
            texture.setString("Value", item_head_texture_string);
        }
        tableNBT.setInteger("CONVENIENCHANT_LEVEL", level);
        tableNBT.setInteger("CONVENIENCHANT_EXP", exp);
        tableNBT.setInteger("CONVENIENCHANT_LAPIS_COUNT", lapis);
        tableNBT.setString("CONVENIENCHANT_UUID", id.toString());

        table = tableNBT.getItem();
        ItemMeta meta = table.getItemMeta();
        meta.setDisplayName(item_display_name);
        meta.setLore(createLore(uuid));

        table.setItemMeta(meta);
        return table;
    }

    ItemStack createStack(int count) { //create a stack with default values
        ItemStack table = new ItemStack(item_material);
        NBTItem tableNBT = new NBTItem(table);

        int exp, level, lapis;

        level = item_level_default;
        exp = item_exp_default;
        lapis = item_lapis_default;

        UUID id = UUID.fromString("e37f04bf-b9e9-47fd-98e2-49a983ad3e5d"); //standard uuid until i TODO think of a better way to give unstackables

        if (item_material == Material.PLAYER_HEAD) {
            NBTCompound skull = tableNBT.addCompound("SkullOwner");
            skull.setString("Name", "CONVENIENCHANT_TABLE");
            skull.setString("Id", item_head_uuid);
            NBTListCompound texture = skull.addCompound("Properties").getCompoundList("textures").addCompound();
            texture.setString("Value", item_head_texture_string);
        }
        tableNBT.setInteger("CONVENIENCHANT_LEVEL", level);
        tableNBT.setInteger("CONVENIENCHANT_EXP", exp);
        tableNBT.setInteger("CONVENIENCHANT_LAPIS_COUNT", lapis);
        tableNBT.setString("CONVENIENCHANT_UUID", id.toString());

        table = tableNBT.getItem();
        ItemMeta meta = table.getItemMeta();
        meta.setDisplayName(item_display_name);
        meta.setLore(createLore(level, exp, lapis));

        table.setItemMeta(meta);
        table.setAmount(count);
        return table;
    }

    ItemStack createStack(int count, int level, int exp, int lapis) { //create a stack from passed values
        ItemStack table = new ItemStack(item_material);
        NBTItem tableNBT = new NBTItem(table);
        UUID id = UUID.fromString("e37f04bf-b9e9-47fd-98e2-49a983ad3e5d");

        if (item_material == Material.PLAYER_HEAD) {
            NBTCompound skull = tableNBT.addCompound("SkullOwner");
            skull.setString("Name", "CONVENIENCHANT_TABLE");
            skull.setString("Id", item_head_uuid);
            NBTListCompound texture = skull.addCompound("Properties").getCompoundList("textures").addCompound();
            texture.setString("Value", item_head_texture_string);
        }
        tableNBT.setInteger("CONVENIENCHANT_LEVEL", level);
        tableNBT.setInteger("CONVENIENCHANT_EXP", exp);
        tableNBT.setInteger("CONVENIENCHANT_LAPIS_COUNT", lapis);
        tableNBT.setString("CONVENIENCHANT_UUID", id.toString());

        table = tableNBT.getItem();
        ItemMeta meta = table.getItemMeta();
        meta.setDisplayName(item_display_name);
        meta.setLore(createLore(level, exp, lapis));

        table.setItemMeta(meta);
        table.setAmount(count);
        return table;
    }

    private void calculateLevels(int level, PrepareItemEnchantEvent e) { //separate method because why not

        Random rand = new Random();
        int baseLevel = (int) ((rand.nextInt(8) + 1) + Math.floor(level / 2)) + (rand.nextInt(level + 1)); //randomize and calculate "base level" using temp level storage value
        EnchantmentOffer[] offers = e.getOffers();

        if (level > 2) { //if bookshelf level is over 1...
            for (int i = 0; i < 3; i++) { //create dummy offer for each slot to ensure each can be assigned the correct "cost" (level minimum)
                offers[i] = new EnchantmentOffer(Enchantment.ARROW_INFINITE, 1, 1);
            }
            offers[0].setCost(Math.max(baseLevel / 3, 1)); //calculate "cost" of each offer
            offers[1].setCost((baseLevel * 2) / 3 + 1);    //thanks minecraft wiki
            offers[2].setCost(Math.max(baseLevel, level * 2));

        } else { //otherwise only calculate for available offers
            if (offers[0] != null)
                offers[0].setCost(Math.max(baseLevel / 3, 1)); //calculate levels as normal for available slots
            if (offers[1] != null && ((baseLevel * 2) / 3 + 1) > 0) offers[1].setCost((baseLevel * 2) / 3 + 1);
            if (offers[2] != null && Math.max(baseLevel, level * 2) > 0)
                offers[2].setCost(Math.max(baseLevel, level * 2));
        }
    }

    private void dropPlacedTable(Player p) { //return the table to enchanter
        UUID u = p.getUniqueId();
        World w = p.getWorld();
        ItemStack s = createStack(u);
        if (return_table_item) {
            if (p.getInventory().addItem(s).isEmpty()) { //attempt to return the item
                return;
            }
            if (place_table_block) { //if inventory is full or table return is disabled
                w.dropItemNaturally(manager.getTableBlock(u).getLocation(), s);
            } else {
                w.dropItemNaturally(p.getLocation(), s);
            }
        }
    }

    private static int calculateRemainingExp(int level, int exp) {
        if ((Math.pow(level, 2) + (6 * level)) - exp < 0) {
            return 0;
        } else {
            return (int) (Math.pow(level, 2) + (6 * level)) - exp;
        }
    }

    private static int calculateExpProgress(int level, int exp) {
        if ((exp / (Math.pow(level, 2) + (6 * level))) * 100 > 100) {
            return 100;
        } else {
            return (int) ((exp / (Math.pow(level, 2) + (6 * level))) * 100);
        }
    }

    private boolean attemptGive(Player p, ItemStack i) {
        if (p.getInventory().addItem(i).isEmpty()) { //attempt to return the item
            return true; //successfully added item
        } else {
            p.getWorld().dropItemNaturally(p.getLocation(), i);
            return false; //item was dropped at player's location
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender.hasPermission("convenienchant.admin")) {
            if (args.length == 0) {
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cConveniEnchant " + PLUGIN_VERSION + " &7by Xiione"));
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7https://www.spigotmc.org/resources/convenienchant.69428/"));
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsage: &7/convenienchant [help|give|reload]"));
            } else switch (args[0].toLowerCase()) {
                case "reload":
                    if (args.length > 1) {
                        commandSender.sendMessage(ChatColor.RED + "Too many arguments provided!");
                        return true;
                    } else {
                        loadConfigs(true);
                        commandSender.sendMessage(ChatColor.GREEN + "ConveniEnchant config reloaded!");
                        return true;
                    }
                case "give":
                    switch (args.length) {
                        case 1: //if no other args are passed
                            if (commandSender instanceof Player) {
                                Player p = (Player) commandSender;
                                if (attemptGive(p, createStack(1))) {
                                    commandSender.sendMessage(ChatColor.GRAY + "Gave 1x " + item_display_name + ChatColor.GRAY + ".");
                                    return true;
                                } else {
                                    commandSender.sendMessage(ChatColor.GRAY + "your inventory was full, so the item was dropped at your location.");
                                    return true;
                                }
                            } else {
                                commandSender.sendMessage(ChatColor.RED + "Provide a player's name, otherwise the command must be executed by a player.");
                                return true;
                            }
                        case 2:  //if only a second arg is passed
                            Player p = Bukkit.getPlayer(args[1]);
                            ItemStack i = createStack(1);
                            if (p != null) { //if the player name passed is valid
                                if (attemptGive(p, createStack(1))) {
                                    commandSender.sendMessage(ChatColor.GRAY + "Gave 1x " + item_display_name + ChatColor.GRAY + " to " + ChatColor.GRAY + p.getName() + ChatColor.GRAY + ".");
                                    return true;
                                } else {
                                    commandSender.sendMessage(ChatColor.GRAY + p.getName() + ChatColor.GRAY + "'s inventory was full, so the item was dropped at their location.");
                                    return true;
                                }
                            } else {
                                commandSender.sendMessage(ChatColor.RED + "Player not found!");
                                return true;
                            }
                        case 5: //if player arg is not passed but rest are
                            if (commandSender instanceof Player) {
                                p = (Player) commandSender;
                                if (Bukkit.getPlayer(args[1]) == null) { //if first arg is not the name of a valid player
                                    try{
                                        int count = Integer.parseInt(args[1]);
                                        int level = Integer.parseInt(args[2]);
                                        int exp = Integer.parseInt(args[3]);
                                        int lapis = Integer.parseInt(args[4]);

                                        if (attemptGive(p, createStack(count, level, exp, lapis))) {
                                            commandSender.sendMessage(ChatColor.GRAY + "Gave 1x " + item_display_name + ChatColor.GRAY + ".");
                                            return true;
                                        } else {
                                            commandSender.sendMessage(ChatColor.GRAY + "your inventory was full, so the item was dropped at your location.");
                                            return true;
                                        }
                                    } catch(NumberFormatException e) {
                                        commandSender.sendMessage(ChatColor.RED + "Invalid arguments!");
                                        return true;
                                    }
                                }
                            } else {
                                commandSender.sendMessage(ChatColor.RED + "Provide a player's name, otherwise the command must be executed by a player.");
                                return true;
                            }
                        case 6: //if all args are passed
                            try {
                                p = Bukkit.getPlayer(args[1]);
                                int count = Integer.parseInt(args[2]);
                                int level = Integer.parseInt(args[3]);
                                int exp = Integer.parseInt(args[4]);
                                int lapis = Integer.parseInt(args[5]);
                                if (p != null) { //if the player name passed is valid
                                    if (attemptGive(p, createStack(count, level, exp, lapis))) {
                                        commandSender.sendMessage(ChatColor.GRAY + "Gave 1x " + item_display_name + ChatColor.GRAY + " to " + ChatColor.GRAY + p.getName() + ChatColor.GRAY + ".");
                                        return true;
                                    } else {
                                        commandSender.sendMessage(ChatColor.GRAY + p.getName() + ChatColor.GRAY + "'s inventory was full, so the item was dropped at their location.");
                                        return true;
                                    }
                                } else {
                                    commandSender.sendMessage(ChatColor.RED + "Player not found!");
                                    return true;
                                }
                            } catch(NumberFormatException e) {
                                commandSender.sendMessage(ChatColor.RED + "Invalid arguments!");
                                return true;
                            }
                        default:
                            commandSender.sendMessage(ChatColor.RED + "Too many or too few arguments provided!");
                            return true;
                    }
                case "help":
                    if (args.length > 1) {
                        commandSender.sendMessage(ChatColor.RED + "Too many arguments provided!");
                        return true;
                    } else {
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/convenienchant&f: Show plugin info."));
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/convenienchant help&f: Show command usages."));
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/convenienchant give [player] [<count> <level> <exp> <lapis>]&f: Give yourself or another player a ConveniEnchant table."));
                        commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7/convenienchant reload&f: Reload the plugin configuration."));
                        return true;
                    }
                default:
                    commandSender.sendMessage(ChatColor.RED + "Unknown subcommand!");
                    return true;
            }
            return false;
        } else {
            commandSender.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        if (command.getName().equalsIgnoreCase("convenienchant")) {
            List<String> emptyList = Arrays.asList("");
            switch (args.length) {
                case 1:
                    return Arrays.asList("help", "give", "reload");
                case 2:
                    switch (args[0].toLowerCase()) {
                        case "help":
                            return emptyList;
                        case "reload":
                            return emptyList;
                        case "give":
                            return null;
                    }
                default:
                    return emptyList;
            }
        }
        return null;
    }
}