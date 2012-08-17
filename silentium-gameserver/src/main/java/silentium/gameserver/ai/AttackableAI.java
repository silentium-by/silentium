/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.ai;

import static silentium.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static silentium.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;
import static silentium.gameserver.ai.CtrlIntention.AI_INTENTION_IDLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import silentium.commons.utils.Rnd;
import silentium.gameserver.GameTimeController;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.NPCConfig;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.instancemanager.DimensionalRiftManager;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2Skill.SkillTargetType;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2DoorInstance;
import silentium.gameserver.model.actor.instance.L2FestivalMonsterInstance;
import silentium.gameserver.model.actor.instance.L2FriendlyMobInstance;
import silentium.gameserver.model.actor.instance.L2GrandBossInstance;
import silentium.gameserver.model.actor.instance.L2GuardInstance;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2RaidBossInstance;
import silentium.gameserver.model.actor.instance.L2RiftInvaderInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.templates.chars.L2NpcTemplate.AIType;
import silentium.gameserver.templates.skills.L2EffectType;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Util;

/**
 * This class manages AI of L2Attackable.
 */
public class AttackableAI extends CharacterAI implements Runnable
{
	private static final int RANDOM_WALK_RATE = 30;
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min

	/** The L2Attackable AI task executed every 1s (call onEvtThink method) */
	private Future<?> _aiTask;

	/** The delay after wich the attacked is stopped */
	private int _attackTimeout;

	/** The L2Attackable aggro counter */
	private int _globalAggro;

	/** The flag used to indicate that a thinking action is in progress */
	private boolean _thinking; // to prevent recursive thinking

	private int chaostime = 0;
	int lastBuffTick;

	private final L2NpcTemplate _skillrender;
	private List<L2Skill> shortRangeSkills = new ArrayList<>();
	private List<L2Skill> longRangeSkills = new ArrayList<>();

	/**
	 * Constructor of AttackableAI.
	 * 
	 * @param accessor
	 *            The AI accessor of the L2Character
	 */
	public AttackableAI(L2Character.AIAccessor accessor)
	{
		super(accessor);

		// Attach the AI template to this NPC template
		_skillrender = NpcTable.getInstance().getTemplate(getActiveChar().getTemplate().getNpcId());

		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 seconds timeout of ATTACK after respawn
	}

	@Override
	public void run()
	{
		// Launch actions corresponding to the Event Think
		onEvtThink();
	}

