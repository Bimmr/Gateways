package com.bimmr.gateways;

import me.bimmr.bimmcore.gui.book.Book;
import me.bimmr.bimmcore.items.Items;
import me.bimmr.bimmcore.messages.fancymessage.FancyClickEvent;
import me.bimmr.bimmcore.messages.fancymessage.FancyMessage;
import me.bimmr.bimmcore.misc.Coords;
import me.bimmr.bimmcore.scoreboard.Board;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;

import java.util.ArrayList;
import java.util.List;

public final class Gateways extends JavaPlugin implements Listener {

    private static List<Gateway> gateways;
    private static Plugin instance;
    private ItemStack gateWayItemStack = new ItemStack(Material.HEART_OF_THE_SEA);
    private NamespacedKey gateWayItemNamespace = new NamespacedKey(this, "gateway_item");

    public static List<Gateway> getGateways() {
        return gateways;
    }

    /**
     * Add a gateway
     *
     * @param gateway
     */
    public static void addGateway(Gateway gateway) {
        gateways.add(gateway);
    }

    /**
     * Remove a gateway
     *
     * @param gateway
     */
    public static void removeGateway(Gateway gateway) {
        gateways.remove(gateway);
    }

    /**
     * Get instance of Plugin
     *
     * @return
     */
    public static Plugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        //Init
        instance = this;
        gateways = new ArrayList<>();

        //Create Gateway item
        gateWayItemStack = new Items(gateWayItemStack)
                .setDisplayName(ChatColor.DARK_AQUA + "Gateway Portal")
                .getItem();

        //Add recipe
        Bukkit.getServer().addRecipe(
                new ShapedRecipe(gateWayItemNamespace, gateWayItemStack)
                        .shape("SSS", "WEW", "W W")
                        .setIngredient('S', Material.SMOOTH_STONE_SLAB)
                        .setIngredient('W', Material.COBBLESTONE_WALL)
                        .setIngredient('E', Material.END_CRYSTAL)
        );
        //Create timer to check if entities are in gateway
        new BukkitRunnable() {
            public void run() {
                for (Gateway gateway : gateways)
                    //Get all entities in the chunk
                    for (Entity e : gateway.getLocation().getChunk().getEntities()) {

                        //Check if they're the same location
                        Location gatewayLocation = gateway.getLocation();
                        Location entityLocation = e.getLocation();

                        //Teleport entity if location matrches
                        if (compareLocations(gatewayLocation, entityLocation)) {
                            e.teleport(gateway.getDestination().clone().add(0.5, 0.3, 0.5));
                        }
                    }
            }

        }.runTaskTimer(this, 0L, 5L);

        //Register events
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

