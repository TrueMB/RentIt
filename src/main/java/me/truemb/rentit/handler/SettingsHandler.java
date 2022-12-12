package me.truemb.rentit.handler;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.Settings;
import me.truemb.rentit.main.Main;

public class SettingsHandler {
	
	private HashMap<Integer, HashMap<Settings, Boolean>> setting = new HashMap<>(); //SETTING : VALUE
	
	public SettingsHandler() {
		
	}
	
	public void init(Main plugin, UUID uuid, RentTypes type) {
		plugin.getPlayerSettingSQL().setupSettings(uuid, type, this);
	}

	public void setSetting(int id, Settings setting, boolean value) {
		
		HashMap<Settings, Boolean> hash = new HashMap<>();
		if(this.setting.containsKey(id))
			hash = this.setting.get(id);
		
		hash.put(setting, value);
		
		this.setting.put(id, hash);
	}
	
	public boolean isSettingActive(int id, Settings setting) {
		HashMap<Settings, Boolean> hash = new HashMap<>();
		if(this.setting.containsKey(id))
			hash = this.setting.get(id);
		
		return hash.containsKey(setting) ? hash.get(setting) : false;
	}
}
