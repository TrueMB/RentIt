package me.truemb.rentit.api;

import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;

import me.truemb.rentit.main.Main;

public class ChestShopAPI {
	
	private Main instance;
	
	public ChestShopAPI(Main plugin) {
		this.instance = plugin;
		
		Plugin chestShop = plugin.getServer().getPluginManager().getPlugin("ChestShop");
		
		if(chestShop == null || !(chestShop instanceof ChestShop))
			return;

		plugin.getLogger().info("ChestShop was found!");
	}
	
	public boolean isShopChest(Block b) {
		return uBlock.couldBeShopContainer(b);
	}
	
	public boolean isShopChestSign(Block b) {
		return ChestShopSign.isValid(b);
	}

}
