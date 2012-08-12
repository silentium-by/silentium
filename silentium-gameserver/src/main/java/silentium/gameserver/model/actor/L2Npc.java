/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor;

import javolution.util.FastList;
import silentium.commons.utils.Rnd;
import silentium.commons.utils.StringUtil;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.configs.*;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.HelperBuffData;
import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.DimensionalRiftManager;
import silentium.gameserver.instancemanager.QuestManager;
import silentium.gameserver.instancemanager.TownManager;
import silentium.gameserver.instancemanager.games.Lottery;
import silentium.gameserver.model.*;
import silentium.gameserver.model.actor.instance.*;
import silentium.gameserver.model.actor.knownlist.NpcKnownList;
import silentium.gameserver.model.actor.stat.NpcStat;
import silentium.gameserver.model.actor.status.NpcStatus;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.model.quest.State;
import silentium.gameserver.model.zone.type.L2TownZone;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import silentium.gameserver.network.serverpackets.*;
import silentium.gameserver.tables.ClanTable;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.tables.SkillTable.FrequentSkill;
import silentium.gameserver.taskmanager.DecayTaskManager;
import silentium.gameserver.templates.L2HelperBuff;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.templates.chars.L2NpcTemplate.AIType;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Broadcast;

import java.text.DateFormat;
import java.util.Collection;
import java.util.List;

import static silentium.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

/**
 * This class represents a Non-Player-Character in the world. It can be a monster or a friendly character. It also uses a template
 * to fetch some static values. The templates are hardcoded in the client, so we can rely on them.<BR>
 * <BR>
 * L2Character :<BR>
 * <BR>
 * <li>L2Attackable</li> <li>L2BoxInstance</li> <li>L2NpcInstance</li>
 */
public class L2Npc extends L2Character
{
	public static final int INTERACTION_DISTANCE = 150;

	private L2Spawn _spawn;

	private boolean _isBusy = false;
	volatile boolean _isDecayed = false;
	private boolean _isSpoil = false;
	private boolean _hasSpoken = false;

	private int _castleIndex = -2;
	private boolean _isInTown = false;

	private String _busyMessage = "";
	private int _isSpoiledBy = 0;

	protected RandomAnimationTask _rAniTask = null;
	private long _lastSocialBroadcast = 0;
	private final int _minimalSocialInterval = 6000;

	private int _currentLHandId;
	private int _currentRHandId;
	private int _currentEnchant;

	private int _currentCollisionHeight; // used for npc grow effect skills
	private int _currentCollisionRadius; // used for npc grow effect skills

	public boolean _soulshotcharged = false;
	public boolean _spiritshotcharged = false;
	private int _soulshotamount = 0;
	private int _spiritshotamount = 0;
	public boolean _ssrecharged = true;
	public boolean _spsrecharged = true;

	private final L2NpcAIData _staticAIData = getTemplate().getAIDataStatic();

	// AI Recall
	public final L2NpcAIData getAIData()
	{
		return _staticAIData;
	}

	public int getSoulShot()
	{
		return _staticAIData.getSoulShot();
	}

	public int getSpiritShot()
	{
		return _staticAIData.getSpiritShot();
	}

	public int getSoulShotChance()
	{
		return _staticAIData.getSoulShotChance();
	}

	public int getSpiritShotChance()
	{
		return _staticAIData.getSpiritShotChance();
	}

	public boolean useSoulShot()
	{
		if (_soulshotcharged)
			return true;

		if (_ssrecharged)
		{
			_soulshotamount = getSoulShot();
			_ssrecharged = false;
		}
		else if (_soulshotamount > 0)
		{
			if (Rnd.get(100) <= getSoulShotChance())
			{
				_soulshotamount = _soulshotamount - 1;
				Broadcast.toSelfAndKnownPlayersInRadiusSq(this, new MagicSkillUse(this, this, 2154, 1, 0, 0), 360000);
				_soulshotcharged = true;
			}
		}
		else
			return false;

		return _soulshotcharged;
	}

	public boolean useSpiritShot()
	{
		if (_spiritshotcharged)
			return true;

		if (_spsrecharged)
		{
			_spiritshotamount = getSpiritShot();
			_spsrecharged = false;
		}
		else if (_spiritshotamount > 0)
		{
			if (Rnd.get(100) <= getSpiritShotChance())
			{
				_spiritshotamount = _spiritshotamount - 1;
				Broadcast.toSelfAndKnownPlayersInRadiusSq(this, new MagicSkillUse(this, this, 2061, 1, 0, 0), 360000);
				_spiritshotcharged = true;
			}
		}
		else
			return false;

		return _spiritshotcharged;
	}

	public int getAggroRange()
	{
		return _staticAIData.getAggroRange();
	}

	public int getEnemyRange()
	{
		return _staticAIData.getEnemyRange();
	}

	public String getEnemyClan()
	{
		return _staticAIData.getEnemyClan();
	}

	public int getClanRange()
	{
		return _staticAIData.getClanRange();
	}

	public String getClan()
	{
		return _staticAIData.getClan();
	}

	public int getPrimaryAttack()
	{
		return _staticAIData.getPrimaryAttack();
	}

	public int getMinSkillChance()
	{
		return _staticAIData.getMinSkillChance();
	}

	public int getMaxSkillChance()
	{
		return _staticAIData.getMaxSkillChance();
	}

	public int getCanMove()
	{
		return _staticAIData.getCanMove();
	}

	public int getIsChaos()
	{
		return _staticAIData.getIsChaos();
	}

	public int getShortRangeSkillChance()
	{
		return _staticAIData.getShortRangeChance();
	}

	public int getLongRangeSkillChance()
	{
		return _staticAIData.getLongRangeChance();
	}

	public int getSwitchRangeChance()
	{
		return _staticAIData.getSwitchRangeChance();
	}

	public boolean hasLongRangeSkill()
	{
		return _staticAIData.getLongRangeSkill() != 0;
	}

	public boolean hasShortRangeSkill()
	{
		return _staticAIData.getShortRangeSkill() != 0;
	}

	public FastList<L2Skill> getLongRangeSkill()
	{
		final FastList<L2Skill> skilldata = new FastList<>();
		if (_staticAIData == null || _staticAIData.getLongRangeSkill() == 0)
			return skilldata;

		switch (_staticAIData.getLongRangeSkill())
		{
			case -1:
			{
				L2Skill[] skills = null;
				skills = getAllSkills();
				if (skills != null)
				{
					for (L2Skill sk : skills)
					{
						if (sk == null || sk.isPassive() || sk.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF)
							continue;

						if (sk.getCastRange() >= 200)
							skilldata.add(sk);
					}
				}
				break;
			}
			case 1:
			{
				if (getTemplate().getUniversalSkills() != null)
				{
					for (L2Skill sk : getTemplate().getUniversalSkills())
					{
						if (sk.getCastRange() >= 200)
							skilldata.add(sk);
					}
				}
				break;
			}
			default:
			{
				for (L2Skill sk : getAllSkills())
				{
					if (sk.getId() == _staticAIData.getLongRangeSkill())
						skilldata.add(sk);
				}
			}
		}
		return skilldata;
	}