	/**
	 * <B><U> Actor is a L2GuardInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk or a Door</li> <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li> <li>The L2PcInstance target has karma (=PK)</li> <li>The
	 * L2MonsterInstance target is aggressive</li><BR>
	 * <BR>
	 * <B><U> Actor is a L2SiegeGuardInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk or a Door</li> <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND too far (>100)</li>
	 * <li>The target is in the actor Aggro range and is at the same height</li> <li>A siege is in progress</li> <li>The L2PcInstance target
	 * isn't a Defender</li> <BR>
	 * <BR>
	 * <B><U> Actor is a L2FriendlyMobInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk, a Door or another L2Npc</li> <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND
	 * too far (>100)</li> <li>The target is in the actor Aggro range and is at the same height</li> <li>The L2PcInstance target has karma (=PK)</li>
	 * <BR>
	 * <BR>
	 * <B><U> Actor is a L2MonsterInstance</U> :</B><BR>
	 * <BR>
	 * <li>The target isn't a Folk, a Door or another L2Npc</li> <li>The target isn't dead, isn't invulnerable, isn't in silent moving mode AND
	 * too far (>100)</li> <li>The target is in the actor Aggro range and is at the same height</li> <li>The actor is Aggressive</li><BR>
	 * <BR>
	 * 
	 * @param target
	 *            The targeted L2Object
	 * @return True if the target is autoattackable (depends on the actor type).
	 */
	private boolean autoAttackCondition(L2Character target)
	{
		if (target == null || getActiveChar() == null)
			return false;

		// Check if the target isn't a Door or dead.
		if (target instanceof L2DoorInstance || target.isAlikeDead())
			return false;

		L2Attackable me = getActiveChar();

		if (target instanceof L2Playable)
		{
			// Check if target is in the Aggro range
			if (!me.isInsideRadius(target, me.getAggroRange(), true, false))
				return false;

			// Check if the AI isn't a Raid Boss, can See Silent Moving players and the target isn't in silent move mode
			if (!(me.isRaid()) && !(me.canSeeThroughSilentMove()) && ((L2Playable) target).isSilentMoving())
				return false;

			// Check if the target is a L2PcInstance
			L2PcInstance targetPlayer = target.getActingPlayer();
			if (targetPlayer != null)
			{
				if (targetPlayer.isGM())
				{
					// Check if the target isn't invulnerable ; requires to check GMs specially
					if (target.isInvul())
						return false;

					// Don't take the aggro if the GM has the access level below or equal to GM_DONT_TAKE_AGGRO
					if (!targetPlayer.getAccessLevel().canTakeAggro())
						return false;
				}

				// Check if player is an ally (comparing mem addr)
				if ("varka_silenos_clan".equals(me.getClan()) && targetPlayer.isAlliedWithVarka())
					return false;

				if ("ketra_orc_clan".equals(me.getClan()) && targetPlayer.isAlliedWithKetra())
					return false;

				// check if the target is within the grace period for JUST getting up from fake death
				if (targetPlayer.isRecentFakeDeath())
					return false;

				if (targetPlayer.isInParty() && targetPlayer.getParty().isInDimensionalRift())
				{
					byte riftType = targetPlayer.getParty().getDimensionalRift().getType();
					byte riftRoom = targetPlayer.getParty().getDimensionalRift().getCurrentRoom();

					if (me instanceof L2RiftInvaderInstance && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
						return false;
				}
			}
		}

		// Check if the actor is a L2GuardInstance
		if (me instanceof L2GuardInstance)
		{
			// Check if the L2PcInstance target has karma (=PK)
			if (target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
				return GeoData.getInstance().canSeeTarget(me, target);

			// Check if the L2MonsterInstance target is aggressive
			if (target instanceof L2MonsterInstance && NPCConfig.GUARD_ATTACK_AGGRO_MOB)
				return (((L2MonsterInstance) target).isAggressive() && GeoData.getInstance().canSeeTarget(me, target));

			return false;
		}
		// The actor is a L2FriendlyMobInstance
		else if (me instanceof L2FriendlyMobInstance)
		{
			// Check if the target isn't another L2Npc
			if (target instanceof L2Npc)
				return false;

			// Check if the L2PcInstance target has karma (=PK)
			if (target instanceof L2PcInstance && ((L2PcInstance) target).getKarma() > 0)
				return GeoData.getInstance().canSeeTarget(me, target); // Los Check

			return false;
		}
		// The actor is a L2MonsterInstance
		else
		{
			if (target instanceof L2Attackable)
			{
				if (me.getEnemyClan() == null || ((L2Attackable) target).getClan() == null)
					return false;

				if (!target.isAutoAttackable(me))
					return false;

				if (me.getEnemyClan().equals(((L2Attackable) target).getClan()))
				{
					if (me.isInsideRadius(target, me.getEnemyRange(), false, false))
						return GeoData.getInstance().canSeeTarget(me, target);

					return false;
				}

				if (me.getIsChaos() > 0 && me.isInsideRadius(target, me.getIsChaos(), false, false))
				{
					if (me.getClan() != null && me.getClan().equals(((L2Attackable) target).getClan()))
						return false;

					// Los Check
					return GeoData.getInstance().canSeeTarget(me, target);
				}

				return false;
			}

			if (target instanceof L2Npc)
				return false;

			// depending on config, do not allow mobs to attack _new_ players in peacezones,
			// unless they are already following those players from outside the peacezone.
			if (!NPCConfig.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(L2Character.ZONE_PEACE))
				return false;

			// Check if the actor is Aggressive
			return (me.isAggressive() && GeoData.getInstance().canSeeTarget(me, target));
		}
	}

	public void startAITask()
	{
		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		if (_aiTask == null)
			_aiTask = ThreadPoolManager.getInstance().scheduleAiAtFixedRate(this, 1000, 1000);
	}

	@Override
	public void stopAITask()
	{
		if (_aiTask != null)
		{
			_aiTask.cancel(false);
			_aiTask = null;
		}
		super.stopAITask();
	}

	/**
	 * Set the Intention of this CharacterAI and create an AI Task executed every 1s (call onEvtThink method) for this L2Attackable.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : If actor _knowPlayer isn't EMPTY, AI_INTENTION_IDLE will be change in
	 * AI_INTENTION_ACTIVE</B></FONT><BR>
	 * <BR>
	 * 
	 * @param intention
	 *            The new Intention to set to the AI
	 * @param arg0
	 *            The first parameter of the Intention
	 * @param arg1
	 *            The second parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1)
	{
		if (intention == AI_INTENTION_IDLE || intention == AI_INTENTION_ACTIVE)
		{
			// Check if actor is not dead
			L2Attackable npc = getActiveChar();
			if (!npc.isAlikeDead())
			{
				// If its _knownPlayer isn't empty set the Intention to AI_INTENTION_ACTIVE
				if (!npc.getKnownList().getKnownPlayers().isEmpty())
					intention = AI_INTENTION_ACTIVE;
				else
				{
					if (npc.getSpawn() != null)
					{
						final int range = NPCConfig.MAX_DRIFT_RANGE;
						if (!npc.isInsideRadius(npc.getSpawn().getLocx(), npc.getSpawn().getLocy(), npc.getSpawn().getLocz(), range + range, true, false))
							intention = AI_INTENTION_ACTIVE;
					}
				}
			}

			if (intention == AI_INTENTION_IDLE)
			{
				// Set the Intention of this AttackableAI to AI_INTENTION_IDLE
				super.changeIntention(AI_INTENTION_IDLE, null, null);

				// Stop AI task and detach AI from NPC
				if (_aiTask != null)
				{
					_aiTask.cancel(true);
					_aiTask = null;
				}

				// Cancel the AI
				_accessor.detachAI();

				return;
			}
		}

		// Set the Intention of this AttackableAI to intention
		super.changeIntention(intention, arg0, arg1);

		// If not idle - create an AI task (schedule onEvtThink repeatedly)
		startAITask();
	}

	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Calculate attack timeout, Start a new Attack and Launch Think Event.<BR>
	 * <BR>
	 * 
	 * @param target
	 *            The L2Character to attack
	 */
	@Override
	protected void onIntentionAttack(L2Character target)
	{
		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		// Buffs
		if (lastBuffTick + 30 < GameTimeController.getGameTicks())
		{
			for (L2Skill sk : _skillrender.getBuffSkills())
			{
				if (cast(sk))
					break;
			}
			lastBuffTick = GameTimeController.getGameTicks();
		}

		// Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Event
		super.onIntentionAttack(target);
	}

	private void thinkCast()
	{
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}

		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
			return;

		clientStopMoving(null);
		setIntention(AI_INTENTION_ACTIVE);
		_accessor.doCast(_skill);
	}

	/**
	 * Manage AI standard thinks of a L2Attackable (called by onEvtThink).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update every 1s the _globalAggro counter to come close to 0</li> <li>If the actor is Aggressive and can attack, add all autoAttackable
	 * L2Character in its Aggro Range to its _aggroList, chose a target and order to attack it</li> <li>If the actor is a L2GuardInstance that
	 * can't attack, order to it to return to its home location</li> <li>If the actor is a L2MonsterInstance that can't attack, order to it to
	 * random walk (1/100)</li><BR>
	 * <BR>
	 */
	private void thinkActive()
	{
		L2Attackable npc = getActiveChar();

		// Update every 1s the _globalAggro counter to come close to 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
				_globalAggro++;
			else
				_globalAggro--;
		}

		// Add all autoAttackable L2Character in L2Attackable Aggro Range to its _aggroList with 0 damage and 1 hate
		// A L2Attackable isn't aggressive during 10s after its spawn because _globalAggro is set to -10
		if (_globalAggro >= 0)
		{
			// Get all visible objects inside its Aggro Range
			Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
			for (L2Object obj : objs)
			{
				if (!(obj instanceof L2Character))
					continue;

				L2Character target = (L2Character) obj;

				/*
				 * Check to see if this is a festival mob spawn. If it is, then check to see if the aggro trigger is a festival participant...if
				 * so, move to attack it.
				 */
				if ((npc instanceof L2FestivalMonsterInstance) && obj instanceof L2PcInstance)
				{
					if (!((L2PcInstance) obj).isFestivalParticipant())
						continue;
				}

				// For each L2Character check if the target is autoattackable
				if (autoAttackCondition(target)) // check aggression
				{
					// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
					int hating = npc.getHating(target);

					// Add the attacker to the L2Attackable _aggroList
					if (hating == 0)
						npc.addDamageHate(target, 0, 0);
				}
			}

			// Chose a target from its aggroList
			L2Character hated;
			if (npc.isConfused())
				hated = getAttackTarget(); // Force mobs to attak anybody if confused
			else
				hated = npc.getMostHated();

			// Order to the L2Attackable to attack the target
			if (hated != null && !npc.isCoreAIDisabled())
			{
				// Get the hate level of the L2Attackable against this L2Character target contained in _aggroList
				int aggro = npc.getHating(hated);

				if (aggro + _globalAggro > 0)
				{
					// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others
					// L2PcInstance
					if (!npc.isRunning())
						npc.setRunning();

					// Set the AI Intention to AI_INTENTION_ATTACK
					setIntention(CtrlIntention.AI_INTENTION_ATTACK, hated);
				}
				return;
			}
		}

		// Chance to forget attackers after some time
		if (npc.getCurrentHp() == npc.getMaxHp() && npc.getCurrentMp() == npc.getMaxMp() && !npc.getAttackByList().isEmpty() && Rnd.nextInt(500) == 0)
		{
			npc.clearAggroList();
			npc.getAttackByList().clear();
			if (npc instanceof L2MonsterInstance)
			{
				if (((L2MonsterInstance) npc).hasMinions())
					((L2MonsterInstance) npc).getMinionList().deleteReusedMinions();
			}
		}

		// Order to the L2GuardInstance to return to its home location because there's no target to attack
		if (npc instanceof L2GuardInstance)
			((L2GuardInstance) npc).returnHome();

		// If this is a festival monster, then it remains in the same location.
		if (npc instanceof L2FestivalMonsterInstance)
			return;

		// Minions following leader
		final L2Character leader = npc.getLeader();
		if (leader != null && !leader.isAlikeDead())
		{
			final int offset;
			final int minRadius = 30;

			if (npc.isRaidMinion())
				offset = 500; // for Raids - need correction
			else
				offset = 200; // for normal minions - need correction :)

			if (leader.isRunning())
				npc.setRunning();
			else
				npc.setWalking();

			if (npc.getPlanDistanceSq(leader) > offset * offset)
			{
				int x1, y1, z1;
				x1 = Rnd.get(minRadius * 2, offset * 2); // x
				y1 = Rnd.get(x1, offset * 2); // distance
				y1 = (int) Math.sqrt(y1 * y1 - x1 * x1); // y

				if (x1 > offset + minRadius)
					x1 = leader.getX() + x1 - offset;
				else
					x1 = leader.getX() - x1 + minRadius;

				if (y1 > offset + minRadius)
					y1 = leader.getY() + y1 - offset;
				else
					y1 = leader.getY() - y1 + minRadius;

				z1 = leader.getZ();
				// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet
				// CharMoveToLocation (broadcast)
				moveTo(x1, y1, z1);
				return;
			}
			else if (Rnd.nextInt(RANDOM_WALK_RATE) == 0)
			{
				for (L2Skill sk : _skillrender.getBuffSkills())
				{
					if (cast(sk))
						return;
				}
			}
		}
		// Order to the L2MonsterInstance to random walk (1/100)
		else if (npc.getSpawn() != null && Rnd.nextInt(RANDOM_WALK_RATE) == 0 && !npc.isNoRndWalk())
		{
			int x1, y1, z1;
			final int range = NPCConfig.MAX_DRIFT_RANGE;

			for (L2Skill sk : _skillrender.getBuffSkills())
			{
				if (cast(sk))
					return;
			}

			x1 = npc.getSpawn().getLocx();
			y1 = npc.getSpawn().getLocy();
			z1 = npc.getSpawn().getLocz();

			if (!npc.isInsideRadius(x1, y1, range, false))
				npc.setIsReturningToSpawnPoint(true);
			else
			{
				x1 = Rnd.nextInt(range * 2); // x
				y1 = Rnd.get(x1, range * 2); // distance
				y1 = (int) Math.sqrt(y1 * y1 - x1 * x1); // y
				x1 += npc.getSpawn().getLocx() - range;
				y1 += npc.getSpawn().getLocy() - range;
				z1 = npc.getZ();
			}

			// Move the actor to Location (x,y,z)
			moveTo(x1, y1, z1);
		}
	}

