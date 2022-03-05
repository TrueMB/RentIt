package me.truemb.rentit.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;

public class BackupManager {
	
	private Main instance;
	
	private File dataDirectory;
	
	public BackupManager(Main plugin) {
		this.instance = plugin;
        this.dataDirectory = new File (this.instance.getDataFolder(), "backups");
        if(!this.dataDirectory.exists())
        	this.dataDirectory.mkdir();
	}
	
	public void save(RentTypes type, int id, BlockVector3 min, BlockVector3 max, World world) {
		
		Bukkit.getScheduler().runTask(this.instance, new Runnable() {
			
			@Override
			public void run() {
				
				File file = new File(dataDirectory, type.toString().toLowerCase() + "_" + String.valueOf(id) + ".schem"); // The schematic file
		        
				CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(world), min, max);
				BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

				com.sk89q.worldedit.world.World weWorld = region.getWorld();
				        
				try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
					
					ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
					forwardExtentCopy.setCopyingEntities(true);
					
					Operations.complete(forwardExtentCopy);
				} catch (WorldEditException ex) {
					ex.printStackTrace();
				}
				
				try (@SuppressWarnings("deprecation") //TO SUPPORT LOWERE WORLDEDIT VERSIONS
				ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
					writer.write(clipboard);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
		});
    }
	
	public void paste(RentTypes type, int id, BlockVector3 min, BlockVector3 max, World world, boolean ignoreAir) {
		
		Bukkit.getScheduler().runTask(this.instance, new Runnable() {
			
			@Override
			public void run() {
				
		        File file = new File(dataDirectory, type.toString().toLowerCase() + "_" + String.valueOf(id) + ".schem"); // The schematic file
		        
		        ClipboardFormat format = ClipboardFormats.findByFile(file);
		        Clipboard clipboard = null;
		        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
		           clipboard = reader.read();
		        } catch (IOException ex) {
					ex.printStackTrace();
				}
		        
		        for(int x = min.getBlockX(); x <= max.getBlockX(); x++)
		            for(int y = min.getBlockY(); y <= max.getBlockY(); y++)
		                for(int z = min.getBlockZ(); z <= max.getBlockZ(); z++)
		                	for(Entity ent : world.getNearbyEntities(new Location(world, x, y, z), 1, 1, 1))
		                		if(!(ent instanceof Player))
		                			ent.remove();
		
		        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
		        
		        
		        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
		            Operation operation = new ClipboardHolder(clipboard)
		                    .createPaste(editSession)
		                    .to(min)
		                    .ignoreAirBlocks(ignoreAir)
		                    .copyEntities(true)
		                    .build();
		            Operations.complete(operation);
		        }catch (WorldEditException ex) {
		            ex.printStackTrace();
			    }
			}
			
		});
	}
	
	public boolean deleteSchem(RentTypes type, int id) {
		
        File file = new File(this.dataDirectory, type.toString().toLowerCase() + "_" + String.valueOf(id) + ".schematic"); // The schematic file
        
        if(file.exists())
        	return file.delete();
        return false;
	}
}