        //Close all currently open gateways
        Gateway[] tempGateways = gateways.toArray(new Gateway[gateways.size()]);
        for (Gateway gateway : tempGateways)
            gateway.close();
    }

    /**
     * Prevent breaking open gateway
     *
     * @param bbe
     */
    @EventHandler
    public void breakEvent(BlockBreakEvent bbe) {
        for (Gateway gateway : gateways)
            if (gateway.getBlockList().contains(bbe.getBlock().getLocation()))
                bbe.setCancelled(true);
    }

    /**
     * Close Gateway if opening player disconnects
     *
     * @param pqe
     */
    @EventHandler
    public void leaveEvent(PlayerQuitEvent pqe) {
        Gateway[] tempGateways = gateways.toArray(new Gateway[gateways.size()]);
        for (Gateway gateway : tempGateways)
            if (gateway.getPlayer().getDisplayName().equals(pqe.getPlayer().getDisplayName()))
                gateway.close();

    }

    /**
     * Click Event handler
     *
     * @param pie
     */
    @EventHandler
    public void clickEvent(PlayerInteractEvent pie) {

        //Check if item is in hand
        if (pie.getItem() != null && (pie.getAction() == Action.RIGHT_CLICK_AIR || pie.getAction() == Action.RIGHT_CLICK_BLOCK))
            if (pie.getItem().getItemMeta() != null)
                if (pie.getItem().getItemMeta().getDisplayName().equals(ChatColor.DARK_AQUA + "Gateway Portal")) {

                    Player player = pie.getPlayer();
                    ItemStack item = pie.getItem();
                    ItemMeta itemMeta = item.getItemMeta();

                    //If Right clicking air - open modification book
                    if (pie.getAction() == Action.RIGHT_CLICK_AIR) {

                        //Create book with locations
                        Book book = new Book();
                        book.addLine("" + ChatColor.DARK_AQUA + ChatColor.BOLD + centerLine(17, "Gateway Portals"));
                        book.addBlankLine();
                        if (itemMeta.hasLore()) {

                            //If only one location, don't allow for deleting
                            if (itemMeta.getLore().size() == 1)
                                book.addLine(" " + ChatColor.BLACK + itemMeta.getLore().get(0).substring(4, itemMeta.getLore().get(0).length() - 4));

                                //Add all locations
                            else
                                for (int i = 0; i < itemMeta.getLore().size(); i++) {
                                    final int index = i;

                                    //Remove the selected tags
                                    String lore = itemMeta.getLore().get(i);
                                    if (lore.startsWith(ChatColor.AQUA + "[ ") && lore.endsWith(ChatColor.AQUA + " ]"))
                                        lore = lore.substring(4, lore.length() - 4);

                                    //Add the location
                                    book.addLine(new FancyMessage(ChatColor.DARK_RED + "[X]").tooltip("Delete Location").onClick(new FancyClickEvent() {

                                        //On click delete location
                                        @Override
                                        public void onClick() {
                                            List<String> lore = itemMeta.getLore();
                                            lore.remove(index);
                                            lore = Gateway.setSelectedLine(lore, 0);
                                            itemMeta.setLore(lore);
                                            item.setItemMeta(itemMeta);
                                            pie.getPlayer().getInventory().setItemInMainHand(item);
                                            pie.getPlayer().updateInventory();

                                            //Re-open book after click
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    Bukkit.getPluginManager().callEvent(new PlayerInteractEvent(player, pie.getAction(), item, pie.getClickedBlock(), pie.getBlockFace()));
                                                }
                                            }.runTaskLater(Gateways.getInstance(), 1L);
                                        }
                                    }).then(" " + ChatColor.BLACK + lore));
                                }
                        }
                        //Add "Add Location" to footer of book
                        book.goToFooter(2);
                        book.addLine(new FancyMessage(ChatColor.GREEN + "[+]").onClick(new FancyClickEvent() {

                            //Add new location when clicked
                            @Override
                            public void onClick() {
                                List<String> lore = itemMeta.getLore();
                                if (lore == null) {
                                    lore = new ArrayList<>();
                                }
                                if (lore.isEmpty())
                                    lore.add(ChatColor.AQUA + "[ " + ChatColor.DARK_GRAY + new Coords(player.getLocation()).toStringIgnoreYawAndPitch() + ChatColor.AQUA + " ]");
                                else
                                    lore.add(ChatColor.DARK_GRAY + new Coords(player.getLocation()).toStringIgnoreYawAndPitch());

                                itemMeta.setLore(lore);
                                item.setItemMeta(itemMeta);
                                pie.getPlayer().getInventory().setItemInMainHand(item);
                                pie.getPlayer().updateInventory();

                                //Re-open book after click
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Bukkit.getPluginManager().callEvent(new PlayerInteractEvent(player, pie.getAction(), item, pie.getClickedBlock(), pie.getBlockFace()));

                                    }
                                }.runTaskLater(Gateways.getInstance(), 1L);
                            }
                        }).then(ChatColor.BLACK + " Add Location"));
                        book.openFor(pie.getPlayer());

                    }
                    //Try opening Gateway
                    else if (pie.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        Location location = pie.getClickedBlock().getLocation().add(0, 1, 0);
                        if (canOpen(location, player) && Gateway.getDestination(player.getInventory().getItemInMainHand()) != null) {
                            new Gateway(location, player);
                        } else
                            player.sendMessage(ChatColor.DARK_RED + "Unable to open portal here");
                    }
                }
    }

    /**
     * Scroll locations on Gateway
     *
     * @param pihe
     */
    @EventHandler
    public void mouseWheelScroll(PlayerItemHeldEvent pihe) {
        ItemStack prevItem = pihe.getPlayer().getInventory().getItem(pihe.getPreviousSlot());
        ItemStack newItem = pihe.getPlayer().getInventory().getItem(pihe.getNewSlot());

        //If scrolling onto item, show scoreboard location
        if (newItem != null && newItem.hasItemMeta() && newItem.getItemMeta().hasDisplayName() && newItem.getItemMeta().getDisplayName().equals(ChatColor.DARK_AQUA + "Gateway Portal")) {
            Board board = new Board("GP-" + pihe.getPlayer().getDisplayName());
            board.setDisplayName(ChatColor.DARK_AQUA + "Gateway Portals");
            ItemMeta itemMeta = newItem.getItemMeta();
            if (itemMeta.hasLore()) {
                List<String> lore = newItem.getItemMeta().getLore();
                for (String loreLine : lore)
                    board.add(loreLine);
            }
            board.send(pihe.getPlayer());
        }

        //If on item and trying to shift scroll off, change selected location
        if (prevItem != null && prevItem.hasItemMeta() && prevItem.getItemMeta().hasDisplayName() && prevItem.getItemMeta().getDisplayName().equals(ChatColor.DARK_AQUA + "Gateway Portal") && Gateway.getDestination(prevItem) != null) {
            if (pihe.getPlayer().isSneaking()) {
                ItemMeta itemMeta = prevItem.getItemMeta();
                List<String> lore = prevItem.getItemMeta().getLore();

                //Determine the direction of the scroll
                int dir = 0;
                if (pihe.getPreviousSlot() == 0 && pihe.getNewSlot() == 8)
                    dir = -1;
                else if (pihe.getPreviousSlot() == 8 && pihe.getNewSlot() == 0)
                    dir = 1;
                else
                    dir = pihe.getPreviousSlot() < pihe.getNewSlot() ? 1 : -1;

                //Get the currently selected line
                int selected = Gateway.getSelectedLine(lore);
                if (selected == -1)
                    lore = Gateway.setSelectedLine(lore, 0);

                //Get the line being scrolled to
                int next = 0;
                if (selected == 0 && dir == -1)
                    next = lore.size() - 1;
                else if (selected == lore.size() - 1 && dir == 1) {
                    next = 0;
                } else
                    next = selected + dir;

                //Update the lore with the new selected line
                lore = Gateway.setSelectedLine(lore, next);

                itemMeta.setLore(lore);
                prevItem.setItemMeta(itemMeta);
                pihe.getPlayer().getInventory().setHeldItemSlot(pihe.getPreviousSlot());

            } else {
                //If scrolling off gateway item and not shifting, remove scoreboard
                if (pihe.getPlayer().getScoreboard().getObjective(DisplaySlot.SIDEBAR) != null && pihe.getPlayer().getScoreboard().getObjective(DisplaySlot.SIDEBAR).getName().contains("GP-" + pihe.getPlayer().getDisplayName()))
                    pihe.getPlayer().getScoreboard().getObjective(DisplaySlot.SIDEBAR).unregister();
            }
        }
    }

    /**
     * Compare 2 locations
     *
     * @param loc1
     * @param loc2
     * @return
     */
    private boolean compareLocations(Location loc1, Location loc2) {
        return loc1.getWorld() == loc2.getWorld() && loc1.getBlockX() == loc2.getBlockX() && loc1.getBlockY() == loc2.getBlockY() && loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Check if the location can have a gateway opened
     *
     * @param location
     * @param player
     * @return
     */
    private boolean canOpen(Location location, Player player) {
        boolean useX = Gateway.xAxis(player);
        for (int x = -1; x != 2; x++)
            for (int y = 0; y != 3; y++)
                if (!location.getWorld().getBlockAt(location.getBlockX() + (useX ? x : 0), location.getBlockY() + y, location.getBlockZ() + (!useX ? x : 0)).getType().equals(Material.AIR))
                    return false;
        return true;
    }

    /**
     * Util to center a line on a book
     *
     * @param size
     * @param line
     * @return
     */
    private String centerLine(int size, String line) {
        int le = (size - line.length()) / 2;

        String newLine = "";
        for (int i = 0; i < le; i++) {
            newLine += " ";
        }
        newLine += line;
        for (int i = 0; i < le; i++) {
            newLine += " ";
        }
        return newLine;
    }
}
