/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor;

import java.util.ArrayList;
import java.util.List;

import javolution.util.FastList;
import javolution.util.FastMap;
import silentium.commons.utils.Rnd;
import silentium.gameserver.ItemsAutoDestroy;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.ai.AttackableAI;
import silentium.gameserver.ai.CharacterAI;
import silentium.gameserver.ai.CtrlEvent;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.ai.SiegeGuardAI;
import silentium.gameserver.configs.CustomConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.NPCConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.data.xml.HerbDropData;
import silentium.gameserver.instancemanager.CursedWeaponsManager;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.L2CommandChannel;
import silentium.gameserver.model.L2DropCategory;
import silentium.gameserver.model.L2DropData;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Manor;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.instance.L2MonsterInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.model.actor.instance.L2SummonInstance;
import silentium.gameserver.model.actor.knownlist.AttackableKnownList;
import silentium.gameserver.model.actor.status.AttackableStatus;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.CreatureSay;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.templates.item.L2EtcItemType;
import silentium.gameserver.utils.Util;

/**
 * This class manages all NPC that can be attacked.<BR>
 * <BR>
 * L2Attackable :<BR>
 * <BR>
 * <li>L2ArtefactInstance</li> <li>L2FriendlyMobInstance</li> <li>L2MonsterInstance</li> <li>L2SiegeGuardInstance</li>
 */
public class L2Attackable extends L2Npc
{
	private boolean _isRaid = false;
	private boolean _isRaidMinion = false;

	/**
	 * This class contains all AggroInfo of the L2Attackable against the attacker L2Character.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>attacker : The attacker L2Character concerned by this AggroInfo of this L2Attackable</li> <li>hate : Hate level of this L2Attackable
	 * against the attacker L2Character (hate = damage)</li> <li>damage : Number of damages that the attacker L2Character gave to this
	 * L2Attackable</li><BR>
	 * <BR>
	 */
	public static final class AggroInfo
	{
		private final L2Character _attacker;
		private int _hate = 0;
		private int _damage = 0;

		AggroInfo(L2Character pAttacker)
		{
			_attacker = pAttacker;
		}

		public final L2Character getAttacker()
		{
			return _attacker;
		}

		public final int getHate()
		{
			return _hate;
		}

		public final int checkHate(L2Character owner)
		{
			if (_attacker.isAlikeDead() || !_attacker.isVisible() || !owner.getKnownList().knowsObject(_attacker))
				_hate = 0;

			return _hate;
		}

		public final void addHate(int value)
		{
			_hate = (int) Math.min(_hate + (long) value, 999999999);
		}

		public final void stopHate()
		{
			_hate = 0;
		}

		public final int getDamage()
		{
			return _damage;
		}

		public final void addDamage(int value)
		{
			_damage = (int) Math.min(_damage + (long) value, 999999999);
		}

		@Override
		public final boolean equals(Object obj)
		{
			if (this == obj)
				return true;

			if (obj instanceof AggroInfo)
				return (((AggroInfo) obj).getAttacker() == _attacker);

			return false;
		}

		@Override
		public final int hashCode()
		{
			return _attacker.getObjectId();
		}
	}

	/**
	 * This class contains all RewardInfo of the L2Attackable against the any attacker L2Character, based on amount of damage done.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>attacker : The attaker L2Character concerned by this RewardInfo of this L2Attackable</li> <li>dmg : Total amount of damage done by the
	 * attacker to this L2Attackable (summon + own)</li>
	 */
	protected static final class RewardInfo
	{
		protected L2Character _attacker;
		protected int _dmg = 0;

		public RewardInfo(L2Character pAttacker, int pDmg)
		{
			_attacker = pAttacker;
			_dmg = pDmg;
		}

		public void addDamage(int pDmg)
		{
			_dmg += pDmg;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;

			if (obj instanceof RewardInfo)
				return (((RewardInfo) obj)._attacker == _attacker);

			return false;
		}

		@Override
		public int hashCode()
		{
			return _attacker.getObjectId();
		}
	}

	/**
	 * This class contains all AbsorberInfo of the L2Attackable against the absorber L2Character. Data: absorber : The attacker L2Character
	 * concerned by this AbsorberInfo of this L2Attackable
	 */
	public static final class AbsorberInfo
	{
		public int _objId;
		public double _absorbedHP;

		AbsorberInfo(int objId, double pAbsorbedHP)
		{
			_objId = objId;
			_absorbedHP = pAbsorbedHP;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;

			if (obj instanceof AbsorberInfo)
				return (((AbsorberInfo) obj)._objId == _objId);

			return false;
		}

		@Override
		public int hashCode()
		{
			return _objId;
		}
	}

	/**
	 * This class is used to create item reward lists instead of creating item instances.<BR>
	 * <BR>
	 */
	public static final class RewardItem
	{
		protected int _itemId;
		protected int _count;

		public RewardItem(int itemId, int count)
		{
			_itemId = itemId;
			_count = count;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getCount()
		{
			return _count;
		}
	}

	private final FastMap<L2Character, AggroInfo> _aggroList = new FastMap<L2Character, AggroInfo>().shared();

	public final FastMap<L2Character, AggroInfo> getAggroList()
	{
		return _aggroList;
	}

	private boolean _isReturningToSpawnPoint = false;
	private boolean _seeThroughSilentMove = false;

	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}

