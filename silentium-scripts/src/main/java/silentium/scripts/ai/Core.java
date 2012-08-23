/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.ai;

import javolution.util.FastList;
import silentium.commons.utils.Rnd;
import silentium.gameserver.ai.DefaultMonsterAI;
import silentium.gameserver.configs.NPCConfig;
import silentium.gameserver.instancemanager.GrandBossManager;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2GrandBossInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.PlaySound;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.templates.StatsSet;

import java.util.List;

/**
 * Core AI
 *
 * @author DrLecter Revised By Emperorc
 */
public class Core extends DefaultMonsterAI implements ScriptFile {
	private static final int CORE = 29006;
	private static final int DEATH_KNIGHT = 29007;
	private static final int DOOM_WRAITH = 29008;
	// private static final int DICOR = 29009;
	// private static final int VALIDUS = 29010;
	private static final int SUSCEPTOR = 29011;
	// private static final int PERUM = 29012;
	// private static final int PREMO = 29013;

	// Status Tracking
	private static final byte ALIVE = 0; // Core is spawned.
	private static final byte DEAD = 1; // Core has been killed.

	private static boolean _FirstAttacked;

	final List<L2Attackable> Minions = new FastList<>();

	public static void onLoad() {
		new Core(-1, "Core", "Core", "ai");
	}

	public Core(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		final int[] mobs = { CORE, DEATH_KNIGHT, DOOM_WRAITH, SUSCEPTOR };
		registerMobs(mobs);

		_FirstAttacked = false;
		final StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
		final int status = GrandBossManager.getInstance().getBossStatus(CORE);
		if (status == DEAD) {
			// load the unlock date and time for Core from DB
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// if Core is locked until a certain time, mark it so and start the unlock timer
			// the unlock time has not yet expired.
			if (temp > 0)
				startQuestTimer("core_unlock", temp, null, null);
			else {
				// the time has already expired while the server was offline. Immediately spawn Core.
				final L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
				GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
				spawnBoss(core);
			}
		} else {
			final String test = loadGlobalQuestVar("Core_Attacked");
			if ("true".equalsIgnoreCase(test))
				_FirstAttacked = true;

			final int loc_x = info.getInteger("loc_x");
			final int loc_y = info.getInteger("loc_y");
			final int loc_z = info.getInteger("loc_z");
			final int heading = info.getInteger("heading");
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");
			final L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, loc_x, loc_y, loc_z, heading, false, 0);
			core.setCurrentHpMp(hp, mp);
			spawnBoss(core);
		}
	}

	@Override
	public void saveGlobalData() {
		final String val = String.valueOf(_FirstAttacked);
		saveGlobalQuestVar("Core_Attacked", val);
	}

	public void spawnBoss(final L2GrandBossInstance npc) {
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));

		// Spawn minions
		L2Attackable mob;
		for (int i = 0; i < 5; i++) {
			final int x = 16800 + i * 360;
			mob = (L2Attackable) addSpawn(DEATH_KNIGHT, x, 110000, npc.getZ(), 280 + Rnd.get(40), false, 0);
			mob.setIsRaidMinion(true);
			Minions.add(mob);
			mob = (L2Attackable) addSpawn(DEATH_KNIGHT, x, 109000, npc.getZ(), 280 + Rnd.get(40), false, 0);
			mob.setIsRaidMinion(true);
			Minions.add(mob);
			final int x2 = 16800 + i * 600;
			mob = (L2Attackable) addSpawn(DOOM_WRAITH, x2, 109300, npc.getZ(), 280 + Rnd.get(40), false, 0);
			mob.setIsRaidMinion(true);
			Minions.add(mob);
		}

		for (int i = 0; i < 4; i++) {
			final int x = 16800 + i * 450;
			mob = (L2Attackable) addSpawn(SUSCEPTOR, x, 110300, npc.getZ(), 280 + Rnd.get(40), false, 0);
			mob.setIsRaidMinion(true);
			Minions.add(mob);
		}
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		if ("core_unlock".equalsIgnoreCase(event)) {
			final L2GrandBossInstance core = (L2GrandBossInstance) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
			GrandBossManager.getInstance().setBossStatus(CORE, ALIVE);
			spawnBoss(core);
		} else if ("spawn_minion".equalsIgnoreCase(event)) {
			final L2Attackable mob = (L2Attackable) addSpawn(npc.getNpcId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0);
			mob.setIsRaidMinion(true);
			Minions.add(mob);
		} else if ("despawn_minions".equalsIgnoreCase(event)) {
			for (final L2Attackable mob : Minions) {
				if (mob != null)
					mob.decayMe();
			}
			Minions.clear();
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onAttack(final L2Npc npc, final L2PcInstance attacker, final int damage, final boolean isPet) {
		if (npc.getNpcId() == CORE) {
			if (_FirstAttacked) {
				if (Rnd.get(100) == 0)
					npc.broadcastNpcSay("Removing intruders.");
			} else {
				_FirstAttacked = true;
				npc.broadcastNpcSay("A non-permitted target has been discovered.");
				npc.broadcastNpcSay("Starting intruder removal system.");
			}
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance killer, final boolean isPet) {
		final int npcId = npc.getNpcId();
		if (npcId == CORE) {
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastNpcSay("A fatal error has occurred.");
			npc.broadcastNpcSay("System is being shut down...");
			npc.broadcastNpcSay("......");

			_FirstAttacked = false;

			addSpawn(31842, 16502, 110165, -6394, 0, false, 900000);
			addSpawn(31842, 18948, 110166, -6397, 0, false, 900000);
			GrandBossManager.getInstance().setBossStatus(CORE, DEAD);

			// time is 60hour +/- 23hour
			final long respawnTime = (long) NPCConfig.SPAWN_INTERVAL_CORE + Rnd.get(NPCConfig.RANDOM_SPAWN_TIME_CORE);
			startQuestTimer("core_unlock", respawnTime, null, null);

			// also save the respawn time so that the info is maintained past reboots
			final StatsSet info = GrandBossManager.getInstance().getStatsSet(CORE);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatsSet(CORE, info);
			startQuestTimer("despawn_minions", 20000, null, null);
			cancelQuestTimers("spawn_minion");
		} else if (GrandBossManager.getInstance().getBossStatus(CORE) == ALIVE && Minions != null && Minions.contains(npc)) {
			Minions.remove(npc);
			startQuestTimer("spawn_minion", 60000, npc, null);
		}
		return super.onKill(npc, killer, isPet);
	}
}