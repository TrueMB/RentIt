package me.truemb.rentit.main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.truemb.rentit.api.AdvancedChestsUtils;
import me.truemb.rentit.api.ChestShopAPI;
import me.truemb.rentit.utils.chests.ChestsUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.Acrobot.ChestShop.ChestShop;
import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import com.jeff_media.updatechecker.UserAgentBuilder;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import me.truemb.rentit.api.NPCUtils;
import me.truemb.rentit.api.PermissionsAPI;
import me.truemb.rentit.api.PlaceholderAPI;
import me.truemb.rentit.api.WorldGuardUtils;
import me.truemb.rentit.commands.FreeHotelsCOMMAND;
import me.truemb.rentit.commands.FreeShopsCOMMAND;
import me.truemb.rentit.commands.HotelCOMMAND;
import me.truemb.rentit.commands.HotelsCOMMAND;
import me.truemb.rentit.commands.RentItCOMMAND;
import me.truemb.rentit.commands.ShopCOMMAND;
import me.truemb.rentit.commands.ShopsCOMMAND;
import me.truemb.rentit.database.AsyncSQL;
import me.truemb.rentit.database.CategoriesSQL;
import me.truemb.rentit.database.HotelsSQL;
import me.truemb.rentit.database.PermissionsSQL;
import me.truemb.rentit.database.PlayerSettingsSQL;
import me.truemb.rentit.database.ShopInventorySQL;
import me.truemb.rentit.database.ShopsSQL;
import me.truemb.rentit.economy.EconomySystem;
import me.truemb.rentit.economy.PlayerPointsEconomy;
import me.truemb.rentit.economy.VaultEconomy;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.filemanager.AreaFileManager;
import me.truemb.rentit.filemanager.DoorFileManager;
import me.truemb.rentit.filemanager.NPCFileManager;
import me.truemb.rentit.filemanager.ShopCacheFileManager;
import me.truemb.rentit.filemanager.SignFileManager;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.listener.AdminHotelListener;
import me.truemb.rentit.listener.AdminShopListener;
import me.truemb.rentit.listener.CategoryGUIListener;
import me.truemb.rentit.listener.HotelAreaListener;
import me.truemb.rentit.listener.ItemBoughtListener;
import me.truemb.rentit.listener.ItemSoldListener;
import me.truemb.rentit.listener.NPCShopListener;
import me.truemb.rentit.listener.OwningListListener;
import me.truemb.rentit.listener.PlayerCommandSendListener;
import me.truemb.rentit.listener.PlayerJoinListener;
import me.truemb.rentit.listener.PlayerQuitListener;
import me.truemb.rentit.listener.RentTimeClickListener;
import me.truemb.rentit.listener.SearchResultGUIListener;
import me.truemb.rentit.listener.ShopAreaListener;
import me.truemb.rentit.listener.ShopBuyOrSellListener;
import me.truemb.rentit.listener.ShopListener;
import me.truemb.rentit.listener.ShopItemsBackupListener;
import me.truemb.rentit.listener.SignListener;
import me.truemb.rentit.listener.UserConfirmationListener;
import me.truemb.rentit.listener.VillagerShopListener;
import me.truemb.rentit.runnable.PaymentRunnable;
import me.truemb.rentit.utils.BackupManager;
import me.truemb.rentit.utils.ConfigUpdater;
import me.truemb.rentit.utils.ShopItemManager;
import me.truemb.rentit.utils.UTF8YamlConfiguration;
import me.truemb.rentit.utils.UtilMethodes;
import me.truemb.rentit.utils.VillagerUtils;
import net.citizensnpcs.api.CitizensPlugin;
import net.milkbowl.vault.chat.Chat;

public class Main extends JavaPlugin {
	
	private Chat chat;
	private WorldEditPlugin worldEdit;
	private WorldGuardPlugin worldGuard;
	
	private PermissionsAPI permsAPI;
	private EconomySystem economySystem;

	private AdvancedChestsUtils advancedChestsUtils;

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
	private ChestsUtils chestsUtils;
	
	//SOFTDEPEND
	private WorldGuardUtils wgUtils;
	private NPCUtils npcUtils;
	private VillagerUtils vilUtils;
	private ChestShopAPI chestShopApi;
	
