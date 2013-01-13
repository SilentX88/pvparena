package net.slipcor.pvparena.classes;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.commands.PAA_Region;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.events.PAStartEvent;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.loadables.ArenaRegionShape;
import net.slipcor.pvparena.loadables.ArenaRegionShape.RegionType;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.InventoryManager;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.StatisticsManager;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.ncloader.NCBLoadable;
import net.slipcor.pvparena.runnables.InventoryRefillRunnable;
import net.slipcor.pvparena.runnables.PVPActivateRunnable;
import net.slipcor.pvparena.runnables.SpawnCampRunnable;

/**
 * <pre>PVP Arena Check class</pre>
 * 
 * This class parses a complex check.
 * 
 * It is called staticly to iterate over all needed/possible modules
 * to return one committing module (inside the result) and to make
 * modules listen to the checked events if necessary
 * 
 * @author slipcor
 * 
 * @version v0.10.2
 */

public class PACheck {
	private int priority = 0;
	private String error = null;
	private String modName = null;
	private static Debug db = new Debug(9);
	
	/**
	 * 
	 * @return the error message
	 */
	public String getError() {
		return error;
	}

	/**
	 * 
	 * @return the module name returning the current result
	 */
	public String getModName() {
		return modName;
	}
	
	/**
	 * 
	 * @return the PACR priority
	 */
	public int getPriority() {
		return priority;
	}
	
	/**
	 * 
	 * @return true if there was an error
	 */
	public boolean hasError() {
		return error != null;
	}
	
	/**
	 * set the error message
	 * @param error the error message
	 */
	public void setError(NCBLoadable loadable, String error) {
		modName = loadable.getName();
		db.i(modName + " is setting error to: " + error);
		this.error = error;
		this.priority += 1000;
	}
	
	/**
	 * set the priority
	 * @param priority the priority
	 */
	public void setPriority(NCBLoadable loadable, int priority) {
		modName = loadable.getName();
		db.i(modName + " is setting priority to: " + priority);
		this.priority = priority;
	}
	
	public static boolean handleCommand(Arena arena, CommandSender sender, String[] args) {
		int priority = 0;
		PACheck res = new PACheck();
		
		ArenaGoal commit = null;
		
		for (ArenaGoal mod : arena.getGoals()) {
			res = mod.checkCommand(res, args[0]);
			if (res.getPriority() > priority && priority >= 0) {
				// success and higher priority
				priority = res.getPriority();
				commit = mod;
			} else if (res.getPriority() < 0 || priority < 0) {
				// fail
				priority = res.getPriority();
				commit = null;
			}
		}
		
		if (res.hasError()) {
			arena.msg(Bukkit.getConsoleSender(), Language.parse(MSG.ERROR_ERROR, res.getError()));
			return false;
		}
		if (commit == null) {
			for (ArenaModule am : arena.getMods()) {
				if (am.checkCommand(args[0].toLowerCase())) {
					am.commitCommand(sender, args);
					return true;
				}
			}
			
			return false;
		}
		
		commit.commitCommand(sender, args);
		return true;
	}
	
	public static boolean handleEnd(Arena arena, boolean force) {
		int priority = 0;
		PACheck res = new PACheck();
		
		ArenaGoal commit = null;
		
		for (ArenaGoal mod : arena.getGoals()) {
			res = mod.checkEnd(res);
			if (res.getPriority() > priority && priority >= 0) {
				// success and higher priority
				priority = res.getPriority();
				commit = mod;
			} else if (res.getPriority() < 0 || priority < 0) {
				// fail
				priority = res.getPriority();
				commit = null;
			}
		}
		
		if (res.hasError()) {
			arena.msg(Bukkit.getConsoleSender(), Language.parse(MSG.ERROR_ERROR, res.getError()));
			return false;
		}
		
		if (commit == null) {
			return false;
		}
		
		commit.commitEnd(force);
		return true;
	}
	
