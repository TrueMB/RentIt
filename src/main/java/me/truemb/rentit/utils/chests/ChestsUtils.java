package me.truemb.rentit.utils.chests;

import com.sk89q.worldedit.math.BlockVector3;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChestsUtils {
    private Main instance;

    public ChestsUtils(Main plugin) {
        this.instance = plugin;
    }

    /**
     * Returns a list of wrappers manipulate any chest supported and enabled.
     * @param shopId The id of the shop to get all the chests from.
     */
    public List<SupportedChest> getShopChests(int shopId) {
        World world = this.instance.getAreaFileManager().getWorldFromArea(RentTypes.SHOP, shopId);
        BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(RentTypes.SHOP, shopId);
        BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(RentTypes.SHOP, shopId);

        List<SupportedChest> chests = new ArrayList<>();

        for (int minX = min.getBlockX(); minX <= max.getBlockX(); minX++) {
            for (int minZ = min.getBlockZ(); minZ <= max.getBlockZ(); minZ++) {
                for (int minY = min.getBlockY(); minY <= max.getBlockY(); minY++) {
                    Location location = new Location(world, minX, minY, minZ);

                    this.getChestInLocation(location).ifPresent(chests::add);
                }
            }
        }

        return chests;
    }

    // CHECK CHESTS FOR ITEM
    /**
     * For each of the supported chest types, checks if there's stock of a given item. If the amount is greater than 1,
     * this method could pull up part of the amount from one chest type and the other part from a different type.
     */
    public boolean checkChestsInArea(int shopId, ItemStack item) {
        List<SupportedChest> chests = this.getShopChests(shopId);
        int amount = item.getAmount();

        for (SupportedChest chest : chests) {
            amount -= chest.count(item);

            if (amount <= 0) {
                return true;
            }
        }

        return false;
    }

    // CHECK IF SPACES
    /**
     * For each of the supported chest types, checks if there is enough space in any of them
     * to fit the given item.
     */
    public boolean checkForSpaceInArea(int shopId, ItemStack item) {
        List<SupportedChest> chests = this.getShopChests(shopId);
        int amount = item.getAmount();

        for (SupportedChest chest : chests) {
            if (chest.hasEmptySlots()) {
                return true;
            }

            amount -= chest.countAvailable(item);

            if (amount <= 0) {
                return true;
            }
        }

        return false;
    }


    // REMOVES ITEMS FROM CHESTS
    /**
     * For each of the supported chest types, removes the given item. If the amount is greater than 1,
     * this method could remove part of the amount from one chest type and the other part from a different type.
     */
    public void removeItemFromChestsInArea(int shopId, ItemStack item) {
        List<SupportedChest> chests = this.getShopChests(shopId);
        int amount = item.getAmount();

        for (SupportedChest chest : chests) {
            for (ItemStack itemStack : chest.getAllSimilarItems(item)) {
                if (amount >= itemStack.getAmount()) {
                    amount -= itemStack.getAmount();
                    itemStack.setAmount(0);
                } else {
                    itemStack.setAmount(itemStack.getAmount() - amount);
                    return;
                }
            }
        }
    }

    // ADDS ITEMS FROM CHESTS
    /**
     * For each of the supported chest types, adds the given item. If the amount is greater than 1,
     * this method could add part of the amount to one chest type and the other part to a different type.
     */
    public void addItemToChestsInArea(int shopId, ItemStack item) {
        List<SupportedChest> chests = this.getShopChests(shopId);
        int amount = item.getAmount();

        for (SupportedChest chest : chests) {
            if (chest.hasEmptySlots()) {
                chest.add(item);
                return;
            }

            int space = chest.countAvailable(item);
            if (amount > space) {
                chest.add(item, amount - space);
                amount -= space;
            } else {
                chest.add(item, amount);
                return;
            }
        }
    }

    /**
     * Gets the supported chest for a given location, if any.
     */
    private Optional<SupportedChest> getChestInLocation(Location location) {
    	Block b = location.getBlock();
    	if(b == null)
    		return null;
    	
    	if(b.getType() == Material.BARREL) {
	        return VanillaBarrel.getChestInLocation(location);
    	}else {
	        if (this.instance.getAdvancedChestsUtils().isEnabled()) {
	            /*
	             * Advanced chests are implemented as a CHEST item.
	             * If we don't early cut the lookup, we end up loading 1 adv chest + 1 vanilla chest
	             * for the same location.
	             */
	            Optional<SupportedChest> adv = AdvancedChestsChest.getChestInLocation(location);
	            if (adv.isPresent()) {
	                return adv;
	            }
	        }
	        
	        return VanillaChest.getChestInLocation(location);
    	}
    }
}
