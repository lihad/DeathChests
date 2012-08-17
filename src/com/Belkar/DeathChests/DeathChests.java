package com.Belkar.DeathChests;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class DeathChests extends JavaPlugin {
	// Where the chests get saved
	private static final String CHEST_FILE = "chests.xml";

	// Every side of the chest and its prioity
	private static final Vector[] SIDE_CHEST_VECTORS = new Vector[] {new Vector(1, 0, 0), new Vector(0, 0, 1), new Vector(-1, 0, 0), new Vector(0, 0, -1) };
//	private static final Vector[] ROUND_CHEST_VECTORS = new Vector[] {new Vector(1, 0, 0), new Vector(0, 0, 1), new Vector(-1, 0, 0), new Vector(0, 0, -1), new Vector(0, 1, 0)};
	
	// Listener for everything
	@SuppressWarnings("unused")
	private DeathChestEventListener listener;
	// List of all Tombstones (to check events, protection, etc.)
	private List<Tombstone> deathChests = null;

	// The Task ID of the autosaver (not really needed but for safety)
	private int autosaveTaskId = -1;
	
	@Override
	public void onDisable() {
		saveConfig();
		
		super.onDisable();
		getLogger().info(this.getDescription().getFullName() + " v" + this.getDescription().getVersion() + "disabled successfully!");
	}
	
	@Override
	public void onEnable() {
		// Generate plugin folders
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdirs();
		}
		final String defaultConf = getDataFolder() + File.separator + "config.yml";
		
		this.getServer().getScheduler().scheduleAsyncDelayedTask(this, new Runnable() {
			
			@Override
			public void run() {
		        if (!(new File(defaultConf).exists())) {
		        	saveDefaultConfig();
		        } else {
		        	reloadConfig();
		        }
			}
		}, 20L);
		
        // Create and register the EventListener
		listener = new DeathChestEventListener(this);
		
		super.onEnable();
		getLogger().info(this.getDescription().getFullName() + "enabled successfully!");
	}

	@Override
	public void reloadConfig() {
		this.getServer().getScheduler().cancelTasks(this);
		this.autosaveTaskId = -1;
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdirs();
		}
		super.reloadConfig();
		
		// Read or re-read the settings
		Settings.loadConfig(this.getConfig());

		// Readout all the chests from the filesystem
		String chestsPath = getDataFolder() + File.separator + CHEST_FILE;
		if (deathChests != null) {
			saveChests(chestsPath);
		} else {
			File chestsFile = new File(chestsPath);
			boolean loaded = false;
			if (chestsFile.exists()) {
				deathChests = Utils.loadTombstone(chestsPath);
//				deathChests = Utils.load(chestsPath);
				loaded = true;
			}
			if (!loaded) {
				deathChests = new LinkedList<>();
			}
		}
		
		// Start the autosaver task
		startAutosave(Settings.AUTOSAVE_PERIOD, chestsPath);
		
	}
	/**Start the autosave-task (kill it if its already running and replace it)
	 * @param autosave TaskID of the async-task
	 * @param chestsPath Path of the chests-file
	 */
	private void startAutosave(int autosave, final String chestsPath) {
		if (this.autosaveTaskId < 0) {
			this.getServer().getScheduler().cancelTask(this.autosaveTaskId);
		}

		// Check if correct path
		if (chestsPath == null || chestsPath.isEmpty())
			throw new InvalidParameterException();
		
		this.autosaveTaskId = this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				saveChests(chestsPath);
			}
		}, Settings.AUTOSAVE_PERIOD * 20, Settings.AUTOSAVE_PERIOD * 20);
	}
	
	/**
	 *  Saves the chest-file to its default path
	 */
	private void saveChests() {
		saveChests(this.getDataFolder() + File.separator + CHEST_FILE);
	}
	/**Saves the chest-file to the specified path
	 * @param chestsPath Path to the chest-file
	 */
	private void saveChests(String chestsPath) {
		// Check if correct path
		if (chestsPath == null || chestsPath.isEmpty())
			throw new InvalidParameterException();
		
		synchronized (deathChests) {
			try {
				Utils.saveTombstones(deathChests, chestsPath);
//				Utils.save(deathChests, chestsPath);
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
	}

	@Override
	public void saveConfig() {
		Settings.saveConfig(getConfig());
		if (deathChests != null) {
			saveChests();
		}
		
		super.saveConfig();
	}
	
	/**Adds a new chests to the plugins dictionary and creates them in the world, sets the listeners and transfers the inventory into it.
	 * @param owner The Owner of the DeathChest
	 * @param inventory The full inventory of the Owner (can't be gathered later since now it's already dropped)
	 * @param countChest Amount of available chests (1 = singleChest, 2 = doubleChest)
	 */
	public void addChest(Player owner, ItemStack[] inventory, int countChest, boolean free) {
		if (owner == null || inventory == null)
			throw new NullPointerException();
		if (countChest <= 0 || countChest > 2)
			throw new IndexOutOfBoundsException();
		
		// List of special blocks (like the one that get broken by placing the chests
		List<ItemStack> additionalBlocks = new LinkedList<>();
//		this.getLogger().info("Adding chest");
		
		// Position of the first chest
		Location chestLoc = owner.getLocation().clone();
		Location chest2Loc = null;
		World world = owner.getWorld();
		Block chestBlock = world.getBlockAt(chestLoc);
		
		// Break the block if it isn't AIR (and add the content to the chest)
		if (chestBlock.getType() != Material.AIR) {
			additionalBlocks.addAll(chestBlock.getDrops(new ItemStack(Material.DIAMOND_PICKAXE)));
		}
		
		// Put down the first chest
		chestBlock.setType(Material.CHEST);
		
		// Put down the second chest if there is the possibility or must to place one.
		if (countChest > 1) {
			// Check what side is empty so it can be used (without breaking anything)
			boolean found = false;
			Block side = null;
			Location sideLoc = null;
			for (int i = 0; !found && i < SIDE_CHEST_VECTORS.length; i++) {
				sideLoc = chestLoc.clone().add(SIDE_CHEST_VECTORS[i]);
				if ((side = world.getBlockAt(sideLoc)).getType() == Material.AIR) {
					found = true;
					side.setType(Material.CHEST);
					chest2Loc = sideLoc;
				}
			}
			
			// All 4 sides are blocked! Use the side with highest priority and break the block there (and add it to the chest)
			if (!found) {
				sideLoc = chestLoc.clone().add(SIDE_CHEST_VECTORS[0]);
				chest2Loc = sideLoc;
				side = world.getBlockAt(sideLoc);
				side.setType(Material.CHEST);
				additionalBlocks.addAll(side.getDrops(new ItemStack(Material.DIAMOND_PICKAXE)));
			}
		}
		
		// Get the (double)chests inventory 
		Chest chest = (Chest) (chestBlock.getState());
		Inventory chestInv = chest.getInventory();	

		// Put every item of the players inventory into the chest and save those that doesn't fit
		Collection<ItemStack> itemsLeft = chestInv.addItem(inventory).values();
		
		// Drop all items that didn't fit into the chest
		for (ItemStack item : itemsLeft) {
			if (item != null && item.getType() != Material.AIR && item.getAmount() > 0)
				owner.getWorld().dropItemNaturally(owner.getLocation(), item);			
		}
		
		// If there are some additional items left (like the ones that got added at breaking the block to make place for the chest)
		if (additionalBlocks.size() > 0) {
			ItemStack[] additionalArray = new ItemStack[additionalBlocks.size()];
			additionalArray = additionalBlocks.toArray(additionalArray);
			
			// add them to the chest
			itemsLeft = chestInv.addItem(additionalArray).values();
			
			// and spill all out that doesn't fit.
			for (ItemStack item : itemsLeft) {
				owner.getWorld().dropItemNaturally(owner.getLocation(), item);			
			}
		}
		
		// Convert the positions of the Chests into Vectors (for easier storing and comparing)
		Vector chestPos = chestLoc.toVector();
		Vector chest2Pos = (chest2Loc == null ? null : chest2Loc.toVector());
		
		// Calculate the position of the sign
		Vector signPosition = null;
		if (owner.hasPermission("deathchest.use.sign")) {
			signPosition = getSignPos(chestLoc, chest2Loc);
			
			// If a position for a sign was found
			if (signPosition != null) {
				Block signBlock = world.getBlockAt(signPosition.getBlockX(), signPosition.getBlockY(), signPosition.getBlockZ());
				
				// Signs only get placed if there is AIR 
				if (signBlock.getType() == Material.AIR) {
					Material signType = (signPosition.getBlockY() > chestLoc.getBlockY()) ? Material.SIGN_POST : Material.WALL_SIGN;
					
					// Put down the sign
					signBlock.setType(signType);
	
					// Get the chest with the sign
					Vector signChest = chestPos;
					if (chest2Pos != null) {
						if (signPosition.distance(chest2Pos) < signPosition.distance(chestPos)) {
							signChest = chest2Pos;
						}
					}
	
					// Set the correct orientation of the sign
					signBlock.setData(getSignOrientation(signChest, signPosition));
					
					// Name the sign
					Sign sign = (Sign) (signBlock.getState());
					sign.setLine(0, owner.getName());
					sign.update();
				}
			}
		}
		
		//TODO: Maybe also save EXP (probably not)
		
		// Add the newly created DeathChest to the collection
		Tombstone stone = new Tombstone(owner, owner.getWorld().getName(), chestPos, chest2Pos, signPosition);
		if (free) {
			stone.setDropChests();
		}
		synchronized (deathChests) {
			deathChests.add(stone);
		}
		
		// Tell the server
		getLogger().info("Added DeathChest of " + stone.getOwnerName() + " at " + stone.getStringLoc());
		
		// Save the chests
		saveChests();
	}

	/**Calculates the orientation of the chest sign
	 * @param chestPos Position of the chest the sign is attached to
	 * @param signPos Position of the sign itselfs
	 * @return dataValue of the specific Orientation
	 */
	private byte getSignOrientation(Vector chestPos, Vector signPos) {
		if (chestPos == null || signPos == null)
			throw new NullPointerException();
		int chestX = chestPos.getBlockX();
		int signX = signPos.getBlockX();
		int chestZ = chestPos.getBlockZ();
		int signZ = signPos.getBlockZ();
		
		if (signX > chestX) {
//			System.out.println("[DEBUG] 1.");
			return Utils.SIGN_EAST;
		} else if (signX < chestX) {
//			System.out.println("[DEBUG] 2.");
			return Utils.SIGN_WEST;
		}
		if (signZ > chestZ) {
//			System.out.println("[DEBUG] 3.");
			return Utils.SIGN_SOUTH;
		} else {
//			System.out.println("[DEBUG] 4.");
			return Utils.SIGN_NORTH;
		}
	}

	/**Calculates the position of the sign if there is an empty position around the chests
	 * @param chestLoc Position of the main chest
	 * @param chest2Loc Position of the second chest (if it's an double chest)
	 * @return The position of the sign or NULL if there isn't any position free
	 */
	private Vector getSignPos(Location chestLoc, Location chest2Loc) {
		if (chestLoc == null)
			throw new NullPointerException();
		
		World world = chestLoc.getWorld();
		Vector sign = null;
		
		// Check both chests
		for (int cnt = 0; sign == null && cnt < 2; cnt++) {
			Location chest = (cnt == 0) ? chestLoc : chest2Loc;
			// Check every side
			for (int i = 0; chest != null && sign == null && i < SIDE_CHEST_VECTORS.length; i++) {
				Location signPos = chest.clone().add(SIDE_CHEST_VECTORS[i]);
				
				if (world.getBlockAt(signPos).getType() == Material.AIR) {
					sign = signPos.toVector();
				}
			}			
		}
		
		// If now position was found, use the spot on top of the chest(s) if they are empty.
		if (sign == null) {
			Location signPos = chestLoc.clone().add(0, 1, 0);
			if (world.getBlockAt(signPos).getType() == Material.AIR) {
				return sign = signPos.toVector();
			}
			if (chest2Loc != null) {
				signPos = chest2Loc.clone().add(0, 1, 0);
				if (world.getBlockAt(signPos).getType() == Material.AIR) {
					return sign = signPos.toVector();
				}
			}
		}
		
		return sign;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		if (args.length <= 0)
			args = new String[]{"version"};
		if (args[0].equalsIgnoreCase("reload")) {
			this.reloadConfig();
			sender.sendMessage("Config reloaded!");
		} else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("version")) {
			sender.sendMessage(this.getDescription().getFullName() + " by " + this.getDescription().getAuthors().get(0));
		} else if (args[0].equalsIgnoreCase("clear")) {
			// Filter for clearing the DeathChest-Container
			String player = null;
			if (args.length >= 2) {
				player = args[1];
			}
			
			int cnt = 0;
			for (int i = 0; i < deathChests.size();) {
				if (player != null && !deathChests.get(i).getOwnerName().equals(player)) {
					i++;
					continue;
				}
				removeChest(i);
				cnt++;
			}
			saveChests();
			
			if (cnt == 0) {
				sender.sendMessage("No chests found" + (player != null? " for player " + player : "") + "!");
			} else {
				sender.sendMessage("Removed " + cnt + " DeathChests!");
			}
		} else { // Unknown Command
			sender.sendMessage("Please look at: http://dev.bukkit.org/server-mods/deathchests/");
		}
//		else if (args[0].equals("dev") && sender instanceof Player) {
//			sender.sendMessage(Integer.toString(Utils.getStacks((Player)sender)));
//		}
		
		return true;
	}

	/**Removes a chest at the specified index
	 * @param index
	 */
	private void removeChest(int index) {
		Tombstone stone = deathChests.get(index);
		removeChest(stone);
	}

	/**Removes the specified chest
	 * @param stone The DeathChest to remove
	 */
	public void removeChest(Tombstone stone) {
		if (stone == null)
			throw new NullPointerException();
		if (!deathChests.contains(stone)) 
			throw new InvalidParameterException("The specified DeathChest doesn't exist!");
		
		World world = this.getServer().getWorld(stone.getWorld());
		
		// If the chest has a name-sign, remove it (without drops)
		Location signPos = stone.getSignLoc(world);
		if (signPos != null) { 
			world.getBlockAt(signPos).setType(Material.AIR);
		}
		
		// If the chest is empty, break them and drop them on the ground
		if (Utils.getStacks(stone.getInventory()) <= 0) {
			Location chestLoc = stone.getChestLoc(world);
			world.getBlockAt(chestLoc).setType(Material.AIR);
			
			// Only drop the items if they aren't picked up yet (using the Shift+Click method)
			if (!stone.isDropped()) {
				world.dropItemNaturally(chestLoc, new ItemStack(Material.CHEST, 1));
			}
			
			// Also remove the second part if there is any
			if (stone.isDoubleChest()) {
				chestLoc = stone.getChest2Loc(world);
				world.getBlockAt(chestLoc).setType(Material.AIR);
				if (!stone.isDropped()) {
					world.dropItemNaturally(chestLoc, new ItemStack(Material.CHEST, 1));
				}
			}
		}
		
		// Completly remove the chest from the Container
		synchronized (deathChests) {
			deathChests.remove(stone);
			getLogger().info("Removed DeathChest of " + stone.getOwnerName() + " at " + stone.getStringLoc());
		}
		saveChests();
	}

	
	/**Gets the DeathChest at a certain position (if there is any)
	 * @param location Position to look at
	 * @return The DeathChest that is positioned there or NULL if nothing was found there.
	 */
	public Tombstone getDeathChestAt(Location location) {
		String world = location.getWorld().getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		for (Tombstone stone : deathChests) {
			if (stone.getWorld().equalsIgnoreCase(world)) {
				if (stone.isChest(x,y,z)) {
					return stone;
				}
			}
		}
		
		return null;
	}

	/**Move the contents of the DeathChest into the Inventory of the Player
	 * @param player Player that picks up the chest
	 * @param stone The chest that gets picked up
	 */
	public void playerPickupTombstone(Player player, Tombstone stone) {
		if (player == null || stone == null)
			throw new NullPointerException();
		
		// Put the contents of the chests into the players inventory
		Inventory chest = stone.getInventory();
		ItemStack[] chestInventory = Utils.compressInventorArray(chest.getContents());
		if (chestInventory.length > 0) {
			Collection<ItemStack> left = player.getInventory().addItem(chestInventory).values();
			chest.clear();
			// If the don't fit, put them back into the chest
			if (left.size() > 0) {
				chest.addItem(Utils.collectionToArray(left));
			}
		}
		
		// If the Chests are empty, put them into the players inventory or drop them on the ground.
		if (Utils.getStacks(chest) <= 0) {
			if (!stone.isDropped()) {
				List<ItemStack> additionalItems = new LinkedList<>(player.getInventory().addItem(stone.getChests()).values());
				if (additionalItems.size() > 0) {
					player.getWorld().dropItem(player.getLocation(), additionalItems.get(0));
				}
				stone.setDropChests();
			}
			
			// Remove the chests from the world and the DeathChest-Container
			removeChest(stone);
		}
	}
}
