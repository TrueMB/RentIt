package me.truemb.rentit.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class ShopsSQL {
	
	private Main instance;
	private RentTypes type = RentTypes.SHOP;
	
	public ShopsSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shops + " (ID INT PRIMARY KEY, alias VARCHAR(100), ownerUUID VARCHAR(50), ownerName VARCHAR(16), catID INT, nextPayment TIMESTAMP DEFAULT CURRENT_TIMESTAMP, autoPayment TINYINT)");
		
		//UPDATES
		sql.addColumn(sql.t_shops, "alias", "VARCHAR(100)");
	}
	
	public void createShop(int id, int catID){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("INSERT INTO " + sql.t_shops + " (ID, ownerUUID, ownerName, catID, autoPayment) VALUES ('" + String.valueOf(id) + "','" + null + "', '" + null + "', '" + catID + "', '" + 1 + "')");
	}

	public void setOwner(int shopId, UUID ownerUUID, String ownerName, boolean autoPayment){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET ownerUUID='" + ownerUUID.toString() + "', ownerName='" + ownerName + "', autoPayment='" + (autoPayment ? 1 : 0) + "' WHERE ID='" + shopId + "'");
	}
	
	public void setAlias(int shopId, String alias){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET alias='" + alias + "' WHERE ID='" + shopId + "'");
	}

	public void setCatID(int shopId, int catID){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET catID='" + catID + "' WHERE ID='" + shopId + "'");
	}
	
	public void setNextPayment(int shopId, Timestamp ts){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET nextPayment='" + ts + "' WHERE ID='" + shopId + "'");
	}

	public void setAutoPayment(int shopId, boolean active){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET autoPayment='" + String.valueOf(active ? 1 : 0) + "' WHERE ID='" + shopId + "'");
	}
	
	public void reset(int shopId, boolean active){

		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET ownerUUID='" + null + "', ownerName='" + null + "', autoPayment='" + String.valueOf(active ? 1 : 0) + "' WHERE ID='" + shopId + "'");
	}

	public void delete(int shopId){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("DELETE FROM " + sql.t_shops + " WHERE ID='" + shopId + "'");
	}
	
	public void setupOwningIds(PlayerHandler playerHandler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		Player p = playerHandler.getPlayer();
		UUID uuid = p.getUniqueId();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shops + " WHERE ownerUUID='" + uuid.toString() + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					List<Integer> ids = playerHandler.getOwningList(type);
					while (rs.next()) {
						int shopId = rs.getInt("ID");
						ids.add(shopId);
						
						// REMIND PLAYER

						DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
						Timestamp now = new Timestamp(System.currentTimeMillis());
						
						RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(type, shopId);
						if(rentHandler == null) continue;
						CategoryHandler catHandler = instance.getMethodes().getCategory(type, rentHandler.getCatID());
						if(catHandler == null) continue;
						
						double costs = catHandler.getPrice();
						String time = catHandler.getTime();
						Timestamp ts = rentHandler.getNextPayment();
						Timestamp reminderTs = rentHandler.getReminder();
						
						//REMIND IF SHOP DOESNT AUTOMATICLY EXTEND
						if(!rentHandler.isAutoPayment() && !rentHandler.isReminded() && reminderTs != null && reminderTs.before(now)) {
							
						    String alias = rentHandler.getAlias() != null ? rentHandler.getAlias() : String.valueOf(rentHandler.getID());
						    String catAlias = catHandler.getAlias() != null ? catHandler.getAlias() : String.valueOf(catHandler.getCatID());

							p.sendMessage(instance.getMessage(type.toString().toLowerCase() + "RentRunningOut")
									.replaceAll("(?i)%" + "shopId" + "%", String.valueOf(shopId))
									.replaceAll("(?i)%" + "alias" + "%", alias)
									.replaceAll("(?i)%" + "catAlias" + "%", catAlias)
									.replaceAll("(?i)%" + "price" + "%", String.valueOf(costs))
									.replaceAll("(?i)%" + "time" + "%", time)
									.replaceAll("(?i)%" + "rentEnd" + "%", df.format(ts)));
							
							rentHandler.setReminded(true);
						}
					}
					playerHandler.setOwningRent(type, ids);
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void setupShops() {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shops + ";", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {

					int shopAmount = 0;
					while (rs.next()) {
						shopAmount++;
						int id = rs.getInt("ID");
						String alias = rs.getString("alias") != null && !rs.getString("alias").equalsIgnoreCase("null") ? rs.getString("alias") : null;
						int catID = rs.getInt("catID");
						
						UUID ownerUUID = !rs.getString("ownerUUID").equalsIgnoreCase("null") ? UUID.fromString(rs.getString("ownerUUID")) : null;
						String ownerName = !rs.getString("ownerName").equalsIgnoreCase("null") ? rs.getString("ownerName") : null;
						
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date nextPaymentDay = formatter.parse(rs.getString("nextPayment"));
						Timestamp nextPayment = new Timestamp(nextPaymentDay.getTime());
								
						boolean autoPayment = rs.getInt("autoPayment") == 1 ? true : false;
						
						RentTypeHandler handler = new RentTypeHandler(instance, type, id, catID, ownerUUID, ownerName, nextPayment, autoPayment);
						handler.setAlias(alias);
						
						String prefix = ownerUUID != null ? instance.getPermissionsAPI().getPrefix(ownerUUID) : "";
						
						instance.getShopsInvSQL().setupShopInventories(handler); //GET SHOP INVENTORIES

						HashMap<Integer, RentTypeHandler> hash = new HashMap<>();
						if(instance.rentTypeHandlers.containsKey(type))
							hash = instance.rentTypeHandlers.get(type);
						
						hash.put(id, handler);
						instance.rentTypeHandlers.put(type, hash);
						
						if(ownerUUID != null) {
							Bukkit.getScheduler().runTask(instance, new Runnable() {
									
								@Override
								public void run() {
									if(instance.getVillagerUtils() != null) {
										instance.getVillagerUtils().spawnVillager(id, prefix, ownerUUID, ownerName);
									}else {
										instance.getNpcUtils().spawnAndEditNPC(id, prefix, ownerUUID, ownerName);
									}
								}
							});
						}

						if(instance.getWorldGuard() != null) {
							World world = instance.getAreaFileManager().getWorldFromArea(type, id);
							if(world != null) {
								if(!instance.getMethodes().existsWorldGuardRegion(type, id, world)) {
									BlockVector3 min = instance.getAreaFileManager().getMinBlockpoint(type, id);
									BlockVector3 max = instance.getAreaFileManager().getMaxBlockpoint(type, id);
									instance.getMethodes().createWorldGuardRegion(type, id, world, min, max);
									
									instance.getMethodes().addMemberToRegion(type, id, world, ownerUUID);
									instance.getPermissionsSQL().setupWorldGuardMembers(world, type, id);
								}
							}else {
								instance.getLogger().warning("Couldn't find the World for Shop '" + id + "' in the Areas.yml! Please report the Issue.");
							}
						}
					}
					instance.getLogger().info(String.valueOf(shopAmount) + " Shops are loaded.");
					return;
				} catch (SQLException | ParseException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
}
