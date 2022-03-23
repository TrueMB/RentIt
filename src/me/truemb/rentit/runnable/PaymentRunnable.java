package me.truemb.rentit.runnable;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import com.sk89q.worldedit.math.BlockVector3;

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
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Collection<RentTypeHandler> shopHandlers = this.instance.getMethodes().getPaymentsOfRentTypes(RentTypes.SHOP);
		Collection<RentTypeHandler> hotelHandlers = this.instance.getMethodes().getPaymentsOfRentTypes(RentTypes.HOTEL);
		
		shopHandlers.forEach(rentHandler -> {
			
			UUID uuid = rentHandler.getOwnerUUID();
			OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);

			int shopId = rentHandler.getID();
			CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, rentHandler.getCatID());

			if (catHandler != null){
				
				double costs = catHandler.getPrice();
				String time = catHandler.getTime();
				Timestamp ts = rentHandler.getNextPayment();
	
				while (ts.before(now)) {
	
					// ONLY IF USER WANTS MONTHLY AUTO PAY OTHERWISE RESET
					if (!rentHandler.isAutoPayment() || !this.instance.getEconomy().has(p, costs)) {
						
						// RESET SHOP
			        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".autoPaymentDefault") ? 
			        			this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + rentHandler.getCatID() + ".autoPaymentDefault") : true;
			        	
			        	rentHandler.setAutoPayment(autoPaymentDefault);
			        	this.instance.getShopsSQL().reset(shopId, autoPaymentDefault);
			        	this.instance.getPermissionsSQL().reset(RentTypes.SHOP, shopId);

						Bukkit.getScheduler().runTask(this.instance, new Runnable() {

							@Override
							public void run() {
					        	instance.getShopCacheFileManager().setShopBackup(uuid, shopId);
								rentHandler.reset(instance);
				
								//REMOVE NPC
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
								
								instance.getShopsInvSQL().updateSellInv(shopId, null);
								instance.getShopsInvSQL().updateBuyInv(shopId, null);
							}
						});
							
						break; //NO PAYMENT WILL BE DONE, SHOP RESETED
					}
					
					this.instance.getEconomy().withdrawPlayer(p, costs);
					ts = UtilitiesAPI.addTimeToTimestamp(ts, time);
				}
				this.instance.getShopsSQL().setNextPayment(shopId, ts);
				rentHandler.setNextPayment(ts);
			}
		});
		

		hotelHandlers.forEach(rentHandler -> {
			
			UUID uuid = rentHandler.getOwnerUUID();
			OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);

			int hotelId = rentHandler.getID();
			CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.HOTEL, rentHandler.getCatID());

			if (catHandler != null){
				
				double costs = catHandler.getPrice();
				String time = catHandler.getTime();
				Timestamp ts = rentHandler.getNextPayment();

				while (ts.before(now)) {

					// ONLY IF USER WANTS MONTHLY AUTO PAY OTHERWISE RESET
					if (!rentHandler.isAutoPayment() || !this.instance.getEconomy().has(p, costs)) {

						// RESET HOTEL
			        	boolean autoPaymentDefault = this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".autoPaymentDefault") ? 
			        			this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + rentHandler.getCatID() + ".autoPaymentDefault") : true;
			        	
			        	rentHandler.setAutoPayment(autoPaymentDefault);
			        	this.instance.getHotelsSQL().reset(hotelId, autoPaymentDefault); // Resets the Owner and Payment
			        	this.instance.getPermissionsSQL().reset(RentTypes.HOTEL, hotelId);

						Bukkit.getScheduler().runTask(this.instance, new Runnable() {

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
						
						break; //NO PAYMENT WILL BE DONE, HOTEL RESETED
					}

					// EXTEND HOTEL RENT
					this.instance.getEconomy().withdrawPlayer(p, costs);
					ts = UtilitiesAPI.addTimeToTimestamp(ts, time);
				}
				
				this.instance.getHotelsSQL().setNextPayment(hotelId, ts);
				rentHandler.setNextPayment(ts);
			}
		});
	}
}
