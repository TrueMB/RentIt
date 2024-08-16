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

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.math.BlockVector3;

import me.truemb.rentit.database.connector.AsyncSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UtilitiesAPI;

public class ShopsSQL {
	
	private Main instance;
	private RentTypes type = RentTypes.SHOP;
	
	public ShopsSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shops + " (ID INT PRIMARY KEY, alias VARCHAR(100), ownerUUID VARCHAR(50), ownerName VARCHAR(16), catID INT, nextPayment TIMESTAMP DEFAULT CURRENT_TIMESTAMP, autoPayment TINYINT, admin TINYINT)");
		
		//UPDATES
		sql.addColumn(sql.t_shops, "alias", "VARCHAR(100)");
		sql.addColumn(sql.t_shops, "admin", "TINYINT"); //Version 2.8.2
	}
	
	public void createShop(int id, int catId, boolean admin){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("INSERT INTO " + sql.t_shops + " (ID, ownerUUID, ownerName, catID, autoPayment, admin) VALUES (?,?,?,?,?,?)",
				String.valueOf(id), null, null, String.valueOf(catId), String.valueOf(1), String.valueOf(admin ? 1 : 0));
	}

	public void setOwner(int shopId, UUID ownerUUID, String ownerName, boolean autoPayment){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET ownerUUID=?, ownerName=?, autoPayment=? WHERE ID=?;",
				ownerUUID.toString(), ownerName, String.valueOf(autoPayment ? 1 : 0), String.valueOf(shopId));
	}
	
	public void setAlias(int shopId, String alias){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET alias=? WHERE ID=?;",
				alias, String.valueOf(shopId));
	}

	public void setCatID(int shopId, int catId){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET catID=? WHERE ID=?;",
				String.valueOf(catId), String.valueOf(shopId));
	}
	
	public void setNextPayment(int shopId, Timestamp ts){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET nextPayment=? WHERE ID=?;",
				ts.toString(), String.valueOf(shopId));
	}

	public void setAutoPayment(int shopId, boolean active){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET autoPayment=? WHERE ID=?;",
				String.valueOf(active ? 1 : 0), String.valueOf(shopId));
	}
	
	public void reset(int shopId, boolean active){

		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shops + " SET ownerUUID=?, ownerName=?, autoPayment=? WHERE ID=?;",
				null, null, String.valueOf(active ? 1 : 0), String.valueOf(shopId));
	}

	public void delete(int shopId){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("DELETE FROM " + sql.t_shops + " WHERE ID=?;",
				String.valueOf(shopId));
	}
	
	public void getNextShopId(Consumer<Integer> consumer){
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.prepareStatement("SELECT " + (sql.isSqlLite() ? "I" : "") + "IF( EXISTS(SELECT * FROM " + sql.t_shops + " WHERE ID='1'), (SELECT t1.id+1 as lowestId FROM " + sql.t_shops + " AS t1 LEFT JOIN " + sql.t_shops + " AS t2 ON t1.id+1 = t2.id WHERE t2.id IS NULL LIMIT 1), 1) as lowestId LIMIT 1;", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {

				try {
					int shopId = 1;
					while (rs.next()) {
						shopId = rs.getInt("lowestId");
						break;
					}
					
					consumer.accept(shopId);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void setupOwningIds(PlayerHandler playerHandler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		Player p = playerHandler.getPlayer();
		UUID uuid = p.getUniqueId();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shops + " WHERE ownerUUID=?;", new Consumer<ResultSet>() {

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
									.replaceAll("(?i)%" + "price" + "%", UtilitiesAPI.getHumanReadablePriceFromNumber(costs))
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
		}, uuid.toString());
	}
	
	public void setupShops() {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shops + ";", (rs) -> {
			
			try {
				int shopAmount = 0;
				while (rs.next()) {
					shopAmount++;
					int id = rs.getInt("ID");
					String alias = rs.getString("alias") != null && !rs.getString("alias").equalsIgnoreCase("null") ? rs.getString("alias") : null;
					int catID = rs.getInt("catID");
						
					UUID ownerUUID = rs.getString("ownerUUID") != null && !rs.getString("ownerUUID").equalsIgnoreCase("null") ? UUID.fromString(rs.getString("ownerUUID")) : null;
					String ownerName = rs.getString("ownerName") != null && !rs.getString("ownerName").equalsIgnoreCase("null") ? rs.getString("ownerName") : null;
					
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date nextPaymentDay = formatter.parse(rs.getString("nextPayment"));
					Timestamp nextPayment = new Timestamp(nextPaymentDay.getTime());
								
					boolean autoPayment = rs.getInt("autoPayment") == 1 ? true : false;
					boolean admin = rs.getInt("admin") == 1 ? true : false;
						
					RentTypeHandler handler = new RentTypeHandler(this.instance, this.type, id, catID, ownerUUID, ownerName, nextPayment, autoPayment, admin);
					handler.setAlias(alias);
						
					String prefix = ownerUUID != null ? instance.getPermissionsAPI().getPrefix(ownerUUID) : "";
						
					this.instance.getShopsInvSQL().setupShopInventories(handler); //GET SHOP INVENTORIES

					HashMap<Integer, RentTypeHandler> hash = new HashMap<>();
					if(this.instance.rentTypeHandlers.containsKey(this.type))
						hash = this.instance.rentTypeHandlers.get(type);
						
					hash.put(id, handler);
					this.instance.rentTypeHandlers.put(type, hash);

					if(handler.isAdmin() || ownerUUID != null) {
							
						if(!this.instance.manageFile().getBoolean("Options.disableNPC")) {
							if(this.instance.getVillagerUtils() != null) {
								//this.instance.getVillagerUtils().spawnVillager(id, prefix, handler.isAdmin() ? this.instance.translateHexColorCodes(this.instance.manageFile().getString("Options.adminShopName")) : ownerName);
							}else {
								this.instance.getNpcUtils().spawnAndEditNPC(id, prefix, handler.isAdmin() ? this.instance.translateHexColorCodes(this.instance.manageFile().getString("Options.adminShopName")) : ownerName);
							}
						}
					}

					if(this.instance.getWorldGuard() != null) {
						World world = instance.getAreaFileManager().getWorldFromArea(this.type, id);
						if(world != null) {
							if(!this.instance.getMethodes().existsWorldGuardRegion(this.type, id, world)) {
								BlockVector3 min = this.instance.getAreaFileManager().getMinBlockpoint(this.type, id);
								BlockVector3 max = this.instance.getAreaFileManager().getMaxBlockpoint(this.type, id);
								this.instance.getMethodes().createWorldGuardRegion(this.type, id, world, min, max);
									
								this.instance.getMethodes().addMemberToRegion(this.type, id, world, ownerUUID);
								this.instance.getPermissionsSQL().setupWorldGuardMembers(world, this.type, id);
							}
						}else {
							this.instance.getLogger().warning("Couldn't find the World for Shop '" + id + "' in the Areas.yml! Please report the Issue.");
						}
					}
				}
				this.instance.getLogger().info(String.valueOf(shopAmount) + " Shops are loaded.");
				return;
			} catch (SQLException | ParseException e) {
				e.printStackTrace();
			}
		});
	}
	
}
