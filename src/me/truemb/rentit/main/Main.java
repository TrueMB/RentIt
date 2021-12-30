package me.truemb.rentit.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import de.jeff_media.updatechecker.UpdateChecker;
import de.jeff_media.updatechecker.UserAgentBuilder;
import me.truemb.rentit.commands.FreeHotelsCOMMAND;
import me.truemb.rentit.commands.FreeShopsCOMMAND;
import me.truemb.rentit.commands.HotelCOMMAND;
import me.truemb.rentit.commands.HotelsCOMMAND;
import me.truemb.rentit.commands.RentItCOMMAND;
import me.truemb.rentit.commands.ShopCOMMAND;
import me.truemb.rentit.commands.ShopsCOMMAND;
import me.truemb.rentit.data.RollbackInventoryManager;
import me.truemb.rentit.database.AsyncSQL;
import me.truemb.rentit.database.CategoriesSQL;
import me.truemb.rentit.database.HotelsSQL;
import me.truemb.rentit.database.PermissionsSQL;
import me.truemb.rentit.database.PlayerSettingsSQL;
import me.truemb.rentit.database.ShopInventorySQL;
import me.truemb.rentit.database.ShopsSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.filemanager.AreaFileManager;
import me.truemb.rentit.filemanager.DoorFileManager;
import me.truemb.rentit.filemanager.NPCFileManager;
import me.truemb.rentit.filemanager.ShopCacheFileManager;
import me.truemb.rentit.filemanager.SignFileManager;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.listener.AdminHotelListener;
import me.truemb.rentit.listener.AdminShopListener;
import me.truemb.rentit.listener.CategoryGUIListener;
import me.truemb.rentit.listener.HotelAreaListener;
import me.truemb.rentit.listener.ItemBoughtListener;
import me.truemb.rentit.listener.ItemSelledListener;
import me.truemb.rentit.listener.NPCShopListener;
import me.truemb.rentit.listener.OwningListListener;
import me.truemb.rentit.listener.PlayerCommandSendListener;
import me.truemb.rentit.listener.PlayerJoinListener;
import me.truemb.rentit.listener.PlayerQuitListener;
import me.truemb.rentit.listener.RentTimeClickListener;
import me.truemb.rentit.listener.ShopAreaListener;
import me.truemb.rentit.listener.ShopBuyOrSellListener;
import me.truemb.rentit.listener.ShopListener;
import me.truemb.rentit.listener.ShopitemsBackupListener;
import me.truemb.rentit.listener.SignListener;
import me.truemb.rentit.listener.UserConfirmationListener;
import me.truemb.rentit.listener.VillagerShopListener;
import me.truemb.rentit.placeholder.PlaceholderAPI;
import me.truemb.rentit.runnable.PaymentRunnable;
import me.truemb.rentit.utils.BackupManager;
import me.truemb.rentit.utils.ConfigUpdater;
import me.truemb.rentit.utils.NPCUtils;
import me.truemb.rentit.utils.PermissionsAPI;
import me.truemb.rentit.utils.ShopItemManager;
import me.truemb.rentit.utils.UTF8YamlConfiguration;
import me.truemb.rentit.utils.UtilMethodes;
import me.truemb.rentit.utils.VillagerUtils;
import me.truemb.rentit.utils.WorldGuardUtils;
import net.citizensnpcs.api.CitizensPlugin;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin {
	
	private Economy econ;
	private Chat chat;
	private WorldEditPlugin worldEdit;
	private WorldGuardPlugin worldGuard;
	
	private PermissionsAPI permsAPI;

	private AsyncSQL sql;
	private HotelsSQL hotelsSQL;
	private ShopsSQL shopsSQL;
	private ShopInventorySQL shopInvSQL;
	private PermissionsSQL permsSQL;
	private CategoriesSQL catSQL;
	private PlayerSettingsSQL psettingSQL;

	private BackupManager backupMGR;
	private ShopItemManager shopItemMGR;
	private UtilMethodes shopMeth;
	
	//SOFTDEPEND
	private WorldGuardUtils wgUtils;
	private NPCUtils npcUtils;
	private VillagerUtils vilUtils;
	
	private NPCFileManager npcFM;
	private SignFileManager signFM;
	private ShopCacheFileManager shopCacheFM;
	private AreaFileManager areaFM;
	private DoorFileManager doorFM;
	
	private RollbackInventoryManager rollbackInvManager;
	
	private UTF8YamlConfiguration config;

	public HashMap<UUID, PlayerHandler> playerHandlers = new HashMap<>(); // UUID = playerUUID - SettingsHandler
	public HashMap<RentTypes, HashMap<Integer, CategoryHandler>> catHandlers = new HashMap<>(); // RentType = hotel/shop - int = catID -  CategoryHandler
	public HashMap<RentTypes, HashMap<Integer, RentTypeHandler>> rentTypeHandlers = new HashMap<>(); // RentType = hotel/shop - int = shop/hotel ID - RentTypeHandler

	private static final int configVersion = 8;
    private static final int SPIGOT_RESOURCE_ID = 90195;
    private static final int BSTATS_PLUGIN_ID = 12060;
    
	private int runnId;
	
	public boolean isSystemRunningOkay = true;
	
	@Override
	public void onEnable() {
		this.manageFile();
		this.startMySql();

		if(!this.manageFile().getBoolean("Options.commands.rentit.disabled"))
			new RentItCOMMAND(this); //SHOULD ALWAYS RUN, EVENT WITHOUT DATABASE
		
		if(!this.isSystemRunningOkay)
			return; //DATABASE MISSING
		
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
			
			@Override
			public void run() {
				initHandlers();
			}
		}, 20);
		
		
		this.setupEconomy();
		this.permsAPI = new PermissionsAPI(this);
		this.setupWorldEdit();
		this.setupWorldGuard();
		
		if(this.manageFile().getBoolean("Options.useNPCs"))
			this.setupCitizens(); //CITIZENS NPC
		else {
			this.vilUtils = new VillagerUtils(this); //VILLAGER NPC
			new VillagerShopListener(this);
		}

		this.shopMeth = new UtilMethodes(this);
		this.backupMGR = new BackupManager(this);
		this.shopItemMGR = new ShopItemManager();
		this.npcFM = new NPCFileManager(this);
		this.signFM = new SignFileManager(this);
		this.shopCacheFM = new ShopCacheFileManager(this);
		this.areaFM = new AreaFileManager(this);
		this.doorFM = new DoorFileManager(this);
		
		this.rollbackInvManager = new RollbackInventoryManager(this);
				
		this.setupPlaceholderAPI();
		
		//LISTENER
		new AdminShopListener(this);
		new AdminHotelListener(this);
		new ShopListener(this);
		new SignListener(this);
		new UserConfirmationListener(this);
		new RentTimeClickListener(this);
		new OwningListListener(this);
		new PlayerCommandSendListener(this);
		new PlayerJoinListener(this);
		new PlayerQuitListener(this);
		new ItemBoughtListener(this);
		new ItemSelledListener(this);
		new ShopAreaListener(this);
		new HotelAreaListener(this);
		new CategoryGUIListener(this);
		new ShopBuyOrSellListener(this);
		new ShopitemsBackupListener(this);
		
		
		//COMMANDS
		if(!this.manageFile().getBoolean("Options.commands.shop.disabled"))
			new ShopCOMMAND(this);
		
		if(!this.manageFile().getBoolean("Options.commands.hotel.disabled"))
			new HotelCOMMAND(this);
		
		if(!this.manageFile().getBoolean("Options.commands.shops.disabled"))
			new ShopsCOMMAND(this);
		
		if(!this.manageFile().getBoolean("Options.commands.hotels.disabled"))
			new HotelsCOMMAND(this);
		
		if(!this.manageFile().getBoolean("Options.commands.freeshops.disabled"))
			new FreeShopsCOMMAND(this);
		
		if(!this.manageFile().getBoolean("Options.commands.freehotels.disabled"))
			new FreeHotelsCOMMAND(this);
		
		//METRICS ANALYTICS
		if(this.manageFile().getBoolean("Options.useMetrics"))
			new Metrics(this, BSTATS_PLUGIN_ID);
		
		//UPDATE CHECKER
		this.checkForUpdate();
		
		//PAYMENT SCHEDULER
		this.runnId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new PaymentRunnable(this), 20 * 10, 20 * 60).getTaskId(); //EVERY MINUTE
		
	}
	
	@Override
	public void onDisable() {
		
		if(this.getVillagerUtils() != null)
			this.getVillagerUtils().disableVillagers();
		else
			this.getNpcUtils().disableNPCs();

		Bukkit.getScheduler().cancelTask(this.runnId);
		
		if(this.getAsyncSQL() != null && this.getAsyncSQL().getMySQL() != null && this.getAsyncSQL().getMySQL().getConnection() != null)
			this.getAsyncSQL().getMySQL().closeConnection();
		
		else if(this.getAsyncSQL() != null && this.getAsyncSQL().getSqlLite() != null && this.getAsyncSQL().getSqlLite().getConnection() != null)
				this.getAsyncSQL().getSqlLite().closeConnection();
	}
	
	public void initRestart() {

		//------ CLOSE EVERTHING ------
		
		//DISABLING VILLAGERS
		if(this.getVillagerUtils() != null)
			this.getVillagerUtils().disableVillagers();
		
		//OR NPCS
		else
			this.getNpcUtils().disableNPCs();

		//DISABLING PAYMENT RUNNABLE
		Bukkit.getScheduler().cancelTask(this.runnId);
		this.runnId = -1;
		
		//CLOSE MYSQL CONNECTION
		if(this.getAsyncSQL() != null && this.getAsyncSQL().getMySQL() != null && this.getAsyncSQL().getMySQL().getConnection() != null)
			this.getAsyncSQL().getMySQL().closeConnection();
		
		//OR SQL LITE CONNECTION
		else if(this.getAsyncSQL() != null && this.getAsyncSQL().getSqlLite() != null && this.getAsyncSQL().getSqlLite().getConnection() != null)
				this.getAsyncSQL().getSqlLite().closeConnection();
		
		//RESET CONFIG CACHE
		this.config = null;
		
		//RESET HASHES
		this.playerHandlers = new HashMap<>();
		this.catHandlers = new HashMap<>();
		this.rentTypeHandlers = new HashMap<>();
		
		
		//------ START EVERTHING AGAIN ------
		
		//CACHE CONFIG
		this.manageFile();
		
		//START MYSQL
		this.startMySql();

		if(!this.isSystemRunningOkay)
			return; //DATABASE MISSING
		
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
			
			@Override
			public void run() {
				initHandlers();
			}
		}, 20);
		
		//SETUP SHOP NPCS
		if(this.manageFile().getBoolean("Options.useNPCs"))
			this.setupCitizens();
		
		//OR VILLAGER
		else {
			this.vilUtils = new VillagerUtils(this);
			new VillagerShopListener(this);
		}
		
		//START MANAGERS
		this.shopMeth = new UtilMethodes(this);
		this.backupMGR = new BackupManager(this);
		this.shopItemMGR = new ShopItemManager();
		this.npcFM = new NPCFileManager(this);
		this.signFM = new SignFileManager(this);
		this.shopCacheFM = new ShopCacheFileManager(this);
		this.areaFM = new AreaFileManager(this);
		this.doorFM = new DoorFileManager(this);
		
		//LOAD ONLINE PLAYERS
		Bukkit.getOnlinePlayers().forEach(p -> {

			PlayerHandler playerHandler = new PlayerHandler(p);
			playerHandler.init(this);
			
		});
		
		//START PAYMENT 5 SECONDS LATER, SO THAT EVERYTHING GETS LOADED FIRST
		Main plugin = this;
		Bukkit.getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
			
			@Override
			public void run() {
				//PAYMENT SCHEDULER
				plugin.runnId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new PaymentRunnable(plugin), 20 * 10, 20 * 60).getTaskId(); //EVERY MINUTE
			}
		}, 5 * 20);
	}

	private void initHandlers() {
		this.getShopsSQL().setupShops();
		this.getHotelsSQL().setupHotels();
		this.getCategorySQL().setupCategories();
	}
	
	//CONFIG
	public String getMessage(String path) {
		String s = this.manageFile().getString("Messages.prefix") + " " + this.manageFile().getString("Messages." + path);
		return ChatColor.translateAlternateColorCodes('&', this.translateHexColorCodes(s));
	}

	public String translateHexColorCodes(String message){
		
        final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
            matcher = hexPattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
	
	public UTF8YamlConfiguration manageFile() {
		File configFile = this.getConfigFile();
		if (!configFile.exists())
			saveResource("config.yml", true);
		
		if(this.config == null) {
			
			//TO GET THE CONFIG VERSION
			this.config = new UTF8YamlConfiguration(configFile);
			
			//UPDATE
			if(!this.config.isSet("ConfigVersion") || this.config.getInt("ConfigVersion") < configVersion) {
				this.getLogger().info("Updating Config!");
				try {
					ConfigUpdater.update(this, "config.yml", configFile, new ArrayList<>());
					this.reloadConfig();
					this.config = new UTF8YamlConfiguration(configFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		return this.config;
	}

	private File getConfigFile() {
		return new File(this.getDataFolder().getPath(), "config.yml");
	}
	
	//CHECK FOR UPDATE
	//https://www.spigotmc.org/threads/powerful-update-checker-with-only-one-line-of-code.500010/
	private void checkForUpdate() {
        UpdateChecker.init(this, SPIGOT_RESOURCE_ID) // A link to a URL that contains the latest version as String
                .setDownloadLink(SPIGOT_RESOURCE_ID) // You can either use a custom URL or the Spigot Resource ID
                .setDonationLink("https://www.paypal.me/truemb")
                .setChangelogLink(SPIGOT_RESOURCE_ID) // Same as for the Download link: URL or Spigot Resource ID
                .setNotifyOpsOnJoin(true) // Notify OPs on Join when a new version is found (default)
                .setNotifyByPermissionOnJoin(this.getDescription().getName() + ".updatechecker") // Also notify people on join with this permission
                .setUserAgent(new UserAgentBuilder().addPluginNameAndVersion())
                .checkEveryXHours(12) // Check every hours
                .checkNow(); // And check right now
        
	}
	
	//MONEY
	private boolean setupEconomy() {
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			this.getLogger().warning("Vault is missing!");
			return false;
	    }
	    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
	    if (rsp == null || rsp.getProvider() == null) {
	    	this.getLogger().warning("An Economy Plugin is missing!");
	    	return false;
	    }
	    this.getLogger().info(rsp.getPlugin().getName() + " Chat System was found.");
	    econ = rsp.getProvider();
	    return econ != null;
	}

	//CHAT
	public boolean setupChat() {
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			this.getLogger().warning("Vault is missing!");
			return false;
	    }
	    RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
	    if (rsp == null || rsp.getProvider() == null) {
	    	this.getLogger().warning("A Chat Plugin is missing!");
	    	return false;
	    }

	    this.getLogger().info(rsp.getPlugin().getName() + " Chat System was found.");
	    chat = rsp.getProvider();
	    return chat != null;
	}
	
	//WORLDEDIT
	private void setupWorldEdit() {
		Plugin worldEdit = this.getServer().getPluginManager().getPlugin("WorldEdit");
		
		if(worldEdit == null || !(worldEdit instanceof WorldEditPlugin)) {
			this.getLogger().warning("WorldEdit is missing!");
			return;
		}

		this.getLogger().info("WorldEdit was found!");
	    this.worldEdit = (WorldEditPlugin) worldEdit;
	}
	
	//WORLDGUARD
	private void setupWorldGuard() {
		Plugin worldGuard = this.getServer().getPluginManager().getPlugin("WorldGuard");
		
		if(worldGuard == null || !(worldGuard instanceof WorldGuardPlugin)) {
			this.worldGuard = null;
			return;
		}

		this.getLogger().info("WorldGuard was found!");
	    this.worldGuard = (WorldGuardPlugin) worldGuard;
	    
		this.wgUtils = new WorldGuardUtils();
	}
	
	//CITIZENS
	private void setupCitizens() {

		Plugin citizens = this.getServer().getPluginManager().getPlugin("Citizens");
		
		if(citizens == null || !(citizens instanceof CitizensPlugin)) {
			this.getLogger().warning("Citizens is missing! (Its not needed, if you turn 'Options.useNPCs' to 'false' in the config)");
			Bukkit.getPluginManager().disablePlugin(this); //DISABLE PLUGIN, SINCE YOU WANT TO USE CITIZENS NPC, BUT THE PLUGIN IS NOT THERE
			return;
		}
		this.getLogger().info("Citizens was found!");
		//PLUGIN WAS FOUND
		
		//LISTENER
		new NPCShopListener(this);
		
		//METHODE CLASS
		this.npcUtils = new NPCUtils(this);
		
	}
	
	private void setupPlaceholderAPI() {
		
		//PLUGIN WAS FOUND
	    if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
	          new PlaceholderAPI(this).register();
			this.getLogger().info("PlacerHolderAPI was found and registered!");
	    }else {
			this.getLogger().info("PlacerHolderAPI was not found. (Is not needed, but supported)");
	    }
		
	}
	
	//MySQL
	private void startMySql() {
		this.getLogger().info("{SQL}  starting SQL . . .");
		try {
			this.sql = new AsyncSQL(this);
			this.hotelsSQL = new HotelsSQL(this);
			this.shopsSQL = new ShopsSQL(this);
			this.shopInvSQL = new ShopInventorySQL(this);
			this.permsSQL = new PermissionsSQL(this);
			this.catSQL = new CategoriesSQL(this);
			this.psettingSQL = new PlayerSettingsSQL(this);
			
			this.getLogger().info("{SQL}  successfully connected to Database.");
		} catch (Exception e) {
			this.getLogger().warning("{SQL}  Failed to start MySql (" + e.getMessage() + ")");
			this.isSystemRunningOkay = false; //STOP PLUGIN HERE
		}
	}

	//ECONOMY
	public Economy getEconomy() {
		return this.econ;
	}

	//PERMISSIONS
	public PermissionsAPI getPermissionsAPI() {
		return this.permsAPI;
	}

	//CHAT
	public Chat getChat() {
		return this.chat;
	}
	
	//WORLDEDIT
	public WorldEditPlugin getWorldEdit() {
		return this.worldEdit;
	}
	
	//WORLDGUARD
	public WorldGuardPlugin getWorldGuard() {
		return this.worldGuard;
	}

	// Return Classes
	public AsyncSQL getAsyncSQL() {
		return this.sql;
	}
	
	public HotelsSQL getHotelsSQL() {
		return this.hotelsSQL;
	}
	
	public ShopsSQL getShopsSQL() {
		return this.shopsSQL;
	}
	
	public ShopInventorySQL getShopsInvSQL() {
		return this.shopInvSQL;
	}
	
	public PermissionsSQL getPermissionsSQL() {
		return this.permsSQL;
	}
	
	public CategoriesSQL getCategorySQL() {
		return this.catSQL;
	}
	
	public PlayerSettingsSQL getPlayerSettingSQL() {
		return this.psettingSQL;
	}
		
	public NPCFileManager getNPCFileManager() {
		return this.npcFM;
	}
	
	public AreaFileManager getAreaFileManager() {
		return this.areaFM;
	}
	
	public UtilMethodes getMethodes() {
		return this.shopMeth;
	}
	
	public SignFileManager getSignFileManager() {
		return this.signFM;
	}
	
	public BackupManager getBackupManager() {
		return this.backupMGR;
	}
	
	public ShopItemManager getShopItemManager() {
		return this.shopItemMGR;
	}
	
	public ShopCacheFileManager getShopCacheFileManager() {
		return this.shopCacheFM;
	}
	
	public DoorFileManager getDoorFileManager() {
		return this.doorFM;
	}

	public WorldGuardUtils getWorldGuardUtils() {
		return this.wgUtils;
	}

	public NPCUtils getNpcUtils() {
		return this.npcUtils;
	}

	public VillagerUtils getVillagerUtils() {
		return this.vilUtils;
	}

	public RollbackInventoryManager getRollbackInventoryManager() {
		return rollbackInvManager;
	}
}
