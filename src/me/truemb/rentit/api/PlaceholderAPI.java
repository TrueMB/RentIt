package me.truemb.rentit.api;

import java.util.Collection;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class PlaceholderAPI extends PlaceholderExpansion{
	
	private Main instance;
	
	public PlaceholderAPI(Main plugin) {
		this.instance = plugin;
	}

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }
    
	@Override
	public @NotNull String getAuthor() {
		return this.instance.getDescription().getAuthors().get(0);
	}

	@Override
	public @NotNull String getIdentifier() {
		return this.instance.getDescription().getName();
	}

	@Override
	public @NotNull String getVersion() {
		return this.instance.getDescription().getVersion();
	}
	
    @Override
    public String onPlaceholderRequest(Player p, String identifier){

        if(p == null || identifier == null)
            return "";
        
        UUID uuid = p.getUniqueId();
        PlayerHandler pHandler = this.instance.getMethodes().getPlayerHandler(uuid);
        
        if(identifier.equalsIgnoreCase("currentId")){
        	
        	int id = -1;
        	for(RentTypes type : RentTypes.values()) {
        		id = this.instance.getAreaFileManager().getIdFromArea(type, p.getLocation());
        		if(id != -1)
        			return String.valueOf(id);
        	}
            return this.instance.manageFile().getString("PlaceholderAPI.default.currentId");
            
        }else if(identifier.equalsIgnoreCase("currentType")){

        	RentTypes type = null;
        	for(RentTypes types : RentTypes.values()) {
        		type = this.instance.getAreaFileManager().getIdFromArea(types, p.getLocation()) >= 0 ? types : null;
        		if(type != null)
        			return type.toString();
        	}
            return this.instance.manageFile().getString("PlaceholderAPI.default.currentType");
            
        }else if(identifier.equalsIgnoreCase("currentOwner")){

        	for(RentTypes types : RentTypes.values()) {
        		int id = this.instance.getAreaFileManager().getIdFromArea(types, p.getLocation());
        		if(id >= 0)
        			return this.instance.getMethodes().getTypeHandler(types, id).getOwnerName() == null ? this.instance.manageFile().getString("PlaceholderAPI.default.currentOwner") : this.instance.getMethodes().getTypeHandler(types, id).getOwnerName();
        	}
            return this.instance.manageFile().getString("PlaceholderAPI.default.currentOwner");
            
        }

        for(RentTypes types : RentTypes.values()) {
            if(identifier.equalsIgnoreCase("player_has" + types.toString()))
            	return pHandler != null && pHandler.getOwningList(types).size() > 0 ? this.instance.manageFile().getString("PlaceholderAPI.values.true") : this.instance.manageFile().getString("PlaceholderAPI.values.false");
            	
            else if(identifier.equalsIgnoreCase("player_" + types.toString())) {
            	if(pHandler != null && pHandler.getOwningList(types).size() > 0) {
            		RentTypeHandler typeHandler = this.instance.getMethodes().getTypeHandler(types, pHandler.getOwningList(types).get(0));
            		if(typeHandler != null)
            			return typeHandler.getAlias() != null ? typeHandler.getAlias() : String.valueOf(typeHandler.getID());
            	}else
            		return this.instance.manageFile().getString("PlaceholderAPI.default.no" + StringUtils.capitalize(types.toString().toLowerCase()) + "Found");
            }
            
            //Check first the category. Otherwise the non category one will be triggert first, since the start is identical.
            else if(identifier.toLowerCase().startsWith("free" + types.toString().toLowerCase() + "_cat_")){
                String idString = identifier.replace("free" + types.toString().toLowerCase() + "_cat_", "");
                if(idString.matches("[0-9]+")){
                	int catId = Integer.parseInt(idString);
                	Collection<RentTypeHandler> free = this.instance.getMethodes().getFreeRentTypesOfCategory(types, catId);
               		return free != null && free.size() > 0 ? this.instance.manageFile().getString("PlaceholderAPI.values.true") : this.instance.manageFile().getString("PlaceholderAPI.values.false");
               	}else
            		return this.instance.manageFile().getString("PlaceholderAPI.default.no" + StringUtils.capitalize(types.toString().toLowerCase()) + "Found");
               	
           } else if(identifier.toLowerCase().startsWith("free" + types.toString().toLowerCase() + "_")){
            	String idString = identifier.replace("free" + types.toString().toLowerCase() + "_", "");
            	if(idString.matches("[0-9]+")){
            		int id = Integer.parseInt(idString);
            		return this.instance.getMethodes().getTypeHandler(types, id) != null && !this.instance.getMethodes().getTypeHandler(types, id).isOwned() ? "true" : "false";
            	}else
            		return this.instance.manageFile().getString("PlaceholderAPI.default.no" + StringUtils.capitalize(types.toString().toLowerCase()) + "Found");
           } else if(identifier.toLowerCase().startsWith("free" + types.toString().toLowerCase())){
           		Collection<RentTypeHandler> free = this.instance.getMethodes().getFreeRentTypes(types);
          		return free != null && free.size() > 0 ? this.instance.manageFile().getString("PlaceholderAPI.values.true") : this.instance.manageFile().getString("PlaceholderAPI.values.false");
            }
        }
        
 
        return null;
    }

}