	public FastList<L2Skill> getShortRangeSkill()
	{
		final FastList<L2Skill> skilldata = new FastList<>();
		if (_staticAIData == null || _staticAIData.getShortRangeSkill() == 0)
			return skilldata;

		switch (_staticAIData.getShortRangeSkill())
		{
			case -1:
			{
				L2Skill[] skills = null;
				skills = getAllSkills();
				if (skills != null)
				{
					for (L2Skill sk : skills)
					{
						if (sk == null || sk.isPassive() || sk.getTargetType() == L2Skill.SkillTargetType.TARGET_SELF)
							continue;

						if (sk.getCastRange() <= 200)
							skilldata.add(sk);
					}
				}
				break;
			}
			case 1:
			{
				if (getTemplate().getUniversalSkills() != null)
				{
					for (L2Skill sk : getTemplate().getUniversalSkills())
					{
						if (sk.getCastRange() <= 200)
							skilldata.add(sk);
					}
				}
				break;
			}
			default:
			{
				for (L2Skill sk : getAllSkills())
				{
					if (sk.getId() == _staticAIData.getShortRangeSkill())
						skilldata.add(sk);
				}
			}
		}
		return skilldata;
	}

	/** Task launching the function onRandomAnimation() */
	protected class RandomAnimationTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				if (this != _rAniTask)
					return; // Shouldn't happen, but who knows... just to make sure every active npc has only one timer.
				if (isMob())
				{
					// Cancel further animation timers until intention is changed to ACTIVE again.
					if (getAI().getIntention() != AI_INTENTION_ACTIVE)
						return;
				}
				else
				{
					if (!isInActiveRegion()) // NPCs in inactive region don't run this task
						return;
				}

				if (!(isDead() || isStunned() || isSleeping() || isParalyzed()))
					onRandomAnimation(Rnd.get(2, 3));