	public final void setIsReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}

	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}

	public void seeThroughSilentMove(boolean val)
	{
		_seeThroughSilentMove = val;
	}

	private RewardItem[] _sweepItems;

	private RewardItem[] _harvestItems;
	private boolean _seeded;
	private int _seedType = 0;
	private int _seederObjId = 0;

	private boolean _overhit;
	private double _overhitDamage;
	private L2Character _overhitAttacker;

	private L2CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;
	private long _commandChannelLastAttack = 0;

	private boolean _absorbed;
	private final FastMap<Integer, AbsorberInfo> _absorbersList = new FastMap<Integer, AbsorberInfo>().shared();

	private boolean _mustGiveExpSp;

	private int _onKillDelay = 5000;

	/**
	 * Constructor of L2Attackable (use L2Character and L2Npc constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2Attackable (copy skills from template to object and link _calculators
	 * to NPC_STD_CALCULATOR)</li> <li>Set the name of the L2Attackable</li> <li>Create a RandomAnimation Task that will be launched after the
	 * calculated delay if the server allow it</li><BR>
	 * <BR>
	 * 
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            Template to apply to the NPC
	 */
	public L2Attackable(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		_mustGiveExpSp = true;
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new AttackableKnownList(this));
	}

	@Override
	public AttackableKnownList getKnownList()
	{
		return (AttackableKnownList) super.getKnownList();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}

	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}

	/**
	 * Return the L2Character AI of the L2Attackable and if its null create a new one.<BR>
	 * <BR>
	 */
	@Override
	public CharacterAI getAI()
	{
		CharacterAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new AttackableAI(new AIAccessor());

				return _ai;
			}
		}
		return ai;
	}

	public void useMagic(L2Skill skill)
	{
		if (skill == null || isAlikeDead())
			return;

		if (skill.isPassive())
			return;

		if (isCastingNow())
			return;

		if (isSkillDisabled(skill))
			return;

		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
			return;

		if (getCurrentHp() <= skill.getHpConsume())
			return;

		if (skill.isMagic())
		{
			if (isMuted())
				return;
		}
		else
		{
			if (isPhysicalMuted())
				return;
		}

		L2Object target = skill.getFirstOfTargetList(this);
		if (target == null)
			return;

		getAI().setIntention(CtrlIntention.AI_INTENTION_CAST, skill, target);
	}

	/**
	 * Reduce the current HP of the L2Attackable.<BR>
	 * <BR>
	 * 
	 * @param damage
	 *            The HP decrease value
	 * @param attacker
	 *            The L2Character who attacks
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill)
	{
		reduceCurrentHp(damage, attacker, true, false, skill);
	}

	/**
	 * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.
	 * 
	 * @param attacker
	 *            The L2Character who attacks
	 * @param awake
	 *            The awake state (If True : stop sleeping)
	 */
	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (isRaid() && !isMinion() && attacker != null && attacker.getParty() != null && attacker.getParty().isInCommandChannel() && attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
		{
			if (_firstCommandChannelAttacked == null) // looting right isn't set
			{
				synchronized (this)
				{
					if (_firstCommandChannelAttacked == null)
					{
						_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
						if (_firstCommandChannelAttacked != null)
						{
							_commandChannelTimer = new CommandChannelTimer(this);
							_commandChannelLastAttack = System.currentTimeMillis();
							ThreadPoolManager.getInstance().scheduleGeneral(_commandChannelTimer, 10000); // check for last attack
							_firstCommandChannelAttacked.broadcastToChannelMembers(new CreatureSay(0, Say2.PARTYROOM_ALL, "", "You have looting rights!")); // TODO:
																																							// retail
																																							// msg
						}
					}
				}
			}
			else if (attacker.getParty().getCommandChannel().equals(_firstCommandChannelAttacked)) // is in same channel
				_commandChannelLastAttack = System.currentTimeMillis(); // update last attack time
		}

		// Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList
		if (attacker != null)
			addDamage(attacker, (int) damage);

		// If this L2Attackable is a L2MonsterInstance and it has spawned minions, call its minions to battle
		if (this instanceof L2MonsterInstance)
		{
			L2MonsterInstance master = (L2MonsterInstance) this;

			if (master.hasMinions())
				master.getMinionList().onAssist(this, attacker);

			master = master.getLeader();
			if (master != null && master.hasMinions())
				master.getMinionList().onAssist(this, attacker);
		}
		// Reduce the current HP of the L2Attackable and launch the doDie Task if necessary
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}

	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp;
	}

	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}

	/**
	 * Kill the L2Attackable (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members</li> <li>
	 * Notify the Quest Engine of the L2Attackable death if necessary</li> <li>Kill the L2Npc (the corpse disappeared after 7 seconds)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR>
	 * <BR>
	 * 
	 * @param killer
	 *            The L2Character that has killed the L2Attackable
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		// Kill the L2Npc (the corpse disappeared after 7 seconds)
		if (!super.doDie(killer))
			return false;

		// Notify the Quest Engine of the L2Attackable death if necessary
		try
		{
			L2PcInstance player = null;

			if (killer != null)
				player = killer.getActingPlayer();

			if (player != null)
			{
				if (getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL) != null)
					for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_KILL))
						ThreadPoolManager.getInstance().scheduleEffect(new OnKillNotifyTask(this, quest, player, killer instanceof L2Summon), _onKillDelay);
			}
		}
		catch (Exception e)
		{
			_log.error("", e);
		}

		return true;
	}

	private static class OnKillNotifyTask implements Runnable
	{
		private final L2Attackable _attackable;
		private final Quest _quest;
		private final L2PcInstance _killer;
		private final boolean _isPet;

		public OnKillNotifyTask(L2Attackable attackable, Quest quest, L2PcInstance killer, boolean isPet)
		{
			_attackable = attackable;
			_quest = quest;
			_killer = killer;
			_isPet = isPet;
		}

		@Override
		public void run()
		{
			_quest.notifyKill(_attackable, _killer, _isPet);
		}
	}

	/**
	 * Distribute Exp and SP rewards to L2PcInstance (including Summon owner) that hit the L2Attackable and to their Party members.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the L2PcInstance owner of the L2SummonInstance (if necessary) and L2Party in progress</li> <li>Calculate the Experience and SP
	 * rewards in function of the level difference</li> <li>Add Exp and SP rewards to L2PcInstance (including Summon penalty) and to Party
	 * members in the known area of the last attacker</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR>
	 * <BR>
	 * 
	 * @param lastAttacker
	 *            The L2Character that has killed the L2Attackable
	 */
	@Override
	protected void calculateRewards(L2Character lastAttacker)
	{
		// Creates an empty list of rewards
		FastMap<L2Character, RewardInfo> rewards = new FastMap<L2Character, RewardInfo>().shared();

		try
		{
			if (getAggroList().isEmpty())
				return;

			int damage;
			L2Character attacker, ddealer;

			L2PcInstance maxDealer = null;
			int maxDamage = 0;

			// Go through the _aggroList of the L2Attackable
			for (AggroInfo info : getAggroList().values())
			{
				if (info == null)
					continue;

				// Get the L2Character corresponding to this attacker
				attacker = info.getAttacker();

				// Get damages done by this attacker
				damage = info.getDamage();

				// Prevent unwanted behavior
				if (damage > 1)
				{
					if ((attacker instanceof L2SummonInstance) || ((attacker instanceof L2PetInstance) && ((L2PetInstance) attacker).getPetLevelData().getOwnerExpTaken() > 0))
						ddealer = ((L2Summon) attacker).getOwner();
					else
						ddealer = info.getAttacker();

					// Check if ddealer isn't too far from this (killed monster)
					if (!Util.checkIfInRange(PlayersConfig.ALT_PARTY_RANGE, this, ddealer, true))
						continue;

					// Calculate real damages (Summoners should get own damage plus summon's damage)
					RewardInfo reward = rewards.get(ddealer);

					if (reward == null)
						reward = new RewardInfo(ddealer, damage);
					else
						reward.addDamage(damage);

					rewards.put(ddealer, reward);

					if (ddealer.getActingPlayer() != null && reward._dmg > maxDamage)
					{
						maxDealer = ddealer.getActingPlayer();
						maxDamage = reward._dmg;
					}
				}
			}

			// Manage Base, Quests and Sweep drops of the L2Attackable
			doItemDrop(maxDealer != null && maxDealer.isOnline() ? maxDealer : lastAttacker);

			if (!getMustRewardExpSP())
				return;

			if (!rewards.isEmpty())
			{
				L2Party attackerParty;
				long exp, exp_premium;
				int levelDiff, partyDmg, partyLvl, sp, sp_premium;
				float partyMul, penalty;
				RewardInfo reward2;
				int[] tmp;

				for (RewardInfo reward : rewards.values())
				{
					if (reward == null)
						continue;

					// Penalty applied to the attacker's XP
					penalty = 0;

					// Attacker to be rewarded
					attacker = reward._attacker;

					// Total amount of damage done
					damage = reward._dmg;

					// If the attacker is a Pet, get the party of the owner
					if (attacker instanceof L2PetInstance)
						attackerParty = ((L2PetInstance) attacker).getParty();
					else if (attacker instanceof L2PcInstance)
						attackerParty = ((L2PcInstance) attacker).getParty();
					else
						return;

					// If this attacker is a L2PcInstance with a summoned L2SummonInstance, get Exp Penalty applied for the
					// current summoned L2SummonInstance
					if (attacker instanceof L2PcInstance && ((L2PcInstance) attacker).getPet() instanceof L2SummonInstance)
						penalty = ((L2SummonInstance) ((L2PcInstance) attacker).getPet()).getExpPenalty();

					// We must avoid "over damage", if any
					if (damage > getMaxHp())
						damage = getMaxHp();

					// If there's NO party in progress
					if (attackerParty == null)
					{
						// Calculate Exp and SP rewards
						if (attacker.getKnownList().knowsObject(this))
						{
							// Calculate the difference of level between this attacker (L2PcInstance or L2SummonInstance owner)
							// and the L2Attackable
							// mob = 24, atk = 10, diff = -14 (full xp)
							// mob = 24, atk = 28, diff = 4 (some xp)
							// mob = 24, atk = 50, diff = 26 (no xp)
							levelDiff = attacker.getLevel() - getLevel();

							tmp = calculateExpAndSp(levelDiff, damage, attacker.getPremiumService());
							exp = tmp[0];
							exp *= 1 - penalty;
							sp = tmp[1];

							if (NPCConfig.CHAMPION_ENABLE && isChampion())
							{
								exp *= NPCConfig.CHAMPION_REWARDS;
								sp *= NPCConfig.CHAMPION_REWARDS;
							}

							// Check for an over-hit enabled strike
							if (attacker instanceof L2PcInstance)
							{
								if (isOverhit() && attacker == getOverhitAttacker())
								{
									((L2PcInstance) attacker).sendPacket(SystemMessageId.OVER_HIT);
									exp += calculateOverhitExp(exp);
								}
							}

							// Distribute the Exp and SP between the L2PcInstance and its L2Summon
							if (!attacker.isDead())
								attacker.addExpAndSp(Math.round(attacker.calcStat(Stats.EXPSP_RATE, exp, null, null)), (int) attacker.calcStat(Stats.EXPSP_RATE, sp, null, null));
						}
					}
					else
					{
						// share with party members
						partyDmg = 0;
						partyMul = 1.f;
						partyLvl = 0;

						// Get all L2Character that can be rewarded in the party
						List<L2Playable> rewardedMembers = new ArrayList<>();

						// Go through all L2PcInstance in the party
						List<L2PcInstance> groupMembers;
						if (attackerParty.isInCommandChannel())
							groupMembers = attackerParty.getCommandChannel().getMembers();
						else
							groupMembers = attackerParty.getPartyMembers();

						for (L2PcInstance pl : groupMembers)
						{
							if (pl == null || pl.isDead())
								continue;

							// Get the RewardInfo of this L2PcInstance from L2Attackable rewards
							reward2 = rewards.get(pl);

							// If the L2PcInstance is in the L2Attackable rewards add its damages to party damages
							if (reward2 != null)
							{
								if (Util.checkIfInRange(PlayersConfig.ALT_PARTY_RANGE, this, pl, true))
								{
									partyDmg += reward2._dmg; // Add L2PcInstance damages to party damages
									rewardedMembers.add(pl);

									if (pl.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
											partyLvl = attackerParty.getCommandChannel().getLevel();
										else
											partyLvl = pl.getLevel();
									}
								}
								rewards.remove(pl); // Remove the L2PcInstance from the L2Attackable rewards
							}
							else
							{
								// Add L2PcInstance of the party (that have attacked or not) to members that can be rewarded
								// and in range of the monster.
								if (Util.checkIfInRange(PlayersConfig.ALT_PARTY_RANGE, this, pl, true))
								{
									rewardedMembers.add(pl);
									if (pl.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
											partyLvl = attackerParty.getCommandChannel().getLevel();
										else
											partyLvl = pl.getLevel();
									}
								}
							}

							L2Playable summon = pl.getPet();
							if (summon != null && summon instanceof L2PetInstance)
							{
								reward2 = rewards.get(summon);
								if (reward2 != null) // Pets are only added if they have done damage
								{
									if (Util.checkIfInRange(PlayersConfig.ALT_PARTY_RANGE, this, summon, true))
									{
										partyDmg += reward2._dmg; // Add summon damages to party damages
										rewardedMembers.add(summon);

										if (summon.getLevel() > partyLvl)
											partyLvl = summon.getLevel();
									}
									rewards.remove(summon); // Remove the summon from the L2Attackable rewards
								}
							}
						}

						// If the party didn't killed this L2Attackable alone
						if (partyDmg < getMaxHp())
							partyMul = ((float) partyDmg / (float) getMaxHp());

						// Avoid "over damage"
						if (partyDmg > getMaxHp())
							partyDmg = getMaxHp();

						// Calculate the level difference between Party and L2Attackable
						levelDiff = partyLvl - getLevel();

						// Calculate Exp and SP rewards
						tmp = calculateExpAndSp(levelDiff, partyDmg, 1);
						exp_premium = tmp[0];
						sp_premium = tmp[1];
						tmp = calculateExpAndSp(levelDiff, partyDmg, 0);
						exp = tmp[0];
						sp = tmp[1];

						if (NPCConfig.CHAMPION_ENABLE && isChampion())
						{
							exp *= NPCConfig.CHAMPION_REWARDS;
							sp *= NPCConfig.CHAMPION_REWARDS;
						}

						exp *= partyMul;
						sp *= partyMul;
						exp_premium *= partyMul;
						sp_premium *= partyMul;

						// Check for an over-hit enabled strike (When in party, the over-hit exp bonus is given to the whole party
						// and splitted proportionally through the party members)
						if (attacker instanceof L2PcInstance)
						{
							if (isOverhit() && attacker == getOverhitAttacker())
							{
								((L2PcInstance) attacker).sendPacket(SystemMessageId.OVER_HIT);
								exp += calculateOverhitExp(exp);
								exp_premium += calculateOverhitExp(exp_premium);
							}
						}

						// Distribute Experience and SP rewards to L2PcInstance Party members in the known area of the last
						// attacker
						if (partyDmg > 0)
							attackerParty.distributeXpAndSp(exp_premium, sp_premium, exp, sp, rewardedMembers, partyLvl);
					}
				}
			}
			rewards = null;
		}
		catch (Exception e)
		{
			_log.error("", e);
		}
	}

	/**
	 * @see silentium.gameserver.model.actor.L2Character#addAttackerToAttackByList(silentium.gameserver.model.actor.L2Character)
	 */
	@Override
	public void addAttackerToAttackByList(L2Character player)
	{
		if (player == null || player == this || getAttackByList().contains(player))
			return;

		getAttackByList().add(player);
	}

	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.<BR>
	 * <BR>
	 * 
	 * @param attacker
	 *            The L2Character that gave damages to this L2Attackable
	 * @param damage
	 *            The number of damages given by the attacker L2Character
	 */
	public void addDamage(L2Character attacker, int damage)
	{
		if (attacker == null)
			return;

		// Notify the L2Attackable AI with EVT_ATTACKED
		if (!isDead())
		{
			try
			{
				L2PcInstance player = attacker.getActingPlayer();
				if (player != null)
				{
					if (getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK) != null)
						for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK))
							quest.notifyAttack(this, player, damage, attacker instanceof L2Summon);
				}
				// for now hard code damage hate caused by an L2Attackable
				else
				{
					getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, attacker);
					addDamageHate(attacker, damage, (damage * 100) / (getLevel() + 7));
				}
			}
			catch (Exception e)
			{
				_log.error("", e);
			}
		}
	}

	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.
	 * 
	 * @param attacker
	 *            The L2Character that gave damages to this L2Attackable
	 * @param damage
	 *            The number of damages given by the attacker L2Character
	 * @param aggro
	 *            The hate (=damage) given by the attacker L2Character
	 */
	public void addDamageHate(L2Character attacker, int damage, int aggro)
	{
		if (attacker == null)
			return;

		// Get or create the AggroInfo of the attacker.
		AggroInfo ai = getAggroList().get(attacker);
		if (ai == null)
		{
			ai = new AggroInfo(attacker);
			getAggroList().put(attacker, ai);
		}
		ai.addDamage(damage);
		ai.addHate(aggro);

		if (aggro == 0)
		{
			final L2PcInstance targetPlayer = attacker.getActingPlayer();
			if (targetPlayer != null)
			{
				if (getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER) != null)
					for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_AGGRO_RANGE_ENTER))
						quest.notifyAggroRangeEnter(this, targetPlayer, (attacker instanceof L2Summon));
			}
			else
			{
				aggro = 1;
				ai.addHate(1);
			}
		}
		else
		{
			// Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
			if (aggro > 0 && getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		}
	}

	public void reduceHate(L2Character target, int amount)
	{
		if (getAI() instanceof SiegeGuardAI)
		{
			stopHating(target);
			setTarget(null);
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			return;
		}

		if (target == null) // whole aggrolist
		{
			L2Character mostHated = getMostHated();

			// If not most hated target is found, makes AI passive for a moment more
			if (mostHated == null)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}

			for (L2Character aggroed : getAggroList().keySet())
			{
				AggroInfo ai = getAggroList().get(aggroed);

				if (ai == null)
					return;
				ai.addHate(-amount);
			}

			amount = getHating(mostHated);

			if (amount <= 0)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
			return;
		}

		AggroInfo ai = getAggroList().get(target);
		if (ai == null)
			return;

		ai.addHate(-amount);

		if (ai.getHate() <= 0)
		{
			if (getMostHated() == null)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				setWalking();
			}
		}
	}

	/**
	 * Clears _aggroList hate of the L2Character without removing from the list.
	 * 
	 * @param target
	 *            The target to clean from that L2Attackable _aggroList.
	 */
	public void stopHating(L2Character target)
	{
		if (target == null)
			return;

		AggroInfo ai = getAggroList().get(target);
		if (ai != null)
			ai.stopHate();
	}

	/**
	 * @return the most hated L2Character of the L2Attackable _aggroList.
	 */
	public L2Character getMostHated()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
			return null;

		L2Character mostHated = null;
		int maxHate = 0;

		// Go through the aggroList of the L2Attackable
		for (AggroInfo ai : getAggroList().values())
		{
			if (ai == null)
				continue;

			if (ai.checkHate(this) > maxHate)
			{
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		return mostHated;
	}

	public List<L2Character> getHateList()
	{
		if (getAggroList().isEmpty() || isAlikeDead())
			return null;

		List<L2Character> result = new ArrayList<>();
		for (AggroInfo ai : getAggroList().values())
		{
			if (ai == null)
				continue;

			ai.checkHate(this);
			result.add(ai.getAttacker());
		}

		return result;
	}

	/**
	 * @param target
	 *            The L2Character whose hate level must be returned
	 * @return the hate level of the L2Attackable against this L2Character contained in _aggroList.
	 */
	public int getHating(final L2Character target)
	{
		if (getAggroList().isEmpty() || target == null)
			return 0;

		AggroInfo ai = getAggroList().get(target);

		if (ai == null)
			return 0;

		if (ai.getAttacker() instanceof L2PcInstance)
		{
			L2PcInstance act = (L2PcInstance) ai.getAttacker();
			if (act.getAppearance().getInvisible() || ai.getAttacker().isInvul())
			{
				// Remove Object Should Use This Method and Can be Blocked While Interating
				getAggroList().remove(target);
				return 0;
			}
		}

		if (!ai.getAttacker().isVisible())
		{
			getAggroList().remove(target);
			return 0;
		}

		if (ai.getAttacker().isAlikeDead())
		{
			ai.stopHate();
			return 0;
		}
		return ai.getHate();
	}

	/**
	 * Calculates quantity of items for specific drop acording to current situation.
	 * 
	 * @param drop
	 *            The L2DropData count is being calculated for
	 * @param lastAttacker
	 *            The L2PcInstance that has killed the L2Attackable
	 * @param levelModifier
	 *            level modifier in %'s (will be subtracted from drop chance)
	 * @param isSweep
	 *            if true, use spoil drop chance.
	 * @return the RewardItem.
	 */
	private RewardItem calculateRewardItem(L2PcInstance lastAttacker, L2DropData drop, int levelModifier, boolean isSweep)
	{
		// Get default drop chance
		float dropChance = drop.getChance();

		int deepBlueDrop = 1;
		if (PlayersConfig.DEEPBLUE_DROP_RULES)
		{
			if (levelModifier > 0)
			{
				// We should multiply by the server's drop rate, so we always get a low chance of drop for deep blue mobs.
				// NOTE: This is valid only for adena drops! Others drops will still obey server's rate
				deepBlueDrop = 3;
				if (drop.getItemId() == 57)
				{
					deepBlueDrop *= isRaid() && !isRaidMinion() ? (int) MainConfig.RATE_DROP_ITEMS_BY_RAID : (int) MainConfig.RATE_DROP_ITEMS;
					if (deepBlueDrop == 0) // avoid div by 0
						deepBlueDrop = 1;
				}
			}

			// Check if we should apply our maths so deep blue mobs will not drop that easy
			dropChance = ((drop.getChance() - ((drop.getChance() * levelModifier) / 100)) / deepBlueDrop);
		}

		// Applies Drop rates
		if (drop.getItemId() == 57)
		{
			if (lastAttacker.getPremiumService() == 1)
				dropChance *= CustomConfig.PREMIUM_RATE_DROP_ADENA;
			else
				dropChance *= MainConfig.RATE_DROP_ADENA;
		}
		else if (isSweep)
		{
			if (lastAttacker.getPremiumService() == 1)
				dropChance *= CustomConfig.PREMIUM_RATE_DROP_SPOIL;
			else
				dropChance *= MainConfig.RATE_DROP_SPOIL;
		}
		else
		{
			if (lastAttacker.getPremiumService() == 1)
				dropChance *= isRaid() && !isRaidMinion() ? CustomConfig.PREMIUM_RATE_DROP_ITEMS_BY_RAID : CustomConfig.PREMIUM_RATE_DROP_ITEMS;
			else
				dropChance *= isRaid() && !isRaidMinion() ? MainConfig.RATE_DROP_ITEMS_BY_RAID : MainConfig.RATE_DROP_ITEMS;
		}

		if (NPCConfig.CHAMPION_ENABLE && isChampion())
			dropChance *= NPCConfig.CHAMPION_REWARDS;

		// Round drop chance
		dropChance = Math.round(dropChance);

		// Set our limits for chance of drop
		if (dropChance < 1)
			dropChance = 1;
		else if (dropChance > L2DropData.MAX_CHANCE)
			dropChance = L2DropData.MAX_CHANCE;

		// Get min and max Item quantity that can be dropped in one time
		final int minCount = drop.getMinDrop();
		final int maxCount = drop.getMaxDrop();

		// Get the item quantity dropped
		int itemCount = 0;

		// Check if the Item must be dropped
		int random = Rnd.get(L2DropData.MAX_CHANCE);
		while (random < dropChance)
		{
			// Get the item quantity dropped
			if (minCount < maxCount)
				itemCount += Rnd.get(minCount, maxCount);
			else if (minCount == maxCount)
				itemCount += minCount;
			else
				itemCount++;

			// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
			dropChance -= L2DropData.MAX_CHANCE;
		}

		if (NPCConfig.CHAMPION_ENABLE && isChampion())
			if (drop.getItemId() == 57 || (drop.getItemId() >= 6360 && drop.getItemId() <= 6362))
				itemCount *= NPCConfig.CHAMPION_ADENAS_REWARDS;

		if (itemCount > 0)
			return new RewardItem(drop.getItemId(), itemCount);
		else if (itemCount == 0 && _log.isTraceEnabled())
			_log.trace("Roll produced 0 items to drop.");

		return null;
	}

	/**
	 * Calculates quantity of items for specific drop CATEGORY according to current situation <br>
	 * Only a max of ONE item from a category is allowed to be dropped.
	 * 
	 * @param lastAttacker
	 *            The L2PcInstance that has killed the L2Attackable
	 * @param categoryDrops
	 *            The category to make checks on.
	 * @param levelModifier
	 *            level modifier in %'s (will be subtracted from drop chance)
	 * @return the RewardItem.
	 */
	private RewardItem calculateCategorizedRewardItem(L2PcInstance lastAttacker, L2DropCategory categoryDrops, int levelModifier)
	{
		if (categoryDrops == null)
			return null;

		// Get default drop chance for the category (that's the sum of chances for all items in the category)
		// keep track of the base category chance as it'll be used later, if an item is drop from the category.
		// for everything else, use the total "categoryDropChance"
		int basecategoryDropChance = categoryDrops.getCategoryChance();
		int categoryDropChance = basecategoryDropChance;

		int deepBlueDrop = 1;
		if (PlayersConfig.DEEPBLUE_DROP_RULES)
		{
			if (levelModifier > 0)
				deepBlueDrop = 3;

			// Check if we should apply our maths so deep blue mobs will not drop that easy
			categoryDropChance = ((categoryDropChance - ((categoryDropChance * levelModifier) / 100)) / deepBlueDrop);
		}

		// Applies Drop rates
		if (lastAttacker.getPremiumService() == 1)
			categoryDropChance *= isRaid() && !isRaidMinion() ? CustomConfig.PREMIUM_RATE_DROP_ITEMS_BY_RAID : CustomConfig.PREMIUM_RATE_DROP_ITEMS;
		else
			categoryDropChance *= isRaid() && !isRaidMinion() ? MainConfig.RATE_DROP_ITEMS_BY_RAID : MainConfig.RATE_DROP_ITEMS;

		if (NPCConfig.CHAMPION_ENABLE && isChampion())
			categoryDropChance *= NPCConfig.CHAMPION_REWARDS;

		// Round drop chance
		categoryDropChance = Math.round(categoryDropChance);

		// Set our limits for chance of drop
		if (categoryDropChance < 1)
			categoryDropChance = 1;
		else if (categoryDropChance > L2DropData.MAX_CHANCE)
			categoryDropChance = L2DropData.MAX_CHANCE;

		// Check if an Item from this category must be dropped
		if (Rnd.get(L2DropData.MAX_CHANCE) < categoryDropChance)
		{
			L2DropData drop = categoryDrops.dropOne(isRaid() && !isRaidMinion());
			if (drop == null)
				return null;

			// Now decide the quantity to drop based on the rates and penalties. To get this value
			// simply divide the modified categoryDropChance by the base category chance. This
			// results in a chance that will dictate the drops amounts: for each amount over 100
			// that it is, it will give another chance to add to the min/max quantities.
			//
			// For example, If the final chance is 120%, then the item should drop between
			// its min and max one time, and then have 20% chance to drop again. If the final
			// chance is 330%, it will similarly give 3 times the min and max, and have a 30%
			// chance to give a 4th time.
			// At least 1 item will be dropped for sure. So the chance will be adjusted to 100%
			// if smaller.

			int dropChance = drop.getChance();
			if (drop.getItemId() == 57)
				dropChance *= MainConfig.RATE_DROP_ADENA;
			else
				dropChance *= isRaid() && !isRaidMinion() ? MainConfig.RATE_DROP_ITEMS_BY_RAID : MainConfig.RATE_DROP_ITEMS;

			if (NPCConfig.CHAMPION_ENABLE && isChampion())
				dropChance *= NPCConfig.CHAMPION_REWARDS;

			dropChance = Math.round(dropChance);

			if (dropChance < L2DropData.MAX_CHANCE)
				dropChance = L2DropData.MAX_CHANCE;

			// Get min and max Item quantity that can be dropped in one time
			final int min = drop.getMinDrop();
			final int max = drop.getMaxDrop();

			// Get the item quantity dropped
			int itemCount = 0;

			// Check if the Item must be dropped
			int random = Rnd.get(L2DropData.MAX_CHANCE);
			while (random < dropChance)
			{
				// Get the item quantity dropped
				if (min < max)
					itemCount += Rnd.get(min, max);
				else if (min == max)
					itemCount += min;
				else
					itemCount++;

				// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
				dropChance -= L2DropData.MAX_CHANCE;
			}

			if (NPCConfig.CHAMPION_ENABLE && isChampion())
				if (drop.getItemId() == 57 || (drop.getItemId() >= 6360 && drop.getItemId() <= 6362))
					itemCount *= NPCConfig.CHAMPION_ADENAS_REWARDS;

			if (itemCount > 0)
				return new RewardItem(drop.getItemId(), itemCount);
			else if (itemCount == 0 && _log.isDebugEnabled())
				_log.debug("Roll produced 0 items to drop...");
		}
		return null;
	}

	/**
	 * @param lastAttacker
	 *            The L2PcInstance that has killed the L2Attackable
	 * @return the level modifier for drop
	 */
	private int calculateLevelModifierForDrop(L2PcInstance lastAttacker)
	{
		if (PlayersConfig.DEEPBLUE_DROP_RULES)
		{
			int highestLevel = lastAttacker.getLevel();

			// Check to prevent very high level player to nearly kill mob and let low level player do the last hit.
			if (!getAttackByList().isEmpty())
			{
				for (L2Character atkChar : getAttackByList())
					if (atkChar != null && atkChar.getLevel() > highestLevel)
						highestLevel = atkChar.getLevel();
			}

			// According to official data (Prima), deep blue mobs are 9 or more levels below players
			if (highestLevel - 9 >= getLevel())
				return ((highestLevel - (getLevel() + 8)) * 9);
		}
		return 0;
	}

	private static RewardItem calculateCategorizedHerbItem(L2PcInstance lastAttacker, L2DropCategory categoryDrops)
	{
		if (categoryDrops == null)
			return null;

		// Get default drop chance for the category (that's the sum of chances for all items in the category)
		// keep track of the base category chance as it'll be used later, if an item is drop from the category.
		// for everything else, use the total "categoryDropChance"
		int basecategoryDropChance = categoryDrops.getCategoryChance();
		int categoryDropChance = basecategoryDropChance;

		// Applies Drop rates
		switch (categoryDrops.getCategoryType())
		{
			case 1:
				categoryDropChance *= MainConfig.RATE_DROP_HP_HERBS;
				break;
			case 2:
				categoryDropChance *= MainConfig.RATE_DROP_MP_HERBS;
				break;
			case 3:
				categoryDropChance *= MainConfig.RATE_DROP_SPECIAL_HERBS;
				break;
			default:
				categoryDropChance *= MainConfig.RATE_DROP_COMMON_HERBS;
		}

		// Round drop chance
		categoryDropChance = Math.round(categoryDropChance);

		// Set our limits for chance of drop
		if (categoryDropChance < 1)
			categoryDropChance = 1;
		else if (categoryDropChance > L2DropData.MAX_CHANCE)
			categoryDropChance = L2DropData.MAX_CHANCE;

		// Check if an Item from this category must be dropped
		if (Rnd.get(L2DropData.MAX_CHANCE) < categoryDropChance)
		{
			L2DropData drop = categoryDrops.dropOne(false);

			if (drop == null)
				return null;

			/*
			 * Now decide the quantity to drop based on the rates and penalties. To get this value, simply divide the modified categoryDropChance
			 * by the base category chance. This results in a chance that will dictate the drops amounts : for each amount over 100 that it is,
			 * it will give another chance to add to the min/max quantities. For example, if the final chance is 120%, then the item should drop
			 * between its min and max one time, and then have 20% chance to drop again. If the final chance is 330%, it will similarly give 3
			 * times the min and max, and have a 30% chance to give a 4th time. At least 1 item will be dropped for sure. So the chance will be
			 * adjusted to 100% if smaller.
			 */
			int dropChance = drop.getChance();

			switch (categoryDrops.getCategoryType())
			{
				case 1:
					dropChance *= MainConfig.RATE_DROP_HP_HERBS;
					break;
				case 2:
					dropChance *= MainConfig.RATE_DROP_MP_HERBS;
					break;
				case 3:
					dropChance *= MainConfig.RATE_DROP_SPECIAL_HERBS;
					break;
				default:
					dropChance *= MainConfig.RATE_DROP_COMMON_HERBS;
			}

			dropChance = Math.round(dropChance);

			if (dropChance < L2DropData.MAX_CHANCE)
				dropChance = L2DropData.MAX_CHANCE;

			// Get min and max Item quantity that can be dropped in one time
			int min = drop.getMinDrop();
			int max = drop.getMaxDrop();

			// Get the item quantity dropped
			int itemCount = 0;

			// Check if the Item must be dropped
			int random = Rnd.get(L2DropData.MAX_CHANCE);
			while (random < dropChance)
			{
				// Get the item quantity dropped
				if (min < max)
					itemCount += Rnd.get(min, max);
				else if (min == max)
					itemCount += min;
				else
					itemCount++;

				// Prepare for next iteration if dropChance > L2DropData.MAX_CHANCE
				dropChance -= L2DropData.MAX_CHANCE;
			}

			if (itemCount > 0)
				return new RewardItem(drop.getItemId(), itemCount);
			else if (itemCount == 0 && _log.isDebugEnabled())
				_log.debug("Roll produced no drops.");
		}
		return null;
	}

	/**
	 * Manage Base & Quests drops of L2Attackable (called by calculateRewards).<BR>
	 * <BR>
	 * <B><U> Actions</U> : </B><BR>
	 * <BR>
	 * <li>Get all possible drops of this L2Attackable from L2NpcTemplate and add it Quest drops</li> <li>For each possible drops (base +
	 * quests), calculate which one must be dropped (random)</li> <li>Get each Item quantity dropped (random)</li> <li>Create this or these
	 * L2ItemInstance corresponding to each Item Identifier dropped</li> <li>If the autoLoot mode is actif and if the L2Character that has killed
	 * the L2Attackable is a L2PcInstance, give this or these Item(s) to the L2PcInstance that has killed the L2Attackable</li> <li>If the
	 * autoLoot mode isn't actif or if the L2Character that has killed the L2Attackable is not a L2PcInstance, add this or these Item(s) in the
	 * world as a visible object at the position where mob was last</li><BR>
	 * <BR>
	 * 
	 * @param mainDamageDealer
	 *            The L2Character that made the most damage.
	 */
	public void doItemDrop(L2Character mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}

	public void doItemDrop(L2NpcTemplate npcTemplate, L2Character mainDamageDealer)
	{
		if (mainDamageDealer == null)
			return;

		// Don't drop anything if the last attacker or owner isn't L2PcInstance
		L2PcInstance player = mainDamageDealer.getActingPlayer();
		if (player == null)
			return;

		// level modifier in %'s (will be subtracted from drop chance)
		int levelModifier = calculateLevelModifierForDrop(player);

		CursedWeaponsManager.getInstance().checkDrop(this, player);

		// now throw all categorized drops and handle spoil.
		if (npcTemplate.getDropData() != null)
		{
			for (L2DropCategory cat : npcTemplate.getDropData())
			{
				RewardItem item = null;
				if (cat.isSweep())
				{
					if (isSpoil())
					{
						FastList<RewardItem> sweepList = new FastList<>();

						for (L2DropData drop : cat.getAllDrops())
						{
							item = calculateRewardItem(player, drop, levelModifier, true);
							if (item == null)
								continue;

							_log.debug("Item id to spoil: " + item.getItemId() + " amount: " + item.getCount());
							sweepList.add(item);
						}

						// Set the table _sweepItems of this L2Attackable
						if (!sweepList.isEmpty())
							_sweepItems = sweepList.toArray(new RewardItem[sweepList.size()]);
					}
				}
				else
				{
					if (isSeeded())
					{
						L2DropData drop = cat.dropSeedAllowedDropsOnly();
						if (drop == null)
							continue;

						item = calculateRewardItem(player, drop, levelModifier, false);
					}
					else
						item = calculateCategorizedRewardItem(player, cat, levelModifier);

					if (item != null)
					{
						_log.debug("Item id to drop: " + item.getItemId() + " amount: " + item.getCount());

						// Check if the autoLoot mode is active
						if ((isRaid() && MainConfig.AUTO_LOOT_RAID) || (!isRaid() && MainConfig.AUTO_LOOT))
							player.doAutoLoot(this, item); // Give this or these Item(s) to the L2PcInstance that has killed the
															// L2Attackable
						else
							dropItem(player, item); // drop the item on the ground

						// Broadcast message if RaidBoss was defeated
						if (isRaid() && !isRaidMinion())
						{
							SystemMessage sm;
							sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DIED_DROPPED_S3_S2);
							sm.addCharName(this);
							sm.addItemName(item.getItemId());
							sm.addNumber(item.getCount());
							broadcastPacket(sm);
						}
					}
				}
			}
		}

		// Apply Special Item drop with rnd qty for champions
		if (NPCConfig.CHAMPION_ENABLE && isChampion() && (player.getLevel() <= getLevel()) && NPCConfig.CHAMPION_REWARD > 0 && (Rnd.get(100) < NPCConfig.CHAMPION_REWARD))
		{
			int champqty = Rnd.get(NPCConfig.CHAMPION_REWARD_QTY);
			champqty++; // quantity should actually vary between 1 and whatever admin specified as max, inclusive.

			RewardItem item = new RewardItem(NPCConfig.CHAMPION_REWARD_ID, champqty);
			if (MainConfig.AUTO_LOOT)
				player.addItem("ChampionLoot", item.getItemId(), item.getCount(), this, true);
			else
				dropItem(player, item);
		}

		// Instant Item Drop
		if (getTemplate().getDropHerbGroup() > 0)
		{
			for (L2DropCategory cat : HerbDropData.getInstance().getHerbDroplist(getTemplate().getDropHerbGroup()))
			{
				RewardItem item = calculateCategorizedHerbItem(player, cat);
				if (item != null)
				{
					// more than one herb cant be auto looted!
					int count = item.getCount();
					if (count > 1)
					{
						item._count = 1;
						for (int i = 0; i < count; i++)
							dropItem(player, item);
					}
					else if (isFlying() || MainConfig.AUTO_LOOT_HERBS)
						player.addItem("Loot", item.getItemId(), count, this, true);
					else
						dropItem(player, item);
				}
			}
		}
	}

	/**
	 * Drop reward item.
	 * 
	 * @param mainDamageDealer
	 *            The player who made highest damage.
	 * @param item
	 *            The RewardItem.
	 * @return the dropped item instance.
	 */
	public L2ItemInstance dropItem(L2PcInstance mainDamageDealer, RewardItem item)
	{
		int randDropLim = 70;

		L2ItemInstance ditem = null;
		for (int i = 0; i < item.getCount(); i++)
		{
			// Randomize drop position
			int newX = getX() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newY = getY() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newZ = Math.max(getZ(), mainDamageDealer.getZ()) + 20;

			if (ItemTable.getInstance().getTemplate(item.getItemId()) != null)
			{
				// Init the dropped L2ItemInstance and add it in the world as a visible object at the position where mob was last
				ditem = ItemTable.getInstance().createItem("Loot", item.getItemId(), item.getCount(), mainDamageDealer, this);
				ditem.getDropProtection().protect(mainDamageDealer);
				ditem.dropMe(this, newX, newY, newZ);

				// Add drop to auto destroy item task
				if (!MainConfig.LIST_PROTECTED_ITEMS.contains(item.getItemId()))
				{
					if ((MainConfig.AUTODESTROY_ITEM_AFTER > 0 && ditem.getItemType() != L2EtcItemType.HERB) || (MainConfig.HERB_AUTO_DESTROY_TIME > 0 && ditem.getItemType() == L2EtcItemType.HERB))
						ItemsAutoDestroy.getInstance().addItem(ditem);
				}
				ditem.setProtected(false);

				// If stackable, end loop as entire count is included in 1 instance of item
				if (ditem.isStackable() || !MainConfig.MULTIPLE_ITEM_DROP)
					break;
			}
			else
				_log.error("Item doesn't exist so cannot be dropped. Item ID: " + item.getItemId());
		}
		return ditem;
	}

	public L2ItemInstance dropItem(L2PcInstance lastAttacker, int itemId, int itemCount)
	{
		return dropItem(lastAttacker, new RewardItem(itemId, itemCount));
	}

	/**
	 * @return the active weapon of this L2Attackable (= null).
	 */
	public L2ItemInstance getActiveWeapon()
	{
		return null;
	}

	/**
	 * @return True if the _aggroList of this L2Attackable is Empty.
	 */
	public boolean noTarget()
	{
		return getAggroList().isEmpty();
	}

	/**
	 * @param player
	 *            The L2Character searched in the _aggroList of the L2Attackable
	 * @return True if the _aggroList of this L2Attackable contains the L2Character.
	 */
	public boolean containsTarget(L2Character player)
	{
		return getAggroList().containsKey(player);
	}

	/**
	 * Clear the _aggroList of the L2Attackable.<BR>
	 * <BR>
	 */
	public void clearAggroList()
	{
		getAggroList().clear();
	}

	/**
	 * @return True if a Dwarf use Sweep on the L2Attackable and if item can be spoiled.
	 */
	public boolean isSweepActive()
	{
		return _sweepItems != null;
	}

	/**
	 * @return array containing all L2ItemInstance that can be spoiled.
	 */
	public synchronized RewardItem[] takeSweep()
	{
		RewardItem[] sweep = _sweepItems;

		_sweepItems = null;

		return sweep;
	}

	/**
	 * @return array containing all L2ItemInstance that can be harvested.
	 */
	public synchronized RewardItem[] takeHarvest()
	{
		RewardItem[] harvest = _harvestItems;
		_harvestItems = null;
		return harvest;
	}

	/**
	 * Set the over-hit flag on the L2Attackable.
	 * 
	 * @param status
	 *            The status of the over-hit flag
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}

	/**
	 * Set the over-hit values like the attacker who did the strike and the amount of damage done by the skill.
	 * 
	 * @param attacker
	 *            The L2Character who hit on the L2Attackable using the over-hit enabled skill
	 * @param damage
	 *            The ammount of damage done by the over-hit enabled skill on the L2Attackable
	 */
	public void setOverhitValues(L2Character attacker, double damage)
	{
		// Calculate the over-hit damage
		// Ex: mob had 10 HP left, over-hit skill did 50 damage total, over-hit damage is 40
		double overhitDmg = ((getCurrentHp() - damage) * (-1));
		if (overhitDmg < 0)
		{
			// we didn't killed the mob with the over-hit strike. (it wasn't really an over-hit strike)
			// let's just clear all the over-hit related values
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}

	/**
	 * Return the L2Character who hit on the L2Attackable using an over-hit enabled skill.
	 * 
	 * @return L2Character attacker
	 */
	public L2Character getOverhitAttacker()
	{
		return _overhitAttacker;
	}

	/**
	 * Return the amount of damage done on the L2Attackable using an over-hit enabled skill.
	 * 
	 * @return double damage
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}

	/**
	 * @return True if the L2Attackable was hit by an over-hit enabled skill.
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}

	/**
	 * Activate the absorbed soul condition on the L2Attackable.
	 */
	public void absorbSoul()
	{
		_absorbed = true;
	}

	/**
	 * @return True if the L2Attackable had his soul absorbed.
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}

	/**
	 * Adds an attacker that successfully absorbed the soul of this L2Attackable into the _absorbersList. Params: attacker - a valid L2PcInstance
	 * condition - an integer indicating the event when mob dies. This should be: = 0 - "the crystal scatters"; = 1 -
	 * "the crystal failed to absorb. nothing happens"; = 2 - "the crystal resonates because you got more than 1 crystal on you"; = 3 -
	 * "the crystal cannot absorb the soul because the mob level is too low"; = 4 - "the crystal successfuly absorbed the soul";
	 * 
	 * @param attacker
	 *            The L2PcInstance who attacked the monster.
	 */
	public void addAbsorber(L2PcInstance attacker)
	{
		// If we have no _absorbersList initiated, do it
		AbsorberInfo ai = _absorbersList.get(attacker.getObjectId());

		// If the L2Character attacker isn't already in the _absorbersList of this L2Attackable, add it
		if (ai == null)
		{
			ai = new AbsorberInfo(attacker.getObjectId(), getCurrentHp());
			_absorbersList.put(attacker.getObjectId(), ai);
		}
		else
		{
			ai._objId = attacker.getObjectId();
			ai._absorbedHP = getCurrentHp();
		}

		// Set this L2Attackable as absorbed
		absorbSoul();
	}

	public void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}

	public FastMap<Integer, AbsorberInfo> getAbsorbersList()
	{
		return _absorbersList;
	}

	/**
	 * Calculate the Experience and SP to distribute to attacker (L2PcInstance, L2SummonInstance or L2Party) of the L2Attackable.<BR>
	 * <BR>
	 * 
	 * @param diff
	 *            The difference of level between attacker (L2PcInstance, L2SummonInstance or L2Party) and the L2Attackable
	 * @param damage
	 *            The damages given by the attacker (L2PcInstance, L2SummonInstance or L2Party)
	 * @return an array consisting of xp and sp values.
	 */
	private int[] calculateExpAndSp(int diff, int damage, int IsPremium)
	{
		double xp;
		double sp;

		if (diff < -5)
			diff = -5;

		xp = (double) getExpReward(IsPremium) * damage / getMaxHp();
		sp = (double) getSpReward(IsPremium) * damage / getMaxHp();

		if (diff > 5) // formula revised May 07
		{
			double pow = Math.pow((double) 5 / 6, diff - 5);
			xp = xp * pow;
			sp = sp * pow;
		}

		if (xp <= 0)
		{
			xp = 0;
			sp = 0;
		}
		else if (sp <= 0)
			sp = 0;

		int[] tmp = { (int) xp, (int) sp };

		return tmp;
	}

	public long calculateOverhitExp(long normalExp)
	{
		// Get the percentage based on the total of extra (over-hit) damage done relative to the total (maximum) ammount of HP on
		// the L2Attackable
		double overhitPercentage = ((getOverhitDamage() * 100) / getMaxHp());

		// Over-hit damage percentages are limited to 25% max
		if (overhitPercentage > 25)
			overhitPercentage = 25;

		// Get the overhit exp bonus according to the above over-hit damage percentage
		// (1/1 basis - 13% of over-hit damage, 13% of extra exp is given, and so on...)
		double overhitExp = ((overhitPercentage / 100) * normalExp);

		// Return the rounded ammount of exp points to be added to the player's normal exp reward
		long bonusOverhit = Math.round(overhitExp);
		return bonusOverhit;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		// Clear mob spoil,seed
		setSpoil(false);

		// Clear all aggro char from list
		clearAggroList();

		// Clear Harvester Rewrard List
		_harvestItems = null;

		// Clear mod Seeded stat
		_seeded = false;
		_seedType = 0;
		_seederObjId = 0;

		// Clear overhit value
		overhitEnabled(false);

		_sweepItems = null;
		resetAbsorbList();

		setWalking();

		// check the region where this mob is, do not activate the AI if region is inactive.
		if (!isInActiveRegion())
		{
			if (hasAI())
				getAI().stopAITask();
		}
	}

	/**
	 * Sets state of the mob to seeded.
	 * 
	 * @param seeder
	 */
	public void setSeeded(L2PcInstance seeder)
	{
		if (_seedType != 0 && _seederObjId == seeder.getObjectId())
			setSeeded(_seedType, seeder.getLevel());
	}

	/**
	 * Sets the seed parametrs, but not the seed state
	 * 
	 * @param id
	 *            - id of the seed
	 * @param seeder
	 *            - player who is sowind the seed
	 */
	public void setSeeded(int id, L2PcInstance seeder)
	{
		if (!_seeded)
		{
			_seedType = id;
			_seederObjId = seeder.getObjectId();
		}
	}

	private void setSeeded(int id, int seederLvl)
	{
		_seeded = true;
		_seedType = id;
		int count = 1;

		int[] skillIds = getTemplate().getSkills().keys();

		if (skillIds != null)
		{
			for (int skillId : skillIds)
			{
				switch (skillId)
				{
					case 4303: // Strong type x2
						count *= 2;
						break;
					case 4304: // Strong type x3
						count *= 3;
						break;
					case 4305: // Strong type x4
						count *= 4;
						break;
					case 4306: // Strong type x5
						count *= 5;
						break;
					case 4307: // Strong type x6
						count *= 6;
						break;
					case 4308: // Strong type x7
						count *= 7;
						break;
					case 4309: // Strong type x8
						count *= 8;
						break;
					case 4310: // Strong type x9
						count *= 9;
						break;
				}
			}
		}

		int diff = (getLevel() - (L2Manor.getInstance().getSeedLevel(_seedType) - 5));
		if (diff > 0)
			count += diff;

		FastList<RewardItem> harvested = new FastList<>();

		harvested.add(new RewardItem(L2Manor.getInstance().getCropType(_seedType), count * MainConfig.RATE_DROP_MANOR));

		_harvestItems = harvested.toArray(new RewardItem[harvested.size()]);
	}

	public int getSeederId()
	{
		return _seederObjId;
	}

	public int getSeedType()
	{
		return _seedType;
	}

	public boolean isSeeded()
	{
		return _seeded;
	}

	/**
	 * Set delay for onKill() call, in ms Default: 5000 ms
	 * 
	 * @param delay
	 */
	public final void setOnKillDelay(int delay)
	{
		_onKillDelay = delay;
	}

	/**
	 * Check if the server allows Random Animation.<BR>
	 * <BR>
	 * This is located here because L2Monster and L2FriendlyMob both extend this class. The other non-pc instances extend either L2Npc or
	 * L2MonsterInstance.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return ((NPCConfig.MAX_MONSTER_ANIMATION > 0) && !isRaid());
	}

	@Override
	public boolean isMob()
	{
		return true; // This means we use MAX_MONSTER_ANIMATION instead of MAX_NPC_ANIMATION
	}

	protected void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}

	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}

	public L2CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}

	public void setFirstCommandChannelAttacked(L2CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}

	public long getCommandChannelLastAttack()
	{
		return _commandChannelLastAttack;
	}

	public void setCommandChannelLastAttack(long channelLastAttack)
	{
		_commandChannelLastAttack = channelLastAttack;
	}

	private static class CommandChannelTimer implements Runnable
	{
		private final L2Attackable _monster;

		public CommandChannelTimer(L2Attackable monster)
		{
			_monster = monster;
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run()
		{
			if ((System.currentTimeMillis() - _monster.getCommandChannelLastAttack()) > 900000)
			{
				_monster.setCommandChannelTimer(null);
				_monster.setFirstCommandChannelAttacked(null);
				_monster.setCommandChannelLastAttack(0);
			}
			else
				ThreadPoolManager.getInstance().scheduleGeneral(this, 10000); // 10sec
		}
	}

	public void returnHome()
	{
		clearAggroList();

		if (hasAI() && getSpawn() != null)
			getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz(), 0));
	}

	/** Return True if the L2Character is RaidBoss or his minion. */
	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}

	/**
	 * Set this Npc as a Raid instance.<BR>
	 * <BR>
	 * 
	 * @param isRaid
	 */
	@Override
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}

	/**
	 * Set this Npc as a Minion instance.
	 * 
	 * @param val
	 */
	public void setIsRaidMinion(boolean val)
	{
		_isRaid = val;
		_isRaidMinion = val;
	}

	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}

	@Override
	public boolean isMinion()
	{
		return getLeader() != null;
	}

	/**
	 * @return leader of this minion or null.
	 */
	public L2Attackable getLeader()
	{
		return null;
	}

	@Override
	protected void moveToLocation(int x, int y, int z, int offset)
	{
		if (isAttackingNow())
			return;

		super.moveToLocation(x, y, z, offset);
	}
}
