package it.moro.smartShulkerInfo;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public final class SmartShulkerInfo extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;
    File fileConfig;

    @Override
    public void onEnable() {
        fileConfig = new File(getDataFolder(), "config.yml");
        createDataFolder();
        loadFiles();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Plugin Enabled!");
    }

    public void onDisable() {
        getLogger().info("Plugin Disabled!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("shulkerinfo")) {
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        if (player.hasPermission("shulkerinfo.reload")) {
                            loadFiles();
                            player.sendMessage("§aConfiguration reloaded!");
                            return true;
                        }
                    } else if (args[0].equalsIgnoreCase("killarmorstand")) {
                        List<Entity> entities = new ArrayList<>(Bukkit.getWorlds().getFirst().getEntities());
                        for (Entity entity : entities) {
                            if (entity instanceof ArmorStand armorStand) {
                                if (!armorStand.isVisible() && armorStand.isCustomNameVisible()) {
                                    armorStand.remove();
                                }
                            }
                        }
                        player.sendMessage("§aAll armorstands have been removed");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(command.getName().equalsIgnoreCase("shulkerinfo")) {
            Player player = (Player) sender;
            if (player.hasPermission("shulkerinfo.killarmorstand")) {
                if (args.length == 1) {
                    return Arrays.asList("reload", "killarmorstand");
                }
                return Collections.emptyList();
            }
        }
        return null;
    }


    private void createDataFolder() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("§cUnable to create plugin data folder! Check permissions.");
        }
    }

    private void loadFiles() {
        File configuration = new File(Objects.requireNonNull(Bukkit.getPluginManager()
                .getPlugin("SmartShulkerInfo")).getDataFolder(), "config.yml");
        if (!configuration.exists()) {
            saveResource("config.yml", false);
            getLogger().info("File config.yml created!");
            config = YamlConfiguration.loadConfiguration(fileConfig);
        } else {
            config = YamlConfiguration.loadConfiguration(fileConfig);
        }
    }

    @EventHandler
    public void onShulkerBoxClick(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("LEFT_CLICK") && event.getClickedBlock() != null) {
            Player player = event.getPlayer();
            if (config.getBoolean("requires-free-hand")) {
                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    Block block = event.getClickedBlock();
                    if (block.getType() == Material.SHULKER_BOX) {
                        Location shulker = block.getLocation();
                        Location location = new Location(block.getWorld(), shulker.getBlockX(), shulker.getBlockY() - 1, shulker.getBlockZ());
                        if (location.getBlock().getType() == Material.CRAFTING_TABLE) {
                            if (config.getBoolean("Hologram.enable")) {
                                showShulkerBoxItems(player, shulker);
                            }
                            if (config.getBoolean("Chat.enable")) {
                                chatShulkerBoxItems(player, shulker);
                            }
                        }
                    }
                }
            } else {
                Block block = event.getClickedBlock();
                if (block.getType() == Material.SHULKER_BOX) {
                    Location shulker = block.getLocation();
                    Location location = new Location(block.getWorld(), shulker.getBlockX(), shulker.getBlockY() - 1, shulker.getBlockZ());
                    if (location.getBlock().getType() == Material.CRAFTING_TABLE) {
                        if (config.getBoolean("Hologram.enable")) {
                            showShulkerBoxItems(player, shulker);
                        }
                        if (config.getBoolean("Chat.enable")) {
                            chatShulkerBoxItems(player, shulker);
                        }
                    }
                }
            }
        }
    }

    private void chatShulkerBoxItems(Player player, Location location) {
        ShulkerBox shulkerBox = (ShulkerBox) location.getBlock().getState();
        Inventory inventory = shulkerBox.getInventory();
        int items = config.getInt("Chat.amount-item");
        player.sendMessage(" \n \n ");
        if (!Objects.requireNonNull(config.getString("Chat.tab")).isEmpty()) {
            String tab = Objects.requireNonNull(config.getString("Chat.tab"))
                    .replaceAll("&", "§");
            player.sendMessage(tab);
        }
        int air = 0;
        for (int i = 0; i < items; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                String itemName = getTranslatedItemName(item);
                String text = Objects.requireNonNull(config.getString("Chat.line-text"))
                        .replaceAll("%n%", String.valueOf(i + 1))
                        .replaceAll("%item%", itemName)
                        .replaceAll("%amount%", String.valueOf(item.getAmount()))
                        .replaceAll("&", "§");
                player.sendMessage(text);
            } else {
                air++;
            }
        }
        if (air == items) {
            String empty = Objects.requireNonNull(config.getString("Chat.empty")).replaceAll("&", "§");
            player.sendMessage(empty);
        }
        if (!Objects.requireNonNull(config.getString("Chat.end-tab")).isEmpty()) {
            String endtab = Objects.requireNonNull(config.getString("Chat.end-tab"))
                    .replaceAll("&", "§");
            player.sendMessage(endtab);
        }
        player.sendMessage(" ");
    }

    private void showShulkerBoxItems(Player player, Location location) {
        ShulkerBox shulkerBox = (ShulkerBox) location.getBlock().getState();
        Inventory inventory = shulkerBox.getInventory();
        List<ArmorStand> armorStands = new ArrayList<>();
        int items = config.getInt("Hologram.amount-item");
        int air = 0;
        for (int i = items - 1; i >= 0; i--) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                ArmorStand armorStand = player.getWorld().spawn(location.clone()
                        .add(0.5, -1 + (items - i - 1) * 0.25 - (air * 0.25), 0.5), ArmorStand.class);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                String itemName = getTranslatedItemName(item);
                String text = Objects.requireNonNull(config.getString("Hologram.line-text"))
                        .replaceAll("%n%", String.valueOf(i + 1))
                        .replaceAll("%item%", itemName)
                        .replaceAll("%amount%", String.valueOf(item.getAmount()))
                        .replaceAll("&", "§");
                armorStand.customName(Component.text(text));
                armorStand.setCustomNameVisible(true);
                armorStands.add(armorStand);

            } else {
                air++;
            }
        }
        if (items == air) {
            ArmorStand armorStand = player.getWorld().spawn(location.clone()
                    .add(0.5, -1 + 1 * 0.25, 0.5), ArmorStand.class);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            String text = Objects.requireNonNull(config.getString("Hologram.empty"))
                    .replaceAll("&", "§");
            armorStand.customName(Component.text(text));
            armorStand.setCustomNameVisible(true);
            armorStands.add(armorStand);
        } else {
            if (!Objects.requireNonNull(config.getString("Hologram.tab")).isEmpty()) {
                ArmorStand armorStand = player.getWorld().spawn(location.clone()
                        .add(0.5, -1 + (items - air) * 0.25, 0.5), ArmorStand.class);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                String text = Objects.requireNonNull(config.getString("Hologram.tab"))
                        .replaceAll("&", "§");
                armorStand.customName(Component.text(text));
                armorStand.setCustomNameVisible(true);
                armorStands.add(armorStand);
            }
        }
        int time = config.getInt("Hologram.timeout");
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ArmorStand armorStand : armorStands) {
                    armorStand.remove();
                }
            }
        }.runTaskLater(this, time * 20L);
    }

    public String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public String getTranslatedItemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return capitalizeFirstLetter(String.valueOf(meta.displayName()));
        } else {
            String itemName = item.getType().toString().toLowerCase()
                    .replace("_", " ");
            return capitalizeFirstLetter(itemName);
        }
    }

}
