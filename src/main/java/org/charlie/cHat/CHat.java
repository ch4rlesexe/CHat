package org.charlie.cHat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class CHat extends JavaPlugin implements Listener, TabExecutor {

    private final NamespacedKey wearableKey = new NamespacedKey(this, "wearable");
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("makewearable").setExecutor(this);
        getCommand("chat").setExecutor(this);
        createMessagesConfig();
    }

    private void createMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messagesConfig.getString(key));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("makewearable")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(getMessage("not-a-player"));
                return true;
            }

            Player player = (Player) sender;

            if (!player.hasPermission("chat.makewearable")) {
                player.sendMessage(getMessage("no-permission"));
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() == Material.AIR) {
                player.sendMessage(getMessage("no-item-in-hand"));
                return true;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(wearableKey, PersistentDataType.BYTE, (byte) 1);

                List<String> lore = new ArrayList<>();
                lore.add(getMessage("wearable-item-lore"));
                lore.add(getMessage("right-click-lore"));
                meta.setLore(lore);

                item.setItemMeta(meta);
                player.sendMessage(getMessage("item-made-wearable"));
            }

            return true;
        } else if (command.getName().equalsIgnoreCase("chat")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("chat.reload")) {
                    reloadConfig();
                    messagesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
                    sender.sendMessage(getMessage("reload-success"));
                } else {
                    sender.sendMessage(getMessage("no-permission"));
                }
                return true;
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(wearableKey, PersistentDataType.BYTE)) {
            ItemStack currentHelmet = player.getInventory().getHelmet();

            player.getInventory().setHelmet(itemInHand);
            player.getInventory().setItemInMainHand(currentHelmet);

            player.sendMessage(getMessage("item-worn"));
            event.setCancelled(true);
        }
    }
}
