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

public class HotelsSQL {
	
	private Main instance;
	public RentTypes type = RentTypes.HOTEL;
	
	public HotelsSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_hotels + " (ID INT PRIMARY KEY, alias VARCHAR(100), ownerUUID VARCHAR(50), ownerName VARCHAR(16), catID INT, nextPayment TIMESTAMP DEFAULT CURRENT_TIMESTAMP, autoPayment TINYINT);");

		//UPDATES
		sql.addColumn(sql.t_hotels, "alias", "VARCHAR(100)");
	}
	
	public void createHotel(int id, int catId){
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("INSERT INTO " + sql.t_hotels + " (ID, ownerUUID, ownerName, catID, autoPayment) VALUES (?,?,?,?,?);",
				String.valueOf(id), null, null, String.valueOf(catId), String.valueOf(1));
	}

	public void setOwner(int hotelId, UUID ownerUUID, String ownerName, boolean autoPayment){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET ownerUUID=?, ownerName=?, autoPayment=? WHERE ID=?;",
				ownerUUID.toString(), ownerName, String.valueOf(autoPayment ? 1 : 0), String.valueOf(hotelId));
	}
	
	public void setAlias(int hotelId, String alias){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET alias=? WHERE ID=?;",
				alias, String.valueOf(hotelId));
	}

	public void setCatID(int hotelId, int catId){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET catID=? WHERE ID=?;",
				String.valueOf(catId), String.valueOf(hotelId));
	}

	public void setNextPayment(int hotelId, Timestamp ts){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET nextPayment=? WHERE ID=?;",
				ts.toString(), String.valueOf(hotelId));
	}

	public void setAutoPayment(int hotelId, boolean active){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET autoPayment=? WHERE ID=?;",
				String.valueOf(active ? 1 : 0), String.valueOf(hotelId));
	}
	
	public void reset(int hotelId, boolean active){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET ownerUUID=?, ownerName=?, autoPayment=? WHERE ID=?;",
				null, null, String.valueOf(active ? 1 : 0), String.valueOf(hotelId));
	}

	public void delete(int hotelId){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("DELETE FROM " + sql.t_hotels + " WHERE ID=?;", 
				String.valueOf(hotelId));
	}
	
	public void getNextHotelId(Consumer<Integer> consumer){
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.prepareStatement("SELECT " + (sql.isSqlLite() ? "I" : "") + "IF( EXISTS(SELECT * FROM " + sql.t_hotels + " WHERE ID='1'), (SELECT t1.id+1 as lowestId FROM " + sql.t_hotels + " AS t1 LEFT JOIN " + sql.t_hotels + " AS t2 ON t1.id+1 = t2.id WHERE t2.id IS NULL LIMIT 1), 1) as lowestId LIMIT 1;", new Consumer<ResultSet>() {

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
		
		sql.prepareStatement("SELECT * FROM " + sql.t_hotels + " WHERE ownerUUID=?;", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					List<Integer> ids = playerHandler.getOwningList(type);
					while (rs.next()) {
						int hotelId = rs.getInt("ID");
						ids.add(hotelId);
						
						// REMIND PLAYER

						DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
						Timestamp now = new Timestamp(System.currentTimeMillis());
						
						RentTypeHandler rentHandler = instance.getMethodes().getTypeHandler(type, hotelId);
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
									.replaceAll("(?i)%" + "hotelId" + "%", String.valueOf(hotelId))
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
	
	public void setupHotels() {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_hotels + ";", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {

					int hotelAmount = 0;
					
					while (rs.next()) {
						hotelAmount++;
						
						int id = rs.getInt("ID");
						String alias = rs.getString("alias") != null && !rs.getString("alias").equalsIgnoreCase("null") ? rs.getString("alias") : null;
						int catID = rs.getInt("catID");

						UUID ownerUUID = rs.getString("ownerUUID") != null && !rs.getString("ownerUUID").equalsIgnoreCase("null") ? UUID.fromString(rs.getString("ownerUUID")) : null;
						String ownerName = rs.getString("ownerName") != null && !rs.getString("ownerName").equalsIgnoreCase("null") ? rs.getString("ownerName") : null;
						
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date nextPaymentDay = formatter.parse(rs.getString("nextPayment"));
						Timestamp nextPayment = new Timestamp(nextPaymentDay.getTime());
								
						boolean autoPayment = rs.getInt("autoPayment") == 1 ? true : false;
						
						RentTypeHandler handler = new RentTypeHandler(instance, type, id, catID, ownerUUID, ownerName, nextPayment, autoPayment, false);
						handler.setAlias(alias);
						
						HashMap<Integer, RentTypeHandler> hash = new HashMap<>();
						if(instance.rentTypeHandlers.containsKey(type))
							hash = instance.rentTypeHandlers.get(type);
						
						hash.put(id, handler);
						instance.rentTypeHandlers.put(type, hash);
						
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
								instance.getLogger().warning("Couldn't find the World for Hotel '" + id + "' in the Areas.yml! Please report the Issue.");
							}
						}
					}
					instance.getLogger().info(String.valueOf(hotelAmount) + " Hotelrooms are loaded.");
					return;
				} catch (SQLException | ParseException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
