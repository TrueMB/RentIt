package me.truemb.rentit.api;

import com.sk89q.worldedit.math.BlockVector3;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.ChestType;
import us.lynuxcraft.deadsilenceiv.advancedchests.utils.LocationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
                    AdvancedChest<?, ?> chest = AdvancedChestsAPI.getChestManager().getAdvancedChest(location);

                    if (chest != null) {
                        String type = chest.getConfigType();
                        if (!chests.containsKey(type)) {
                            chests.put(type, new ArrayList<>());
                        }

                        chests.get(type).add(LocationUtils.serializeLoc(location));
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
                    ChestType chestType = AdvancedChestsAPI.getDataManager().getChestType(chest);
                    int size = AdvancedChestsAPI.getDataManager().getChestSize(chest);

                    locations.forEach(location -> {
                        chestType.newBuilder(size, chest, location, new HashSet<>()).build();
                    });
                });
    }
}
