package me.truemb.rentit.placeholder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.truemb.rentit.enums.RentTypes;
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

        if(p == null){
            return "";
        }

        if(identifier.equals("currentId")){
        	int id = -1;
        	for(RentTypes type : RentTypes.values()) {
        		id = this.instance.getAreaFileManager().getIdFromArea(type, p.getLocation());
        		if(id != -1)
        			return String.valueOf(id);
        	}
            return this.instance.manageFile().getString("PlaceholderAPI.default.currentId");
            
        }else if(identifier.equals("currentType")){

        	RentTypes type = null;
        	for(RentTypes types : RentTypes.values()) {
        		type = this.instance.getAreaFileManager().getIdFromArea(types, p.getLocation()) >= 0 ? types : null;
        		if(type != null)
        			return type.toString();
        	}
            return this.instance.manageFile().getString("PlaceholderAPI.default.currentType");
            
        }else if(identifier.equals("currentOwner")){

        	for(RentTypes types : RentTypes.values()) {
        		int id = this.instance.getAreaFileManager().getIdFromArea(types, p.getLocation());
        		if(id >= 0)
        			return this.instance.getMethodes().getTypeHandler(types, id).getOwnerName() == null ? this.instance.manageFile().getString("PlaceholderAPI.default.currentOwner") : this.instance.getMethodes().getTypeHandler(types, id).getOwnerName();
        	}
            return this.instance.manageFile().getString("PlaceholderAPI.default.currentOwner");
        }
 
        return null;
    }

}
