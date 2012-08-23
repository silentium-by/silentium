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
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.ai.DefaultMonsterAI;
import silentium.gameserver.configs.NPCConfig;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.instancemanager.GrandBossManager;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2GrandBossInstance;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.zone.type.L2BossZone;
import silentium.gameserver.network.serverpackets.Earthquake;
import silentium.gameserver.network.serverpackets.PlaySound;
import silentium.gameserver.network.serverpackets.SocialAction;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.utils.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Following animations are handled in that time tempo :
 * <ul>
 * <li>wake(2), 0-13 secs</li>
 * <li>neck(3), 14-24 secs.</li>
 * <li>roar(1), 25-37 secs.</li>
 * </ul>
 * Waker's sacrifice is handled between neck and roar animation.
 */
public class Baium extends DefaultMonsterAI implements ScriptFile {
	private L2Character _target;
	private L2Skill _skill;
	private L2PcInstance _waker;

	private static final int STONE_BAIUM = 29025;
	private static final int LIVE_BAIUM = 29020;
	private static final int ARCHANGEL = 29021;

	// Baium status tracking
	public static final byte ASLEEP = 0; // baium is in the stone version, waiting to be woken up. Entry is unlocked.
	public static final byte AWAKE = 1; // baium is awake and fighting. Entry is locked.
	public static final byte DEAD = 2; // baium has been killed and has not yet spawned. Entry is locked.

	// Archangels spawns
	private static final int[][] ANGEL_LOCATION = { { 114239, 17168, 10080, 63544 }, { 115780, 15564, 10080, 13620 }, { 114880, 16236, 10080, 5400 }, { 115168, 17200, 10080, 0 }, { 115792, 16608, 10080, 0 }, };

	private long _LastAttackVsBaiumTime = 0;
	private final List<L2Npc> _Minions = new ArrayList<>(5);
	private L2BossZone _Zone;

	public static void onLoad() {
		new Baium(-1, "baium", "", "ai");
	}

