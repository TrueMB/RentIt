package me.truemb.rentit.api;

import com.sk89q.worldedit.math.BlockVector3;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.ChestBuilder;
import us.lynuxcraft.deadsilenceiv.advancedchests.utils.LocationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancedChestsUtils {
    private final Main instance;

    private final boolean isEnabled;

    public AdvancedChestsUtils(Main plugin) {
        this.instance = plugin;
        this.isEnabled = this.instance.manageFile().getBoolean("Options.useAdvancedChests")
                && Bukkit.getServer().getPluginManager().isPluginEnabled("AdvancedChests");
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public Map<String, List<String>> getChestsInArea(BlockVector3 min, BlockVector3 max, World world) {
        Map<String, List<String>> chests = new HashMap<>();

        if (!this.isEnabled) {
            return chests;
        }

        for(int x = min.getBlockX(); x <= max.getBlockX(); x++)
            for(int y = min.getBlockY(); y <= max.getBlockY(); y++)
                for(int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location location = new Location(world, x, y, z);
                    AdvancedChest chest = AdvancedChestsAPI.getChestManager().getAdvancedChest(location);

                    if (chest != null) {
                        /*
                         * If there is an advanced chest in the given location, we recover the location
                         * directional, so we can decorate our serialized data with the chest facing.
                         */
                        Directional d = (Directional) location.getBlock().getBlockData();
                        String loc = LocationUtils.serializeLoc(location) + ":@f;" + d.getFacing().name();

                        String type = chest.getType();
                        if (!chests.containsKey(type)) {
                            chests.put(type, new ArrayList<>());
                        }

                        chests.get(type).add(loc);
                    }
                }

        return chests;
    }

    public void pasteChestsInArea(RentTypes type, int id) {
        if (!this.isEnabled) {
            return;
        }

        this.instance.getAreaFileManager().getAdvancedChests(type, id)
                .forEach((chest, locations) -> {
                    int size = AdvancedChestsAPI.getDataManager().getChestSize(chest);

                    locations.forEach(location -> {
                        /*
                         * Split the serialized location to recover the facing data
                         */
                        String[] loc = location.split(":@f;");
                        AdvancedChest adv = new ChestBuilder(size, chest, loc[0]).build();
                        Directional d = (Directional) adv.getLocation().getBlock().getBlockData();
                        d.setFacing(BlockFace.valueOf(loc[1]));
                        adv.getLocation().getBlock().setBlockData(d);
                    });
                });
    }
}