	private NPCFileManager npcFM;
	private SignFileManager signFM;
	private ShopCacheFileManager shopCacheFM;
	private AreaFileManager areaFM;
	private DoorFileManager doorFM;
	
	private UTF8YamlConfiguration config;

	public HashMap<UUID, PlayerHandler> playerHandlers = new HashMap<>(); // UUID = playerUUID - SettingsHandler
	public HashMap<RentTypes, HashMap<Integer, CategoryHandler>> catHandlers = new HashMap<>(); // RentType = hotel/shop - int = catID -  CategoryHandler
	public HashMap<RentTypes, HashMap<Integer, RentTypeHandler>> rentTypeHandlers = new HashMap<>(); // RentType = hotel/shop - int = shop/hotel ID - RentTypeHandler
	private HashMap<UUID, ShopInventoryBuilder> shopInvBuilder = new HashMap<>(); 

	public HashMap<UUID, Material> search = new HashMap<>(); 
	
	//NAMESPACES
	public NamespacedKey guiItem = new NamespacedKey(this, "guiItem");
	public NamespacedKey idKey = new NamespacedKey(this, "ID");
	public NamespacedKey siteKey = new NamespacedKey(this, "Site");

	private static final int configVersion = 18;
    private static final String SPIGOT_RESOURCE_ID = "90195";
    private static final int BSTATS_PLUGIN_ID = 12060;
    
	private int runnId;
	
	public boolean isSystemRunningOkay = true;
	
	@Override
	public void onEnable() {
		this.manageFile();
		this.startMySql();
		
		this.shopMeth = new UtilMethodes(this);

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

		this.advancedChestsUtils = new AdvancedChestsUtils(this);
		this.chestsUtils = new ChestsUtils(this);
		this.backupMGR = new BackupManager(this);
		this.shopItemMGR = new ShopItemManager();
		this.npcFM = new NPCFileManager(this);
		this.signFM = new SignFileManager(this);
		this.shopCacheFM = new ShopCacheFileManager(this);
		this.areaFM = new AreaFileManager(this);
		this.doorFM = new DoorFileManager(this);
		
		this.permsAPI = new PermissionsAPI(this);
		this.economySystem = this.manageFile().getBoolean("Options.usePlayerPoints") ? new PlayerPointsEconomy(this) : new VaultEconomy(this);
		this.setupWorldEdit();
		this.setupWorldGuard();
		this.setupChestShop();

		if(!this.manageFile().getBoolean("Options.disableNPC")) {
			if(this.manageFile().getBoolean("Options.useNPCs")) {
				this.setupCitizens(); //CITIZENS NPC
			}else {
				this.vilUtils = new VillagerUtils(this); //VILLAGER NPC
				new VillagerShopListener(this);
			}
		}
		
		//TODO REMOVE/SET IF DisableNPC was changed
		//Maybe not needed, because Villager/NPC are getting destroyed on server stop.
				
		this.setupPlaceholderAPI();
		
		//LISTENER
		new AdminShopListener(this);
		new AdminHotelListener(this);
		new ShopListener(this);
		new SignListener(this);
		new UserConfirmationListener(this);
		new RentTimeClickListener(this);
		new SearchResultGUIListener(this);
		new OwningListListener(this);
		new PlayerCommandSendListener(this);
		new PlayerJoinListener(this);
		new PlayerQuitListener(this);
		new ItemBoughtListener(this);
		new ItemSoldListener(this);
		new ShopAreaListener(this);
		new HotelAreaListener(this);
		new CategoryGUIListener(this);
		new ShopBuyOrSellListener(this);
		new ShopItemsBackupListener(this);
		
		try{
		    Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
		    commandMapField.setAccessible(true);
		    CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

			//COMMANDS
			if(!this.manageFile().getBoolean("Options.commands.shop.disabled"))
				commandMap.register(this.getDescription().getName(), new ShopCOMMAND(this));
			
			if(!this.manageFile().getBoolean("Options.commands.hotel.disabled"))
				commandMap.register(this.getDescription().getName(), new HotelCOMMAND(this));
			
			if(!this.manageFile().getBoolean("Options.commands.shops.disabled"))
				commandMap.register(this.getDescription().getName(), new ShopsCOMMAND(this));
			
			if(!this.manageFile().getBoolean("Options.commands.hotels.disabled"))
				commandMap.register(this.getDescription().getName(), new HotelsCOMMAND(this));
			
			if(!this.manageFile().getBoolean("Options.commands.freeshops.disabled"))
				commandMap.register(this.getDescription().getName(), new FreeShopsCOMMAND(this));
			
			if(!this.manageFile().getBoolean("Options.commands.freehotels.disabled"))
				commandMap.register(this.getDescription().getName(), new FreeHotelsCOMMAND(this));
			
			if(!this.manageFile().getBoolean("Options.commands.rentit.disabled"))
				commandMap.register(this.getDescription().getName(), new RentItCOMMAND(this));
		    
		}catch(Exception ex){
		    ex.printStackTrace();
		}
		
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
		
		for(RentTypeHandler shopHandler : this.rentTypeHandlers.get(RentTypes.SHOP).values()) {
			HashMap<UUID, List<Inventory>> rollbackHash = shopHandler.getRollbackInventories();
			
			//SAVE INVENTORIES IN THE FILE
			for(UUID uuid : rollbackHash.keySet())
				this.getShopCacheFileManager().updateShopBackup(uuid, shopHandler.getID(), rollbackHash.get(uuid));
		}
		
		if(this.getVillagerUtils() != null)
			this.getVillagerUtils().disableVillagers();

		Bukkit.getScheduler().cancelTask(this.runnId);
		
		if(this.getAsyncSQL() != null && this.getAsyncSQL().getMySQL() != null && this.getAsyncSQL().getMySQL().getConnection() != null)
			this.getAsyncSQL().getMySQL().closeConnection();
		
		else if(this.getAsyncSQL() != null && this.getAsyncSQL().getSqlLite() != null && this.getAsyncSQL().getSqlLite().getConnection() != null)
			this.getAsyncSQL().getSqlLite().closeConnection();
	}
	
