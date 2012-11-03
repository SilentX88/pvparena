package net.slipcor.pvparena.commands;

import java.util.HashSet;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.core.Help;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.SpawnManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * <pre>PVP Arena SPAWN Command class</pre>
 * 
 * A command to set / remove arena spawns
 * 
 * @author slipcor
 * 
 * @version v0.9.5
 */

public class PAA_Spawn extends PAA__Command {
	static HashSet<String> spawns = new HashSet<String>();
	static {
		spawns.add("exit");
	}

	public PAA_Spawn() {
		super(new String[] {});
	}

	@Override
	public void commit(Arena arena, CommandSender sender, String[] args) {
		if (!this.hasPerms(sender, arena)) {
			return;
		}
		
		if (!this.argCountValid(sender, arena, args, new Integer[]{1,2})) {
			return;
		}
		
		if (!(sender instanceof Player)) {
			Arena.pmsg(sender, Language.parse(MSG.ERROR_ONLY_PLAYERS));
			return;
		}
		
		if (args.length < 2) {
			// usage: /pa {arenaname} spawn [spawnname] | set a spawn

			ArenaPlayer ap = ArenaPlayer.parsePlayer(sender.getName());
			
			if (spawns.contains(args[0])) {
				commitSet(arena, sender, new PALocation(ap.get().getLocation()), args[0]);
				return;
			}
			
			for (ArenaModule mod : PVPArena.instance.getAmm().getModules()) {
				if (mod.isActive(arena) && mod.hasSpawn(arena, args[0])) {
					commitSet(arena, sender, new PALocation(ap.get().getLocation()), args[0]);
					return;
				}
			}
			
			for (ArenaGoal mod : arena.getGoals()) {
				if (mod.hasSpawn(args[0])) {
					commitSet(arena, sender, new PALocation(ap.get().getLocation()), args[0]);
					return;
				}
			}

			arena.msg(sender, Language.parse(MSG.ERROR_SPAWN_UNKNOWN, args[0]));
			
		} else {
			// usage: /pa {arenaname} spawn [spawnname] remove | remove a spawn
			PALocation loc = SpawnManager.getCoords(arena, args[0]);
			if (loc == null) {
				arena.msg(sender, Language.parse(MSG.SPAWN_NOTSET, args[0]));
			} else {
				arena.msg(sender, Language.parse(MSG.SPAWN_REMOVED, args[0]));
				arena.spawnUnset(args[0]);
			}
		}
	}
	
	void commitSet(Arena arena, CommandSender sender, PALocation loc, String name) {
		arena.spawnSet(name.toLowerCase(), loc);
		arena.msg(sender, Language.parse(MSG.SPAWN_SET, name));
		return;
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public void displayHelp(CommandSender sender) {
		Arena.pmsg(sender, Help.parse(HELP.SPAWN));
	}
}
