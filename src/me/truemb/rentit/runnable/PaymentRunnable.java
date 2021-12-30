package me.truemb.rentit.runnable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.database.AsyncSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class PaymentRunnable implements Runnable {

	private Main instance;

	public PaymentRunnable(Main plugin) {
		this.instance = plugin;
	}

	@Override
	public void run() {

		Timestamp current = new Timestamp(System.currentTimeMillis());
		AsyncSQL sql = this.instance.getAsyncSQL();

		// SHOP
		sql.prepareStatement("SELECT * FROM " + sql.t_shops + " WHERE ownerUUID!='null' AND  nextPayment <= '" + current + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet payRS) {

				Timestamp now = new Timestamp(System.currentTimeMillis());

				try {
					while (payRS.next()) {

						int shopId = payRS.getInt("ID");
						RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);

						if (rentHandler == null)
							continue;

						UUID uuid = rentHandler.getOwnerUUID();
						if (uuid == null)
							continue;

						CategoryHandler catHandler = instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

						if (catHandler == null)
							continue;

						double costs = catHandler.getPrice();
						String time = catHandler.getTime();
						Timestamp ts = rentHandler.getNextPayment();

						while (ts.before(now)) {

							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);

							// ONLY IF USER WANTS MONTHLY AUTO PAY OTHERWISE RESET
							if (!rentHandler.isAutoPayment() || !instance.getEconomy().has(p, costs)) {

								// CACHE INV
								/*
								if (rentHandler.getSellInv() != null) {
									ItemStack[] content = rentHandler.getSellInv().getContents();
									instance.getShopCacheFileManager().setShopBackup(uuid, shopId, content);
								}
								*/
								
								// RESET SHOP
					        	boolean autoPaymentDefault = instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".autoPaymentDefault") ? instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".autoPaymentDefault") : true;
					        	rentHandler.setAutoPayment(autoPaymentDefault);
								instance.getShopsSQL().reset(shopId, autoPaymentDefault);
								
								instance.getPermissionsSQL().reset(RentTypes.SHOP, shopId);

								Bukkit.getScheduler().runTask(instance, new Runnable() {
									
									@Override
									public void run() {
										instance.getShopCacheFileManager().setShopBackup(uuid, shopId);
										
										rentHandler.reset(instance);

										if(instance.getNpcUtils() != null) {
											if (instance.getNpcUtils().isNPCSpawned(shopId))
												instance.getNpcUtils().despawnNPC(shopId);
										}else {
											if(instance.getVillagerUtils().isVillagerSpawned(shopId))
												instance.getVillagerUtils().destroyVillager(shopId);
										}

										BlockVector3 min = instance.getAreaFileManager().getMinBlockpoint(RentTypes.SHOP, shopId);
										BlockVector3 max = instance.getAreaFileManager().getMaxBlockpoint(RentTypes.SHOP, shopId);
										World world = instance.getAreaFileManager().getWorldFromArea(RentTypes.SHOP, shopId);
										
										instance.getBackupManager().paste(RentTypes.SHOP, shopId, min, max, world, false);
										instance.getAreaFileManager().clearMember(RentTypes.SHOP, shopId);
										instance.getAreaFileManager().setOwner(RentTypes.SHOP, shopId, null);
										instance.getMethodes().clearPlayersFromRegion(RentTypes.SHOP, shopId, instance.getAreaFileManager().getWorldFromArea(RentTypes.SHOP, shopId));

										instance.getDoorFileManager().closeDoors(RentTypes.SHOP, shopId);
										instance.getAreaFileManager().unsetDoorClosed(RentTypes.SHOP, shopId);
										
										instance.getMethodes().updateSign(RentTypes.SHOP, shopId, null, time, costs, shopId);
									}
								});
								
								instance.getShopsInvSQL().updateSellInv(shopId, null);
								instance.getShopsInvSQL().updateBuyInv(shopId, null);
								
								return;
							}

							// VERL�NGER SHOP
							instance.getEconomy().withdrawPlayer(p, costs);
							ts = UtilitiesAPI.addTimeToTimestamp(ts, time);
						}
						instance.getShopsSQL().setNextPayment(shopId, ts);
						rentHandler.setNextPayment(ts);
					}
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});

		// HOTEL
		sql.prepareStatement("SELECT * FROM " + sql.t_hotels + " WHERE ownerUUID!='null' AND nextPayment <= '" + current + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet payRS) {

				Timestamp now = new Timestamp(System.currentTimeMillis());

				try {
					while (payRS.next()) {

						int hotelId = payRS.getInt("ID");
						RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(RentTypes.HOTEL, hotelId);

						if (rentHandler == null)
							continue;

						UUID uuid = rentHandler.getOwnerUUID();
						if (uuid == null)
							continue;

						CategoryHandler catHandler = instance.getMethodes().getCategory(RentTypes.HOTEL, rentHandler.getCatID());

						if (catHandler == null)
							continue;

						double costs = catHandler.getPrice();
						String time = catHandler.getTime();
						Timestamp ts = rentHandler.getNextPayment();

						while (ts.before(now)) {
							
							OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);

							// ONLY IF USER WANTS MONTHLY AUTO PAY OTHERWISE RESET
							if (!rentHandler.isAutoPayment() || !instance.getEconomy().has(p, costs)) {

								// RESET HOTEL
					        	boolean autoPaymentDefault = instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".autoPaymentDefault") ? instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".autoPaymentDefault") : true;
					        	rentHandler.setAutoPayment(autoPaymentDefault);
								instance.getHotelsSQL().reset(hotelId, autoPaymentDefault); // Resets the Owner and Payment
								
								instance.getPermissionsSQL().reset(RentTypes.HOTEL, hotelId);

								Bukkit.getScheduler().runTask(instance, new Runnable() {

									@Override
									public void run() {
										rentHandler.reset(instance);
										BlockVector3 min = instance.getAreaFileManager().getMinBlockpoint(RentTypes.HOTEL, hotelId);
										BlockVector3 max = instance.getAreaFileManager().getMaxBlockpoint(RentTypes.HOTEL, hotelId);
										World world = instance.getAreaFileManager().getWorldFromArea(RentTypes.HOTEL, hotelId);
										
										instance.getBackupManager().paste(RentTypes.HOTEL, hotelId, min, max, world, false);
										instance.getAreaFileManager().clearMember(RentTypes.HOTEL, hotelId);
										instance.getMethodes().clearPlayersFromRegion(RentTypes.HOTEL, hotelId, instance.getAreaFileManager().getWorldFromArea(RentTypes.HOTEL, hotelId));

										instance.getDoorFileManager().closeDoors(RentTypes.HOTEL, hotelId);
										instance.getAreaFileManager().unsetDoorClosed(RentTypes.HOTEL, hotelId);
										
										instance.getMethodes().updateSign(RentTypes.HOTEL, hotelId, null, time, costs, hotelId);
									}
								});
								return;
							}

							// EXTEND HOTEL RENT
							instance.getEconomy().withdrawPlayer(p, costs);
							ts = UtilitiesAPI.addTimeToTimestamp(ts, time);
						}
						instance.getShopsSQL().setNextPayment(hotelId, ts);
						rentHandler.setNextPayment(ts);
					}
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