	public Baium(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		final int[] mob = { LIVE_BAIUM };
		registerMobs(mob);

		// Quest NPC starter initialization
		addStartNpc(STONE_BAIUM);
		addTalkId(STONE_BAIUM);

		_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);
		final StatsSet info = GrandBossManager.getInstance().getStatsSet(LIVE_BAIUM);
		final int status = GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM);

		if (status == DEAD) {
			// load the unlock date and time for baium from DB
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			if (temp > 0) {
				// The time has not yet expired. Mark Baium as currently locked (dead).
				startQuestTimer("baium_unlock", temp, null, null);
			} else {
				// The time has already expired while the server was offline. Delete the saved time and
				// immediately spawn the stone-baium. Also the state need not be changed from ASLEEP
				addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0);
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			}
		} else if (status == AWAKE) {
			final int loc_x = info.getInteger("loc_x");
			final int loc_y = info.getInteger("loc_y");
			final int loc_z = info.getInteger("loc_z");
			final int heading = info.getInteger("heading");
			final int hp = info.getInteger("currentHP");
			final int mp = info.getInteger("currentMP");

			final L2Npc baium = addSpawn(LIVE_BAIUM, loc_x, loc_y, loc_z, heading, false, 0);
			GrandBossManager.getInstance().addBoss((L2GrandBossInstance) baium);

			baium.setCurrentHpMp(hp, mp);
			baium.setRunning();

			// start monitoring baium's inactivity
			_LastAttackVsBaiumTime = System.currentTimeMillis();
			startQuestTimer("baium_despawn", 60000, baium, null, true);
			startQuestTimer("skill_range", 500, baium, null, true);

			// Spawns angels
			for (final int[] element : ANGEL_LOCATION) {
				final L2Npc angel = addSpawn(ARCHANGEL, element[0], element[1], element[2], element[3], false, 0, true);
				((L2Attackable) angel).setIsRaidMinion(true);
				angel.setRunning();
				_Minions.add(angel);
			}

			// Angels AI
			startQuestTimer("angels_aggro_reconsider", 1000, null, null, true);
		} else
			addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0);
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		if ("baium_unlock".equalsIgnoreCase(event)) {
			GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP);
			addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0);
		} else if ("skill_range".equalsIgnoreCase(event) && npc != null) {
			callSkillAI(npc);
		} else if ("clean_player".equalsIgnoreCase(event)) {
			_target = getRandomTarget(npc);
		} else if ("baium_neck".equalsIgnoreCase(event) && npc != null) {
			if (npc.getNpcId() == LIVE_BAIUM)
				npc.broadcastPacket(new SocialAction(npc, 3));
		} else if ("sacrifice_waker".equalsIgnoreCase(event) && npc != null) {
			if (npc.getNpcId() == LIVE_BAIUM) {
				if (_waker != null) {
					// If player is far of Baium, teleport him back.
					if (!Util.checkIfInShortRadius(300, _waker, npc, true))
						_waker.teleToLocation(115929, 17349, 10077);

					// 60% to die.
					if (Rnd.get(100) < 60)
						_waker.doDie(npc);
				}
			}
		} else if ("baium_roar".equalsIgnoreCase(event) && npc != null) {
			if (npc.getNpcId() == LIVE_BAIUM) {
				// Roar animation
				npc.broadcastPacket(new SocialAction(npc, 1));

				// Spawn angels
				for (final int[] element : ANGEL_LOCATION) {
					final L2Npc angel = addSpawn(ARCHANGEL, element[0], element[1], element[2], element[3], false, 0, true);
					((L2Attackable) angel).setIsRaidMinion(true);
					angel.setRunning();
					_Minions.add(angel);
				}

				// Angels AI
				startQuestTimer("angels_aggro_reconsider", 1000, null, null, true);
			}
		}
		// despawn the live baium after 30 minutes of inactivity
		// also check if the players are cheating, having pulled Baium outside his zone...
		else if ("baium_despawn".equalsIgnoreCase(event) && npc != null) {
			if (npc.getNpcId() == LIVE_BAIUM) {
				// just in case the zone reference has been lost (somehow...), restore the reference
				if (_Zone == null)
					_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);

				if (_LastAttackVsBaiumTime + 1800000 < System.currentTimeMillis()) {
					// despawn the live-baium
					npc.deleteMe();

					// Unspawn angels
					for (final L2Npc minion : _Minions) {
						if (minion != null) {
							minion.getSpawn().stopRespawn();
							minion.deleteMe();
						}
					}
					_Minions.clear();

					addSpawn(STONE_BAIUM, 116033, 17447, 10104, 40188, false, 0); // spawn stone-baium
					GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, ASLEEP); // mark that Baium is not awake any more
					_Zone.oustAllPlayers();
					cancelQuestTimer("baium_despawn", npc, null);
				} else if (_LastAttackVsBaiumTime + 300000 < System.currentTimeMillis() && npc.getCurrentHp() < npc.getMaxHp() * 3 / 4.0) {
					npc.setIsCastingNow(false);
					npc.setTarget(npc);
					final L2Skill skill = SkillTable.getInstance().getInfo(4135, 1);
					npc.doCast(skill);
					npc.setIsCastingNow(true);
				} else if (!_Zone.isInsideZone(npc))
					npc.teleToLocation(116033, 17447, 10104);
			}
		} else if ("angels_aggro_reconsider".equalsIgnoreCase(event)) {
			boolean updateTarget = false; // Update or no the target

			for (final L2Npc minion : _Minions) {
				final L2Attackable angel = (L2Attackable) minion;
				if (angel == null)
					continue;

				final L2Character victim = angel.getMostHated();

				if (Rnd.get(100) == 0) // Chaos time
					updateTarget = true;
				else {
					if (victim != null) // Target is a unarmed player ; clean aggro.
					{
						if (victim instanceof L2PcInstance && victim.getActiveWeaponInstance() == null) {
							angel.stopHating(victim); // Clean the aggro number of previous victim.
							updateTarget = true;
						}
					} else
						// No target currently.
						updateTarget = true;
				}

				if (updateTarget) {
					final L2Character newVictim = getRandomTarget(minion);
					if (newVictim != null && victim != newVictim) {
						angel.addDamageHate(newVictim, 0, 10000);
						angel.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, newVictim);
					}
				}
			}
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		final int npcId = npc.getNpcId();
		String htmltext = "";

		if (_Zone == null) {
			_Zone = GrandBossManager.getInstance().getZone(113100, 14500, 10077);

			// If the zone is still null, it means the area is disabled / missing in DP.
			if (_Zone == null)
				return "<html><body>Angelic Vortex:<br>You may not enter while admin disabled this zone.</body></html>";
		}

		if (npcId == STONE_BAIUM && GrandBossManager.getInstance().getBossStatus(LIVE_BAIUM) == ASLEEP) {
			if (_Zone.isPlayerAllowed(player)) {
				// once Baium is awaken, no more people may enter until he dies, the server reboots, or
				// 30 minutes pass with no attacks made against Baium.
				GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, AWAKE);
				npc.deleteMe();

				final L2Npc baium = addSpawn(LIVE_BAIUM, npc, true);
				GrandBossManager.getInstance().addBoss((L2GrandBossInstance) baium);

				// Baium is stuck for the following time : 35secs
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
					@Override
					public void run() {
						baium.setIsInvul(false);
						baium.setIsImmobilized(false);

						// Start monitoring baium's inactivity and activate the AI
						_LastAttackVsBaiumTime = System.currentTimeMillis();
						startQuestTimer("baium_despawn", 60000, baium, null, true);
						startQuestTimer("skill_range", 500, baium, null, true);
					}
				}, 35000L);

				// First animation
				baium.setIsInvul(true);
				baium.setRunning();
				baium.broadcastPacket(new SocialAction(baium, 2));
				baium.broadcastPacket(new Earthquake(baium.getX(), baium.getY(), baium.getZ(), 40, 10));

				_waker = player;

				// Second animation, waker sacrifice, followed by angels spawn and third animation.
				startQuestTimer("baium_neck", 13000, baium, null);
				startQuestTimer("sacrifice_waker", 24000, baium, null);
				startQuestTimer("baium_roar", 28000, baium, null);

				baium.setShowSummonAnimation(false);
			} else
				htmltext = "Conditions are not right to wake up Baium";
		}
		return htmltext;
	}

	@Override
	public String onSpellFinished(final L2Npc npc, final L2PcInstance player, final L2Skill skill) {
		if (npc.isInvul()) {
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			return null;
		} else if (npc.getNpcId() == LIVE_BAIUM && !npc.isInvul())
			callSkillAI(npc);

		return super.onSpellFinished(npc, player, skill);
	}

	@Override
	public String onSpawn(final L2Npc npc) {
		npc.disableCoreAI(true);
		return super.onSpawn(npc);
	}

	@Override
	public String onAttack(final L2Npc npc, final L2PcInstance attacker, final int damage, final boolean isPet) {
		if (!_Zone.isInsideZone(attacker)) {
			attacker.reduceCurrentHp(attacker.getCurrentHp(), attacker, false, false, null);
			return super.onAttack(npc, attacker, damage, isPet);
		}

		if (npc.isInvul()) {
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			return super.onAttack(npc, attacker, damage, isPet);
		} else if (npc.getNpcId() == LIVE_BAIUM && !npc.isInvul()) {
			if (attacker.getMountType() == 1) {
				final L2Skill skill = SkillTable.getInstance().getInfo(4258, 1);
				if (attacker.getFirstEffect(skill) == null) {
					npc.setTarget(attacker);
					npc.doCast(skill);
				}
			}
			// update a variable with the last action against baium
			_LastAttackVsBaiumTime = System.currentTimeMillis();
			callSkillAI(npc);
		}
		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance killer, final boolean isPet) {
		cancelQuestTimer("baium_despawn", npc, null);
		npc.broadcastPacket(new PlaySound(1, "BS01_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));

		// spawn the "Teleportation Cubic" for 15 minutes (to allow players to exit the lair)
		addSpawn(29055, 115203, 16620, 10078, 0, false, 900000);

		// "lock" baium for 5 days + 1-8 hours
		final long respawnTime = (long) NPCConfig.SPAWN_INTERVAL_BAIUM + Rnd.get(NPCConfig.RANDOM_SPAWN_TIME_BAIUM);
		GrandBossManager.getInstance().setBossStatus(LIVE_BAIUM, DEAD);
		startQuestTimer("baium_unlock", respawnTime, null, null);

		// also save the respawn time so that the info is maintained past reboots
		final StatsSet info = GrandBossManager.getInstance().getStatsSet(LIVE_BAIUM);
		info.set("respawn_time", System.currentTimeMillis() + respawnTime);
		GrandBossManager.getInstance().setStatsSet(LIVE_BAIUM, info);

		// Unspawn angels.
		for (final L2Npc minion : _Minions) {
			if (minion != null) {
				minion.getSpawn().stopRespawn();
				minion.deleteMe();
			}
		}
		_Minions.clear();

		// Clean Baium AI
		cancelQuestTimer("skill_range", npc, null);

		// Clean angels AI
		cancelQuestTimer("angels_aggro_reconsider", null, null);

		return super.onKill(npc, killer, isPet);
	}

	@Override
	public String onSkillSee(final L2Npc npc, final L2PcInstance caster, final L2Skill skill, final L2Object[] targets, final boolean isPet) {
		if (npc.isInvul()) {
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			return null;
		}
		npc.setTarget(caster);
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}

	/**
	 * That method allows to select a random target.
	 *
	 * @param npc to check.
	 * @return the random target.
	 */
	private L2Character getRandomTarget(final L2Npc npc) {
		final int npcId = npc.getNpcId();
		final FastList<L2Character> result = FastList.newInstance();

		final Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
		for (final L2Object obj : objs) {
			if (obj != null) {
				if (obj instanceof L2Playable) {
					if (obj instanceof L2PcInstance) {
						if (((L2PcInstance) obj).getAppearance().getInvisible())
							continue;

						if (npcId == ARCHANGEL && ((L2PcInstance) obj).getActiveWeaponInstance() == null)
							continue;
					}

					if (obj.getZ() < npc.getZ() - 100 && obj.getZ() > npc.getZ() + 100 || !GeoData.getInstance().canSeeTarget(obj.getX(), obj.getY(), obj.getZ(), npc.getX(), npc.getY(), npc.getZ()))
						continue;

					if (Util.checkIfInRange(2000, npc, obj, true) && !((L2Character) obj).isDead())
						result.add((L2Character) obj);
				}

				// Case of Archangels, they can hit Baium.
				if (npcId == ARCHANGEL && obj instanceof L2GrandBossInstance)
					result.add((L2Character) obj);
			}
		}

		// If there's no players available, Baium and Angels are hitting each other.
		if (result.isEmpty()) {
			if (npcId == LIVE_BAIUM) // Case of Baium. Angels should never be without target.
			{
				for (final L2Npc minion : _Minions)
					if (minion != null)
						result.add(minion);
			}
		}

		if (result.isEmpty()) {
			FastList.recycle(result);
			return null;
		}

		final Object[] characters = result.toArray();

		cancelQuestTimer("clean_player", npc, null);
		startQuestTimer("clean_player", 20000, npc, null);

		final L2Character target = (L2Character) characters[Rnd.get(characters.length)];
		FastList.recycle(result);
		return target;
	}

	/**
	 * That method checks if angels are near.
	 *
	 * @param npc : baium.
	 * @return the number of angels surrounding the target.
	 */
	private int getSurroundingAngelsNumber(final L2Npc npc) {
		int count = 0;

		final Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
		for (final L2Object obj : objs) {
			if (obj instanceof L2MonsterInstance) {
				if (((L2Npc) obj).getNpcId() == 29021)
					if (Util.checkIfInRange(600, npc, obj, true))
						count++;
			}
		}
		return count;
	}

	/**
	 * The personal casting AI for Baium.
	 *
	 * @param npc baium, basically...
	 */
	private synchronized void callSkillAI(final L2Npc npc) {
		if (npc.isInvul() || npc.isCastingNow())
			return;

		if (_target == null || _target.isDead() || !_Zone.isInsideZone(_target)) {
			_target = getRandomTarget(npc);
			if (_target != null)
				_skill = SkillTable.getInstance().getInfo(getRandomSkill(npc), 1);
		}

		final L2Character target = _target;
		L2Skill skill = _skill;
		if (skill == null)
			skill = SkillTable.getInstance().getInfo(getRandomSkill(npc), 1);

		if (target == null || target.isDead() || !_Zone.isInsideZone(target)) {
			npc.setIsCastingNow(false);
			return;
		}

		// Adapt the skill range, because Baium is fat.
		if (Util.checkIfInRange(skill.getCastRange() + npc.getCollisionRadius(), npc, target, true)) {
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			npc.setTarget(skill.getId() == 4135 ? npc : target);
			npc.setIsCastingNow(true);

			_target = null;
			_skill = null;

			try {
				Thread.sleep(1000);
				npc.stopMove(null);
				npc.doCast(skill);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target, null);
			npc.setIsCastingNow(false);
		}
	}

	/**
	 * Pick a random skill through that list.<br>
	 * If Baium feels surrounded, he will use AoE skills. Same behavior if he is near 2+ angels.<br>
	 *
	 * @param npc baium
	 * @return a usable skillId
	 */
	private int getRandomSkill(final L2Npc npc) {
		// Baium's selfheal. It happens exceptionaly.
		if (npc.getCurrentHp() < npc.getMaxHp() / 10) {
			if (Rnd.get(10000) == 777) // His lucky day.
				return 4135;
		}

		int skill = 4127; // Default attack if nothing is possible.
		final int chance = Rnd.get(100); // Remember, it's 0 to 99, not 1 to 100.

		// If Baium feels surrounded or see 2+ angels, he unleashes his wrath upon heads :).
		if (Util.getPlayersCountInRadius(600, npc, true, false) >= 20 || getSurroundingAngelsNumber(npc) >= 2) {
			if (chance < 25)
				skill = 4130;
			else if (chance >= 25 && chance < 50)
				skill = 4131;
			else if (chance >= 50 && chance < 75)
				skill = 4128;
			else if (chance >= 75 && chance < 100)
				skill = 4129;
		} else {
			if (npc.getCurrentHp() > npc.getMaxHp() * 3 / 4) // > 75%
			{
				if (chance < 10)
					skill = 4128;
				else skill = chance >= 10 && chance < 20 ? 4129 : 4127;
			} else if (npc.getCurrentHp() > npc.getMaxHp() * 2 / 4) // > 50%
			{
				if (chance < 10)
					skill = 4131;
				else if (chance >= 10 && chance < 20)
					skill = 4128;
				else skill = chance >= 20 && chance < 30 ? 4129 : 4127;
			} else if (npc.getCurrentHp() > npc.getMaxHp() / 4) // > 25%
			{
				if (chance < 10)
					skill = 4130;
				else if (chance >= 10 && chance < 20)
					skill = 4131;
				else if (chance >= 20 && chance < 30)
					skill = 4128;
				else skill = chance >= 30 && chance < 40 ? 4129 : 4127;
			} else
			// < 25%
			{
				if (chance < 10)
					skill = 4130;
				else if (chance >= 10 && chance < 20)
					skill = 4131;
				else if (chance >= 20 && chance < 30)
					skill = 4128;
				else skill = chance >= 30 && chance < 40 ? 4129 : 4127;
			}
		}
		return skill;
	}
}