	/**
	 * Manage AI attack thinks of a L2Attackable (called by onEvtThink).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Update the attack timeout if actor is running</li> <li>If target is dead or timeout is expired, stop this attack and set the Intention
	 * to AI_INTENTION_ACTIVE</li> <li>Call all L2Object of its Faction inside the Faction Range</li> <li>Chose a target and order to attack it
	 * with magic skill or physical attack</li><BR>
	 * <BR>
	 */
	private void thinkAttack()
	{
		final L2Attackable npc = getActiveChar();
		if (npc.isCastingNow())
			return;

		L2Character originalAttackTarget = getAttackTarget();
		// If target is dead / dced / teleported, or if timeout is expired
		if (originalAttackTarget == null || originalAttackTarget.isAlikeDead() || _attackTimeout < GameTimeController.getGameTicks())
		{
			// Stop hating this target
			if (originalAttackTarget != null)
				npc.stopHating(originalAttackTarget);

			// Set the AI Intention to AI_INTENTION_ACTIVE
			setIntention(AI_INTENTION_ACTIVE);

			npc.setWalking();
			return;
		}

		final int collision = npc.getTemplate().getCollisionRadius();

		// Handle all L2Object of its Faction inside the Faction Range
		String factionId = getActiveChar().getClan();
		if (factionId != null && !factionId.isEmpty())
		{
			int factionRange = npc.getClanRange() + collision;
			Collection<L2Object> objs = npc.getKnownList().getKnownObjects().values();
			for (L2Object obj : objs)
			{
				if (obj != null && obj instanceof L2Npc)
				{
					L2Npc called = (L2Npc) obj;

					final String npcfaction = called.getClan();
					if (npcfaction == null || npcfaction.isEmpty())
						continue;

					boolean sevenSignFaction = false;

					// TODO: Unhardcode this by AI scripts (DrHouse)
					// Catacomb mobs should assist lilim and nephilim other than dungeon
					if ("c_dungeon_clan".equals(factionId) && ("c_dungeon_lilim".equals(npcfaction) || "c_dungeon_nephi".equals(npcfaction)))
						sevenSignFaction = true;
					// Lilim mobs should assist other Lilim and catacomb mobs
					else if ("c_dungeon_lilim".equals(factionId) && "c_dungeon_clan".equals(npcfaction))
						sevenSignFaction = true;
					// Nephilim mobs should assist other Nephilim and catacomb mobs
					else if ("c_dungeon_nephi".equals(factionId) && "c_dungeon_clan".equals(npcfaction))
						sevenSignFaction = true;

					if (!factionId.equals(npcfaction) && !sevenSignFaction)
						continue;

					// Check if the L2Object is inside the Faction Range of the actor
					if ((npc.isInsideRadius(called, factionRange, true, false) && called.hasAI()) && GeoData.getInstance().canSeeTarget(npc, called) && npc.getAttackByList().contains(originalAttackTarget) && (called.getAI()._intention == CtrlIntention.AI_INTENTION_IDLE || called.getAI()._intention == CtrlIntention.AI_INTENTION_ACTIVE))
					{
						if (originalAttackTarget instanceof L2Playable)
						{
							Quest[] quests = called.getTemplate().getEventQuests(Quest.QuestEventType.ON_FACTION_CALL);
							if (quests != null)
							{
								L2PcInstance player = originalAttackTarget.getActingPlayer();
								boolean isSummon = originalAttackTarget instanceof L2Summon;
								for (Quest quest : quests)
									quest.notifyFactionCall(called, getActiveChar(), player, isSummon);
							}
						}
						else if (called instanceof L2Attackable && called.getAI()._intention != CtrlIntention.AI_INTENTION_ATTACK)
						{
							((L2Attackable) called).addDamageHate(originalAttackTarget, 0, npc.getHating(originalAttackTarget));
							called.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, originalAttackTarget);
						}
					}
				}
			}
		}

