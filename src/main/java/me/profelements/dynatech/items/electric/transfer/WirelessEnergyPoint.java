package me.profelements.dynatech.items.electric.transfer;

import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.profelements.dynatech.DynaTech;
import me.profelements.dynatech.registries.Items;
import me.profelements.dynatech.utils.EnergyUtils;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WirelessEnergyPoint extends SlimefunItem implements EnergyNetProvider {

    private static final NamespacedKey WIRELESS_LOCATION_KEY = new NamespacedKey(DynaTech.getInstance(),
            "wireless-location");
    private final int capacity;
    private final int energyRate;

    @ParametersAreNonnullByDefault
    public WirelessEnergyPoint(ItemGroup itemGroup, int capacity, int energyRate, SlimefunItemStack item,
            RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        this.capacity = capacity;
        this.energyRate = energyRate;

        addItemHandler(onRightClick(), onBlockPlace(), onBlockBreak());
    }

    @Override
    public int getGeneratedOutput(Location l, Config data) {
        String wirelessBankLocation = BlockStorage.getLocationInfo(l, "wireless-location");

        int chargedNeeded = getCapacity() - getCharge(l);

        if (chargedNeeded != 0 && wirelessBankLocation != null) {
            Location wirelessEnergyBank = stringToLocation(wirelessBankLocation);

            // Note: You should probably also see if the Future from getChunkAtAsync is
            // finished here.
            // you don't really want to possibly trigger the chunk to load in another thread
            // twice.
            if (!wirelessEnergyBank.getWorld().isChunkLoaded(wirelessEnergyBank.getBlockX() >> 4,
                    wirelessEnergyBank.getBlockZ() >> 4)) {
                CompletableFuture<Chunk> chunkLoad = PaperLib.getChunkAtAsync(wirelessEnergyBank);
                if (!chunkLoad.isDone()) {
                    return 0;
                }
            }

            if (BlockStorage.checkID(wirelessEnergyBank) != null && BlockStorage.checkID(wirelessEnergyBank)
                    .equals(Items.WIRELESS_ENERGY_BANK.stack().getItemId())) {

                String energyCharge = BlockStorage.getLocationInfo(l, "energy-charge");
                if (energyCharge == null) {
                    BlockStorage.addBlockInfo(l, "energy-charge", String.valueOf(0));
                }

                EnergyUtils.moveEnergyFromTo(new BlockPosition(wirelessEnergyBank), new BlockPosition(l),
                        getEnergyRate(), getCapacity());
            }
            return 0;
        }
        return 0;
    }

    private ItemHandler onRightClick() {
        return new ItemUseHandler() {

            @Override
            public void onRightClick(PlayerRightClickEvent event) {

                Optional<Block> blockClicked = event.getClickedBlock();
                Optional<SlimefunItem> sfBlockClicked = event.getSlimefunBlock();
                if (blockClicked.isPresent() && sfBlockClicked.isPresent()) {
                    Location blockLoc = blockClicked.get().getLocation();
                    SlimefunItem sfBlock = sfBlockClicked.get();
                    ItemStack item = event.getItem();

                    if (sfBlock != null
                            && Slimefun.getProtectionManager().hasPermission(event.getPlayer(), blockLoc,
                                    Interaction.INTERACT_BLOCK)
                            && sfBlock.getId().equals(Items.WIRELESS_ENERGY_BANK.stack().getItemId())
                            && blockLoc != null) {
                        event.cancel();
                        ItemMeta im = item.getItemMeta();
                        String locationString = locationToString(blockLoc);

                        PersistentDataAPI.setString(im, WIRELESS_LOCATION_KEY, locationString);
                        item.setItemMeta(im);
                        setItemLore(item, blockLoc);
                    }
                }
            }
        };
    }

    private ItemHandler onBlockPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent event) {

                Location blockLoc = event.getBlockPlaced().getLocation();
                ItemStack item = event.getItemInHand();
                String locationString = PersistentDataAPI.getString(item.getItemMeta(), WIRELESS_LOCATION_KEY);

                if (item.getType() == Items.WIRELESS_ENERGY_POINT.stack().getType() && item.hasItemMeta()
                        && locationString != null) {
                    BlockStorage.addBlockInfo(blockLoc, "wireless-location", locationString);

                }
            }

        };
    }

    private ItemHandler onBlockBreak() {
        return new BlockBreakHandler(false, false) {

            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack block, List<ItemStack> drops) {
                BlockStorage.clearBlockInfo(event.getBlock().getLocation());
            }

        };
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public int getEnergyRate() {
        return energyRate;
    }

    private void setItemLore(ItemStack item, Location l) {
        ItemMeta im = item.getItemMeta();
        List<Component> lore = im.lore();
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(Component.text("Location: "))) {
                lore.remove(i);
            }
        }

        lore.add(Component.text(
                ChatColor.WHITE + "Location: " + l.getWorld().getName() + " " + l.getBlockX() + " " + l.getBlockY()
                        + " " + l.getBlockZ()));

        im.lore(lore);
        item.setItemMeta(im);

    }

    private String locationToString(Location l) {
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    private Location stringToLocation(String str) {
        String[] locComponents = str.split(":");
        return new Location(Bukkit.getWorld(locComponents[0]), Double.parseDouble(locComponents[1]),
                Double.parseDouble(locComponents[2]), Double.parseDouble(locComponents[3]));
    }

}
