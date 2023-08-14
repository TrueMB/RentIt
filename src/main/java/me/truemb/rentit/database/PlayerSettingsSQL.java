package me.truemb.rentit.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Consumer;

import me.truemb.rentit.database.connector.AsyncSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.Settings;
import me.truemb.rentit.handler.SettingsHandler;
import me.truemb.rentit.main.Main;

public class PlayerSettingsSQL {
	
	private Main instance;
	
	public PlayerSettingsSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_settings + " (uuid VARCHAR(50), type VARCHAR(30), id INT, setting VARCHAR(50), value TINYINT,  PRIMARY KEY (uuid, type, id, setting))");
	}

	public void setSetting(UUID uuid, RentTypes type, int id, Settings setting, boolean value){
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_settings + " (uuid, type, id, setting, value) VALUES ('" + uuid.toString() + "', '" + type.toString() + "', '" + id + "', '" + setting.toString() + "', '" + (value ? 1 : 0) + "') "
					+ "ON CONFLICT(uuid, type, id, setting) DO UPDATE SET value='" + (value ? 1 : 0) + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_settings + " (uuid, type, id, setting, value) VALUES ('" + uuid.toString() + "', '" + type.toString() + "', '" + id + "', '" + setting.toString() + "', '" + (value ? 1 : 0) + "') "
					+ "ON DUPLICATE KEY UPDATE value='" + (value ? 1 : 0) + "';");
	}
	
	public void setupSettings(UUID uuid, RentTypes type, SettingsHandler settingsHandler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_settings + " WHERE uuid='" + uuid.toString() + "' AND type='" + type.toString() + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					while (rs.next()) {
						settingsHandler.setSetting(rs.getInt("id"), Settings.valueOf(rs.getString("setting")), rs.getInt("value") == 1 ? true : false);
					}
					return;
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		});
	}
}