	public static int handleGetLives(Arena arena, ArenaPlayer ap) {
		PACheck res = new PACheck();
		int priority = 0;
		for (ArenaGoal mod : arena.getGoals()) {
			res = mod.getLives(res, ap);
			if (res.getPriority() > priority && priority >= 0) {
				// success and higher priority
				priority = res.getPriority();
			} else if (res.getPriority() < 0 || priority < 0) {
				// fail
				priority = res.getPriority();
			}
		}
		
		if (res.hasError()) {
			return Integer.valueOf(res.getError());
		}
		return 0;
	}

	public static void handleInteract(Arena arena, Player player, Cancellable event, Block clickedBlock) {

		int priority = 0;
		PACheck res = new PACheck();
		
		ArenaGoal commit = null;
		
		for (ArenaGoal mod : arena.getGoals()) {
			res = mod.checkInteract(res, player, clickedBlock);
			if (res.getPriority() > priority && priority >= 0) {
				// success and higher priority
				priority = res.getPriority();
				commit = mod;
			} else if (res.getPriority() < 0 || priority < 0) {
				// fail
				priority = res.getPriority();
				commit = null;
			}
		}
		
		if (res.hasError()) {
			arena.msg(Bukkit.getConsoleSender(), Language.parse(MSG.ERROR_ERROR, res.getError()));
			return;
		}
		
		if (commit == null) {
			return;
		}
		
		event.setCancelled(true);
		
		commit.commitInteract(player, clickedBlock);
	}

	public static void handleJoin(Arena arena, CommandSender sender, String[] args) {
		int priority = 0;
		PACheck res = new PACheck();
				
			ArenaModule commModule = null;
			
			for (ArenaModule mod : arena.getMods()) {
				res = mod.checkJoin(sender, res, true);
				if (res.getPriority() > priority && priority >= 0) {
					// success and higher priority
					priority = res.getPriority();
					commModule = mod;
				} else if (res.getPriority() < 0 || priority < 0) {
					// fail
					priority = res.getPriority();
					commModule = null;
				}
			}
			
			if (commModule != null) {
				if (!ArenaManager.checkJoin((Player) sender, arena)) {
					res.setError(commModule, Language.parse(MSG.ERROR_JOIN_REGION));
				}
			}
			
			if (res.hasError() && !res.getModName().equals("LateLounge")) {
				arena.msg(sender, Language.parse(MSG.ERROR_ERROR, res.getError()));
				return;
			}
			
			if (res.hasError()) {
				arena.msg(sender, Language.parse(MSG.NOTICE_NOTICE, res.getError()));
				return;
			}
			
			ArenaGoal commGoal = null;
			
			for (ArenaGoal mod : arena.getGoals()) {
				res = mod.checkJoin(sender, res, args);
				if (res.getPriority() > priority && priority >= 0) {
					// success and higher priority
					priority = res.getPriority();
					commGoal = mod;
				} else if (res.getPriority() < 0 || priority < 0) {
					// fail
					priority = res.getPriority();
					commGoal = null;
				}
			}
			
			if (commGoal != null) {
				if (!ArenaManager.checkJoin((Player) sender, arena)) {
					res.setError(commGoal, Language.parse(MSG.ERROR_JOIN_REGION));
				}
			}
			
			if (res.hasError()) {
				arena.msg(sender, Language.parse(MSG.ERROR_ERROR, res.getError()));
				return;
			}
			
			if (args.length < 1 || (arena.getTeam(args[0]) == null)) {
				// usage: /pa {arenaname} join | join an arena

				args = new String[]{TeamManager.calcFreeTeam(arena)};
			}
			
			ArenaTeam team = arena.getTeam(args[0]);
			
			if (team == null && args != null) {
				arena.msg(sender, Language.parse(MSG.ERROR_TEAMNOTFOUND, args[0]));
				return;
			} else if (team == null) {
				arena.msg(sender, Language.parse(MSG.ERROR_JOIN_ARENA_FULL));
				return;
			}
			
			
			ArenaModuleManager.choosePlayerTeam(arena, (Player) sender, team.getColoredName());
			
			arena.markPlayedPlayer(sender.getName());
			
			if ((commModule == null) || (commGoal == null)) {
				if (commModule != null) {
					commModule.commitJoin((Player) sender, team);
					
					ArenaModuleManager.parseJoin(res, arena, (Player) sender, team);
					return;
				}
				if (!ArenaManager.checkJoin((Player) sender, arena)) {
					arena.msg(sender, Language.parse(MSG.ERROR_JOIN_REGION));
					return;
				}
				// both null, just put the joiner to some spawn
				
				if (!arena.tryJoin((Player) sender, team)) {
					return;
				}
				
				if (arena.isFreeForAll()) {
					arena.msg(sender, arena.getArenaConfig().getString(CFG.MSG_YOUJOINED));
					arena.broadcastExcept(sender, Language.parse(arena, CFG.MSG_PLAYERJOINED, sender.getName()));
				} else {
					arena.msg(sender, arena.getArenaConfig().getString(CFG.MSG_YOUJOINEDTEAM).replace("%1%", team.getColoredName() + "�r"));
					arena.broadcastExcept(sender, Language.parse(arena, CFG.MSG_PLAYERJOINEDTEAM, sender.getName(), team.getColoredName() + "�r"));
				}
				
				PVPArena.instance.getAgm().initiate(arena, (Player) sender);
				ArenaModuleManager.initiate(arena, (Player) sender);
				
				if (arena.getFighters().size() > 1 && arena.getFighters().size() >= arena.getArenaConfig().getInt(CFG.READY_MINPLAYERS)) {
					arena.broadcast(Language.parse(MSG.FIGHT_BEGINS));
					arena.setFightInProgress(true);
					for (ArenaPlayer p : arena.getFighters()) {
						if (p.getName().equals(sender.getName())) {
							continue;
						}
						arena.tpPlayerToCoordName(p.get(), (arena.isFreeForAll()?"":p.getArenaTeam().getName())
								+ "spawn");
					}
					
					for (ArenaGoal goal : arena.getGoals()) {
						goal.parseStart();
					}
					
					for (ArenaModule mod : arena.getMods()) {
						mod.parseStart();
					}
				}
				
				return;
			}

			commModule.commitJoin((Player) sender, team);
			
			ArenaModuleManager.parseJoin(res, arena, (Player) sender, team);
	}

