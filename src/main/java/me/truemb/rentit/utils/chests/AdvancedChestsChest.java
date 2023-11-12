package me.truemb.rentit.utils.chests;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdvancedChestsChest extends SupportedChest {
    private final AdvancedChest<?, ?> chest;

    public AdvancedChestsChest(AdvancedChest<?, ?> chest) {
        this.chest = chest;
    }

    public static Optional<SupportedChest> getChestInLocation(Location location) {
        AdvancedChest<?, ?> chest = AdvancedChestsAPI.getChestManager().getAdvancedChest(location);

        if (chest != null) {
            return Optional.of(new AdvancedChestsChest(chest));
        }

        return Optional.empty();
    }

    @Override
    public void add(ItemStack item) {
        this.chest.getChestType().getDispenserService().dispenseItemToChest(this.chest, item);
    }

    @Override
    public void add(ItemStack item, int amount) {
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        this.add(clone);
    }

    @Override
    public List<ItemStack> getAllItems() {
        return Arrays.stream(this.chest.getOrderedPages())
                        .flatMap((chestPage) -> Arrays.stream(chestPage.getItems())
                                .filter(item -> item instanceof ItemStack)
                                .map(o -> (ItemStack) o))
                        .collect(Collectors.toList());
    }

    @Override
    public boolean hasEmptySlots() {
        return this.chest.getSlotsLeft() > 0;
    }

    @Override
    public void remove() {
        this.chest.remove(null, null, false);
    }
}
