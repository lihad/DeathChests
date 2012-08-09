package com.Belkar.DeathChest;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class DeathChestEventListener implements Listener {

	private DeathChest plugin;

	public DeathChestEventListener(DeathChest deathChest) {
		this.plugin = deathChest;
		
		// Register the EventListener
		this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (player == null)
			return;
		
		//TODO: Editable settings for EXP/concurrency loss
		
		// Only proceed if the player is allowed to drop DeathChests
		if (!player.hasPermission("deathchest.use")) {
			return;
		}
		
		// Get the items from the player and convert them into a usable form.
		List<ItemStack> inv = event.getDrops();
		ItemStack[] inventory = new ItemStack[inv.size()];
		inventory = inv.toArray(inventory);
		
		// Count the used inventory spaces and look if there are enough chests
		int countStacks = Utils.getStacks(player.getInventory());
		boolean doubleChest = false;
		boolean free = player.hasPermission("deathchest.use.free");
		if (!player.getInventory().contains(Material.CHEST) && !free) {
			return;
		}
		event.getDrops().clear();
		
		// Check if a DoubleChest is needed and possible to create.
		if (countStacks > 9 * 3 && (free || player.getInventory().contains(Material.CHEST, 2))) {
			doubleChest = true;
		}
		
		int needed = (doubleChest ? 2 : 1);
		int countChest = 0;
		
		// Remove the chests from the players inventory
		if (!free) {
			for (int i = 0; i < inventory.length && needed > 0; i++) {
				ItemStack item = inventory[i];
				// Skip empty inventory spaces
				if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
					if (item.getType() == Material.CHEST) {
						// If the stack is bigger than needed, substract from the stack and set needed to 0 (since there are enough to use)
						if (item.getAmount() >= needed) {
							item.setAmount(item.getAmount() - needed);
							countChest += needed;
							needed = 0;
						// if the stack is less than needed, remove the stack and substract the stack-size from needed
						} else {
							needed -= item.getAmount();
							countChest += item.getAmount();
							item.setAmount(0);
						}
					}
				}
			}
		}
		
		// Recheck if the chests are available and place them if possible.
		if (countChest > 0 || free) {
			plugin.addChest(player, inventory, countChest);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		boolean sign = false;
		
		// Event only should trigger if DeathChest-Items are affected
		if (block != null && block.getType() == Material.CHEST || (sign = (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST))) {
			// Get the hit block. Stop handling the event if it doesn't belong to a DeathChest
			Tombstone stone = plugin.getDeathChestAt(block.getLocation());
			if (stone == null)
				return;

			// If the broken block is a sign, swallow it (since those signs are created from nothing)
			// else remove the chest from the Container (no more protection)
			if (sign) { // Swallow the drops
				block.setType(Material.AIR);
			} else {
				plugin.removeChest(stone);
			}
		}
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		Block block = event.getBlock();
		if (block != null && block.getType() == Material.CHEST || block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
			// Get the hit block. Stop handling the event if it doesn't belong to a DeathChest
			Tombstone stone = plugin.getDeathChestAt(block.getLocation());
			if (stone == null)
				return;
			
			// Get the owner and the attacker (of the DeathChest)
			Player owner = stone.getOwner();
			Player player = event.getPlayer();
			
			// Only allow the attacker to damage the block if it is the owner or it has the permissions to damage the Chest 
			if (!player.hasPermission("deathchest.breakOthers") || owner == null || !(player.equals(owner))) {
				if (player != null) {
					player.sendMessage("You are not allowed to break this tombstone! It belongs to: " + owner.getName());
				}				
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onBlockInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (player == null)
			return;
		
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block block = player.getTargetBlock(null, 4);
			if (block != null && block.getType() == Material.CHEST || block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
				// Get the hit block. Stop handling the event if it doesn't belong to a DeathChest
				Tombstone stone = plugin.getDeathChestAt(block.getLocation());
				if (stone == null)
					return;

				// Get the owner (of the DeathChest)
				Player owner = stone.getOwner();
				
				// Only allow the owner or those who are permitted to loot/break others DeathChests
				if (!player.hasPermission("deathchest.breakOthers") || owner == null || !(player.equals(owner))) {
					player.sendMessage("You are not allowed to open this tombstone! It belongs to: " + owner.getName());
					event.setCancelled(true);
				} else {
					// If the player is also sneaking, invoke the auto-transfer method
					if (player.isSneaking()) {
						plugin.playerPickupTombstone(player, stone);
					}
				}
			}	
		}
	}
}
