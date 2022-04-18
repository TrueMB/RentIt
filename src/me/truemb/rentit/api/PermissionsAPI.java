package me.truemb.rentit.api;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import me.truemb.rentit.main.Main;
import net.milkbowl.vault.permission.Permission;

public class PermissionsAPI {
	
	private LuckPermsAPI luckPermsAPI;
	private Permission permission;
	private Main instance;
	
	public PermissionsAPI(Main plugin) {
		this.instance = plugin;
		
		if(!this.setupPermissions() || !this.instance.setupChat()) { //IF VAULT DIDNT FIND IT, TRY LUCKPERMS
			if(Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
				this.luckPermsAPI = new LuckPermsAPI(this.instance);
				return;
			}
			this.instance.getLogger().info("No Permission System was found. (optional)");
		}
	}
	
	private boolean setupPermissions() {
		if (this.instance.getServer().getPluginManager().getPlugin("Vault") == null) {
			this.instance.getLogger().warning("Vault is missing!");
			return false;
	    }
	    RegisteredServiceProvider<Permission> rsp = this.instance.getServer().getServicesManager().getRegistration(Permission.class);
	    if (rsp == null || rsp.getProvider() == null)
	    	return false;
	    
	    this.permission = rsp.getProvider();
	    this.instance.getLogger().info(rsp.getPlugin().getName() + " Permission System was found.");
	    return permission != null;
	}
	//
	public String getPrimaryGroup(UUID uuid) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			return this.getPerms().getPrimaryGroup(null, Bukkit.getOfflinePlayer(uuid));
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().getPrimaryGroup(uuid);
		}
		return null;
	}
	
	public String getPrefix(UUID uuid) {
		if(this.instance.getChat() != null && this.getPerms() != null) {
			return this.instance.getChat().getPlayerPrefix(null, Bukkit.getOfflinePlayer(uuid));
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().getPrefix(uuid);
		}
		return "";
	}
	//
	public void addGroup(UUID uuid, String groupS) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			this.getPerms().playerAddGroup(null, Bukkit.getOfflinePlayer(uuid), groupS);
		}else if(this.getLuckPermsAPI() != null) {
			this.getLuckPermsAPI().addGroup(uuid, groupS);
		}
	}
	
	public void removeGroup(UUID uuid, String groupS) {
		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			this.getPerms().playerRemoveGroup(null, Bukkit.getOfflinePlayer(uuid), groupS);
		}else if(this.getLuckPermsAPI() != null) {
			this.getLuckPermsAPI().removeGroup(uuid, groupS);
		}
	}
	
	public boolean isPlayerInGroup(Player player, String group) {

		if(this.getPerms() != null && this.getPerms().hasGroupSupport()) {
			return this.getPerms().playerInGroup(player, group);
		}else if(this.getLuckPermsAPI() != null) {
			return player.hasPermission("group." + group);
		}
		return false;
	}
	
	public boolean hasPermission(UUID uuid, String permission) {
		if(this.getPerms() != null) {
			return this.getPerms().playerHas(null, Bukkit.getOfflinePlayer(uuid), permission);
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().hasPermission(uuid, permission);
		}
		return false;
	}
	
	public boolean addPlayerPermission(UUID uuid, String permission) {

		if(this.getPerms() != null) {
			this.getPerms().playerAdd(null, Bukkit.getOfflinePlayer(uuid), permission);
		}else if(this.getLuckPermsAPI() != null) {
			return this.getLuckPermsAPI().addPlayerPermission(uuid, permission);
		}
		return false;
	}

	public LuckPermsAPI getLuckPermsAPI() {
		return this.luckPermsAPI;
	}
	
	public Permission getPerms() {
		return this.permission;
	}
}
