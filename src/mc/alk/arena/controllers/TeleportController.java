package mc.alk.arena.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mc.alk.arena.BattleArena;
import mc.alk.arena.Defaults;
import mc.alk.arena.listeners.BAPlayerListener;
import mc.alk.arena.util.InventoryUtil;
import mc.alk.arena.util.Log;
import mc.alk.arena.util.PermissionsUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;


public class TeleportController implements Listener{
	static Set<String> teleporting = Collections.synchronizedSet(new HashSet<String>());
	private final int TELEPORT_FIX_DELAY = 15; // ticks

	///TODO remove these work around teleport hacks when bukkit fixes the invisibility on teleport issue
	/// modified from the teleportFix2 found online
	public static void teleportPlayer(final Player player, final Location location, final boolean wipe, boolean giveBypassPerms){
		if (!player.isOnline() || player.isDead()){
			if (Defaults.DEBUG)Log.warn(BattleArena.getPName()+" Offline teleporting Player=" + player.getName() + " loc=" + location +":"+ wipe);
			BAPlayerListener.teleportOnReenter(player.getName(),location);
			if (wipe){
				InventoryUtil.clearInventory(player);}
			return;
		}
		teleport(player,location,giveBypassPerms);
	}

	public static void teleport(final Player player, final Location location){
		teleport(player,location,false);
	}

	public static void teleport(final Player player, final Location location, boolean giveBypassPerms){
		Location loc = location.clone();
		loc.setY(loc.getY() + Defaults.TELEPORT_Y_OFFSET);
		teleporting(player,true);
		/// Close their inventory so they arent taking things in/out
		InventoryUtil.closeInventory(player);
		player.setFireTicks(0);

		/// Deal with vehicles
		if (player.isInsideVehicle()){
			try{ player.leaveVehicle(); } catch(Exception e){}
		}

		/// Load the chunk if its not already loaded
		try {
			if(!loc.getWorld().isChunkLoaded(loc.getBlock().getChunk())){
				loc.getWorld().loadChunk(loc.getBlock().getChunk());}
		} catch (Exception e){}

		/// MultiInv and Multiverse-Inventories stores/restores items when changing worlds
		/// or game states ... lets not let this happen
		PermissionsUtil.givePlayerInventoryPerms(player);
		/// Give bypass perms for Teleport checks like noTeleport, and noChangeWorld
		if (giveBypassPerms && BattleArena.getSelf().isEnabled())
			player.addAttachment(BattleArena.getSelf(), Defaults.TELEPORT_BYPASS_PERM, true, 1);

		if (!player.teleport(loc)){
			if (Defaults.DEBUG)Log.warn("[BattleArena] Couldnt teleport player=" + player.getName() + " loc=" + loc);}
	}

	private static void teleporting(Player player, boolean isteleporting){
		if (isteleporting){
			teleporting.add(player.getName());
		} else {
			teleporting.remove(player.getName());
		}
	}

	/**
	 * This prevents other plugins from cancelling the teleport
	 * removes the player from the set after allowing the tp
	 * @param event
	 */
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerTeleport(PlayerTeleportEvent event){
		if (teleporting.remove(event.getPlayer().getName())){
			event.setCancelled(false);

			final Player player = event.getPlayer();
			final Server server = Bukkit.getServer();
			final Plugin plugin = BattleArena.getSelf();
			final int visibleDistance = server.getViewDistance() * 16;
			// Fix the visibility issue one tick later
			server.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					// Refresh nearby clients
					final List<Player> nearby = getPlayersWithin(player, visibleDistance);
					// Hide every player
					updateEntities(player, nearby, false);
					// Then show them again
					server.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
						@Override
						public void run() {
							updateEntities(player, nearby, true);
						}
					}, 2);
				}
			}, TELEPORT_FIX_DELAY);

		}
	}

	private void updateEntities(Player tpedPlayer, List<Player> players, boolean visible) {
		// Hide or show every player to tpedPlayer
		// and hide or show tpedPlayer to every player.
		for (Player player : players) {
			if (visible){
				tpedPlayer.showPlayer(player);
				player.showPlayer(tpedPlayer);
			} else {
				tpedPlayer.hidePlayer(player);
				player.hidePlayer(tpedPlayer);
			}
		}
	}

	private List<Player> getPlayersWithin(Player player, int distance) {
		List<Player> res = new ArrayList<Player>();
		int d2 = distance * distance;
		for (Player p : Bukkit.getOnlinePlayers()) {
			try{
				if (p.getWorld().getUID() == player.getWorld().getUID() &&
						p != player && p.getLocation().distanceSquared(player.getLocation()) <= d2) {
					res.add(p);
				}
			} catch(IllegalArgumentException e){}
		}
		return res;
	}

}
