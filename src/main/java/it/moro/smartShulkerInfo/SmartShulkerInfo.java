package it.moro.smartShulkerInfo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SmartShulkerInfo extends JavaPlugin implements Listener, CommandExecutor {

    FileConfiguration config;

    @Override
    public void onEnable() {
        createDataFolder();
        loadFiles();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);

    }

    private void createDataFolder() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("§cImpossibile creare la cartella dati del plugin! Controlla i permessi.");
        }
    }

    private void loadFiles() {
        File configuration = new File(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("SmartShulkerInfo")).getDataFolder(), "config.yml");
        if (!configuration.exists()) {
            saveResource("config.yml", false);
            getLogger().info("File config.yml creato!");
        }
    }

    @EventHandler
    public void onShulkerBoxClick(PlayerInteractEvent event) {
        // Verifica che il giocatore stia cliccando una shulker box
        if (event.getAction().toString().contains("LEFT_CLICK") && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.SHULKER_BOX) {
                Location shulker = block.getLocation();
                Location location = new Location(block.getWorld(), shulker.getBlockX(), shulker.getBlockY() - 1, shulker.getBlockZ());
                if (location.getBlock().getType() == Material.CRAFTING_TABLE) {
                    if (config.getBoolean("Hologram.enable")) {
                        Player player = event.getPlayer();
                        showShulkerBoxItems(player, shulker);
                    }
                    if (config.getBoolean("Chat.enable")) {
                        Player player = event.getPlayer();
                        chatShulkerBoxItems(player, shulker);
                    }
                }
            }
        }
    }

    private void chatShulkerBoxItems(Player player, Location location) {
        ShulkerBox shulkerBox = (ShulkerBox) location.getBlock().getState();
        Inventory inventory = shulkerBox.getInventory();
        List<ArmorStand> armorStands = new ArrayList<>();
        int items = config.getInt("Chat.amount-item");
        for (int i = 0; i < items; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                String text = Objects.requireNonNull(config.getString("Hologram.line-text"))
                        .replaceAll("%n%", String.valueOf(i + 1))
                        .replaceAll("%item%", item.getType().toString())
                        .replaceAll("%amount%", String.valueOf(item.getAmount()))
                        .replaceAll("&", "§");
                player.sendMessage(text);
            }
        }
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
                ArmorStand armorStand = player.getWorld().spawn(location.clone().add(0.5, -1 + (items - i - 1) * 0.25 - (air * 0.25), 0.5), ArmorStand.class);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                String itemName = getTranslatedItemName(item);
                String text = Objects.requireNonNull(config.getString("Hologram.line-text"))
                        .replaceAll("%n%", String.valueOf(i + 1))
                        .replaceAll("%item%", itemName)
                        .replaceAll("%amount%", String.valueOf(item.getAmount()))
                        .replaceAll("&", "§");
                armorStand.setCustomName(text);
                armorStand.setCustomNameVisible(true);
                armorStands.add(armorStand);

            } else {
                air++;
            }
        }
        if(items == air){
            ArmorStand armorStand = player.getWorld().spawn(location.clone().add(0.5, -1 + 1 * 0.25, 0.5), ArmorStand.class);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            String text = Objects.requireNonNull(config.getString("Hologram.empty"))
                    .replaceAll("&", "§");
            armorStand.setCustomName(text);
            armorStand.setCustomNameVisible(true);
            armorStands.add(armorStand);
        } else {
            if (!Objects.requireNonNull(config.getString("Hologram.tab")).isEmpty()) {
                ArmorStand armorStand = player.getWorld().spawn(location.clone().add(0.5, -1 + (items - air) * 0.25, 0.5), ArmorStand.class);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                String text = Objects.requireNonNull(config.getString("Hologram.tab"))
                        .replaceAll("&", "§");
                armorStand.setCustomName(text);
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
            return input; // Se l'input è vuoto o nullo, restituisci com'è
        }
        // Capitalizza la prima lettera e aggiungi il resto della stringa invariata
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public String getTranslatedItemName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "Nessun oggetto"; // Nome di fallback se non c'è alcun oggetto
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            // Ottieni il nome tradotto e capitalizza la prima lettera
            return capitalizeFirstLetter(meta.getDisplayName());
        } else {
            // Se non ha un displayName, restituire il nome dell'oggetto basato sul tipo
            String itemName = item.getType().toString().toLowerCase().replace("_", " ");
            return capitalizeFirstLetter(itemName);
        }
    }

}
