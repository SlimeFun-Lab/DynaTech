package me.profelements.dynatech;

import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.BlobBuildUpdater;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.profelements.dynatech.items.backpacks.PicnicBasket;
import me.profelements.dynatech.items.misc.DimensionalHomeDimension;
import me.profelements.dynatech.items.tools.ElectricalStimulator;
import me.profelements.dynatech.listeners.BlockBreakBlockListener;
import me.profelements.dynatech.listeners.CoalCokeListener;
import me.profelements.dynatech.listeners.ElectricalStimulatorListener;
import me.profelements.dynatech.listeners.ExoticGardenIntegrationListener;
import me.profelements.dynatech.listeners.GastronomiconIntegrationListener;
import me.profelements.dynatech.listeners.InventoryFilterListener;
import me.profelements.dynatech.listeners.PicnicBasketListener;
import me.profelements.dynatech.listeners.RegistryListeners;
import me.profelements.dynatech.listeners.UpgradesListener;
import me.profelements.dynatech.registries.ItemGroups;
import me.profelements.dynatech.registries.Items;
import me.profelements.dynatech.registries.RecipeTypes;
import me.profelements.dynatech.registries.Recipes;
import me.profelements.dynatech.registries.Registries;
import me.profelements.dynatech.setup.DynaTechItemsSetup;
import me.profelements.dynatech.tasks.ItemBandTask;
import me.profelements.dynatech.utils.Liquid;
import me.profelements.dynatech.utils.LiquidRegistry;
import me.profelements.dynatech.utils.RecipeRegistry;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

public class DynaTech extends JavaPlugin implements SlimefunAddon {

    private static DynaTech instance;
    private static boolean exoticGardenInstalled;
    private static boolean infinityExpansionInstalled;
    private static RecipeRegistry rRegistry;
    private static LiquidRegistry lRegistry;

    private int tickInterval;

    @Override
    public void onEnable() {
        setInstance(this);
        rRegistry = RecipeRegistry.init();
        lRegistry = LiquidRegistry.init();
        setExoticGardenInstalled(Bukkit.getPluginManager().isPluginEnabled("ExoticGarden"));
        setInfinityExpansionInstalled(Bukkit.getPluginManager().isPluginEnabled("InfinityExpansion"));

        final int TICK_TIME = Slimefun.getTickerTask().getTickRate();

        saveDefaultConfig();

        new Metrics(this, 9689);

        if (!getConfig().getBoolean("options.disable-dimensionalhome-world")) {
            WorldCreator worldCreator = new WorldCreator("dimensionalhome");
            worldCreator.generator(new DimensionalHomeDimension());
            worldCreator.createWorld();
        }
        DynaTechLiquids.registerLiquids(DynaTech.getLiquidRegistry());

        DynaTechItemsSetup.setup(this);
        new PicnicBasketListener(this, (PicnicBasket) Items.PICNIC_BASKET.stack().getItem());
        new ElectricalStimulatorListener(this, (ElectricalStimulator) Items.ELECTRICAL_STIMULATOR.stack().getItem());
        new InventoryFilterListener(this);
        new UpgradesListener(this);
        new CoalCokeListener(this);
        new BlockBreakBlockListener(this);
        new RegistryListeners(this);
        try {
            Class.forName("io.github.schntgaispock.gastronomicon.api.items.FoodItemStack");
            new GastronomiconIntegrationListener(this);
        } catch (ClassNotFoundException ex) {

        }

        try {
            Class.forName("io.github.thebusybiscuit.exoticgarden.items.CustomFood");
            new ExoticGardenIntegrationListener(this);
        } catch (ClassNotFoundException ex) {
        }

        // Tasks
        getServer().getScheduler().runTaskTimerAsynchronously(DynaTech.getInstance(), new ItemBandTask(), 0L, 5 * 20L);
        getServer().getScheduler().runTaskTimer(DynaTech.getInstance(), () -> this.tickInterval++, 0, TICK_TIME);

        if (getConfig().getBoolean("options.auto-update", true) && getDescription().getVersion().startsWith("Main")) {
            new BlobBuildUpdater(this, getFile(), "DynaTech", "Main").start();
        }

        if (!Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_19)) {
            getLogger().warning("DynaTech only support 1.19+, disabling.");
            getServer().getPluginManager().disablePlugin(this);
        }

        setupRegistries();
    }

    private static void setupRegistries() {
        ItemGroups.init(Registries.ITEM_GROUPS);
        RecipeTypes.init(Registries.RECIPE_TYPES);
        Recipes.init(Registries.RECIPES);
        Registries.ITEMS.freeze();
        Registries.ITEM_GROUPS.freeze();
        Registries.RECIPE_TYPES.freeze();
        Registries.RECIPES.freeze();

    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        setInstance(null);
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/ProfElements/DynaTech/issues";
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nonnull
    public static DynaTech getInstance() {
        return instance;
    }

    @Nonnull
    public static RecipeRegistry getRecipeRegistry() {
        return RecipeRegistry.getInstance();
    }

    @Nonnull
    public static LiquidRegistry getLiquidRegistry() {
        return LiquidRegistry.getInstance();
    }

    public int getTickInterval() {
        return tickInterval;
    }

    public static boolean isExoticGardenInstalled() {
        return exoticGardenInstalled;
    }

    public static boolean isInfinityExpansionInstalled() {
        return infinityExpansionInstalled;
    }

    public static void setInstance(DynaTech inst) {
        instance = inst;
    }

    public static void setExoticGardenInstalled(boolean isExoticGardenInstalled) {
        exoticGardenInstalled = isExoticGardenInstalled;
    }

    public static void setInfinityExpansionInstalled(boolean isInfinityExpansionInstalled) {
        infinityExpansionInstalled = isInfinityExpansionInstalled;
    }

    @Nullable
    public static BukkitTask runSync(@Nonnull Runnable runnable) {
        Preconditions.checkNotNull(runnable, "Cannot run null");

        if (instance == null || !instance.isEnabled()) {
            return null;
        }

        return instance.getServer().getScheduler().runTask(getInstance(), runnable);
    }

}
