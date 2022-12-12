package me.truemb.rentit.handler;

import java.util.HashMap;
import java.util.UUID;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;

public class PermissionsHandler {
	
	private HashMap<Integer, HashMap<String, Boolean>> permissions = new HashMap<>(); //PERMISSION : VALUE
	
	public PermissionsHandler() {
		
	}

	public void init(Main plugin, UUID uuid, RentTypes type) {
		plugin.getPermissionsSQL().setupPermissions(uuid, type, this);
	}

	public void reset(int id) {		
		this.permissions.remove(id);
	}
	
	public void setPermission(int id, String permission, boolean value) {
		
		HashMap<String, Boolean> perms = new HashMap<>();
		if(this.permissions.containsKey(id))
			perms = this.permissions.get(id);
			
		perms.put(permission, value);
		this.permissions.put(id, perms);
	}
	
	public boolean hasPermission(int id, String permission) {
		
		HashMap<String, Boolean> perms = new HashMap<>();
		if(this.permissions.containsKey(id))
			perms = this.permissions.get(id);
		
		if(perms.containsKey(permission))
			return perms.get(permission);
		return false;
	}
}
