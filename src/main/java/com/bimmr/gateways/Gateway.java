package com.bimmr.gateways;

import me.bimmr.bimmcore.misc.Coords;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;


public class Gateway {

    private static Material TOP = Material.SMOOTH_STONE_SLAB;
    private static Material WALL = Material.COBBLESTONE_WALL;

    private Location location;
    private Location destination;
    private Player player;
    private List<Location> blockList = new ArrayList<>();

    private BukkitTask task;
    private ItemStack itemStack;

    public Gateway(Location location, Player player) {
        itemStack = player.getInventory().getItemInMainHand();
        destination = getDestination(itemStack).asLocation();

        this.location = location;
        this.player = player;

        Gateways.addGateway(this);
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        //Create Gateway
        boolean useX = xAxis(player);
        for (int x = -1; x != 2; x++) {
            for (int y = 0; y != 3; y++) {
                blockList.add(location.getWorld().getBlockAt(location.getBlockX() + (useX ? x : 0), location.getBlockY() + y, location.getBlockZ() + (!useX ? x : 0)).getLocation());
                if (x != 0 && y != 2)
                    location.getWorld().getBlockAt(location.getBlockX() + (useX ? x : 0), location.getBlockY() + y, location.getBlockZ() + (!useX ? x : 0)).setType(WALL);
                else if (y == 2)
                    location.getWorld().getBlockAt(location.getBlockX() + (useX ? x : 0), location.getBlockY() + y, location.getBlockZ() + (!useX ? x : 0)).setType(TOP);
            }
        }
        //While portal open, run effect - close portal after 10 seconds
        task = new BukkitRunnable() {
            int timeOpen = 0;

            @Override
            public void run() {
                if (timeOpen == 10) {
                    this.cancel();
                    close();
                } else {
                    timeOpen++;
                    location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location.clone().add(0.5, .5, .5), 25);
                    location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location.clone().add(0.5, 1.5, .5), 25);
                }
            }

        }.runTaskTimer(Gateways.getInstance(), 0L, 10L);
    }

    /**
     * Cehck if player is facing direction to use X Axis with Gateway
     *
     * @param player
     * @return
     */
    static boolean xAxis(Player player) {
        int degrees = (Math.round(player.getLocation().getYaw()) + 270) % 360;
        return ((degrees > 67 && degrees <= 112) || (degrees > 247 && degrees <= 292));
    }

    /**
     * Get Destination from item's lore
     *
     * @param itemStack
     * @return
     */
    static Coords getDestination(ItemStack itemStack) {
        if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasLore()) {
            String loreLine = itemStack.getItemMeta().getLore().get(getSelectedLine(itemStack.getItemMeta().getLore()));
            String loc = loreLine.substring(6, loreLine.length() - 4);
            return new Coords(loc);
        }
        return null;
    }

    /**
     * Get selected line by looking for the tags
     *
     * @param lore
     * @param next
     * @return
     */
    static List<String> setSelectedLine(List<String> lore, int next) {
        int selected = getSelectedLine(lore);

        if (selected != next) {
            if (selected >= 0) {
                lore.add(selected + 1, lore.get(selected).substring(4, lore.get(selected).length() - 4));
                lore.remove(selected);
            }
            lore.add(next + 1, ChatColor.AQUA + "[ " + lore.get(next) + ChatColor.AQUA + " ]");
            lore.remove(next);
        }
        return lore;
    }

    /**
     * Get the index of the selected line by looking for the tags
     *
     * @param lore
     * @return
     */
    static int getSelectedLine(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            String loreLine = lore.get(i);
            if (loreLine.startsWith(ChatColor.AQUA + "[ ") && loreLine.endsWith(ChatColor.AQUA + " ]"))
                return i;
        }
        return -1;
    }

    /**
     * Get Itemstack
     *
     * @return
     */
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Get Player
     *
     * @return
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get Location
     *
     * @return
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get List of blocks
     *
     * @return
     */
    public List<Location> getBlockList() {
        return blockList;
    }

    /**
     * Get destination
     *
     * @return
     */
    public Location getDestination() {
        return destination;
    }

    /**
     * Close Gateway
     */
    public void close() {
        if (!task.isCancelled())
            task.cancel();

        player.getInventory().addItem(itemStack);

        for (Location location : blockList)
            location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ()).setType(Material.AIR);

        Gateways.removeGateway(this);

        location = null;
        player = null;
        blockList = null;
        task = null;
        itemStack = null;
    }
}
