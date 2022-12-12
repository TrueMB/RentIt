package me.truemb.rentit.utils.chests;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public abstract class SupportedChest {

    /**
     * Adds the item to the chest
     */
    public abstract void add(ItemStack item);

    /**
     * Adds the requested amount to any similar item in the chest
     */
    public abstract void add(ItemStack item, int amount);

    public abstract List<ItemStack> getAllItems();

    /**
     * Counts how many of a given item there is in the chest
     */
    public int count(ItemStack item) {
        return this.getAllSimilarItems(item).stream().mapToInt(ItemStack::getAmount).sum();
    }

    /**
     * Counts how many available space for a given item there is by completing the stacks in the chest
     */
    public int countAvailable(ItemStack item) {
        return this.getAllSimilarItems(item).stream().mapToInt(i -> i.getMaxStackSize() - i.getAmount()).sum();
    }

    /**
     * Checks if there are any empty spaces in the items collection
     */
    public boolean hasEmptySlots() {
        return getAllItems().stream().anyMatch(this::isEmptySlot);
    }

    /**
     * Returns a subset of the chest's items which are similar to the given item.
     */
    public List<ItemStack> getAllSimilarItems(ItemStack item) {
        return this.getAllItems().stream().filter(i -> item.isSimilar(i)).collect(Collectors.toList());
    }

    /**
     * Safely removes the chest from the area
     */
    public void remove() {}

    protected boolean isEmptySlot(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }
}