	public void initRestart(CommandSender sender) {

		//------ CLOSE EVERTHING ------
		Main plugin = this;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
		
				//DISABLING VILLAGERS
				if(getVillagerUtils() != null) {
					Bukkit.getScheduler().runTask(plugin, new Runnable() {
						
						@Override
						public void run() {
							getVillagerUtils().disableVillagers();
						}
					});
				}
				
				//DISABLING PAYMENT RUNNABLE
				Bukkit.getScheduler().cancelTask(runnId);
				runnId = -1;
				
				//CLOSE MYSQL CONNECTION
				if(getAsyncSQL() != null && getAsyncSQL().getMySQL() != null && getAsyncSQL().getMySQL().getConnection() != null)
					getAsyncSQL().getMySQL().closeConnection();
				
				//OR SQL LITE CONNECTION
				else if(getAsyncSQL() != null && getAsyncSQL().getSqlLite() != null && getAsyncSQL().getSqlLite().getConnection() != null)
						getAsyncSQL().getSqlLite().closeConnection();
				
				//RESET CONFIG CACHE
				config = null;
				reloadConfig();
				
				//RESET HASHES
				playerHandlers = new HashMap<>();
				catHandlers = new HashMap<>();
				rentTypeHandlers = new HashMap<>();
				
				
				//------ START EVERTHING AGAIN ------
				
				//CACHE CONFIG
				manageFile();
				
				//START MYSQL
				startMySql();
		
				if(!isSystemRunningOkay) {
					sender.sendMessage(getMessage("reloadedError"));
					return; //DATABASE MISSING
				}
				
				Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
					
					@Override
					public void run() {
						initHandlers();
					}
				}, 20);
				

				if(!manageFile().getBoolean("Options.disableNPC")) {
					if(manageFile().getBoolean("Options.useNPCs")) {
						setupCitizens(); //CITIZENS NPC
					}else {
						vilUtils = new VillagerUtils(plugin); //VILLAGER NPC
						new VillagerShopListener(plugin);
					}
				}
				
				
				//START MANAGERS
				advancedChestsUtils = new AdvancedChestsUtils(plugin);
				shopMeth = new UtilMethodes(plugin);
				chestsUtils = new ChestsUtils(plugin);
				backupMGR = new BackupManager(plugin);
				shopItemMGR = new ShopItemManager();
				npcFM = new NPCFileManager(plugin);
				signFM = new SignFileManager(plugin);
				shopCacheFM = new ShopCacheFileManager(plugin);
				areaFM = new AreaFileManager(plugin);
				doorFM = new DoorFileManager(plugin);
				
				//LOAD ONLINE PLAYERS
				Bukkit.getOnlinePlayers().forEach(p -> {
		
					PlayerHandler playerHandler = new PlayerHandler(p);
					playerHandler.init(plugin);
					
				});
				
				//START PAYMENT 5 SECONDS LATER, SO THAT EVERYTHING GETS LOADED FIRST
				Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new Runnable() {
					
					@Override
					public void run() {
						//PAYMENT SCHEDULER
						plugin.runnId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new PaymentRunnable(plugin), 20 * 10, 20 * 60).getTaskId(); //EVERY MINUTE
					}
				}, 5 * 20);

				sender.sendMessage(getMessage("reloaded"));
			}
		}).start();
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
	
	public YamlConfiguration manageFile() {
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
					List<String> ignore = new ArrayList<>();
					
					ignore.add("Options.categorySettings");
					ignore.add("Options.maxPossible");
					
					ignore.add("GUI.categoryShop.items");
					ignore.add("GUI.categoryHotel.items");
					
					ConfigUpdater.update(this, "config.yml", configFile, ignore);
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
		
		new UpdateChecker(this, UpdateCheckSource.SPIGET, SPIGOT_RESOURCE_ID)
                .setDownloadLink(SPIGOT_RESOURCE_ID) // You can either use a custom URL or the Spigot Resource ID
                .setDonationLink("https://www.paypal.me/truemb")
                .setChangelogLink(SPIGOT_RESOURCE_ID) // Same as for the Download link: URL or Spigot Resource ID
                .setNotifyOpsOnJoin(true) // Notify OPs on Join when a new version is found (default)
                .setNotifyByPermissionOnJoin(this.getDescription().getName() + ".updatechecker") // Also notify people on join with this permission
                .setUserAgent(new UserAgentBuilder().addPluginNameAndVersion())
                .checkEveryXHours(12) // Check every hours
                .checkNow(); // And check right now
        
	}

	//CHAT
	public boolean setupChat() {
		if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
			this.getLogger().warning("Vault is missing!");
			return false;
	    }
	    RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
	    if (rsp == null || rsp.getProvider() == null) {
	    	this.getLogger().warning("A Chat Plugin is missing! (Needed for Player Shop NPC Prefix)");
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
	
	//PLACEHOLDERAPI
	private void setupPlaceholderAPI() {
		
		//PLUGIN WAS FOUND
	    if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
	          new PlaceholderAPI(this).register();
			this.getLogger().info("PlacerHolderAPI was found and registered!");
	    }else {
			this.getLogger().info("PlacerHolderAPI was not found. (Is not needed, but supported)");
	    }
		
	}
	
	//CHEST SHOP API
	private void setupChestShop() {
		Plugin chestShop = this.getServer().getPluginManager().getPlugin("ChestShop");
		
		if(chestShop == null || !(chestShop instanceof ChestShop))
			return;

		this.getLogger().info("ChestShop was found!");
		this.chestShopApi = new ChestShopAPI();
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

			this.isSystemRunningOkay = true; //STOP PLUGIN HERE
			this.getLogger().info("{SQL}  successfully connected to Database.");
		} catch (Exception e) {
			this.getLogger().warning("{SQL}  Failed to start MySql (" + e.getMessage() + ")");
			this.isSystemRunningOkay = false; //STOP PLUGIN HERE
		}
	}

	public void setShopInvBuilder(UUID uuid, ShopInventoryBuilder builder) {
		this.shopInvBuilder.put(uuid, builder);
	}

	//ECONOMY
	public EconomySystem getEconomySystem() {
		return this.economySystem;
	}

	//PERMISSIONS
	public PermissionsAPI getPermissionsAPI() {
		return this.permsAPI;
	}

	//ADVANCED CHESTS PLUGIN INTEGRATION
	public AdvancedChestsUtils getAdvancedChestsUtils() {
		return this.advancedChestsUtils;
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

	public ChestsUtils getChestsUtils() {
		return this.chestsUtils;
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

	public ChestShopAPI getChestShopApi() {
		return this.chestShopApi;
	}

	public ShopInventoryBuilder getShopInvBuilder(UUID uuid) {
		return this.shopInvBuilder.get(uuid);
	}
}