	public static void handlePlayerDeath(Arena arena, Player player, PlayerDeathEvent event) {
		boolean doesRespawn = true;
		
		int priority = 0;
		PACheck res = new PACheck();
		db.i("handlePlayerDeath", player);
		
		ArenaGoal commit = null;
		
		for (ArenaGoal mod : arena.getGoals()) {
			res = mod.checkPlayerDeath(res, player);
			if (res.getPriority() > priority && priority >= 0) {
				db.i("success and higher priority", player);
				priority = res.getPriority();
				commit = mod;
			} else if (res.getPriority() < 0 || priority < 0) {
				db.i("fail", player);
				// fail
				priority = res.getPriority();
				commit = null;
			} else {
				db.i("else", player);
			}
		}
		
		if (res.hasError()) {
			db.i("has error: " + res.getError(), player);
			if (res.getError().equals("0")) {
				doesRespawn = false;
			}
		}

		StatisticsManager.kill(arena, player.getKiller(), player, doesRespawn);
		event.setDeathMessage(null);
		
		if (!arena.getArenaConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
			db.i("don't drop inventory", player);
			event.getDrops().clear();
		}
		
		if (commit == null) {
			db.i("no mod handles player deaths", player);

			if (arena.isCustomClassAlive()
					|| arena.getArenaConfig().getBoolean(CFG.PLAYER_DROPSINVENTORY)) {
				InventoryManager.drop(player);
				event.getDrops().clear();
			}
			ArenaTeam respawnTeam = ArenaPlayer.parsePlayer(player.getName()).getArenaTeam();
			
			if (arena.getArenaConfig().getBoolean(CFG.USES_DEATHMESSAGES)) {
				arena.broadcast(Language.parse(MSG.FIGHT_KILLED_BY,
						respawnTeam.colorizePlayer(player) + ChatColor.YELLOW,
						arena.parseDeathCause(player, event.getEntity().getLastDamageCause().getCause(), event.getEntity().getKiller())));
			}
		
			handleRespawn(arena, ArenaPlayer.parsePlayer(player.getName()), event.getDrops());
			
			for (ArenaGoal g : arena.getGoals()) {
				g.parsePlayerDeath(player, player.getLastDamageCause());
			}
			ArenaModuleManager.parsePlayerDeath(arena, player, event.getEntity().getLastDamageCause());
			
			return;
		}

		db.i("handled by: " + commit.getName(), player);
		commit.commitPlayerDeath(player, doesRespawn, res.getError(), event);
		for (ArenaGoal g : arena.getGoals()) {
			db.i("parsing death: " + g.getName(), player);
			g.parsePlayerDeath(player, player.getLastDamageCause());
		}

		ArenaModuleManager.parsePlayerDeath(arena, player, player.getLastDamageCause());
	}

