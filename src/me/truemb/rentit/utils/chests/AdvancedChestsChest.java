package me.truemb.rentit.utils.chests;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;

import java.util.List;
import java.util.Optional;

public class AdvancedChestsChest extends SupportedChest {
    private final AdvancedChest chest;

    public AdvancedChestsChest(AdvancedChest chest) {
        this.chest = chest;
    }

    public static Optional<SupportedChest> getChestInLocation(Location location) {
        AdvancedChest chest = AdvancedChestsAPI.getChestManager().getAdvancedChest(location);

        if (chest != null) {
            return Optional.of(new AdvancedChestsChest(chest));
        }

        return Optional.empty();
    }

    @Override
    public void add(ItemStack item) {
        AdvancedChestsAPI.addItemToChest(this.chest, item);
    }

    @Override
    public void add(ItemStack item, int amount) {
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        this.add(clone);
    }

    @Override
    public boolean hasEmptySlots() {
        return this.chest.getSlotsLeft() > 0;
    }

    @Override
    protected List<ItemStack> getAllItems() {
        return this.chest.getAllItems();
    }
}