		if (npc.isCoreAIDisabled())
			return;

		// Initialize data
		L2Character mostHate = npc.getMostHated();
		if (mostHate == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}

		setAttackTarget(mostHate);
		npc.setTarget(mostHate);

		if (!_skillrender.getSuicideSkills().isEmpty() && (int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30)
		{
			final L2Skill skill = _skillrender.getSuicideSkills().get(Rnd.nextInt(_skillrender.getSuicideSkills().size()));
			if (Util.checkIfInRange(skill.getSkillRadius(), getActiveChar(), mostHate, false) && Rnd.get(100) < Rnd.get(npc.getMinSkillChance(), npc.getMaxSkillChance()))
			{
				if (cast(skill))
					return;

				for (L2Skill sk : _skillrender.getSuicideSkills())
				{
					if (cast(sk))
						return;
				}
			}
		}

		final int combinedCollision = collision + mostHate.getTemplate().getCollisionRadius();

		// ------------------------------------------------------
		// In case many mobs are trying to hit from same place, move a bit, circling around the target
		if (!npc.isMovementDisabled() && Rnd.nextInt(100) <= 3)
		{
			for (L2Object nearby : npc.getKnownList().getKnownObjects().values())
			{
				if (nearby instanceof L2Attackable && npc.isInsideRadius(nearby, collision, false, false) && nearby != mostHate)
				{
					int newX = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
						newX = mostHate.getX() + newX;
					else
						newX = mostHate.getX() - newX;

					int newY = combinedCollision + Rnd.get(40);
					if (Rnd.nextBoolean())
						newY = mostHate.getY() + newY;
					else
						newY = mostHate.getY() - newY;

					if (!npc.isInsideRadius(newX, newY, collision, false))
					{
						int newZ = npc.getZ() + 30;
						if (MainConfig.GEODATA == 0 || GeoData.getInstance().canMoveFromToTarget(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ))
							moveTo(newX, newY, newZ);
					}
					return;
				}
			}
		}

		/*
		 * Archer dodge behavior : running from target. The movement is according of player's position. Monster always flee by behind. Distance
		 * is always the same.
		 */
		if (!npc.isMovementDisabled() && npc.getAiType() == AIType.ARCHER)
		{
			if (Rnd.get(4) < 1)
			{
				double distance2 = npc.getPlanDistanceSq(mostHate.getX(), mostHate.getY());
				if (Math.sqrt(distance2) <= 60 + combinedCollision)
				{
					int posX = npc.getX();
					int posY = npc.getY();
					int posZ = npc.getZ() + 30;

					if (originalAttackTarget.getX() < posX)
						posX = posX + 300;
					else
						posX = posX - 300;

					if (originalAttackTarget.getY() < posY)
						posY = posY + 300;
					else
						posY = posY - 300;

					if (MainConfig.GEODATA == 0 || GeoData.getInstance().canMoveFromToTarget(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ))
						setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, 0));

					return;
				}
			}
		}

		// BOSS/Raid Minion Target Reconsider
		if (npc.isRaid() || npc.isRaidMinion())
		{
			chaostime++;
			if (npc instanceof L2RaidBossInstance)
			{
				if (!((L2MonsterInstance) npc).hasMinions())
				{
					if (chaostime > NPCConfig.RAID_CHAOS_TIME && (Rnd.get(100) <= 100 - (npc.getCurrentHp() * 100 / npc.getMaxHp())))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
				else
				{
					if (chaostime > NPCConfig.RAID_CHAOS_TIME && (Rnd.get(100) <= 100 - (npc.getCurrentHp() * 200 / npc.getMaxHp())))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
			}
			else if (npc instanceof L2GrandBossInstance)
			{
				if (chaostime > NPCConfig.GRAND_CHAOS_TIME)
				{
					double chaosRate = 100 - (npc.getCurrentHp() * 300 / npc.getMaxHp());
					if ((chaosRate <= 10 && Rnd.get(100) <= 10) || (chaosRate > 10 && Rnd.get(100) <= chaosRate))
					{
						aggroReconsider();
						chaostime = 0;
						return;
					}
				}
			}
			else
			{
				if (chaostime > NPCConfig.MINION_CHAOS_TIME && (Rnd.get(100) <= 100 - (npc.getCurrentHp() * 200 / npc.getMaxHp())))
				{
					aggroReconsider();
					chaostime = 0;
					return;
				}
			}
		}

		// -------------------------------------------------------------------------------
		// Heal Condition
		if (!_skillrender.getHealSkills().isEmpty())
		{
			double percentage = npc.getCurrentHp() / npc.getMaxHp() * 100;

			// First priority is to heal leader (if npc is a minion).
			if (npc.isMinion())
			{
				L2Character leader = npc.getLeader();
				if (leader != null && !leader.isDead() && Rnd.get(100) > (leader.getCurrentHp() / leader.getMaxHp() * 100))
				{
					for (L2Skill sk : _skillrender.getHealSkills())
					{
						if (sk.getTargetType() == SkillTargetType.TARGET_SELF)
							continue;

						if (!checkSkillCastConditions(sk))
							continue;

						if (!Util.checkIfInRange((sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(sk) && !npc.isMovementDisabled())
						{
							moveToPawn(leader, sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
							return;
						}

						if (GeoData.getInstance().canSeeTarget(npc, leader))
						{
							clientStopMoving(null);
							npc.setTarget(leader);
							clientStopMoving(null);
							npc.doCast(sk);
							return;
						}
					}
				}
			}

			// Second priority is to heal himself.
			if (Rnd.get(100) < (100 - percentage) / 3)
			{
				for (L2Skill sk : _skillrender.getHealSkills())
				{
					if (!checkSkillCastConditions(sk))
						continue;

					clientStopMoving(null);
					npc.setTarget(npc);
					npc.doCast(sk);
					return;
				}
			}

			for (L2Skill sk : _skillrender.getHealSkills())
			{
				if (!checkSkillCastConditions(sk))
					continue;

				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					for (L2Character obj : npc.getKnownList().getKnownCharactersInRadius(sk.getCastRange() + collision))
					{
						if (!(obj instanceof L2Attackable) || obj.isDead())
							continue;

						L2Attackable targets = ((L2Attackable) obj);
						if (npc.getClan() != null && !npc.getClan().equals(targets.getClan()))
							continue;

						percentage = targets.getCurrentHp() / targets.getMaxHp() * 100;
						if (Rnd.get(100) < (100 - percentage) / 10)
						{
							if (GeoData.getInstance().canSeeTarget(npc, targets))
							{
								clientStopMoving(null);
								npc.setTarget(obj);
								npc.doCast(sk);
								return;
							}
						}
					}

					if (isParty(sk))
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}
		}

		double dist = Math.sqrt(npc.getPlanDistanceSq(mostHate.getX(), mostHate.getY()));
		int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (mostHate.isMoving())
		{
			range = range + 50;
			if (npc.isMoving())
				range = range + 50;
		}

		// -------------------------------------------------------------------------------
		// Immobilize Condition
		if ((npc.isMovementDisabled() && (dist > range || mostHate.isMoving())) || (dist > range && mostHate.isMoving()))
		{
			movementDisable();
			return;
		}

		// --------------------------------------------------------------------------------
		// General Skill Use
		if (!_skillrender.getGeneralSkills().isEmpty())
		{
			if (Rnd.get(100) < Rnd.get(npc.getMinSkillChance(), npc.getMaxSkillChance()))
			{
				L2Skill skills = _skillrender.getGeneralSkills().get(Rnd.nextInt(_skillrender.getGeneralSkills().size()));
				if (cast(skills))
					return;

				for (L2Skill sk : _skillrender.getGeneralSkills())
				{
					if (cast(sk))
						return;
				}
			}

			// --------------------------------------------------------------------------------
			// Long/Short Range skill Usage
			if (npc.hasLongRangeSkill() || npc.hasShortRangeSkill())
			{
				final List<L2Skill> longRangeSkillsList = longRangeSkillRender();
				if (!longRangeSkillsList.isEmpty() && npc.hasLongRangeSkill() && dist2 > 150 && Rnd.get(100) <= npc.getLongRangeSkillChance())
				{
					final L2Skill longRangeSkill = longRangeSkillsList.get(Rnd.get(longRangeSkillsList.size()));
					if (cast(longRangeSkill))
						return;

					for (L2Skill sk : longRangeSkills)
					{
						if (cast(sk))
							return;
					}
				}

				final List<L2Skill> shortRangeSkillsList = shortRangeSkillRender();
				if (!shortRangeSkillsList.isEmpty() && npc.hasShortRangeSkill() && dist2 <= 150 && Rnd.get(100) <= npc.getShortRangeSkillChance())
				{
					final L2Skill shortRangeSkill = shortRangeSkillsList.get(Rnd.get(shortRangeSkillsList.size()));
					if (cast(shortRangeSkill))
						return;

					for (L2Skill sk : shortRangeSkills)
					{
						if (cast(sk))
							return;
					}
				}
			}
		}

		// --------------------------------------------------------------------------------
		// Starts Melee or Primary Skill
		if (maybeMoveToPawn(getAttackTarget(), npc.getPhysicalAttackRange()))
			return;

		clientStopMoving(null);
		melee(npc.getPrimaryAttack());
	}

	private void melee(int type)
	{
		if (type != 0)
		{
			switch (type)
			{
				case -1:
					if (_skillrender.getGeneralSkills() != null)
					{
						for (L2Skill sk : _skillrender.getGeneralSkills())
						{
							if (cast(sk))
								return;
						}
					}
					break;

				case 1:
					for (L2Skill sk : _skillrender.getAtkSkills())
					{
						if (cast(sk))
							return;
					}
					break;

				default:
					for (L2Skill sk : _skillrender.getGeneralSkills())
					{
						if (sk.getId() == getActiveChar().getPrimaryAttack())
							if (cast(sk))
								return;
					}
					break;
			}
		}
		_accessor.doAttack(getAttackTarget());
	}

	private boolean cast(L2Skill sk)
	{
		if (sk == null)
			return false;

		final L2Attackable caster = getActiveChar();

		if (caster.isCastingNow() && !sk.isSimultaneousCast())
			return false;

		if (!checkSkillCastConditions(sk))
			return false;

		if (getAttackTarget() == null)
			if (caster.getMostHated() != null)
				setAttackTarget(caster.getMostHated());

		L2Character attackTarget = getAttackTarget();
		if (attackTarget == null)
			return false;

		double dist = Math.sqrt(caster.getPlanDistanceSq(attackTarget.getX(), attackTarget.getY()));
		double dist2 = dist - attackTarget.getTemplate().getCollisionRadius();
		double range = caster.getPhysicalAttackRange() + caster.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
		double srange = sk.getCastRange() + caster.getTemplate().getCollisionRadius();
		if (attackTarget.isMoving())
			dist2 = dist2 - 30;

		switch (sk.getSkillType())
		{
			case BUFF:
			{
				if (caster.getFirstEffect(sk) == null)
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					return true;
				}

				// ----------------------------------------
				// If actor already have buff, start looking at others same faction mob to cast
				if (sk.getTargetType() == SkillTargetType.TARGET_SELF)
					return false;

				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, true);
					if (target != null)
					{
						clientStopMoving(null);
						L2Object targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}

				if (canParty(sk))
				{
					clientStopMoving(null);
					L2Object targets = attackTarget;
					caster.setTarget(caster);
					caster.doCast(sk);
					caster.setTarget(targets);
					return true;
				}
				break;
			}

			case HEAL:
			case HOT:
			case HEAL_PERCENT:
			case HEAL_STATIC:
			case BALANCE_LIFE:
			{
				double percentage = caster.getCurrentHp() / caster.getMaxHp() * 100;
				if (caster.isMinion() && sk.getTargetType() != SkillTargetType.TARGET_SELF)
				{
					L2Character leader = caster.getLeader();
					if (leader != null && !leader.isDead() && Rnd.get(100) > (leader.getCurrentHp() / leader.getMaxHp() * 100))
					{
						if (!Util.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
							moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());

						if (GeoData.getInstance().canSeeTarget(caster, leader))
						{
							clientStopMoving(null);
							caster.setTarget(leader);
							caster.doCast(sk);
							return true;
						}
					}
				}

				if (Rnd.get(100) < (100 - percentage) / 3)
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					return true;
				}

				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					for (L2Character obj : caster.getKnownList().getKnownCharactersInRadius(sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
					{
						if (!(obj instanceof L2Attackable) || obj.isDead())
							continue;

						L2Attackable targets = ((L2Attackable) obj);
						if (caster.getClan() != null && !caster.getClan().equals(targets.getClan()))
							continue;

						percentage = targets.getCurrentHp() / targets.getMaxHp() * 100;
						if (Rnd.get(100) < (100 - percentage) / 10)
						{
							if (GeoData.getInstance().canSeeTarget(caster, targets))
							{
								clientStopMoving(null);
								caster.setTarget(obj);
								caster.doCast(sk);
								return true;
							}
						}
					}
				}

				if (isParty(sk))
				{
					for (L2Character obj : caster.getKnownList().getKnownCharactersInRadius(sk.getSkillRadius() + caster.getTemplate().getCollisionRadius()))
					{
						if (!(obj instanceof L2Attackable))
							continue;

						L2Npc targets = ((L2Npc) obj);
						if (caster.getClan() != null && targets.getClan().equals(caster.getClan()))
						{
							if (obj.getCurrentHp() < obj.getMaxHp() && Rnd.get(100) <= 20)
							{
								clientStopMoving(null);
								caster.setTarget(caster);
								caster.doCast(sk);
								return true;
							}
						}
					}
				}
				break;
			}

			case DEBUFF:
			case POISON:
			case DOT:
			case MDOT:
			case BLEED:
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && dist2 <= srange)
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if (sk.getTargetType() == SkillTargetType.TARGET_AURA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}

					if ((sk.getTargetType() == SkillTargetType.TARGET_AREA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && dist2 <= srange)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}

			case SLEEP:
			{
				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					if (!attackTarget.isDead() && dist2 <= srange)
					{
						if (dist2 > range || attackTarget.isMoving())
						{
							if (attackTarget.getFirstEffect(sk) == null)
							{
								clientStopMoving(null);
								caster.doCast(sk);
								return true;
							}
						}
					}

					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if (sk.getTargetType() == SkillTargetType.TARGET_AURA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}

					if ((sk.getTargetType() == SkillTargetType.TARGET_AREA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && dist2 <= srange)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}

			case ROOT:
			case STUN:
			case PARALYZE:
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && dist2 <= srange)
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if (sk.getTargetType() == SkillTargetType.TARGET_AURA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					else if ((sk.getTargetType() == SkillTargetType.TARGET_AREA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && dist2 <= srange)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}

			case MUTE:
			case FEAR:
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && dist2 <= srange)
				{
					if (attackTarget.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if (sk.getTargetType() == SkillTargetType.TARGET_AURA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}

					if ((sk.getTargetType() == SkillTargetType.TARGET_AREA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && dist2 <= srange)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}

			case CANCEL:
			case NEGATE:
			{
				// decrease cancel probability
				if (Rnd.get(50) != 0)
					return true;

				if (sk.getTargetType() == SkillTargetType.TARGET_ONE)
				{
					if (attackTarget.getFirstEffect(L2EffectType.BUFF) != null && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && dist2 <= srange)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}

					L2Character target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						L2Object targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == SkillTargetType.TARGET_AURA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AURA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AURA) && GeoData.getInstance().canSeeTarget(caster, attackTarget))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					else if ((sk.getTargetType() == SkillTargetType.TARGET_AREA || sk.getTargetType() == SkillTargetType.TARGET_BEHIND_AREA || sk.getTargetType() == SkillTargetType.TARGET_FRONT_AREA) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && dist2 <= srange)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				break;
			}

			default:
			{
				if (!canAura(sk))
				{
					if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && dist2 <= srange)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}

					L2Character target = skillTargetReconsider(sk);
					if (target != null)
					{
						clientStopMoving(null);
						L2Object targets = attackTarget;
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(targets);
						return true;
					}
				}
				else
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
				break;
		}

		return false;
	}

	/**
	 * This AI task will start when ACTOR cannot move and attack range larger than distance
	 */
	private void movementDisable()
	{
		final L2Attackable npc = getActiveChar();
		if (npc == null)
			return;

		final L2Character victim = getAttackTarget();
		if (victim == null)
		{
			setIntention(AI_INTENTION_ACTIVE);
			return;
		}

		if (npc.getTarget() == null)
			npc.setTarget(victim);

		double dist = Math.sqrt(npc.getPlanDistanceSq(victim.getX(), victim.getY()));
		double dist2 = dist - npc.getTemplate().getCollisionRadius();
		int range = npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius();

		if (victim.isMoving())
		{
			dist = dist - 30;
			if (npc.isMoving())
				dist = dist - 50;
		}

		// Check if activeChar has any skill
		if (!_skillrender.getGeneralSkills().isEmpty())
		{
			// Try to stop the target or disable the target as priority
			int random = Rnd.get(100);
			if (random < 2 && !_skillrender.getImmobilizeSkills().isEmpty() && !victim.isImmobilized())
			{
				for (L2Skill sk : _skillrender.getImmobilizeSkills())
				{
					if (!checkSkillCastConditions(sk) || (sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius() <= dist2 && !canAura(sk)))
						continue;

					if (!GeoData.getInstance().canSeeTarget(npc, victim))
						continue;

					if (victim.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}

			// Same as above, but with Mute/FEAR etc....
			if (random < 5 && !_skillrender.getCostOverTimeSkills().isEmpty())
			{
				for (L2Skill sk : _skillrender.getCostOverTimeSkills())
				{
					if (!checkSkillCastConditions(sk) || (sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius() <= dist2 && !canAura(sk)))
						continue;

					if (!GeoData.getInstance().canSeeTarget(npc, victim))
						continue;

					if (victim.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}

			// Try to debuff target
			if (random < 8 && !_skillrender.getDebuffSkills().isEmpty())
			{
				for (L2Skill sk : _skillrender.getDebuffSkills())
				{
					if (!checkSkillCastConditions(sk) || (sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius() <= dist2 && !canAura(sk)))
						continue;

					if (!GeoData.getInstance().canSeeTarget(npc, victim))
						continue;

					if (victim.getFirstEffect(sk) == null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}

			// Try to debuff target ; side effect skill like CANCEL or NEGATE
			if (random < 9 && !_skillrender.getNegativeSkills().isEmpty())
			{
				for (L2Skill sk : _skillrender.getNegativeSkills())
				{
					if (!checkSkillCastConditions(sk) || (sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius() <= dist2 && !canAura(sk)))
						continue;

					if (!GeoData.getInstance().canSeeTarget(npc, victim))
						continue;

					if (victim.getFirstEffect(L2EffectType.BUFF) != null)
					{
						clientStopMoving(null);
						npc.doCast(sk);
						return;
					}
				}
			}

			// Start ATK SKILL when nothing can be done
			if (!_skillrender.getAtkSkills().isEmpty() && (npc.isMovementDisabled() || npc.getAiType() == AIType.MAGE || npc.getAiType() == AIType.HEALER))
			{
				for (L2Skill sk : _skillrender.getAtkSkills())
				{
					if (!checkSkillCastConditions(sk) || (sk.getCastRange() + npc.getTemplate().getCollisionRadius() + victim.getTemplate().getCollisionRadius() <= dist2 && !canAura(sk)))
						continue;

					if (!GeoData.getInstance().canSeeTarget(npc, victim))
						continue;

					clientStopMoving(null);
					npc.doCast(sk);
					return;
				}
			}
		}

		if (npc.isMovementDisabled())
		{
			targetReconsider();
			return;
		}

		if (dist > range || !GeoData.getInstance().canSeeTarget(npc, victim))
		{
			if (victim.isMoving())
				range -= 100;

			if (range < 5)
				range = 5;

			moveToPawn(victim, range);
			return;
		}

		melee(npc.getPrimaryAttack());
	}

	/**
	 * @param skill
	 *            the skill to check.
	 * @return {@code true} if the skill is available for casting {@code false} otherwise.
	 */
	private boolean checkSkillCastConditions(L2Skill skill)
	{
		// Not enough MP.
		if (skill.getMpConsume() >= getActiveChar().getCurrentMp())
			return false;

		// Character is in "skill disabled" mode.
		if (getActiveChar().isSkillDisabled(skill))
			return false;

		// Is a magic skill and character is magically muted or is a physical skill and character is physically muted.
		if ((skill.isMagic() && getActiveChar().isMuted()) || getActiveChar().isPhysicalMuted())
			return false;

		return true;
	}

	private L2Character effectTargetReconsider(L2Skill sk, boolean positive)
	{
		if (sk == null)
			return null;

		L2Attackable actor = getActiveChar();
		if (sk.getSkillType() != L2SkillType.NEGATE || sk.getSkillType() != L2SkillType.CANCEL)
		{
			if (!positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;

				for (L2Character obj : actor.getAttackByList())
				{
					if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || obj == getAttackTarget())
						continue;

					actor.setTarget(getAttackTarget());
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
						dist2 = dist2 - 70;

					if (dist2 <= range)
					{
						if (getAttackTarget().getFirstEffect(sk) == null)
							return obj;
					}
				}

				// If there is nearby Target with aggro, start going on random target that is attackable
				for (L2Character obj : actor.getKnownList().getKnownCharactersInRadius(range))
				{
					if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
						continue;

					actor.setTarget(getAttackTarget());
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist;
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
						dist2 = dist2 - 70;

					if (obj instanceof L2Attackable)
					{
						if (actor.getEnemyClan() != null && actor.getEnemyClan().equals(((L2Attackable) obj).getClan()))
						{
							if (dist2 <= range)
							{
								if (getAttackTarget().getFirstEffect(sk) == null)
									return obj;
							}
						}
					}
					else if (obj instanceof L2PcInstance || obj instanceof L2Summon)
					{
						if (dist2 <= range)
						{
							if (getAttackTarget().getFirstEffect(sk) == null)
								return obj;
						}
					}
				}
			}
			else if (positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;

				for (L2Character obj : actor.getKnownList().getKnownCharactersInRadius(range))
				{
					if (obj == null || obj.isDead() || !(obj instanceof L2Attackable) || !GeoData.getInstance().canSeeTarget(actor, obj))
						continue;

					L2Attackable targets = ((L2Attackable) obj);
					if (actor.getClan() != null && !actor.getClan().equals(targets.getClan()))
						continue;

					actor.setTarget(getAttackTarget());
					dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
						dist2 = dist2 - 70;

					if (dist2 <= range)
					{
						if (obj.getFirstEffect(sk) == null)
							return obj;
					}
				}
			}
			return null;
		}

		double dist = 0;
		double dist2 = 0;
		int range = 0;
		range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();

		for (L2Character obj : actor.getKnownList().getKnownCharactersInRadius(range))
		{
			if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
				continue;

			actor.setTarget(getAttackTarget());
			dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
			dist2 = dist - actor.getTemplate().getCollisionRadius();
			range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
			if (obj.isMoving())
				dist2 = dist2 - 70;

			if (obj instanceof L2Attackable)
			{
				if (actor.getEnemyClan() != null && actor.getEnemyClan().equals(((L2Attackable) obj).getClan()))
				{
					if (dist2 <= range)
					{
						if (getAttackTarget().getFirstEffect(L2EffectType.BUFF) != null)
							return obj;
					}
				}
			}
			else if (obj instanceof L2PcInstance || obj instanceof L2Summon)
			{
				if (dist2 <= range)
				{
					if (getAttackTarget().getFirstEffect(L2EffectType.BUFF) != null)
						return obj;
				}
			}
		}
		return null;
	}

	private L2Character skillTargetReconsider(L2Skill sk)
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		L2Attackable actor = getActiveChar();

		if (actor.getHateList() != null)
		{
			for (L2Character obj : actor.getHateList())
			{
				if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
					continue;

				actor.setTarget(getAttackTarget());
				dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
				dist2 = dist - actor.getTemplate().getCollisionRadius();
				range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();

				if (dist2 <= range)
					return obj;
			}
		}

		if (!(actor instanceof L2GuardInstance))
		{
			Collection<L2Object> objs = actor.getKnownList().getKnownObjects().values();
			for (L2Object target : objs)
			{
				if (target == null)
					continue;

				actor.setTarget(getAttackTarget());
				dist = Math.sqrt(actor.getPlanDistanceSq(target.getX(), target.getY()));
				dist2 = dist;
				range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();

				L2Character obj = null;
				if (target instanceof L2Character)
					obj = (L2Character) target;

				if (obj == null || !GeoData.getInstance().canSeeTarget(actor, obj) || dist2 > range)
					continue;

				if (obj instanceof L2PcInstance)
					return obj;

				if (obj instanceof L2Attackable)
				{
					if (actor.getEnemyClan() != null && actor.getEnemyClan().equals(((L2Attackable) obj).getClan()))
						return obj;

					if (actor.getIsChaos() != 0)
					{
						if (((L2Attackable) obj).getClan() != null && ((L2Attackable) obj).getClan().equals(actor.getClan()))
							continue;

						return obj;
					}
				}

				if (obj instanceof L2Summon)
					return obj;
			}
		}
		return null;
	}

	private void targetReconsider()
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		L2Attackable actor = getActiveChar();
		L2Character MostHate = actor.getMostHated();

		if (actor.getHateList() != null)
		{
			for (L2Character obj : actor.getHateList())
			{
				if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || obj != MostHate || obj == actor)
					continue;

				dist = Math.sqrt(actor.getPlanDistanceSq(obj.getX(), obj.getY()));
				dist2 = dist - actor.getTemplate().getCollisionRadius();
				range = actor.getPhysicalAttackRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
				if (obj.isMoving())
					dist2 = dist2 - 70;

				if (dist2 <= range)
				{
					if (MostHate != null)
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					else
						actor.addDamageHate(obj, 0, 2000);

					actor.setTarget(obj);
					setAttackTarget(obj);
					return;
				}
			}
		}

		if (!(actor instanceof L2GuardInstance))
		{
			Collection<L2Object> objs = actor.getKnownList().getKnownObjects().values();
			for (L2Object target : objs)
			{
				if (target == null)
					continue;

				L2Character obj = null;
				if (target instanceof L2Character)
					obj = (L2Character) target;

				if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || obj != MostHate || obj == actor || obj == getAttackTarget())
					continue;

				if (obj instanceof L2PcInstance)
				{
					if (MostHate != null)
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					else
						actor.addDamageHate(obj, 0, 2000);

					actor.setTarget(obj);
					setAttackTarget(obj);

				}
				else if (obj instanceof L2Attackable)
				{
					if (actor.getEnemyClan() != null && actor.getEnemyClan().equals(((L2Attackable) obj).getClan()))
					{
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
						actor.setTarget(obj);
					}

					if (actor.getIsChaos() != 0)
					{
						if (((L2Attackable) obj).getClan() != null && ((L2Attackable) obj).getClan().equals(actor.getClan()))
							continue;

						if (MostHate != null)
							actor.addDamageHate(obj, 0, actor.getHating(MostHate));
						else
							actor.addDamageHate(obj, 0, 2000);

						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj instanceof L2Summon)
				{
					if (MostHate != null)
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					else
						actor.addDamageHate(obj, 0, 2000);

					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			}
		}
	}

	private void aggroReconsider()
	{
		L2Attackable actor = getActiveChar();
		L2Character MostHate = actor.getMostHated();

		if (actor.getHateList() != null)
		{
			int rand = Rnd.get(actor.getHateList().size());
			int count = 0;

			for (L2Character obj : actor.getHateList())
			{
				if (count < rand)
				{
					count++;
					continue;
				}

				if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || obj == getAttackTarget() || obj == actor)
					continue;

				actor.setTarget(getAttackTarget());

				if (MostHate != null)
					actor.addDamageHate(obj, 0, actor.getHating(MostHate));
				else
					actor.addDamageHate(obj, 0, 2000);

				actor.setTarget(obj);
				setAttackTarget(obj);
				return;

			}
		}

		if (!(actor instanceof L2GuardInstance))
		{
			Collection<L2Object> objs = actor.getKnownList().getKnownObjects().values();
			for (L2Object target : objs)
			{
				if (target == null)
					continue;

				L2Character obj = null;
				if (target instanceof L2Character)
					obj = (L2Character) target;

				if (obj == null || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || obj != MostHate || obj == actor)
					continue;

				if (obj instanceof L2PcInstance)
				{
					if (MostHate != null && !MostHate.isDead())
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					else
						actor.addDamageHate(obj, 0, 2000);

					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj instanceof L2Attackable)
				{
					if (actor.getEnemyClan() != null && actor.getEnemyClan().equals(((L2Attackable) obj).getClan()))
					{
						if (MostHate != null)
							actor.addDamageHate(obj, 0, actor.getHating(MostHate));
						else
							actor.addDamageHate(obj, 0, 2000);

						actor.setTarget(obj);
					}

					if (actor.getIsChaos() != 0)
					{
						if (((L2Attackable) obj).getClan() != null && ((L2Attackable) obj).getClan().equals(actor.getClan()))
							continue;

						if (MostHate != null)
							actor.addDamageHate(obj, 0, actor.getHating(MostHate));
						else
							actor.addDamageHate(obj, 0, 2000);

						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj instanceof L2Summon)
				{
					if (MostHate != null)
						actor.addDamageHate(obj, 0, actor.getHating(MostHate));
					else
						actor.addDamageHate(obj, 0, 2000);

					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			}
		}
	}

	private List<L2Skill> longRangeSkillRender()
	{
		longRangeSkills = _skillrender.getLongRangeSkills();
		if (longRangeSkills.isEmpty())
			longRangeSkills = getActiveChar().getLongRangeSkill();

		return longRangeSkills;
	}

	private List<L2Skill> shortRangeSkillRender()
	{
		shortRangeSkills = _skillrender.getShortRangeSkills();
		if (shortRangeSkills.isEmpty())
			shortRangeSkills = getActiveChar().getShortRangeSkill();

		return shortRangeSkills;
	}

	/**
	 * Manage AI thinking actions of a L2Attackable.
	 */
	@Override
	protected void onEvtThink()
	{
		// Check if the actor can't use skills and if a thinking action isn't already in progress
		if (_thinking || getActiveChar().isAllSkillsDisabled())
			return;

		// Start thinking action
		_thinking = true;

		try
		{
			// Manage AI thoughts
			switch (getIntention())
			{
				case AI_INTENTION_ACTIVE:
					thinkActive();
					break;
				case AI_INTENTION_ATTACK:
					thinkAttack();
					break;
				case AI_INTENTION_CAST:
					thinkCast();
					break;
			}
		}
		finally
		{
			// Stop thinking action
			_thinking = false;
		}
	}

	/**
	 * Launch actions corresponding to the Event Attacked.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Init the attack : Calculate the attack timeout, Set the _globalAggro to 0, Add the attacker to the actor _aggroList</li> <li>Set the
	 * L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance</li> <li>Set the Intention to
	 * AI_INTENTION_ATTACK</li> <BR>
	 * <BR>
	 * 
	 * @param attacker
	 *            The L2Character that attacks the actor
	 */
	@Override
	protected void onEvtAttacked(L2Character attacker)
	{
		L2Attackable me = getActiveChar();

		// Calculate the attack timeout
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeController.getGameTicks();

		// Set the _globalAggro to 0 to permit attack even just after spawn
		if (_globalAggro < 0)
			_globalAggro = 0;

		// Add the attacker to the _aggroList of the actor
		me.addDamageHate(attacker, 0, 1);

		// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance
		if (!me.isRunning())
			me.setRunning();

		// Set the Intention to AI_INTENTION_ATTACK
		if (getIntention() != AI_INTENTION_ATTACK)
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);
		else if (me.getMostHated() != getAttackTarget())
			setIntention(CtrlIntention.AI_INTENTION_ATTACK, attacker);

		if (me instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance) me;

			if (master.hasMinions())
				master.getMinionList().onAssist(me, attacker);

			master = master.getLeader();
			if (master != null && master.hasMinions())
				master.getMinionList().onAssist(me, attacker);
		}

		super.onEvtAttacked(attacker);
	}

	/**
	 * Launch actions corresponding to the Event Aggression.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the target to the actor _aggroList or update hate if already present</li> <li>Set the actor Intention to AI_INTENTION_ATTACK (if
	 * actor is L2GuardInstance check if it isn't too far from its home location)</li><BR>
	 * <BR>
	 * 
	 * @param target
	 *            The L2Character that attacks
	 * @param aggro
	 *            The value of hate to add to the actor against the target
	 */
	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{
		L2Attackable me = getActiveChar();

		if (target != null)
		{
			// Add the target to the actor _aggroList or update hate if already present
			me.addDamageHate(target, 0, aggro);

			// Set the actor AI Intention to AI_INTENTION_ATTACK
			if (getIntention() != CtrlIntention.AI_INTENTION_ATTACK)
			{
				// Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others
				// L2PcInstance
				if (!me.isRunning())
					me.setRunning();

				setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
			}

			if (me instanceof L2MonsterInstance)
			{
				L2MonsterInstance master = (L2MonsterInstance) me;

				if (master.hasMinions())
					master.getMinionList().onAssist(me, target);

				master = master.getLeader();
				if (master != null && master.hasMinions())
					master.getMinionList().onAssist(me, target);
			}
		}
	}

	@Override
	protected void onIntentionActive()
	{
		// Cancel attack timeout
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}

	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}

	public L2Attackable getActiveChar()
	{
		return (L2Attackable) _actor;
	}
}