	public static void handleRespawn(Arena arena, ArenaPlayer ap, List<ItemStack> drops) {
		
		for (ArenaModule mod : arena.getMods()) {
			if (mod.tryDeathOverride(ap, drops)) {
				return;
			}
		}
		db.i("handleRespawn!", ap.getName());
		new InventoryRefillRunnable(arena, ap.get(), drops);
		SpawnManager.respawn(arena,  ap);
		arena.unKillPlayer(ap.get(), ap.get().getLastDamageCause()==null?null:ap.get().getLastDamageCause().getCause(), ap.get().getKiller());
		
	}

	public static boolean handleSetFlag(Player player, Block block) {
		Arena arena = PAA_Region.activeSelections.get(player.getName());
		
		if (arena == null) {
			return false;
		}
		
		int priority = 0;
		PACheck res = new PACheck();
		
		ArenaGoal commit = null;
		
		for (ArenaGoal mod : arena.getGoals()) {
			res = mod.checkSetBlock(res, player, block);
			if (res.getPriority() > priority && priority >= 0) {
				// success and higher priority
				priority = res.getPriority();
				commit = mod;
			} else if (res.getPriority() < 0 || priority < 0) {
				// fail
				priority = res.getPriority();
				commit = null;
			}
		}
		
		if (res.hasError()) {
			arena.msg(Bukkit.getConsoleSender(), Language.parse(MSG.ERROR_ERROR, res.getError()));
			return false;
		}
		
		if (commit == null) {
			return false;
		}
		
		return commit.commitSetFlag(player, block);
	}

	public static void handleSpectate(Arena arena, CommandSender sender) {
		int priority = 0;
		PACheck res = new PACheck();

		db.i("handling spectator", sender);
		
		// priority will be set by flags, the max priority will be called
		
		ArenaModule commit = null;
		
		for (ArenaModule mod : arena.getMods()) {
			res = mod.checkJoin(sender, res, false);
			if (res.getPriority() > priority && priority >= 0) {
				db.i("success and higher priority", sender);
				priority = res.getPriority();
				commit = mod;
			} else if (res.getPriority() < 0 || priority < 0) {
				db.i("fail", sender);
				priority = res.getPriority();
				commit = null;
			}
		}
		
		if (res.hasError()) {
			arena.msg(sender, Language.parse(MSG.ERROR_ERROR, res.getError()));
			return;
		}
		
		if (commit == null) {
			db.i("commit null", sender);
			return;
		}
		
		commit.commitSpectate((Player) sender);
	}

