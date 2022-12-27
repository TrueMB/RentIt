package me.truemb.rentit.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.World;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.PermissionsHandler;
import me.truemb.rentit.main.Main;

public class PermissionsSQL {
	
	private Main instance;
	
	public PermissionsSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_perms + " (ID INT, type VARCHAR(10), userUUID VARCHAR(50), permission VARCHAR(30), value TINYINT, PRIMARY KEY (ID, type, userUUID, permission))");
		
	}
	
	public void setPermission(UUID uuid, RentTypes type, int id, String permission, boolean value){
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_perms + " (ID, type, userUUID, permission, value) VALUES ('" + id + "', '" + type.toString() + "', '" + uuid.toString() + "', '" + permission + "', '" + (value ? 1 : 0) + "') "
					+ "ON CONFLICT(ID, type, userUUID, permission) DO UPDATE SET value='" + (value ? 1 : 0) + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_perms + " (ID, type, userUUID, permission, value) VALUES ('" + id + "', '" + type.toString() + "', '" + uuid.toString() + "', '" + permission + "', '" + (value ? 1 : 0) + "') "
					+ "ON DUPLICATE KEY UPDATE value='" + (value ? 1 : 0) + "';");
		
	}
	
	public void reset(RentTypes type, int Id){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("DELETE FROM " + sql.t_perms + " WHERE type='" + type.toString() + "' AND ID='" + Id + "'");
	}
	
	
	public void setupPermissions(UUID uuid, RentTypes type, PermissionsHandler permsHandler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_perms + " WHERE userUUID='" + uuid.toString() + "' AND type='" + type.toString() + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					while (rs.next()) {
						permsHandler.setPermission(rs.getInt("ID"), rs.getString("permission"), rs.getInt("value") == 1 ? true : false);
					}
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void hasBuildPermissions(UUID uuid, RentTypes type, int id, Consumer<Boolean> callback) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		String adminPerm = this.instance.manageFile().getString("UserPermissions." + type.toString().toLowerCase() + ".Admin");
		String buildPerm = this.instance.manageFile().getString("UserPermissions." + type.toString().toLowerCase() + ".Build");
		
		sql.prepareStatement("SELECT * FROM " + sql.t_perms + " WHERE userUUID='" + uuid.toString() + "' AND type='" + type.toString() + "' AND ID='" + String.valueOf(id) + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					while (rs.next()) {
						String permission = rs.getString("permission");
						boolean value = rs.getInt("value") == 1 ? true : false;
						if((permission.equalsIgnoreCase(buildPerm) || permission.equalsIgnoreCase(adminPerm)) && value) {
							callback.accept(true);
							return;
						}
					}
					callback.accept(false);
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void setupWorldGuardMembers(World world, RentTypes type, int id) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_perms + " WHERE type='" + type.toString() + "' AND ID='" + String.valueOf(id) + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					while (rs.next()) {
						UUID uuid = UUID.fromString(rs.getString("userUUID"));
						instance.getMethodes().addMemberToRegion(type, id, world, uuid);
					}
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
}