				startRandomAnimationTimer();
			}
			catch (Exception e)
			{
				_log.warn(e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2Npc and create a new RandomAnimation Task.
	 *
	 * @param animationId
	 *            the animation id.
	 */
	public void onRandomAnimation(int animationId)
	{
		// Send a packet SocialAction to all L2PcInstance in the _KnownPlayers of the L2Npc
		long now = System.currentTimeMillis();
		if (now - _lastSocialBroadcast > _minimalSocialInterval)
		{
			_lastSocialBroadcast = now;
			broadcastPacket(new SocialAction(this, animationId));
		}
	}

	/**
	 * Create a RandomAnimation Task that will be launched after the calculated delay.
	 */
	public void startRandomAnimationTimer()
	{
		if (!hasRandomAnimation())
			return;

		// Calculate the delay before the next animation
		int interval = 1000 * (isMob() ? Rnd.get(NPCConfig.MIN_MONSTER_ANIMATION, NPCConfig.MAX_MONSTER_ANIMATION) : Rnd.get(NPCConfig.MIN_NPC_ANIMATION, NPCConfig.MAX_NPC_ANIMATION));

		// Create a RandomAnimation Task that will be launched after the calculated delay
		_rAniTask = new RandomAnimationTask();
		ThreadPoolManager.getInstance().scheduleGeneral(_rAniTask, interval);
	}

	/**
	 * @return true if the server allows Random Animation, false if not or the AItype is a corpse.
	 */
	public boolean hasRandomAnimation()
	{
		return (NPCConfig.MAX_NPC_ANIMATION > 0 && !getAiType().equals(AIType.CORPSE));
	}

	/**
	 * Constructor of L2Npc (use L2Character constructor).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Call the L2Character constructor to set the _template of the L2Character (copy skills from template to object and link
	 * _calculators to NPC_STD_CALCULATOR)</li> <li>Set the name of the L2Character</li> <li>Create a RandomAnimation Task that
	 * will be launched after the calculated delay if the server allow it</li><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            The L2NpcTemplate to apply to the NPC
	 */
	public L2Npc(int objectId, L2NpcTemplate template)
	{
		// Call the L2Character constructor to set the _template of the L2Character, copy skills from template to object and link
		// _calculators to NPC_STD_CALCULATOR
		super(objectId, template);
		initCharStatusUpdateValues();

		// initialize the "current" equipment
		_currentLHandId = getTemplate().getLeftHand();
		_currentRHandId = getTemplate().getRightHand();
		_currentEnchant = getTemplate().getEnchantEffect();

		// initialize the "current" collisions
		_currentCollisionHeight = getTemplate().getCollisionHeight();
		_currentCollisionRadius = getTemplate().getCollisionRadius();

		if (template == null)
		{
			_log.error("No template for Npc. Please check your datapack is setup correctly.");
			return;
		}

		// Set the name of the L2Character
		setName(template.getName());
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new NpcKnownList(this));
	}

	@Override
	public NpcKnownList getKnownList()
	{
		return (NpcKnownList) super.getKnownList();
	}

	@Override
	public void initCharStat()
	{
		setStat(new NpcStat(this));
	}

	@Override
	public NpcStat getStat()
	{
		return (NpcStat) super.getStat();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new NpcStatus(this));
	}

	@Override
	public NpcStatus getStatus()
	{
		return (NpcStatus) super.getStatus();
	}

	/** Return the L2NpcTemplate of the L2Npc. */
	@Override
	public final L2NpcTemplate getTemplate()
	{
		return (L2NpcTemplate) super.getTemplate();
	}

	/**
	 * @return the generic Identifier of this L2Npc contained in the L2NpcTemplate.
	 */
	public int getNpcId()
	{
		return getTemplate().getNpcId();
	}

	@Override
	public boolean isAttackable()
	{
		return true;
	}

	/**
	 * Return the Level of this L2Npc contained in the L2NpcTemplate.
	 */
	@Override
	public final int getLevel()
	{
		return getTemplate().getLevel();
	}

	/**
	 * @return True if the L2Npc is agressive (ex : L2MonsterInstance in function of aggroRange).
	 */
	public boolean isAggressive()
	{
		return false;
	}

	/**
	 * Return True if this L2Npc is undead in function of the L2NpcTemplate.
	 */
	@Override
	public boolean isUndead()
	{
		return getTemplate().isUndead();
	}

	/**
	 * Send a packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the L2Npc.
	 */
	@Override
	public void updateAbnormalEffect()
	{
		// Send a Server->Client packet NpcInfo with state of abnormal effect to all L2PcInstance in the _KnownPlayers of the
		// L2Npc
		Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player == null)
				continue;

			if (getRunSpeed() == 0)
				player.sendPacket(new ServerObjectInfo(this, player));
			else
				player.sendPacket(new NpcInfo(this, player));
		}
	}

	/**
	 * <B><U> Values </U> :</B>
	 * <ul>
	 * <li>object is a L2NpcInstance or isn't a L2Character : 0 (don't remember it)</li>
	 * <li>object is a L2Playable : 1500</li>
	 * <li>others : 500</li>
	 * </ul>
	 *
	 * @param object
	 *            The Object to add to _knownObject
	 * @return the distance under which the object must be add to _knownObject in function of the object type.
	 */
	public int getDistanceToWatchObject(L2Object object)
	{
		if (object instanceof L2Playable)
			return 1500;

		if (object instanceof L2NpcInstance || !(object instanceof L2Character))
			return 0;

		if (object instanceof L2FestivalGuideInstance)
			return 10000;

		return 500;
	}

	/**
	 * <B><U> Overriden in </U> :</B>
	 * <ul>
	 * <li>L2MonsterInstance : Check if the attacker is not another L2MonsterInstance</li>
	 * <li>L2PcInstance</li>
	 * </ul>
	 */
	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	/**
	 * @return the Identifier of the item in the left hand of this L2Npc contained in the L2NpcTemplate.
	 */
	public int getLeftHandItem()
	{
		return _currentLHandId;
	}

	/**
	 * @return the Identifier of the item in the right hand of this L2Npc contained in the L2NpcTemplate.
	 */
	public int getRightHandItem()
	{
		return _currentRHandId;
	}

	public int getEnchantEffect()
	{
		return _currentEnchant;
	}

	/**
	 * @return True if this L2Npc has drops that can be sweeped.
	 */
	public boolean isSpoil()
	{
		return _isSpoil;
	}

	/**
	 * Set the spoil state of this L2Npc.
	 *
	 * @param isSpoil
	 *            boolean value.
	 */
	public void setSpoil(boolean isSpoil)
	{
		_isSpoil = isSpoil;
	}

	public final int getIsSpoiledBy()
	{
		return _isSpoiledBy;
	}

	public final void setIsSpoiledBy(int value)
	{
		_isSpoiledBy = value;
	}

	/**
	 * @return True if this L2Npc has spoken (used for SpeakingNPCs script).
	 */
	public boolean hasSpoken()
	{
		return _hasSpoken;
	}

	/**
	 * Set the speak state of this L2Npc.
	 *
	 * @param hasSpoken
	 *            boolean value.
	 */
	public void setHasSpoken(boolean hasSpoken)
	{
		_hasSpoken = hasSpoken;
	}

	/**
	 * @return the busy status of this L2Npc.
	 */
	public final boolean isBusy()
	{
		return _isBusy;
	}

	/**
	 * Set the busy status of this L2Npc.
	 *
	 * @param isBusy
	 *            boolean value.
	 */
	public void setBusy(boolean isBusy)
	{
		_isBusy = isBusy;
	}

	/**
	 * @return the busy message of this L2Npc.
	 */
	public final String getBusyMessage()
	{
		return _busyMessage;
	}

	/**
	 * Set the busy message of this L2Npc.
	 *
	 * @param message
	 *            String message to send to this L2Npc.
	 */
	public void setBusyMessage(String message)
	{
		_busyMessage = message;
	}

	/**
	 * Overidden in L2CastleWarehouse, L2ClanHallManager and L2Warehouse.
	 *
	 * @return true if this L2Npc instance can be warehouse manager.
	 */
	public boolean isWarehouse()
	{
		return false;
	}

	/**
	 * Manage actions when a player click on the L2Npc.<BR>
	 * <BR>
	 * <B><U> Actions on first click on the L2Npc (Select it)</U> :</B><BR>
	 * <BR>
	 * <li>Set the L2Npc as target of the L2PcInstance player (if necessary)</li> <li>Send a Server->Client packet
	 * MyTargetSelected to the L2PcInstance player (display the select window)</li> <li>If L2Npc is autoAttackable, send a
	 * Server->Client packet StatusUpdate to the L2PcInstance in order to update L2Npc HP bar</li> <li>Send a Server->Client
	 * packet ValidateLocation to correct the L2Npc position and heading on the client</li><BR>
	 * <BR>
	 * <B><U> Actions on second click on the L2Npc (Attack it/Intercat with it)</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MyTargetSelected to the L2PcInstance player (display the select window)</li> <li>If L2Npc
	 * is autoAttackable, notify the L2PcInstance AI with AI_INTENTION_ATTACK (after a height verification)</li> <li>If L2Npc is
	 * NOT autoAttackable, notify the L2PcInstance AI with AI_INTENTION_INTERACT (after a distance verification) and show message</li>
	 * <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in
	 * order to avoid that client wait an other packet</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action, AttackRequest</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2ArtefactInstance : Manage only fisrt click to select Artefact</li><BR>
	 * <BR>
	 * <li>L2GuardInstance :</li><BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance that start an action on the L2Npc
	 */
	@Override
	public void onAction(L2PcInstance player)
	{
		if (!player.canTarget())
			return;

		// Check if the L2PcInstance already target the L2Npc
		if (this != player.getTarget())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				getAI(); // wake up ai

				// Send MyTargetSelected to the L2PcInstance player
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

				// Send StatusUpdate of the L2Npc to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(this);
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}
			// Send MyTargetSelected to the L2PcInstance player
			else
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));

			// Send a Server->Client packet ValidateLocation to correct the L2Npc position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			// Check if the player is attackable (without a forced attack) and isn't dead
			if (isAutoAttackable(player) && !isAlikeDead())
			{
				// Check the height difference
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
				{
					// Set the L2PcInstance Intention to AI_INTENTION_ATTACK
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
				}
				else
				{
					// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another
					// packet
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
			}
			else if (!isAutoAttackable(player))
			{
				// Calculate the distance between the L2PcInstance and the L2Npc
				if (!canInteract(player))
				{
					// Notify the L2PcInstance AI with AI_INTENTION_INTERACT
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				}
				else
				{
					// Rotate the player to face the instance
					player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));

					// Send ActionFailed to the player in order to avoid he stucks
					player.sendPacket(ActionFailed.STATIC_PACKET);

					if (hasRandomAnimation())
						onRandomAnimation(Rnd.get(8));

					Quest[] qlsa = getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);
					if ((qlsa != null) && qlsa.length > 0)
						player.setLastQuestNpcObject(getObjectId());
					Quest[] qlst = getTemplate().getEventQuests(Quest.QuestEventType.ON_FIRST_TALK);
					if ((qlst != null) && qlst.length == 1)
						qlst[0].notifyFirstTalk(this, player);
					else
						showChatWindow(player);
				}
			}
		}
	}

	/**
	 * Manage and Display the GM console to modify the L2Npc (GM only).<BR>
	 * <BR>
	 * <B><U> Actions (If the L2PcInstance is a GM only)</U> :</B><BR>
	 * <BR>
	 * <li>Set the L2Npc as target of the L2PcInstance player (if necessary)</li> <li>Send a Server->Client packet
	 * MyTargetSelected to the L2PcInstance player (display the select window)</li> <li>If L2Npc is autoAttackable, send a
	 * Server->Client packet StatusUpdate to the L2PcInstance in order to update L2Npc HP bar</li> <li>Send a Server->Client
	 * NpcHtmlMessage() containing the GM console about this L2Npc</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : Each group of Server->Client packet must be terminated by a ActionFailed packet in
	 * order to avoid that client wait an other packet</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Client packet : Action</li><BR>
	 * <BR>
	 *
	 * @param client
	 *            The thread that manage the player that pessed Shift and click on the L2Npc
	 */
	@Override
	public void onActionShift(L2GameClient client)
	{
		// Get the L2PcInstance corresponding to the thread
		L2PcInstance player = client.getActiveChar();
		if (player == null)
			return;

		// Check if the L2PcInstance is a GM
		if (player.isGM())
		{
			// Set the target of the L2PcInstance player
			player.setTarget(this);

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel() - getLevel()));

			// Check if the player is attackable (without a forced attack)
			if (isAutoAttackable(player))
			{
				// Send a Server->Client packet StatusUpdate of the L2Npc to the L2PcInstance to update its HP bar
				StatusUpdate su = new StatusUpdate(this);
				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				player.sendPacket(su);
			}

			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(StaticHtmPath.AdminHtmPath + "npcinfo.htm");

			html.replace("%objid%", String.valueOf(getObjectId()));
			html.replace("%class%", getClass().getSimpleName());
			html.replace("%id%", String.valueOf(getTemplate().getNpcId()));
			html.replace("%lvl%", String.valueOf(getTemplate().getLevel()));
			html.replace("%name%", String.valueOf(getTemplate().getName()));
			html.replace("%tmplid%", String.valueOf(getTemplate().getNpcId()));
			html.replace("%aggro%", String.valueOf((this instanceof L2Attackable) ? ((L2Attackable) this).getAggroRange() : 0));
			html.replace("%corpse%", String.valueOf(getTemplate().getCorpseDecayTime()));
			html.replace("%enchant%", String.valueOf(getTemplate().getEnchantEffect()));
			html.replace("%hp%", String.valueOf((int) ((L2Character) this).getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(((L2Character) this).getMaxHp()));
			html.replace("%mp%", String.valueOf((int) ((L2Character) this).getCurrentMp()));
			html.replace("%mpmax%", String.valueOf(((L2Character) this).getMaxMp()));

			html.replace("%patk%", String.valueOf(((L2Character) this).getPAtk(null)));
			html.replace("%matk%", String.valueOf(((L2Character) this).getMAtk(null, null)));
			html.replace("%pdef%", String.valueOf(((L2Character) this).getPDef(null)));
			html.replace("%mdef%", String.valueOf(((L2Character) this).getMDef(null, null)));
			html.replace("%accu%", String.valueOf(((L2Character) this).getAccuracy()));
			html.replace("%evas%", String.valueOf(((L2Character) this).getEvasionRate(null)));
			html.replace("%crit%", String.valueOf(((L2Character) this).getCriticalHit(null, null)));
			html.replace("%rspd%", String.valueOf(((L2Character) this).getRunSpeed()));
			html.replace("%aspd%", String.valueOf(((L2Character) this).getPAtkSpd()));
			html.replace("%cspd%", String.valueOf(((L2Character) this).getMAtkSpd()));
			html.replace("%str%", String.valueOf(((L2Character) this).getSTR()));
			html.replace("%dex%", String.valueOf(((L2Character) this).getDEX()));
			html.replace("%con%", String.valueOf(((L2Character) this).getCON()));
			html.replace("%int%", String.valueOf(((L2Character) this).getINT()));
			html.replace("%wit%", String.valueOf(((L2Character) this).getWIT()));
			html.replace("%men%", String.valueOf(((L2Character) this).getMEN()));
			html.replace("%loc%", String.valueOf(getX() + " " + getY() + " " + getZ()));
			html.replace("%dist%", String.valueOf((int) Math.sqrt(player.getDistanceSq(this))));

			// byte attackAttribute = ((L2Character)this).getAttackElement();
			html.replace("%ele_atk_value%", "%todo%" /* String.valueOf(((L2Character)this).getAttackElementValue(attackAttribute)) */);
			html.replace("%ele_dfire%", String.valueOf(((L2Character) this).getDefenseElementValue((byte) 2)));
			html.replace("%ele_dwater%", String.valueOf(((L2Character) this).getDefenseElementValue((byte) 3)));
			html.replace("%ele_dwind%", String.valueOf(((L2Character) this).getDefenseElementValue((byte) 1)));
			html.replace("%ele_dearth%", String.valueOf(((L2Character) this).getDefenseElementValue((byte) 4)));
			html.replace("%ele_dholy%", String.valueOf(((L2Character) this).getDefenseElementValue((byte) 5)));
			html.replace("%ele_ddark%", String.valueOf(((L2Character) this).getDefenseElementValue((byte) 6)));

			if (getSpawn() != null)
			{
				html.replace("%spawn%", getSpawn().getLocx() + " " + getSpawn().getLocy() + " " + getSpawn().getLocz());
				html.replace("%loc2d%", String.valueOf((int) Math.sqrt(((L2Character) this).getPlanDistanceSq(getSpawn().getLocx(), getSpawn().getLocy()))));
				html.replace("%loc3d%", String.valueOf((int) Math.sqrt(((L2Character) this).getDistanceSq(getSpawn().getLocx(), getSpawn().getLocy(), getSpawn().getLocz()))));
				html.replace("%resp%", String.valueOf(getSpawn().getRespawnDelay() / 1000));
			}
			else
			{
				html.replace("%spawn%", "<font color=FF0000>null</font>");
				html.replace("%loc2d%", "<font color=FF0000>--</font>");
				html.replace("%loc3d%", "<font color=FF0000>--</font>");
				html.replace("%resp%", "<font color=FF0000>--</font>");
			}

			if (hasAI())
			{
				html.replace("%ai_intention%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>Intention:</font></td><td align=right width=170>" + String.valueOf(getAI().getIntention().name()) + "</td></tr></table></td></tr>");
				html.replace("%ai%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>AI</font></td><td align=right width=170>" + getAI().getClass().getSimpleName() + "</td></tr></table></td></tr>");
				html.replace("%ai_type%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>AIType</font></td><td align=right width=170>" + String.valueOf(getAiType()) + "</td></tr></table></td></tr>");
				html.replace("%ai_clan%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>Clan & Range:</font></td><td align=right width=170>" + String.valueOf(getClan()) + " " + String.valueOf(getClanRange()) + "</td></tr></table></td></tr>");
				html.replace("%ai_enemy_clan%", "<tr><td><table width=270 border=0><tr><td width=100><font color=FFAA00>Enemy & Range:</font></td><td align=right width=170>" + String.valueOf(getEnemyClan()) + " " + String.valueOf(getEnemyRange()) + "</td></tr></table></td></tr>");
			}
			else
			{
				html.replace("%ai_intention%", "");
				html.replace("%ai%", "");
				html.replace("%ai_type%", "");
				html.replace("%ai_clan%", "");
				html.replace("%ai_enemy_clan%", "");
			}

			if (this instanceof L2MerchantInstance)
				html.replace("%butt%", "<button value=\"Shop\" action=\"bypass -h admin_showShop " + String.valueOf(getTemplate().getNpcId()) + "\" width=65 height=19 back=\"L2UI_ch3.smallbutton2_over\" fore=\"L2UI_ch3.smallbutton2\">");
			else
				html.replace("%butt%", "");

			player.sendPacket(html);
		}

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * @return the L2Castle this L2Npc belongs to.
	 */
	public final Castle getCastle()
	{
		// Get castle this NPC belongs to (excluding L2Attackable)
		if (_castleIndex < 0)
		{
			L2TownZone town = TownManager.getTown(getX(), getY(), getZ());

			if (town != null)
				_castleIndex = CastleManager.getInstance().getCastleIndex(town.getTaxById());

			if (_castleIndex < 0)
				_castleIndex = CastleManager.getInstance().findNearestCastleIndex(this);
			else
				_isInTown = true; // Npc was spawned in town
		}

		if (_castleIndex < 0)
			return null;

		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}

	public final boolean getIsInTown()
	{
		if (_castleIndex < 0)
			getCastle();

		return _isInTown;
	}

	/**
	 * Open a quest or chat window on client with the text of the L2Npc in function of the command.
	 *
	 * @param player
	 *            The player to test
	 * @param command
	 *            The command string received from client
	 */
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (canInteract(player))
		{
			if (isBusy() && getBusyMessage().length() > 0)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(StaticHtmPath.NpcHtmPath + "npcbusy.htm");
				html.replace("%busymessage%", getBusyMessage());
				html.replace("%npcname%", getName());
				html.replace("%playername%", player.getName());
				player.sendPacket(html);
			}
			else if (command.equalsIgnoreCase("TerritoryStatus"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

				if (getCastle().getOwnerId() > 0)
				{
					html.setFile(StaticHtmPath.NpcHtmPath + "territorystatus.htm");
					L2Clan clan = ClanTable.getInstance().getClan(getCastle().getOwnerId());
					html.replace("%clanname%", clan.getName());
					html.replace("%clanleadername%", clan.getLeaderName());
				}
				else
					html.setFile(StaticHtmPath.NpcHtmPath + "territorynoclan.htm");

				html.replace("%castlename%", getCastle().getName());
				html.replace("%taxpercent%", "" + getCastle().getTaxPercent());
				html.replace("%objectId%", String.valueOf(getObjectId()));

				if (getCastle().getCastleId() > 6)
					html.replace("%territory%", "The Kingdom of Elmore");
				else
					html.replace("%territory%", "The Kingdom of Aden");

				player.sendPacket(html);
			}
			else if (command.startsWith("Quest"))
			{
				String quest = "";
				try
				{
					quest = command.substring(5).trim();
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}

				if (quest.length() == 0)
					showQuestWindow(player, this);
				else
					showQuestWindow(player, this, quest);
			}
			else if (command.startsWith("Chat"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}

				showChatWindow(player, val);
			}
			else if (command.startsWith("Link"))
			{
				String path = command.substring(5).trim();
				if (path.indexOf("..") != -1)
					return;
				String filename = StaticHtmPath.NpcHtmPath + path;
				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				player.sendPacket(html);
			}
			else if (command.startsWith("Loto"))
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException ioobe)
				{
				}
				catch (NumberFormatException nfe)
				{
				}

				if (val == 0)
				{
					// new loto ticket
					for (int i = 0; i < 5; i++)
						player.setLoto(i, 0);
				}
				showLotoWindow(player, val);
			}
			else if (command.startsWith("CPRecovery"))
			{
				makeCPRecovery(player);
			}
			else if (command.startsWith("SupportMagic"))
			{
				makeSupportMagic(player);
			}
			else if (command.startsWith("multisell"))
			{
				L2Multisell.getInstance().separateAndSend(Integer.parseInt(command.substring(9).trim()), player, false, getCastle().getTaxRate());
			}
			else if (command.startsWith("exc_multisell"))
			{
				L2Multisell.getInstance().separateAndSend(Integer.parseInt(command.substring(13).trim()), player, true, getCastle().getTaxRate());
			}
			else if (command.startsWith("Augment"))
			{
				int cmdChoice = Integer.parseInt(command.substring(8, 9).trim());
				switch (cmdChoice)
				{
					case 1:
						player.sendPacket(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED);
						player.sendPacket(ExShowVariationMakeWindow.STATIC_PACKET);
						break;
					case 2:
						player.sendPacket(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION);
						player.sendPacket(ExShowVariationCancelWindow.STATIC_PACKET);
						break;
				}
			}
			else if (command.startsWith("EnterRift"))
			{
				try
				{
					Byte b1 = Byte.parseByte(command.substring(10)); // Selected Area: Recruit, Soldier etc
					DimensionalRiftManager.getInstance().start(player, b1, this);
				}
				catch (Exception e)
				{
				}
			}
			else if (command.startsWith("ChangeRiftRoom"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
				{
					player.getParty().getDimensionalRift().manualTeleport(player, this);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, this);
				}
			}
			else if (command.startsWith("ExitRift"))
			{
				if (player.isInParty() && player.getParty().isInDimensionalRift())
				{
					player.getParty().getDimensionalRift().manualExitRift(player, this);
				}
				else
				{
					DimensionalRiftManager.getInstance().handleCheat(player, this);
				}
			}
		}
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	/**
	 * Return the weapon item equipped in the right hand of the L2Npc or null.<BR>
	 * <BR>
	 */
	@Override
	public L2Weapon getActiveWeaponItem()
	{
		// Get the weapon identifier equipped in the right hand of the L2Npc
		int weaponId = getTemplate().getRightHand();
		if (weaponId < 1)
			return null;

		// Get the weapon item equipped in the right hand of the L2Npc
		L2Item item = ItemTable.getInstance().getTemplate(weaponId);
		if (!(item instanceof L2Weapon))
			return null;

		return (L2Weapon) item;
	}

	/**
	 * Return null (regular NPCs don't have weapons instancies).<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	/**
	 * Return the item equipped in the left hand of the L2Npc or null.<BR>
	 * <BR>
	 */
	@Override
	public L2Item getSecondaryWeaponItem()
	{
		// Get the weapon identifier equipped in the right hand of the L2Npc
		int itemId = getTemplate().getLeftHand();
		if (itemId < 1)
			return null;

		// Return the item equipped in the left hand of the L2Npc
		return ItemTable.getInstance().getTemplate(itemId);
	}

	/**
	 * Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2Npc.<BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance who talks with the L2Npc
	 * @param content
	 *            The text of the L2NpcMessage
	 */
	public void insertObjectIdAndShowChatWindow(L2PcInstance player, String content)
	{
		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2Npc
		content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));
		NpcHtmlMessage npcReply = new NpcHtmlMessage(getObjectId());
		npcReply.setHtml(content);
		player.sendPacket(npcReply);
	}

	/**
	 * <B><U> Format of the pathfile </U> :</B><BR>
	 * <BR>
	 * <li>if the file exists on the server (page number = 0) : <B>../npc/default/12006.htm</B> (npcId-page number)</li> <li>if
	 * the file exists on the server (page number > 0) : <B>../npc/default/12006-1.htm</B> (npcId-page number)</li> <li>if the
	 * file doesn't exist on the server : <B>../npc/npcdefault.htm</B> (message : "I have nothing to say to you")</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2GuardInstance : Set the pathfile to ../npc/guard/12006-1.htm (npcId-page number)</li><BR>
	 * <BR>
	 *
	 * @param npcId
	 *            The Identifier of the L2Npc whose text must be display
	 * @param val
	 *            The number of the page to display
	 * @return the pathfile of the selected HTML file in function of the npcId and of the page number.
	 */
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		String temp = StaticHtmPath.DefaultHtmPath + pom + ".htm";

		if (HtmCache.getInstance().isLoadable(temp))
			return temp;

		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return StaticHtmPath.NpcHtmPath + "npcdefault.htm";
	}

	/**
	 * Open a choose quest window on client with all quests available of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param npc
	 *            The L2Npc instance.
	 * @param quests
	 *            The table containing quests of the L2NpcInstance
	 */
	public static void showQuestChooseWindow(L2PcInstance player, L2Npc npc, Quest[] quests)
	{
		final StringBuilder sb = StringUtil.startAppend(150, "<html><body>");

		for (Quest q : quests)
		{
			if (q == null)
				continue;

			StringUtil.append(sb, "<a action=\"bypass -h npc_", String.valueOf(npc.getObjectId()), "_Quest ", q.getName(), "\">[", q.getDescr());

			QuestState qs = player.getQuestState(q.getScriptName());
			if (qs != null)
			{
				if (qs.isStarted() && (qs.getInt("cond") > 0))
					sb.append(" (In Progress)");
				else if (qs.isCompleted())
					sb.append(" (Done)");
			}
			sb.append("]</a><br>");
		}

		sb.append("</body></html>");

		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		npc.insertObjectIdAndShowChatWindow(player, sb.toString());
	}

	/**
	 * Open a quest window on client with the text of the L2NpcInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the quest state in the folder ../quests/questId/stateId.htm</li> <li>Send a Server->Client
	 * NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance</li> <li>Send a Server->Client ActionFailed to
	 * the L2PcInstance in order to avoid that the client wait another packet</li><BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param npc
	 *            The L2Npc instance.
	 * @param questId
	 *            The Identifier of the quest to display the message
	 */
	public static void showQuestWindow(L2PcInstance player, L2Npc npc, String questId)
	{
		String content = null;

		Quest q = QuestManager.getInstance().getQuest(questId);

		// Get the state of the selected quest
		QuestState qs = player.getQuestState(questId);

		if (q != null)
		{
			if ((q.getQuestIntId() >= 1 && q.getQuestIntId() < 20000) && (player.getWeightPenalty() >= 3 || !player.isInventoryUnder80(true)))
			{
				player.sendPacket(SystemMessageId.INVENTORY_LESS_THAN_80_PERCENT);
				return;
			}

			if (qs == null)
			{
				if (q.getQuestIntId() >= 1 && q.getQuestIntId() < 20000)
				{
					if (player.getAllActiveQuests().length > 40) // if too many ongoing quests, don't show window and send message
					{
						player.sendPacket(SystemMessageId.TOO_MANY_QUESTS);
						return;
					}
				}
				// check for start point
				Quest[] qlst = npc.getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

				if (qlst != null && qlst.length > 0)
				{
					for (Quest temp : qlst)
					{
						if (temp == q)
						{
							qs = q.newQuestState(player);
							break;
						}
					}
				}
			}
		}
		else
			content = Quest.getNoQuestMsg(); // no quests found

		if (qs != null)
		{
			// If the quest is alreday started, no need to show a window
			if (!qs.getQuest().notifyTalk(npc, qs))
				return;

			questId = qs.getQuest().getName();
			String stateId = State.getStateName(qs.getState());
			String path = StaticHtmPath.QuestHtmPath + questId + "/" + stateId + ".htm";
			content = HtmCache.getInstance().getHtm(path); // TODO path for quests html

			if (_log.isTraceEnabled())
			{
				if (content != null)
					_log.trace("Showing quest window for quest " + questId + " html path: " + path);
				else
					_log.trace("File not exists for quest " + questId + " html path: " + path);
			}
		}

		// Send a Server->Client packet NpcHtmlMessage to the L2PcInstance in order to display the message of the L2NpcInstance
		if (content != null)
			npc.insertObjectIdAndShowChatWindow(player, content);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Collect awaiting quests/start points and display a QuestChooseWindow (if several available) or QuestWindow.<BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance that talk with the L2NpcInstance
	 * @param npc
	 *            The L2Npc instance.
	 */
	public static void showQuestWindow(L2PcInstance player, L2Npc npc)
	{
		// collect awaiting quests and start points
		List<Quest> options = new FastList<>();

		QuestState[] awaits = player.getQuestsForTalk(npc.getTemplate().getNpcId());
		Quest[] starts = npc.getTemplate().getEventQuests(Quest.QuestEventType.QUEST_START);

		// Quests are limited between 1 and 999 because those are the quests that are supported by the client.
		// By limiting them there, we are allowed to create custom quests at higher IDs without interfering
		if (awaits != null)
		{
			for (QuestState x : awaits)
			{
				if (!options.contains(x.getQuest()))
					if ((x.getQuest().getQuestIntId() > 0) && (x.getQuest().getQuestIntId() < 20000))
						options.add(x.getQuest());
			}
		}

		if (starts != null)
		{
			for (Quest x : starts)
			{
				if (!options.contains(x))
					if ((x.getQuestIntId() > 0) && (x.getQuestIntId() < 20000))
						options.add(x);
			}
		}

		// Display a QuestChooseWindow (if several quests are available) or QuestWindow
		if (options.size() > 1)
			showQuestChooseWindow(player, npc, options.toArray(new Quest[options.size()]));
		else if (options.size() == 1)
			showQuestWindow(player, npc, options.get(0).getName());
		else
			showQuestWindow(player, npc, "");
	}

	/**
	 * Make the NPC speaks to his current knownlist.
	 *
	 * @param message
	 *            The String message to send.
	 */
	public void broadcastNpcSay(String message)
	{
		broadcastPacket(new NpcSay(getObjectId(), Say2.ALL, getNpcId(), message));
	}

	/**
	 * Open a Loto window on client with the text of the L2Npc.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li> <li>Send a Server->Client
	 * NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance</li> <li>Send a Server->Client ActionFailed to the
	 * L2PcInstance in order to avoid that the client wait another packet</li><BR>
	 *
	 * @param player
	 *            The L2PcInstance that talk with the L2Npc
	 * @param val
	 *            The number of the page of the L2Npc to display
	 */
	// 0 - first buy lottery ticket window
	// 1-20 - buttons
	// 21 - second buy lottery ticket window
	// 22 - selected ticket with 5 numbers
	// 23 - current lottery jackpot
	// 24 - Previous winning numbers/Prize claim
	// >24 - check lottery ticket by item object id
	public void showLotoWindow(L2PcInstance player, int val)
	{
		int npcId = getTemplate().getNpcId();
		String filename;
		SystemMessage sm;
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

		if (val == 0) // 0 - first buy lottery ticket window
		{
			filename = (getHtmlPath(npcId, 1));
			html.setFile(filename);
		}
		else if (val >= 1 && val <= 21) // 1-20 - buttons, 21 - second buy lottery ticket window
		{
			if (!Lottery.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD);
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE);
				return;
			}

			filename = (getHtmlPath(npcId, 5));
			html.setFile(filename);

			int count = 0;
			int found = 0;
			// counting buttons and unsetting button if found
			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == val)
				{
					// unsetting button
					player.setLoto(i, 0);
					found = 1;
				}
				else if (player.getLoto(i) > 0)
				{
					count++;
				}
			}

			// if not rearched limit 5 and not unseted value
			if (count < 5 && found == 0 && val <= 20)
				for (int i = 0; i < 5; i++)
					if (player.getLoto(i) == 0)
					{
						player.setLoto(i, val);
						break;
					}

			// setting pusshed buttons
			count = 0;
			for (int i = 0; i < 5; i++)
				if (player.getLoto(i) > 0)
				{
					count++;
					String button = String.valueOf(player.getLoto(i));
					if (player.getLoto(i) < 10)
						button = "0" + button;
					String search = "fore=\"L2UI.lottoNum" + button + "\" back=\"L2UI.lottoNum" + button + "a_check\"";
					String replace = "fore=\"L2UI.lottoNum" + button + "a_check\" back=\"L2UI.lottoNum" + button + "\"";
					html.replace(search, replace);
				}

			if (count == 5)
			{
				String search = "0\">Return";
				String replace = "22\">The winner selected the numbers above.";
				html.replace(search, replace);
			}
		}
		else if (val == 22) // 22 - selected ticket with 5 numbers
		{
			if (!Lottery.getInstance().isStarted())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_CURRENT_SOLD);
				return;
			}
			if (!Lottery.getInstance().isSellableTickets())
			{
				// tickets can't be sold
				player.sendPacket(SystemMessageId.NO_LOTTERY_TICKETS_AVAILABLE);
				return;
			}

			int price = EventsConfig.ALT_LOTTERY_TICKET_PRICE;
			int lotonumber = Lottery.getInstance().getId();
			int enchant = 0;
			int type2 = 0;

			for (int i = 0; i < 5; i++)
			{
				if (player.getLoto(i) == 0)
					return;

				if (player.getLoto(i) < 17)
					enchant += Math.pow(2, player.getLoto(i) - 1);
				else
					type2 += Math.pow(2, player.getLoto(i) - 17);
			}
			if (player.getAdena() < price)
			{
				sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				player.sendPacket(sm);
				return;
			}
			if (!player.reduceAdena("Loto", price, this, true))
				return;
			Lottery.getInstance().increasePrize(price);

			sm = SystemMessage.getSystemMessage(SystemMessageId.ACQUIRED_S1_S2);
			sm.addNumber(lotonumber);
			sm.addItemName(4442);
			player.sendPacket(sm);

			L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), 4442);
			item.setCount(1);
			item.setCustomType1(lotonumber);
			item.setEnchantLevel(enchant);
			item.setCustomType2(type2);
			player.getInventory().addItem("Loto", item, player, this);

			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(item);
			L2ItemInstance adenaupdate = player.getInventory().getItemByItemId(57);
			iu.addModifiedItem(adenaupdate);
			player.sendPacket(iu);

			filename = (getHtmlPath(npcId, 3));
			html.setFile(filename);
		}
		else if (val == 23) // 23 - current lottery jackpot
		{
			filename = (getHtmlPath(npcId, 3));
			html.setFile(filename);
		}
		else if (val == 24) // 24 - Previous winning numbers/Prize claim
		{
			filename = (getHtmlPath(npcId, 4));
			html.setFile(filename);

			int lotonumber = Lottery.getInstance().getId();
			String message = "";
			for (L2ItemInstance item : player.getInventory().getItems())
			{
				if (item == null)
					continue;
				if (item.getItemId() == 4442 && item.getCustomType1() < lotonumber)
				{
					message = message + "<a action=\"bypass -h npc_%objectId%_Loto " + item.getObjectId() + "\">" + item.getCustomType1() + " Event Number ";
					int[] numbers = Lottery.decodeNumbers(item.getEnchantLevel(), item.getCustomType2());
					for (int i = 0; i < 5; i++)
					{
						message += numbers[i] + " ";
					}
					int[] check = Lottery.checkTicket(item);
					if (check[0] > 0)
					{
						switch (check[0])
						{
							case 1:
								message += "- 1st Prize";
								break;
							case 2:
								message += "- 2nd Prize";
								break;
							case 3:
								message += "- 3th Prize";
								break;
							case 4:
								message += "- 4th Prize";
								break;
						}
						message += " " + check[1] + "a.";
					}
					message += "</a><br>";
				}
			}

			if (message.isEmpty())
				message += "There is no winning lottery ticket...<br>";

			html.replace("%result%", message);
		}
		else if (val > 24) // >24 - check lottery ticket by item object id
		{
			int lotonumber = Lottery.getInstance().getId();
			L2ItemInstance item = player.getInventory().getItemByObjectId(val);
			if (item == null || item.getItemId() != 4442 || item.getCustomType1() >= lotonumber)
				return;
			int[] check = Lottery.checkTicket(item);

			sm = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
			sm.addItemName(4442);
			player.sendPacket(sm);

			int adena = check[1];
			if (adena > 0)
				player.addAdena("Loto", adena, this, true);
			player.destroyItem("Loto", item, this, false);
			return;
		}
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%race%", "" + Lottery.getInstance().getId());
		html.replace("%adena%", "" + Lottery.getInstance().getPrize());
		html.replace("%ticket_price%", "" + EventsConfig.ALT_LOTTERY_TICKET_PRICE);
		html.replace("%prize5%", "" + (EventsConfig.ALT_LOTTERY_5_NUMBER_RATE * 100));
		html.replace("%prize4%", "" + (EventsConfig.ALT_LOTTERY_4_NUMBER_RATE * 100));
		html.replace("%prize3%", "" + (EventsConfig.ALT_LOTTERY_3_NUMBER_RATE * 100));
		html.replace("%prize2%", "" + EventsConfig.ALT_LOTTERY_2_AND_1_NUMBER_PRIZE);
		html.replace("%enddate%", "" + DateFormat.getDateInstance().format(Lottery.getInstance().getEndDate()));
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	public void makeCPRecovery(L2PcInstance player)
	{
		if (getNpcId() != 31225 && getNpcId() != 31226)
			return;

		if (player.isCursedWeaponEquipped())
		{
			player.sendMessage("Go away, you're not welcome here.");
			return;
		}

		// Consume 100 adenas
		if (player.reduceAdena("RestoreCP", 100, player.getCurrentFolkNPC(), true))
		{
			setTarget(player);
			doCast(FrequentSkill.ARENA_CP_RECOVERY.getSkill());
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CP_WILL_BE_RESTORED).addPcName(player));
		}
		else
			player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
	}

	/**
	 * Add Newbie helper buffs to L2Player according to its level.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the range level in wich player must be to obtain buff</li> <li>If player level is out of range, display a message
	 * and return</li> <li>According to player level cast buff</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> Newbie Helper Buff list is define in sql table helper_buff_list</B></FONT><BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance that talk with the L2Npc
	 */
	public void makeSupportMagic(L2PcInstance player)
	{
		if (player == null)
			return;

		// Prevent a cursed weapon weilder of being buffed
		if (player.isCursedWeaponEquipped())
			return;

		int player_level = player.getLevel();
		int lowestLevel = 0;
		int higestLevel = 0;

		// Select the player
		setTarget(player);

		// Calculate the min and max level between wich the player must be to obtain buff
		if (player.isMageClass())
		{
			lowestLevel = HelperBuffData.getInstance().getMagicClassLowestLevel();
			higestLevel = HelperBuffData.getInstance().getMagicClassHighestLevel();
		}
		else
		{
			lowestLevel = HelperBuffData.getInstance().getPhysicClassLowestLevel();
			higestLevel = HelperBuffData.getInstance().getPhysicClassHighestLevel();
		}

		// If the player is too high level, display a message and return
		if (player_level > higestLevel || !player.isNewbie())
		{
			String content = "<html><body>Newbie Guide:<br>Only a <font color=\"LEVEL\">novice character of level " + higestLevel + " or less</font> can receive my support magic.<br>Your novice character is the first one that you created and raised in this world.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		// If the player is too low level, display a message and return
		if (player_level < lowestLevel)
		{
			String content = "<html><body>Come back here when you have reached level " + lowestLevel + ". I will give you support magic then.</body></html>";
			insertObjectIdAndShowChatWindow(player, content);
			return;
		}

		L2Skill skill = null;
		// Go through the Helper Buff list define in sql table helper_buff_list and cast skill
		for (L2HelperBuff helperBuffItem : HelperBuffData.getInstance().getHelperBuffData())
		{
			if (helperBuffItem.isMagicClassBuff() == player.isMageClass())
			{
				if (player_level >= helperBuffItem.getLowerLevel() && player_level <= helperBuffItem.getUpperLevel())
				{
					skill = SkillTable.getInstance().getInfo(helperBuffItem.getSkillID(), helperBuffItem.getSkillLevel());
					if (skill.getSkillType() == L2SkillType.SUMMON)
						player.doCast(skill);
					else
						doCast(skill);
				}
			}
		}
	}

	/**
	 * Returns true if html exists
	 *
	 * @param player
	 * @param type
	 * @return boolean
	 */
	private boolean showPkDenyChatWindow(L2PcInstance player, String type)
	{
		String html = HtmCache.getInstance().getHtm(StaticHtmPath.NpcHtmPath + type + "/" + getNpcId() + "-pk.htm");

		if (html != null)
		{
			NpcHtmlMessage pkDenyMsg = new NpcHtmlMessage(getObjectId());
			pkDenyMsg.setHtml(html);
			player.sendPacket(pkDenyMsg);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}

		return false;
	}

	/**
	 * Open a chat window on client with the text of the L2Npc.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the text of the selected HTML file in function of the npcId and of the page number</li> <li>Send a Server->Client
	 * NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance</li> <li>Send a Server->Client ActionFailed to the
	 * L2PcInstance in order to avoid that the client wait another packet</li><BR>
	 *
	 * @param player
	 *            The L2PcInstance that talk with the L2Npc
	 */
	public void showChatWindow(L2PcInstance player)
	{
		showChatWindow(player, 0);
	}

	public void showChatWindow(L2PcInstance player, int val)
	{
		if (player.getKarma() > 0)
		{
			if (!PlayersConfig.KARMA_PLAYER_CAN_SHOP && this instanceof L2MerchantInstance)
			{
				if (showPkDenyChatWindow(player, "merchant"))
					return;
			}
			else if (!PlayersConfig.KARMA_PLAYER_CAN_USE_GK && this instanceof L2TeleporterInstance)
			{
				if (showPkDenyChatWindow(player, "teleporter"))
					return;
			}
			else if (!PlayersConfig.KARMA_PLAYER_CAN_USE_WH && this instanceof L2WarehouseInstance)
			{
				if (showPkDenyChatWindow(player, "warehouse"))
					return;
			}
			else if (!PlayersConfig.KARMA_PLAYER_CAN_SHOP && this instanceof L2FishermanInstance)
			{
				if (showPkDenyChatWindow(player, "fisherman"))
					return;
			}
		}

		final int npcId = getNpcId();
		String filename;

		if (npcId >= 31865 && npcId <= 31918)
			filename = SevenSigns.SEVEN_SIGNS_HTML_PATH + "rift/GuardianOfBorder.htm";
		else
			filename = getHtmlPath(npcId, val);

		// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * Open a chat window on client with the text specified by the given file name and path,<BR>
	 * relative to the datapack root. <BR>
	 * <BR>
	 * Added by Tempy
	 *
	 * @param player
	 *            The L2PcInstance that talk with the L2Npc
	 * @param filename
	 *            The filename that contains the text to send
	 */
	public void showChatWindow(L2PcInstance player, String filename)
	{
		// Send a Server->Client NpcHtmlMessage containing the text of the L2Npc to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);

		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	/**
	 * @return the Exp Reward of this L2Npc contained in the L2NpcTemplate (modified by RATE_XP).
	 */
	public int getExpReward(int isPremium)
	{
		if (isPremium == 1)
			return (int) (getTemplate().getRewardExp() * CustomConfig.PREMIUM_RATE_XP);
		else
			return (int) (getTemplate().getRewardExp() * MainConfig.RATE_XP);
	}

	/**
	 * @return the SP Reward of this L2Npc contained in the L2NpcTemplate (modified by RATE_SP).
	 */
	public int getSpReward(int isPremium)
	{
		if (isPremium == 1)
			return (int) (getTemplate().getRewardSp() * CustomConfig.PREMIUM_RATE_SP);
		else
			return (int) (getTemplate().getRewardSp() * MainConfig.RATE_SP);
	}

	/**
	 * Kill the L2Npc (the corpse disappeared after 7 seconds).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create a DecayTask to remove the corpse of the L2Npc after 7 seconds</li> <li>Set target to null and cancel Attack or
	 * Cast</li> <li>Stop movement</li> <li>Stop HP/MP/CP Regeneration task</li> <li>Stop all active skills effects in progress on
	 * the L2Character</li> <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to
	 * inform</li> <li>Notify L2Character AI</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Attackable</li><BR>
	 * <BR>
	 *
	 * @param killer
	 *            The L2Character who killed it
	 */
	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		// normally this wouldn't really be needed, but for those few exceptions,
		// we do need to reset the weapons back to the initial templated weapon.
		_currentLHandId = getTemplate().getLeftHand();
		_currentRHandId = getTemplate().getRightHand();
		_currentCollisionHeight = getTemplate().getCollisionHeight();
		_currentCollisionRadius = getTemplate().getCollisionRadius();
		DecayTaskManager.getInstance().addDecayTask(this);
		return true;
	}

	/**
	 * Set the spawn of the L2Npc.<BR>
	 * <BR>
	 *
	 * @param spawn
	 *            The L2Spawn that manage the L2Npc
	 */
	public void setSpawn(L2Spawn spawn)
	{
		_spawn = spawn;
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();

		if (getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN) != null)
			for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_SPAWN))
				quest.notifySpawn(this);
	}

	/**
	 * Remove the L2Npc from the world and update its spawn object (for a complete removal use the deleteMe method).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2Npc from the world when the decay task is launched</li> <li>Decrease its spawn counter</li> <li>Manage
	 * Siege task (killFlag, killCT)</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void onDecay()
	{
		if (isDecayed())
			return;

		setDecayed(true);

		// Remove the L2Npc from the world when the decay task is launched
		super.onDecay();

		// Decrease its spawn counter
		if (_spawn != null)
			_spawn.decreaseCount(this);
	}

	/**
	 * Remove PROPERLY the L2Npc from the world.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the L2Npc from the world and update its spawn object</li> <li>Remove all L2Object from _knownObjects and
	 * _knownPlayer of the L2Npc then cancel Attak or Cast and notify AI</li> <li>Remove L2Object object from _allObjects of
	 * L2World</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	@Override
	public void deleteMe()
	{
		// Decay
		try
		{
			onDecay();
		}
		catch (Exception e)
		{
			_log.warn("Failed decayMe().", e);
		}

		try
		{
			if (_fusionSkill != null)
				abortCast();

			for (L2Character character : getKnownList().getKnownCharacters())
				if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this)
					character.abortCast();
		}
		catch (Exception e)
		{
			_log.warn(e.getLocalizedMessage(), e);
		}

		L2WorldRegion oldRegion = getWorldRegion();
		if (oldRegion != null)
			oldRegion.removeFromZones(this);

		// Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
		try
		{
			getKnownList().removeAllKnownObjects();
		}
		catch (Exception e)
		{
			_log.warn("Failed removing cleaning knownlist.", e);
		}

		// Remove L2Object object from _allObjects of L2World
		L2World.getInstance().removeObject(this);

		super.deleteMe();
	}

	/**
	 * @return the L2Spawn object that manage this L2Npc.
	 */
	public L2Spawn getSpawn()
	{
		return _spawn;
	}

	@Override
	public String toString()
	{
		return getTemplate().getName();
	}

	public boolean isDecayed()
	{
		return _isDecayed;
	}

	public void setDecayed(boolean decayed)
	{
		_isDecayed = decayed;
	}

	public void endDecayTask()
	{
		if (!isDecayed())
		{
			DecayTaskManager.getInstance().cancelDecayTask(this);
			onDecay();
		}
	}

	/**
	 * Used for animation timers, overridden in L2Attackable.
	 *
	 * @return true if L2Attackable, false otherwise.
	 */
	public boolean isMob()
	{
		return false; // This means we use MAX_NPC_ANIMATION instead of MAX_MONSTER_ANIMATION
	}

	public void setLHandId(int newWeaponId)
	{
		_currentLHandId = newWeaponId;
	}

	public void setRHandId(int newWeaponId)
	{
		_currentRHandId = newWeaponId;
	}

	public void setCollisionHeight(int height)
	{
		_currentCollisionHeight = height;
	}

	public int getCollisionHeight()
	{
		return _currentCollisionHeight;
	}

	public void setCollisionRadius(int radius)
	{
		_currentCollisionRadius = radius;
	}

	public int getCollisionRadius()
	{
		return _currentCollisionRadius;
	}

	public int getCorpseDecayTime()
	{
		return getTemplate().getCorpseDecayTime() * 1000;
	}

	public L2Npc scheduleDespawn(long delay)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(this.new DespawnTask(), delay);
		return this;
	}

	protected class DespawnTask implements Runnable
	{
		@Override
		public void run()
		{
			if (!isDecayed())
				deleteMe();
		}
	}

	@Override
	protected final void notifyQuestEventSkillFinished(L2Skill skill, L2Object target)
	{
		try
		{
			if (getTemplate().getEventQuests(Quest.QuestEventType.ON_SPELL_FINISHED) != null)
			{
				L2PcInstance player = null;
				if (target != null)
					player = target.getActingPlayer();
				for (Quest quest : getTemplate().getEventQuests(Quest.QuestEventType.ON_SPELL_FINISHED))
				{
					quest.notifySpellFinished(this, player, skill);
				}
			}
		}
		catch (Exception e)
		{
			_log.warn(e.getLocalizedMessage(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see silentium.gameserver.model.actor.L2Character#isMovementDisabled()
	 */
	@Override
	public boolean isMovementDisabled()
	{
		return super.isMovementDisabled() || getCanMove() == 0 || getAiType().equals(AIType.CORPSE);
	}

	public AIType getAiType()
	{
		return _staticAIData.getAiType();
	}

	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		if (getRunSpeed() == 0)
			activeChar.sendPacket(new ServerObjectInfo(this, activeChar));
		else
			activeChar.sendPacket(new NpcInfo(this, activeChar));
	}
}
