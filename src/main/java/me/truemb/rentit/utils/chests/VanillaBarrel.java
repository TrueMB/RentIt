package me.truemb.rentit.utils.chests;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VanillaBarrel extends SupportedChest {
    private final Inventory inventory;

    public VanillaBarrel(Inventory inventory) {
        this.inventory = inventory;
    }

    public static Optional<SupportedChest> getChestInLocation(Location location) {
        Block b = location.getBlock();
        if (b != null && b.getType() == Material.BARREL) {
        	Barrel chest = (Barrel) b.getState();
            Inventory chestInv = chest.getInventory();

            return Optional.of(new VanillaBarrel(chestInv));
        }

        return Optional.empty();
    }

    @Override
    public void add(ItemStack item) {
        this.inventory.addItem(item);
    }

    @Override
    public void add(ItemStack item, int amount) {
        int remaining = amount;

        for (ItemStack i : this.getAllSimilarItems(item)) {
            int available = i.getMaxStackSize() - i.getAmount();
            i.setAmount(available > remaining ? i.getAmount() + remaining : i.getMaxStackSize());
            remaining -= available;

            if (remaining <= 0) {
                return;
            }
        }
    }

    @Override
    public List<ItemStack> getAllItems() {
        return Arrays.stream(this.inventory.getContents()).collect(Collectors.toList());
    }
}