	public static void handleStart(Arena arena, CommandSender sender) {
		PACheck res = new PACheck();

		ArenaGoal commit = null;
		int priority = 0;
		
		for (ArenaGoal mod : arena.getGoals()) {
			res = mod.checkStart(res);
			if (res.getPriority() > priority && priority >= 0) {
				// success and higher priority
				priority = res.getPriority();
				commit = mod;
			} else if (res.getPriority() < 0 || priority < 0) {
				// fail
				priority = res.getPriority();
				commit = null;
			}
		}
		
		if (res.hasError()) {
			if (sender == null) {
				sender = Bukkit.getConsoleSender();
			}
			arena.msg(sender, Language.parse(MSG.ERROR_ERROR, res.getError()));
			return;
		}
		
		PAStartEvent event = new PAStartEvent(arena);
		Bukkit.getPluginManager().callEvent(event);
		
		db.i("teleporting all players to their spawns", sender);

		if (commit != null) {
			commit.commitStart(); // override spawning
		} else {
		
			if (!arena.isFreeForAll()) {
				for (ArenaTeam team : arena.getTeams()) {
					SpawnManager.distribute(arena, team);
				}
			} else {
				for (ArenaTeam team : arena.getTeams()) {
					SpawnManager.distribute(arena, team.getTeamMembers());
				}
			}
		}

		db.i("teleported everyone!", sender);

		arena.broadcast(Language.parse(MSG.FIGHT_BEGINS));
		
		for (ArenaGoal x : arena.getGoals()) {
			x.parseStart();
		}
		
		for (ArenaModule x : arena.getMods()) {
			x.parseStart();
		}

		SpawnCampRunnable scr = new SpawnCampRunnable(arena, 0);
		arena.spawnCampRunnerID = Bukkit.getScheduler().scheduleSyncRepeatingTask(
				PVPArena.instance, scr, 100L,
				arena.getArenaConfig().getInt(CFG.TIME_REGIONTIMER));
		scr.setId(arena.spawnCampRunnerID);

		for (ArenaRegionShape region : arena.getRegions()) {
			if (region.getFlags().size() > 0) {
				region.initTimer();
			} else if (region.getType().equals(RegionType.BATTLE)) {
				region.initTimer();
			} else if (region.getType().equals(RegionType.JOIN) && arena.getArenaConfig().getBoolean(CFG.JOIN_FORCE)) {
				region.initTimer();
			}
		}
		
		if (arena.getArenaConfig().getInt(CFG.TIME_PVP)>0) {
			arena.pvpRunner = new PVPActivateRunnable(arena, arena.getArenaConfig().getInt(CFG.TIME_PVP));
		}
	}
}
/*
 * AVAILABLE PACheckResults:
 * 
 * ArenaGoal.checkCommand() => ArenaGoal.commitCommand()
 * ( onCommand() )
 * > default: nothing
 * 
 * 
 * ArenaGoal.checkEnd() => ArenaGoal.commitEnd()
 * ( ArenaGoalManager.checkEndAndCommit(arena) ) < used
 * > 1: PlayerLives
 * > 2: PlayerDeathMatch
 * > 3: TeamLives
 * > 4: TeamDeathMatch
 * > 5: Flags
 * 
 * ArenaGoal.checkInteract() => ArenaGoal.commitInteract()
 * ( PlayerListener.onPlayerInteract() )
 * > 5: Flags
 * 
 * ArenaGoal.checkJoin() => ArenaGoal.commitJoin()
 * ( PAG_Join ) < used
 * > default: tp inside
 * 
 * ArenaGoal.checkPlayerDeath() => ArenaGoal.commitPlayerDeath()
 * ( PlayerLister.onPlayerDeath() )
 * > 1: PlayerLives
 * > 2: PlayerDeathMatch
 * > 3: TeamLives
 * > 4: TeamDeathMatch
 * > 5: Flags
 * 
 * ArenaGoal.checkSetFlag() => ArenaGoal.commitSetFlag()
 * ( PlayerListener.onPlayerInteract() )
 * > 5: Flags
 * 
 * =================================
 * 
 * ArenaModule.checkJoin()
 * ( PAG_Join | PAG_Spectate ) < used
 * > 1: StandardLounge
 * > 2: BattlefieldJoin
 * > default: nothing
 * 
 * ArenaModule.checkStart()
 * ( PAI_Ready | StartRunnable.commit() ) < used
 * > default: tp players to (team) spawns
 * 
 */
