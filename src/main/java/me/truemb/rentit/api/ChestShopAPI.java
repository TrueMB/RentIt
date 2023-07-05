package me.truemb.rentit.api;

import org.bukkit.block.Block;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;

public class ChestShopAPI {
	
	public boolean isShopChest(Block b) {
		return uBlock.couldBeShopContainer(b);
	}
	
	public boolean isShopChestSign(Block b) {
		return ChestShopSign.isValid(b);
	}

}
