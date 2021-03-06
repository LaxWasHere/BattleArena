package mc.alk.arena.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import mc.alk.arena.Defaults;
import mc.alk.arena.controllers.PlayerController;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchParams;
import mc.alk.arena.objects.teams.Team;
import mc.alk.tracker.Tracker;
import mc.alk.tracker.TrackerInterface;
import mc.alk.tracker.objects.Stat;
import mc.alk.tracker.objects.StatType;
import mc.alk.tracker.objects.WLT;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class BTInterface {
	public static Tracker battleTracker = null;
	public static TrackerInterface aBTI = null;
	static private HashMap<String, TrackerInterface> btis = new HashMap<String, TrackerInterface>();
	static private HashMap<String, TrackerInterface> currentInterfaces = new HashMap<String, TrackerInterface>();
	final TrackerInterface ti;
	boolean valid;
	public BTInterface(MatchParams mp){
		ti = getInterface(mp);
		valid = battleTracker != null && ti != null;
	}
	public boolean isValid(){
		return valid;
	}
	public Stat getRecord(TrackerInterface bti, Team t){
		try{return bti.getRecord(t.getBukkitPlayers());} catch(Exception e){e.printStackTrace();return null;}
	}
	public Stat loadRecord(TrackerInterface bti, Team t){
		try{return bti.loadRecord(t.getBukkitPlayers());} catch(Exception e){e.printStackTrace();return null;}
	}
	public static TrackerInterface getInterface(MatchParams sq){
		if (sq == null)
			return null;
		final String db = sq.getDBName();
		return db == null ? null : btis.get(db);
	}

	public static void addRecord(TrackerInterface bti, Set<Team> victors,Set<Team> losers, Set<Team> drawers, WLT win) {
		if (victors != null){
			Set<ArenaPlayer> winningPlayers = new HashSet<ArenaPlayer>();
			for (Team w : victors){
				winningPlayers.addAll(w.getPlayers());
			}
			addRecord(bti,winningPlayers, losers,win);
		}
	}
	public static void addRecord(TrackerInterface bti, Set<ArenaPlayer> players, Collection<Team> losers, WLT win) {
		if (bti == null)
			return;
		try{
			Set<Player> winningPlayers = PlayerController.toPlayerSet(players);
			if (losers.size() == 1){
				Set<Player> losingPlayers = new HashSet<Player>();
				for (Team t: losers){losingPlayers.addAll(t.getBukkitPlayers());}
				if (Defaults.DEBUG_TRACKING) System.out.println("BA Debug: addRecord ");
				for (Player p: winningPlayers){
					if (Defaults.DEBUG_TRACKING) System.out.println("BA Debug: winner = "+p.getName());}
				for (Player p: losingPlayers){
					if (Defaults.DEBUG_TRACKING) System.out.println("BA Debug: loser = "+p.getName());}
				bti.addTeamRecord(winningPlayers, losingPlayers, WLT.WIN);
			} else {
				Collection<Collection<Player>> plosers = new ArrayList<Collection<Player>>();
				for (Team t: losers){
					plosers.add(t.getBukkitPlayers());
				}
				bti.addRecordGroup(winningPlayers, plosers, WLT.WIN);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static boolean addBTI(MatchParams pi) {
		final String dbName = pi.getDBName();
		TrackerInterface bti = btis.get(dbName);
		if (bti == null){
			/// Try to first the interface from our existing ones
			bti = currentInterfaces.get(dbName);
			if (bti==null){ /// no current interface, do we even have the BattleTracker plugin?
				battleTracker = (Tracker) Bukkit.getPluginManager().getPlugin("BattleTracker");
				if (battleTracker == null) {
					/// Well BattleTracker obviously isnt enabled.. not much we can do about that
					return false;
				}
				/// yay, we have it, now get our interface
				bti = Tracker.getInterface(dbName);
				currentInterfaces.put(dbName, bti);
				if (aBTI == null)
					aBTI = bti;
			}
			if (Defaults.DEBUG) System.out.println("adding BTI for " + pi +"  " + dbName);
			btis.put(dbName, bti);
		}
		return true;
	}

	public static void resumeTracking(ArenaPlayer p) {
		if (aBTI != null)
			aBTI.resumeTracking(p.getName());
	}
	public static void stopTracking(ArenaPlayer p) {
		if (aBTI != null)
			aBTI.stopTracking(p.getName());
	}
	public static void resumeTracking(Set<Player> players) {
		if (aBTI != null)
			aBTI.resumeTracking(players);
	}
	public static void stopTracking(Set<Player> players) {
		if (aBTI != null)
			aBTI.stopTracking(players);
	}
	public static boolean hasInterface(MatchParams pi) {
		return btis.containsKey(pi.getName());
	}

	@SuppressWarnings("deprecation")
	public Integer getElo(Team t) {
		if (!isValid())
			return new Integer((int) Defaults.DEFAULT_ELO);
		Stat s = getRecord(ti,t);
		return (int) (s == null ? Defaults.DEFAULT_ELO : s.getRanking());
	}
	public Stat loadRecord(Team team) {
		if (!isValid()) return null;
		return loadRecord(ti, team);
	}
	public Stat loadRecord(OfflinePlayer player){
		if (!isValid()) return null;
		try{return ti.loadRecord(player);} catch(Exception e){e.printStackTrace();return null;}
	}

	@SuppressWarnings("deprecation")
	public String getRankMessage(OfflinePlayer player) {
		Stat stat = loadRecord(player);
		if (stat == null){
			return "&eCouldn't find stats for player " + player.getName();}
		Integer rank = ti.getRank(player.getName());
		if (rank == null)
			rank = -1;
		return "&eRank:&6"+rank+"&e (&4"+stat.getWins()+"&e:&8"+stat.getLosses()+"&e)&6["+stat.getRanking()+"]&e" +
				". Highest &6["+ stat.getMaxRanking()+"]&e Longest Streak &b"+stat.getMaxStreak();
	}
	public boolean setRanking(OfflinePlayer player, Integer elo) {
		return ti.setRanking(player, elo);
	}
	public void resetStats() {
		ti.resetStats();
	}
	public void printTopX(CommandSender sender, int x, int minTeamSize, String headerMsg, String bodyMsg) {
		ti.printTopX(sender, StatType.RANKING, x, minTeamSize,headerMsg,bodyMsg);
	}

}
