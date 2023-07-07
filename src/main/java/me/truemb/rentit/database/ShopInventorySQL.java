package me.truemb.rentit.database;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.gui.UserShopGUI;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.InventoryUtils;

public class ShopInventorySQL {
	
	private Main instance;

	public ShopInventorySQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shop_inv + " (ID INT PRIMARY KEY, sellInv LONGTEXT, buyInv LONGTEXT)");
		

		//TODO NEW
		//sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shop_inv_new + " (ID INT, site ID, sellInv LONGTEXT, buyInv LONGTEXT, (PRIMARY KEY ID, site))");
		// Column kann nicht nur hinzugefügt werden, da der Primary Key mit diesem zusammen gestellt werden muss
		
	}
	
	public void updateSellInv(int shopId, ItemStack[] contents){
		AsyncSQL sql = this.instance.getAsyncSQL();
		String contentsS = contents != null ? InventoryUtils.itemStackArrayToBase64(contents) : null;

		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv + " (ID, sellInv, buyInv) VALUES ('" + shopId + "', '" + contentsS + "', '" + null + "') "
					+ "ON CONFLICT(ID) DO UPDATE SET sellInv='" + contentsS + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv + " (ID, sellInv, buyInv) VALUES ('" + shopId + "', '" + contentsS + "', '" + null + "') "
					+ "ON DUPLICATE KEY UPDATE sellInv='" + contentsS + "';");
	}
	
	public void updateBuyInv(int shopId, ItemStack[] contents){
		AsyncSQL sql = this.instance.getAsyncSQL();
		String contentsS = contents != null ? InventoryUtils.itemStackArrayToBase64(contents) : null;

		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv + " (ID, sellInv, buyInv) VALUES ('" + shopId + "', '" + null + "', '" + contentsS + "') "
					+ "ON CONFLICT(ID) DO UPDATE SET buyInv='" + contentsS + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv + " (ID, sellInv, buyInv) VALUES ('" + shopId + "', '" + null + "', '" + contentsS + "') "
					+ "ON DUPLICATE KEY UPDATE buyInv='" + contentsS + "';");
	}
	
	public void setupShopInventories(RentTypeHandler handler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		int id = handler.getID();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shop_inv + " WHERE ID='" + id + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {

					while (rs.next()) {
						
						String sellInv = rs.getString("sellInv");
						String buyInv = rs.getString("buyInv");
						
						ItemStack[] sellContents = !sellInv.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(sellInv) : null;
						ItemStack[] buyContents = !buyInv.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(buyInv) : null;
						
						handler.setSellInv(UserShopGUI.getSellInv(instance, id, sellContents));
						handler.setBuyInv(UserShopGUI.getBuyInv(instance, id, buyContents));
						return;
					}

					//NO ENTRY FOUND
					handler.setSellInv(UserShopGUI.getSellInv(instance, id, null));
					handler.setBuyInv(UserShopGUI.getBuyInv(instance, id, null));
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
}
