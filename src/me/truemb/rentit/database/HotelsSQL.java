package me.truemb.rentit.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.PlayerManager;

public class HotelsSQL {
	
	private Main instance;
	public RentTypes type = RentTypes.HOTEL;
	
	public HotelsSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_hotels + " (ID INT PRIMARY KEY, ownerUUID VARCHAR(50), ownerName VARCHAR(16), catID INT, nextPayment TIMESTAMP DEFAULT CURRENT_TIMESTAMP, autoPayment TINYINT)");
		
	}
	
	public void createHotel(int id, int catID){
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("INSERT INTO " + sql.t_hotels + " (ID, ownerUUID, ownerName, catID, autoPayment) VALUES ('" + String.valueOf(id) + "','" + null + "', '" + null + "', '" + catID + "', '" + 1 + "')");
	}

	public void setOwner(int hotelId, UUID ownerUUID, String ownerName, boolean autoPayment){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET ownerUUID='" + ownerUUID.toString() + "', ownerName='" + ownerName + "', autoPayment='" + (autoPayment ? 1 : 0) + "' WHERE ID='" + hotelId + "'");
	}

	public void setCatID(int hotelId, int catID){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET catID='" + catID + "' WHERE ID='" + hotelId + "'");
	}

	public void setNextPayment(int hotelId, Timestamp ts){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET nextPayment='" + ts + "' WHERE ID='" + hotelId + "'");
	}

	public void setAutoPayment(int hotelId, boolean active){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET autoPayment='" + String.valueOf(active ? 1 : 0) + "' WHERE ID='" + hotelId + "'");
	}
	
	public void reset(int hotelId, boolean active){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_hotels + " SET ownerUUID='" + null + "', ownerName='" + null + "', autoPayment='" + String.valueOf(active ? 1 : 0) + "' WHERE ID='" + hotelId + "'");
	}

	public void delete(int hotelId){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("DELETE FROM " + sql.t_hotels + " WHERE ID='" + hotelId + "'");
	}

	public void setupOwningIds(PlayerHandler playerHandler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		Player p = playerHandler.getPlayer();
		UUID uuid = PlayerManager.getUUID(p);
		
		sql.prepareStatement("SELECT * FROM " + sql.t_hotels + " WHERE ownerUUID='" + uuid.toString() + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					List<Integer> ids = playerHandler.getOwningList(type);
					while (rs.next()) {
						ids.add(rs.getInt("ID"));
					}
					playerHandler.setOwningRent(type, ids);
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
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
						int catID = rs.getInt("catID");

						UUID ownerUUID = !rs.getString("ownerUUID").equalsIgnoreCase("null") ? UUID.fromString(rs.getString("ownerUUID")) : null;
						String ownerName = !rs.getString("ownerName").equalsIgnoreCase("null") ? rs.getString("ownerName") : null;
						
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date nextPaymentDay = formatter.parse(rs.getString("nextPayment"));
						Timestamp nextPayment = new Timestamp(nextPaymentDay.toInstant().getEpochSecond()); //rs.getTimestamp("nextPayment");
								
						boolean autoPayment = rs.getInt("autoPayment") == 1 ? true : false;
						
						RentTypeHandler handler = new RentTypeHandler(type, id, catID, ownerUUID, ownerName, nextPayment, autoPayment);
						
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
				} catch (SQLException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
