/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor;

import com.google.common.collect.Sets;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.utils.Point3D;
import silentium.commons.utils.Rnd;
import silentium.gameserver.GameTimeController;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.ai.AttackableAI;
import silentium.gameserver.ai.CharacterAI;
import silentium.gameserver.ai.CtrlEvent;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.NPCConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.data.xml.DoorData;
import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.data.xml.MapRegionData.TeleportWhereType;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.geo.pathfinding.AbstractNodeLoc;
import silentium.gameserver.geo.pathfinding.PathFinding;
import silentium.gameserver.handler.ISkillHandler;
import silentium.gameserver.handler.SkillHandler;
import silentium.gameserver.instancemanager.DimensionalRiftManager;
import silentium.gameserver.instancemanager.TownManager;
import silentium.gameserver.model.*;
import silentium.gameserver.model.L2Skill.SkillTargetType;
import silentium.gameserver.model.actor.instance.*;
import silentium.gameserver.model.actor.instance.L2PcInstance.SkillDat;
import silentium.gameserver.model.actor.knownlist.CharKnownList;
import silentium.gameserver.model.actor.position.CharPosition;
import silentium.gameserver.model.actor.stat.CharStat;
import silentium.gameserver.model.actor.status.CharStatus;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import silentium.gameserver.network.serverpackets.*;
import silentium.gameserver.network.serverpackets.FlyToLocation.FlyType;
import silentium.gameserver.skills.AbnormalEffect;
import silentium.gameserver.skills.Calculator;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;
import silentium.gameserver.skills.effects.EffectChanceSkillTrigger;
import silentium.gameserver.tables.SkillTable.FrequentSkill;
import silentium.gameserver.taskmanager.AttackStanceTaskManager;
import silentium.gameserver.templates.chars.L2CharTemplate;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.templates.item.L2Weapon;
import silentium.gameserver.templates.item.L2WeaponType;
import silentium.gameserver.templates.skills.L2EffectType;
import silentium.gameserver.templates.skills.L2SkillType;
import silentium.gameserver.utils.Broadcast;
import silentium.gameserver.utils.L2TIntObjectHashMap;
import silentium.gameserver.utils.Util;

import java.util.*;
import java.util.concurrent.Future;

import static silentium.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;
import static silentium.gameserver.ai.CtrlIntention.AI_INTENTION_FOLLOW;

/**
 * L2Character is the mother class of all character objects of the world (PC, NPC...) :<br>
 * <br>
 * <b><u> Instances using it </u> :</b> <li>L2CastleGuardInstance</li> <li>L2DoorInstance</li> <li>L2Npc</li> <li>L2Playable</li>
 */
public abstract class L2Character extends L2Object
{
	public static final Logger _log = LoggerFactory.getLogger(L2Character.class.getName());

	private Set<L2Character> _attackByList;

	private volatile boolean _isCastingNow = false;
	private volatile boolean _isCastingSimultaneouslyNow = false;
	private L2Skill _lastSkillCast;
	private L2Skill _lastSimultaneousSkillCast;

	private boolean _isFlying = false; // Is flying wyvern ?
	private boolean _isRiding = false; // Is riding strider ?

	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false;
	private boolean _isParalyzed = false;
	private boolean _isDead = false;
	private boolean _isPendingRevive = false;
	private boolean _isRunning = false;
	protected boolean _isTeleporting = false;
	protected boolean _showSummonAnimation = false;

	protected boolean _isInvul = false;
	private boolean _isMortal = true;

	private boolean _isNoRndWalk = false;
	private boolean _AIdisabled = false;

	private CharStat _stat;
	private CharStatus _status;
	private L2CharTemplate _template; // The link on the L2CharTemplate object containing generic and static properties

	private String _title;
	private String _aiClass = "default";

	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	private boolean _champion = false;

	private Calculator[] _calculators;
	protected final L2TIntObjectHashMap<L2Skill> _skills;

	private ChanceSkillList _chanceSkills;
	protected FusionSkill _fusionSkill;

	/** Zone system */
	public static final byte ZONE_PVP = 0;
	public static final byte ZONE_PEACE = 1;
	public static final byte ZONE_SIEGE = 2;
	public static final byte ZONE_MOTHERTREE = 3;
	public static final byte ZONE_CLANHALL = 4;
	public static final byte ZONE_NOLANDING = 5;
	public static final byte ZONE_WATER = 6;
	public static final byte ZONE_JAIL = 7;
	public static final byte ZONE_MONSTERTRACK = 8;
	public static final byte ZONE_CASTLE = 9;
	public static final byte ZONE_SWAMP = 10;
	public static final byte ZONE_NOSUMMONFRIEND = 11;
	public static final byte ZONE_NOSTORE = 12;
	public static final byte ZONE_TOWN = 13;
	public static final byte ZONE_SCRIPT = 14;
	public static final byte ZONE_HQ = 15;
	public static final byte ZONE_DANGERAREA = 16;
	public static final byte ZONE_CASTONARTIFACT = 17;
	public static final byte ZONE_NORESTART = 18;

	private final byte[] _zones = new byte[19];
	protected byte _zoneValidateCounter = 4;

	private boolean _isRaid = false;

	/**
	 * Constructor of L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...). All of those
	 * properties are stored in a different template for each type of L2Character. Each template is loaded once in the server
	 * cache memory (reduce memory use). When a new instance of L2Character is spawned, server just create a link between the
	 * instance and the template This link is stored in <B>_template</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _template of the L2Character</li> <li>Set _overloaded to false (the charcater can take more items)</li><BR>
	 * <BR>
	 * <li>If L2Character is a L2Npc, copy skills from template to object</li> <li>If L2Character is a L2Npc, link _calculators to
	 * NPC_STD_CALCULATOR</li><BR>
	 * <BR>
	 * <li>If L2Character is NOT a L2Npc, create an empty _skills slot</li> <li>If L2Character is a L2PcInstance or L2Summon, copy
	 * basic Calculator set to object</li><BR>
	 * <BR>
	 *
	 * @param objectId
	 *            Identifier of the object to initialized
	 * @param template
	 *            The L2CharTemplate to apply to the object
	 */
	public L2Character(int objectId, L2CharTemplate template)
	{
		super(objectId);
		initCharStat();
		initCharStatus();

		// Set its template to the new L2Character
		_template = template;

		if (this instanceof L2Npc)
		{
			// Copy the Standard Calcultors of the L2Npc in _calculators
			_calculators = NPC_STD_CALCULATOR;

			// Copy the skills of the L2Npc from its template to the L2Character Instance
			// The skills list can be affected by spell effects so it's necessary to make a copy
			// to avoid that a spell affecting a L2Npc, affects others L2Npc of the same type too.
			_skills = new L2TIntObjectHashMap<>();
			if (((L2NpcTemplate) template).getSkills() != null)
				_skills.putAll(((L2NpcTemplate) template).getSkills());

			if (_skills != null)
			{
				for (L2Skill skill : getAllSkills())
					addStatFuncs(skill.getStatFuncs(null, this));
			}
		}
		else
		{
			// If L2Character is a L2PcInstance or a L2Summon, create the basic calculator set
			_calculators = new Calculator[Stats.NUM_STATS];

			if (this instanceof L2Summon)
			{
				// Copy the skills of the L2Summon from its template to the L2Character Instance
				// The skills list can be affected by spell effects so it's necessary to make a copy
				// to avoid that a spell affecting a L2Summon, affects others L2Summon of the same type too.
				_skills = new L2TIntObjectHashMap<>();
				_skills.putAll(((L2NpcTemplate) template).getSkills());
				if (_skills != null)
				{
					for (L2Skill skill : getAllSkills())
						addStatFuncs(skill.getStatFuncs(null, this));
				}
			}
			// Initialize the FastMap _skills to null
			else
				_skills = new L2TIntObjectHashMap<>();

			Formulas.addFuncsToNewCharacter(this);
		}
	}

	protected void initCharStatusUpdateValues()
	{
		_hpUpdateInterval = getMaxHp() / 352.0; // MAX_HP div MAX_HP_BAR_PX
		_hpUpdateIncCheck = getMaxHp();
		_hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
	}

	/**
	 * Remove the L2Character from the world when the decay task is launched.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
	 * <BR>
	 */
	public void onDecay()
	{
		L2WorldRegion reg = getWorldRegion();
		decayMe();
		if (reg != null)
			reg.removeFromZones(this);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		revalidateZone(true);
	}

	public void onTeleported()
	{
		if (!isTeleporting())
			return;

		spawnMe(getPosition().getX(), getPosition().getY(), getPosition().getZ());
		setIsTeleporting(false);

		if (_isPendingRevive)
			doRevive();
	}

	/**
	 * @return character inventory, default null, overridden in L2Playable types and in L2Npc.
	 */
	public Inventory getInventory()
	{
		return null;
	}

	public boolean destroyItemByItemId(String process, int itemId, int count, L2Object reference, boolean sendMessage)
	{
		return true;
	}

	public boolean destroyItem(String process, int objectId, int count, L2Object reference, boolean sendMessage)
	{
		return true;
	}

	public final boolean isInsideZone(final byte zone)
	{
		return zone == ZONE_PVP ? _zones[ZONE_PVP] > 0 && _zones[ZONE_PEACE] == 0 : _zones[zone] > 0;
	}

	public final void setInsideZone(final byte zone, final boolean state)
	{
		if (state)
			_zones[zone]++;
		else
		{
			_zones[zone]--;
			if (_zones[zone] < 0)
				_zones[zone] = 0;
		}
	}

	/**
	 * @return true if the player is GM.
	 */
	public boolean isGM()
	{
		return false;
	}

	/**
	 * Add L2Character instance that is attacking to the attacker list.
	 *
	 * @param player
	 *            The L2Character that attacks this one.
	 */
	public void addAttackerToAttackByList(L2Character player)
	{
		// DS: moved to L2Attackable
	}

	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character.
	 *
	 * @param mov
	 *            The packet to send.
	 */
	public void broadcastPacket(L2GameServerPacket mov)
	{
		Broadcast.toSelfAndKnownPlayers(this, mov);
	}

	/**
	 * Send a packet to the L2Character AND to all L2PcInstance in the radius (max knownlist radius) from the L2Character.
	 *
	 * @param mov
	 *            The packet to send.
	 * @param radius
	 *            The radius to make check on.
	 */
	public void broadcastPacket(L2GameServerPacket mov, int radius)
	{
		Broadcast.toSelfAndKnownPlayersInRadius(this, mov, radius);
	}

	/**
	 * @param barPixels
	 * @return boolean true if hp update should be done, false if not.
	 */
	protected boolean needHpUpdate(int barPixels)
	{
		double currentHp = getCurrentHp();

		if (currentHp <= 1.0 || getMaxHp() < barPixels)
			return true;

		if (currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck)
		{
			if (currentHp == getMaxHp())
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;

				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			return true;
		}
		return false;
	}

	/**
	 * Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create the Server->Client packet StatusUpdate with current HP and MP</li> <li>Send the Server->Client packet
	 * StatusUpdate with current HP and MP to all L2Character called _statusListener that must be informed of HP/MP updates of
	 * this L2Character</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND CP information</B></FONT><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Send current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other
	 * L2PcInstance of the Party</li><BR>
	 * <BR>
	 */
	public void broadcastStatusUpdate()
	{
		if (getStatus().getStatusListener().isEmpty())
			return;

		if (!needHpUpdate(352))
			return;

		_log.debug("Broadcast Status Update for " + getObjectId() + "(" + getName() + "). HP: " + getCurrentHp());

		// Create the Server->Client packet StatusUpdate with current HP
		StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());

		// Go through the StatusListener
		for (L2Character temp : getStatus().getStatusListener())
		{
			if (temp != null)
				temp.sendPacket(su);
		}
	}

	/**
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param mov
	 *            The packet to send.
	 */
	public void sendPacket(L2GameServerPacket mov)
	{
		// default implementation
	}

	/**
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 *
	 * @param text
	 *            The string to send.
	 */
	public void sendMessage(String text)
	{
		// default implementation
	}

	/**
	 * Teleport a L2Character and its pet if necessary.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Stop the movement of the L2Character</li> <li>Set the x,y,z position of the L2Object and if necessary modify its
	 * _worldRegion</li> <li>Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in its
	 * _KnownPlayers</li> <li>Modify the position of the pet if necessary</li><BR>
	 * <BR>
	 *
	 * @param x
	 * @param y
	 * @param z
	 * @param allowRandomOffset
	 */
	public void teleToLocation(int x, int y, int z, boolean allowRandomOffset)
	{
		// Stop movement
		stopMove(null, false);
		abortAttack();
		abortCast();

		setIsTeleporting(true);
		setTarget(null);

		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);

		if (PlayersConfig.RESPAWN_RANDOM_ENABLED && allowRandomOffset)
		{
			x += Rnd.get(-PlayersConfig.RESPAWN_RANDOM_MAX_OFFSET, PlayersConfig.RESPAWN_RANDOM_MAX_OFFSET);
			y += Rnd.get(-PlayersConfig.RESPAWN_RANDOM_MAX_OFFSET, PlayersConfig.RESPAWN_RANDOM_MAX_OFFSET);
		}

		z += 5;

		_log.debug("Teleporting to: " + x + ", " + y + ", " + z);

		// Send a Server->Client packet TeleportToLocationt to the L2Character AND to all L2PcInstance in the _KnownPlayers of the
		// L2Character
		broadcastPacket(new TeleportToLocation(this, x, y, z));

		// remove the object from its old location
		decayMe();

		// Set the x,y,z position of the L2Object and if necessary modify its _worldRegion
		getPosition().setXYZ(x, y, z);

		if (!(this instanceof L2PcInstance) || (((L2PcInstance) this).getClient() != null && ((L2PcInstance) this).getClient().isDetached()))
			onTeleported();

		revalidateZone(true);
	}

	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, false);
	}

	public void teleToLocation(Location loc, boolean allowRandomOffset)
	{
		int x = loc.getX();
		int y = loc.getY();
		int z = loc.getZ();

		if (this instanceof L2PcInstance && DimensionalRiftManager.getInstance().checkIfInRiftZone(getX(), getY(), getZ(), true)) // true
																																	// ->
																																	// ignore
																																	// waiting
																																	// room
																																	// :)
		{
			L2PcInstance player = (L2PcInstance) this;
			player.sendMessage("You have been sent to the waiting room.");
			if (player.isInParty() && player.getParty().isInDimensionalRift())
			{
				player.getParty().getDimensionalRift().usedTeleport(player);
			}
			int[] newCoords = DimensionalRiftManager.getInstance().getRoom((byte) 0, (byte) 0).getTeleportCoords();
			x = newCoords[0];
			y = newCoords[1];
			z = newCoords[2];
		}
		teleToLocation(x, y, z, allowRandomOffset);
	}

	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionData.getInstance().getTeleToLocation(this, teleportWhere), true);
	}

	// =========================================================
	// Method - Private
	/**
	 * Launch a physical attack against a target (Simple, Bow, Pole or Dual).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get the active weapon (always equipped in the right hand)</li><BR>
	 * <BR>
	 * <li>If weapon is a bow, check for arrows, MP and bow re-use delay (if necessary, equip the L2PcInstance with arrows in left
	 * hand)</li> <li>If weapon is a bow, consume MP and set the new period of bow non re-use</li><BR>
	 * <BR>
	 * <li>Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)</li> <li>Select the type of attack
	 * to start (Simple, Bow, Pole or Dual) and verify if SoulShot are charged then start calculation</li> <li>If the
	 * Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack to the L2Character AND to all
	 * L2PcInstance in the _KnownPlayers of the L2Character</li> <li>Notify AI with EVT_READY_TO_ACT</li><BR>
	 * <BR>
	 *
	 * @param target
	 *            The L2Character targeted
	 */
	protected void doAttack(L2Character target)
	{
		_log.debug(getName() + " doAttack: target=" + target);

		if (!isAlikeDead() && target != null)
		{
			if (this instanceof L2Npc && target.isAlikeDead() || !getKnownList().knowsObject(target))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			else if (this instanceof L2PcInstance)
			{
				if (target.isDead())
				{
					getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}

				L2PcInstance actor = (L2PcInstance) this;
				// Players riding wyvern can only do melee attacks
				if (actor.isMounted() && actor.getMountNpcId() == 12621)
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
		}

		if (isAttackingDisabled())
			return;

		if (this instanceof L2PcInstance)
		{
			if (((L2PcInstance) this).inObserverMode())
			{
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			// Checking if target has moved to peace zone
			if (target.isInsidePeaceZone((L2PcInstance) this))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		stopEffectsOnAction();

		// Get the active weapon instance (always equipped in the right hand)
		L2ItemInstance weaponInst = getActiveWeaponInstance();

		// Get the active weapon item corresponding to the active weapon instance (always equipped in the right hand)
		L2Weapon weaponItem = getActiveWeaponItem();

		if (weaponItem != null && weaponItem.getItemType() == L2WeaponType.FISHINGROD)
		{
			// You can't make an attack with a fishing pole.
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE));
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// GeoData Los Check here (or dz > 1000)
		if (!GeoData.getInstance().canSeeTarget(this, target))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
			getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Check for a bow
		if ((weaponItem != null && weaponItem.getItemType() == L2WeaponType.BOW))
		{
			// Check for arrows and MP
			if (this instanceof L2PcInstance)
			{
				// Equip arrows needed in left hand and send a Server->Client packet ItemList to the L2PcINstance then return True
				if (!checkAndEquipArrows())
				{
					// Cancel the action because the L2PcInstance have no arrow
					getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

					sendPacket(ActionFailed.STATIC_PACKET);
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ARROWS));
					return;
				}

				// Verify if the bow can be use
				if (_disableBowAttackEndTime <= GameTimeController.getGameTicks())
				{
					// Verify if L2PcInstance owns enough MP
					int saMpConsume = (int) getStat().calcStat(Stats.MP_CONSUME, 0, null, null);
					int mpConsume = saMpConsume == 0 ? weaponItem.getMpConsume() : saMpConsume;
					mpConsume = (int) calcStat(Stats.BOW_MP_CONSUME_RATE, mpConsume, null, null);

					if (getCurrentMp() < mpConsume)
					{
						// If L2PcInstance doesn't have enough MP, stop the attack
						ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), 1000);

						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					// If L2PcInstance have enough MP, the bow consummes it
					if (mpConsume > 0)
						getStatus().reduceMp(mpConsume);

					// Set the period of bow non re-use
					_disableBowAttackEndTime = 5 * GameTimeController.TICKS_PER_SECOND + GameTimeController.getGameTicks();
				}
				else
				{
					// Cancel the action because the bow can't be re-use at this moment
					ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), 1000);

					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (this instanceof L2Npc)
			{
				if (_disableBowAttackEndTime > GameTimeController.getGameTicks())
					return;
			}
		}

		// Add the L2PcInstance to _knownObjects and _knownPlayer of the target
		target.getKnownList().addKnownObject(this);

		// Verify if soulshots are charged.
		boolean wasSSCharged;

		if (this instanceof L2Summon && !(this instanceof L2PetInstance && weaponInst != null))
			wasSSCharged = (((L2Summon) this).getChargedSoulShot() != L2ItemInstance.CHARGED_NONE);
		else
			wasSSCharged = (weaponInst != null && weaponInst.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE);

		if (this instanceof L2Attackable)
		{
			if (((L2Npc) this).useSoulShot())
				wasSSCharged = true;
		}

		// Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
		int timeAtk = calculateTimeBetweenAttacks(target, weaponItem);
		// the hit is calculated to happen halfway to the animation - might need further tuning e.g. in bow case
		int timeToHit = timeAtk / 2;
		_attackEndTime = GameTimeController.getGameTicks();
		_attackEndTime += (timeAtk / GameTimeController.MILLIS_IN_TICK);
		_attackEndTime -= 1;

		int ssGrade = 0;

		if (weaponItem != null)
			ssGrade = weaponItem.getCrystalType();

		// Create a Server->Client packet Attack
		Attack attack = new Attack(this, wasSSCharged, ssGrade);

		// Set the Attacking Body part to CHEST
		setAttackingBodypart();

		// Make sure that char is facing selected target
		setHeading(Util.calculateHeadingFrom(this, target));

		// Get the Attack Reuse Delay of the L2Weapon
		int reuse = calculateReuseTime(target, weaponItem);
		boolean hitted;

		// Select the type of attack to start
		if (weaponItem == null)
			hitted = doAttackHitSimple(attack, target, timeToHit);
		else if (weaponItem.getItemType() == L2WeaponType.BOW)
			hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
		else if (weaponItem.getItemType() == L2WeaponType.POLE)
			hitted = doAttackHitByPole(attack, target, timeToHit);
		else if (isUsingDualWeapon())
			hitted = doAttackHitByDual(attack, target, timeToHit);
		else
			hitted = doAttackHitSimple(attack, target, timeToHit);

		// Flag the attacker if it's a L2PcInstance outside a PvP area
		L2PcInstance player = getActingPlayer();

		if (player != null)
		{
			AttackStanceTaskManager.getInstance().addAttackStanceTask(player);

			if (player.getPet() != target)
				player.updatePvPStatus(target);
		}

		// Check if hit isn't missed
		if (!hitted)
			// Abort the attack of the L2Character and send Server->Client ActionFailed packet
			abortAttack();
		else
		{
			// IA implementation for ON_ATTACK_ACT (mob which attacks a player).
			if (this instanceof L2Attackable)
			{
				try
				{
					// Bypass behavior if the victim isn't a player
					L2PcInstance victim = target.getActingPlayer();
					if (victim != null)
					{
						L2Npc mob = ((L2Npc) this);
						if (mob.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK_ACT) != null)
							for (Quest quest : mob.getTemplate().getEventQuests(Quest.QuestEventType.ON_ATTACK_ACT))
								quest.notifyAttackAct(mob, victim);
					}
				}
				catch (Exception e)
				{
					_log.warn(e.getLocalizedMessage(), e);
				}
			}

			// If we didn't miss the hit, discharge the shoulshots, if any
			if (this instanceof L2Summon && !(this instanceof L2PetInstance && weaponInst != null))
				((L2Summon) this).setChargedSoulShot(L2ItemInstance.CHARGED_NONE);
			else if (weaponInst != null)
				weaponInst.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);

			if (player != null)
			{
				if (player.isCursedWeaponEquipped())
				{
					// If hitted by a cursed weapon, Cp is reduced to 0
					if (!target.isInvul())
						target.setCurrentCp(0);
				}
				else if (player.isHero())
				{
					if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
						// If a cursed weapon is hitted by a Hero, Cp is reduced to 0
						target.setCurrentCp(0);
				}
			}
		}

		// If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
		// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		if (attack.hasHits())
			broadcastPacket(attack);

		// Notify AI with EVT_READY_TO_ACT
		ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_READY_TO_ACT), timeAtk + reuse);
	}

	private class AutoSS implements Runnable
	{
		private L2Character _character;
		private L2Skill _skill = null;

		public AutoSS(L2Character Character, L2Skill Skill)
		{
			_character = Character;
			_skill = Skill;
		}

		@Override
		public void run()
		{
			// Recharge AutoSoulShot
			if (_skill != null)
			{
				if (_skill.useSoulShot())
				{
					// if (_character instanceof L2Npc)
					// ((L2Npc) _character).rechargeAutoSoulShot(true, false);
					/* else */if (_character instanceof L2PcInstance)
						((L2PcInstance) _character).rechargeAutoSoulShot(true, false, false);
					else if (_character instanceof L2Summon)
						((L2Summon) _character).getOwner().rechargeAutoSoulShot(true, false, true);
				}
				else if (_skill.useSpiritShot())
				{
					// if (_character instanceof L2Npc)
					// ((L2Npc) _character).rechargeAutoSoulShot(true, true);
					/* else */if (_character instanceof L2PcInstance)
						((L2PcInstance) _character).rechargeAutoSoulShot(true, true, false);
					else if (_character instanceof L2Summon)
						((L2Summon) _character).getOwner().rechargeAutoSoulShot(false, true, true);
				}
			}
			else
			{
				if (_character instanceof L2PcInstance)
					((L2PcInstance) _character).rechargeAutoSoulShot(true, false, false);
				else if (_character instanceof L2Summon)
					((L2Summon) _character).getOwner().rechargeAutoSoulShot(true, false, true);
			}
		}
	}

	/**
	 * Launch a Bow attack.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate if hit is missed or not</li> <li>Consumme arrows</li> <li>If hit isn't missed, calculate if shield defense is
	 * efficient</li> <li>If hit isn't missed, calculate if hit is critical</li> <li>If hit isn't missed, calculate physical
	 * damages</li> <li>If the L2Character is a L2PcInstance, Send a Server->Client packet SetupGauge</li> <li>Create a new hit
	 * task with Medium priority</li> <li>Calculate and set the disable delay of the bow in function of the Attack Speed</li> <li>
	 * Add this hit to the Server-Client packet Attack</li><BR>
	 * <BR>
	 *
	 * @param attack
	 *            Server->Client packet Attack in which the hit will be added
	 * @param target
	 *            The L2Character targeted
	 * @param sAtk
	 *            The Attack Speed of the attacker
	 * @param reuse
	 *            The reuse timer of the item.
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitByBow(Attack attack, L2Character target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;

		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);

		// Consume arrows
		reduceArrowCount();

		_move = null;

		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);

			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);
		}

		// Check if the L2Character is a L2PcInstance
		if (this instanceof L2PcInstance)
		{
			// Send a system message
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.GETTING_READY_TO_SHOOT_AN_ARROW));

			// Send a Server->Client packet SetupGauge
			SetupGauge sg = new SetupGauge(SetupGauge.RED, sAtk + reuse);
			sendPacket(sg);
		}

		// Create a new hit task with Medium priority
		ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk);
		ThreadPoolManager.getInstance().scheduleGeneral(new AutoSS(this, null), sAtk);

		// Calculate and set the disable delay of the bow in function of the Attack Speed
		_disableBowAttackEndTime = (sAtk + reuse) / GameTimeController.MILLIS_IN_TICK + GameTimeController.getGameTicks();

		// Add this hit to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));

		// Return true if hit isn't missed
		return !miss1;
	}

	/**
	 * Launch a Dual attack.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate if hits are missed or not</li> <li>If hits aren't missed, calculate if shield defense is efficient</li> <li>
	 * If hits aren't missed, calculate if hit is critical</li> <li>If hits aren't missed, calculate physical damages</li> <li>
	 * Create 2 new hit tasks with Medium priority</li> <li>Add those hits to the Server-Client packet Attack</li><BR>
	 * <BR>
	 *
	 * @param attack
	 *            Server->Client packet Attack in which the hit will be added
	 * @param target
	 *            The L2Character targeted
	 * @param sAtk
	 *            The Attack Speed of the attacker
	 * @return True if hit 1 or hit 2 isn't missed
	 */
	private boolean doAttackHitByDual(Attack attack, L2Character target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;

		// Calculate if hits are missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);
		boolean miss2 = Formulas.calcHitMiss(this, target);

		// Check if hit 1 isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient against hit 1
			shld1 = Formulas.calcShldUse(this, target);

			// Calculate if hit 1 is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

			// Calculate physical damages of hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, true, attack.soulshot);
			damage1 /= 2;
		}

		// Check if hit 2 isn't missed
		if (!miss2)
		{
			// Calculate if shield defense is efficient against hit 2
			shld2 = Formulas.calcShldUse(this, target);

			// Calculate if hit 2 is critical
			crit2 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

			// Calculate physical damages of hit 2
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, true, attack.soulshot);
			damage2 /= 2;
		}

		if (this instanceof L2Attackable)
		{
			if (((L2Attackable) this)._soulshotcharged)
			{
				// Create a new hit task with Medium priority for hit 1
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, true, shld1), sAtk / 2);

				// Create a new hit task with Medium priority for hit 2 with a higher delay
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, true, shld2), sAtk);
			}
			else
			{
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, false, shld1), sAtk / 2);

				// Create a new hit task with Medium priority for hit 2 with a higher delay
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, false, shld2), sAtk);
			}
		}
		else
		{
			// Create a new hit task with Medium priority for hit 1
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk / 2);

			// Create a new hit task with Medium priority for hit 2 with a higher delay
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2), sAtk);

			ThreadPoolManager.getInstance().scheduleGeneral(new AutoSS(this, null), sAtk);
		}

		// Add those hits to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1), attack.createHit(target, damage2, miss2, crit2, shld2));

		// Return true if hit 1 or hit 2 isn't missed
		return (!miss1 || !miss2);
	}

	/**
	 * Launch a Pole attack.<BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <li>Get all visible objects in a spherical area near the L2Character to obtain possible targets</li> <li>If possible target
	 * is the L2Character targeted, launch a simple attack against it</li> <li>If possible target isn't the L2Character targeted
	 * but is attackable, launch a simple attack against it</li><BR>
	 *
	 * @param attack
	 *            Server->Client packet Attack in which the hit will be added
	 * @param target
	 *            The L2Character targeted
	 * @param sAtk
	 *            The Attack Speed of the attacker
	 * @return True if one hit isn't missed
	 */
	private boolean doAttackHitByPole(Attack attack, L2Character target, int sAtk)
	{
		int maxRadius = getPhysicalAttackRange();
		int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);

		_log.debug("doAttackHitByPole: Max radius = " + maxRadius + " Max angle = " + maxAngleDiff);

		// Get the number of targets (-1 because the main target is already used)
		int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 0, null, null) - 1;
		int attackcount = 0;

		boolean hitted = doAttackHitSimple(attack, target, 100, sAtk);
		double attackpercent = 85;
		L2Character temp;

		Collection<L2Object> objs = getKnownList().getKnownObjects().values();
		for (L2Object obj : objs)
		{
			if (obj == target)
				continue;

			// Check if the L2Object is a L2Character
			if (obj instanceof L2Character)
			{
				if (obj instanceof L2PetInstance && this instanceof L2PcInstance && ((L2PetInstance) obj).getOwner() == ((L2PcInstance) this))
					continue;

				if (!Util.checkIfInRange(maxRadius, this, obj, false))
					continue;

				// otherwise hit too high/low. 650 because mob z coord sometimes wrong on hills
				if (Math.abs(obj.getZ() - getZ()) > 650)
					continue;

				if (!isFacing(obj, maxAngleDiff))
					continue;

				if (this instanceof L2Attackable && obj instanceof L2PcInstance && getTarget() instanceof L2Attackable)
					continue;

				if (this instanceof L2Attackable && obj instanceof L2Attackable && ((L2Attackable) this).getEnemyClan() == null && ((L2Attackable) this).getIsChaos() == 0)
					continue;

				if (this instanceof L2Attackable && obj instanceof L2Attackable && !((L2Attackable) this).getEnemyClan().equals(((L2Attackable) obj).getClan()) && ((L2Attackable) this).getIsChaos() == 0)
					continue;

				temp = (L2Character) obj;

				// Launch an attack on each character, until attackRandomCountMax is reached.
				if (!temp.isAlikeDead())
				{
					if (temp == getAI().getAttackTarget() || temp.isAutoAttackable(this))
					{
						attackcount++;
						if (attackcount > attackRandomCountMax)
							break;

						hitted |= doAttackHitSimple(attack, temp, attackpercent, sAtk);
						attackpercent /= 1.15;
					}
				}
			}
		}
		// Return true if one hit isn't missed
		return hitted;
	}

	/**
	 * Launch a simple attack.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Calculate if hit is missed or not</li> <li>If hit isn't missed, calculate if shield defense is efficient</li> <li>If
	 * hit isn't missed, calculate if hit is critical</li> <li>If hit isn't missed, calculate physical damages</li> <li>Create a
	 * new hit task with Medium priority</li> <li>Add this hit to the Server-Client packet Attack</li><BR>
	 * <BR>
	 *
	 * @param attack
	 *            Server->Client packet Attack in which the hit will be added
	 * @param target
	 *            The L2Character targeted
	 * @param sAtk
	 *            The Attack Speed of the attacker
	 * @return True if the hit isn't missed
	 */
	private boolean doAttackHitSimple(Attack attack, L2Character target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk);
	}

	private boolean doAttackHitSimple(Attack attack, L2Character target, double attackpercent, int sAtk)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;

		// Calculate if hit is missed or not
		boolean miss1 = Formulas.calcHitMiss(this, target);

		// Check if hit isn't missed
		if (!miss1)
		{
			// Calculate if shield defense is efficient
			shld1 = Formulas.calcShldUse(this, target);

			// Calculate if hit is critical
			crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

			// Calculate physical damages
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, false, attack.soulshot);

			if (attackpercent != 100)
				damage1 = (int) (damage1 * attackpercent / 100);
		}

		// Create a new hit task with Medium priority
		if (this instanceof L2Attackable)
		{
			if (((L2Attackable) this)._soulshotcharged)
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, true, shld1), sAtk);
			else
				ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, false, shld1), sAtk);
		}
		else
		{
			ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk);
			ThreadPoolManager.getInstance().scheduleGeneral(new AutoSS(this, null), sAtk);
		}

		// Add this hit to the Server-Client packet Attack
		attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));

		// Return true if hit isn't missed
		return !miss1;
	}

	/**
	 * Manage the casting task (casting and interrupt time, re-use delay...) and display the casting bar and animation on client.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Verify the possibilty of the the cast : skill is a spell, caster isn't muted...</li> <li>Get the list of all targets
	 * (ex : area effects) and define the L2Charcater targeted (its stats will be used in calculation)</li> <li>Calculate the
	 * casting time (base + modifier of MAtkSpd), interrupt time and re-use delay</li> <li>Send a Server->Client packet
	 * MagicSkillUse (to diplay casting animation), a packet SetupGauge (to display casting bar) and a system message</li> <li>
	 * Disable all skills during the casting time (create a task EnableAllSkills)</li> <li>Disable the skill during the re-use
	 * delay (create a task EnableSkill)</li> <li>Create a task MagicUseTask (that will call method onMagicUseTimer) to launch the
	 * Magic Skill at the end of the casting time</li><BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to use
	 */
	public void doCast(L2Skill skill)
	{
		beginCast(skill, false);
	}

	public void doSimultaneousCast(L2Skill skill)
	{
		beginCast(skill, true);
	}

	private void beginCast(L2Skill skill, boolean simultaneously)
	{
		if (!checkDoCastConditions(skill))
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);

			if (this instanceof L2PcInstance)
				getAI().setIntention(AI_INTENTION_ACTIVE);

			return;
		}
		// Override casting type
		if (skill.isSimultaneousCast() && !simultaneously)
			simultaneously = true;

		stopEffectsOnAction();

		// Set the target of the skill in function of Skill Type and Target Type
		L2Character target = null;
		// Get all possible targets of the skill in a table in function of the skill target type
		L2Object[] targets = skill.getTargetList(this);

		boolean doit = false;

		// AURA skills should always be using caster as target
		switch (skill.getTargetType())
		{
			case TARGET_AREA_SUMMON: // We need it to correct facing
				target = getPet();
				break;
			case TARGET_AURA:
			case TARGET_FRONT_AURA:
			case TARGET_BEHIND_AURA:
			case TARGET_GROUND:
				target = this;
				break;
			case TARGET_SELF:
			case TARGET_PET:
			case TARGET_SUMMON:
			case TARGET_OWNER_PET:
			case TARGET_PARTY:
			case TARGET_CLAN:
			case TARGET_ALLY:
				doit = true;
			default:
				if (targets.length == 0)
				{
					if (simultaneously)
						setIsCastingSimultaneouslyNow(false);
					else
						setIsCastingNow(false);
					// Send a Server->Client packet ActionFailed to the L2PcInstance
					if (this instanceof L2PcInstance)
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						getAI().setIntention(AI_INTENTION_ACTIVE);
					}
					return;
				}

				switch (skill.getSkillType())
				{
					case BUFF:
					case HEAL:
					case COMBATPOINTHEAL:
					case MANAHEAL:
					case SEED:
					case REFLECT:
						doit = true;
						break;
				}

				target = (doit) ? (L2Character) targets[0] : (L2Character) getTarget();
		}
		beginCast(skill, simultaneously, target, targets);
	}

	private void beginCast(L2Skill skill, boolean simultaneously, L2Character target, L2Object[] targets)
	{
		if (target == null)
		{
			if (simultaneously)
				setIsCastingSimultaneouslyNow(false);
			else
				setIsCastingNow(false);

			if (this instanceof L2PcInstance)
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(AI_INTENTION_ACTIVE);
			}
			return;
		}

		// Get the casting time of the skill (base)
		int hitTime = skill.getHitTime();
		int coolTime = skill.getCoolTime();

		boolean effectWhileCasting = skill.getSkillType() == L2SkillType.FUSION || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME;

		// Calculate the casting time of the skill (base + modifier of MAtkSpd)
		// Don't modify the skill time for FUSION skills. The skill time for those skills represent the buff time.
		if (!effectWhileCasting)
		{
			hitTime = Formulas.calcAtkSpd(this, skill, hitTime);
			if (coolTime > 0)
				coolTime = Formulas.calcAtkSpd(this, skill, coolTime);
		}

		int shotSave = L2ItemInstance.CHARGED_NONE;

		// Calculate altered Cast Speed due to BSpS/SpS
		L2ItemInstance weaponInst = getActiveWeaponInstance();
		if (weaponInst != null)
		{
			if (skill.isMagic() && !effectWhileCasting && skill.getTargetType() != SkillTargetType.TARGET_SELF)
			{
				if ((weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT) || (weaponInst.getChargedSpiritshot() == L2ItemInstance.CHARGED_SPIRITSHOT))
				{
					// Only takes 70% of the time to cast a BSpS/SpS cast
					hitTime = (int) (0.70 * hitTime);
					coolTime = (int) (0.70 * coolTime);

					// Because the following are magic skills that do not actively 'eat' BSpS/SpS,
					// I must 'eat' them here so players don't take advantage of infinite speed increase
					switch (skill.getSkillType())
					{
						case BUFF:
						case MANAHEAL:
						case MANARECHARGE:
						case RESURRECT:
						case RECALL:
							weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
							break;
					}
				}
			}

			// Save shots value for repeats
			if (skill.useSoulShot())
				shotSave = weaponInst.getChargedSoulshot();
			else if (skill.useSpiritShot())
				shotSave = weaponInst.getChargedSpiritshot();
			ThreadPoolManager.getInstance().scheduleGeneral(new AutoSS(this, skill), hitTime + 15); // TODO: Проверить задержку
																									// заряда. (с) Demon
		}

		if (this instanceof L2Npc)
		{
			if (((L2Npc) this).useSpiritShot())
			{
				hitTime = (int) (0.70 * hitTime);
				coolTime = (int) (0.70 * coolTime);
			}
		}

		// Don't modify skills HitTime if staticHitTime is specified for skill in datapack.
		if (skill.isStaticHitTime())
		{
			hitTime = skill.getHitTime();
			coolTime = skill.getCoolTime();
		}
		// if basic hitTime is higher than 500 than the min hitTime is 500
		else if (skill.getHitTime() >= 500 && hitTime < 500)
			hitTime = 500;

		// Set the _castInterruptTime and casting status (L2PcInstance already has this true)
		if (simultaneously)
		{
			// queue herbs and potions
			if (isCastingSimultaneouslyNow())
			{
				ThreadPoolManager.getInstance().scheduleAi(new UsePotionTask(this, skill), 100);
				return;
			}
			setIsCastingSimultaneouslyNow(true);
			setLastSimultaneousSkillCast(skill);
		}
		else
		{
			setIsCastingNow(true);
			_castInterruptTime = -2 + GameTimeController.getGameTicks() + hitTime / GameTimeController.MILLIS_IN_TICK;
			setLastSkillCast(skill);
		}

		// Init the reuse time of the skill
		int reuseDelay;

		if (skill.isStaticReuse())
			reuseDelay = (skill.getReuseDelay());
		else
		{
			if (skill.isMagic())
				reuseDelay = (int) (skill.getReuseDelay() * getStat().getMReuseRate(skill));
			else
				reuseDelay = (int) (skill.getReuseDelay() * getStat().getPReuseRate(skill));

			// reuse is influenced by atkspd / matkspd
			reuseDelay *= 333.0 / (skill.isMagic() ? getMAtkSpd() : getPAtkSpd());
		}

		boolean skillMastery = Formulas.calcSkillMastery(this, skill);

		// Skill reuse check
		if (reuseDelay > 30000 && !skillMastery)
			addTimeStamp(skill, reuseDelay);

		// Check if this skill consume mp on start casting
		int initmpcons = getStat().getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			getStatus().reduceMp(initmpcons);
			StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
			sendPacket(su);
		}

		// Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of
		// the re-use delay
		if (reuseDelay > 10)
		{
			if (skillMastery)
			{
				reuseDelay = 100;

				if (getActingPlayer() != null)
					getActingPlayer().sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN);
			}

			disableSkill(skill, reuseDelay);
		}

		// Make sure that char is facing selected target
		if (target != this)
			setHeading(Util.calculateHeadingFrom(this, target));

		// For force buff skills, start the effect as long as the player is casting.
		if (effectWhileCasting)
		{
			// Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the
			// L2Character
			if (skill.getItemConsumeId() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, true))
				{
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
					if (simultaneously)
						setIsCastingSimultaneouslyNow(false);
					else
						setIsCastingNow(false);

					if (this instanceof L2PcInstance)
						getAI().setIntention(AI_INTENTION_ACTIVE);
					return;
				}
			}

			if (skill.getSkillType() == L2SkillType.FUSION)
				startFusionSkill(target, skill);
			else
				callSkill(skill, targets);
		}

		// Get the Display Identifier for a skill that client can't display
		int displayId = skill.getDisplayId();

		// Get the level of the skill
		int level = skill.getLevel();
		if (level < 1)
			level = 1;

		// Send a Server->Client packet MagicSkillUse with target, displayId, level, skillTime, reuseDelay
		// to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
		if (!skill.isPotion())
		{
			broadcastPacket(new MagicSkillUse(this, target, displayId, level, hitTime, reuseDelay, false));
			broadcastPacket(new MagicSkillLaunched(this, displayId, level, (targets == null || targets.length == 0) ? new L2Object[] { target } : targets));
		}
		else
			broadcastPacket(new MagicSkillUse(this, target, displayId, level, 0, 0));

		if (this instanceof L2Playable)
		{
			// Send a system message USE_S1 to the L2Character
			if (this instanceof L2PcInstance && skill.getId() != 1312)
			{
				SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.USE_S1);
				sm.addSkillName(skill);
				sendPacket(sm);
			}

			if (!effectWhileCasting && skill.getItemConsumeId() > 0)
			{
				if (!destroyItemByItemId("Consume", skill.getItemConsumeId(), skill.getItemConsume(), null, true))
				{
					getActingPlayer().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					abortCast();
					return;
				}
			}

			// Before start AI Cast Broadcast Fly Effect is Need
			if (this instanceof L2PcInstance && skill.getFlyType() != null)
				ThreadPoolManager.getInstance().scheduleEffect(new FlyToLocationTask(this, target, skill), 50);
		}

		MagicUseTask mut = new MagicUseTask(targets, skill, hitTime, coolTime, simultaneously, shotSave);

		// launch the magic in hitTime milliseconds
		if (hitTime > 410)
		{
			// Send a Server->Client packet SetupGauge with the color of the gauge and the casting time
			if (this instanceof L2PcInstance && !effectWhileCasting)
				sendPacket(new SetupGauge(SetupGauge.BLUE, hitTime));

			if (effectWhileCasting)
				mut.phase = 2;

			if (simultaneously)
			{
				Future<?> future = _skillCast2;
				if (future != null)
				{
					future.cancel(true);
					_skillCast2 = null;
				}

				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
				// For client animation reasons (party buffs especially) 400 ms before!
				_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 400);
			}
			else
			{
				Future<?> future = _skillCast;
				if (future != null)
				{
					future.cancel(true);
					_skillCast = null;
				}

				// Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
				// For client animation reasons (party buffs especially) 400 ms before!
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 400);
			}
		}
		else
		{
			mut.hitTime = 0;
			onMagicLaunchedTimer(mut);
		}
	}

	/**
	 * Check if casting of skill is possible
	 *
	 * @param skill
	 * @return True if casting is possible
	 */
	protected boolean checkDoCastConditions(L2Skill skill)
	{
		if (skill == null || isSkillDisabled(skill))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Check if the caster has enough MP
		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
		{
			// Send a System Message to the caster
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));

			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Check if the caster has enough HP
		if (getCurrentHp() <= skill.getHpConsume())
		{
			// Send a System Message to the caster
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));

			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Verify the different types of silence (magic and physic)
		if (!skill.isPotion() && ((skill.isMagic() && isMuted()) || isPhysicalMuted()))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// prevent casting signets to peace zone
		if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
		{
			L2WorldRegion region = getWorldRegion();
			if (region == null)
				return false;

			if (skill.getTargetType() == SkillTargetType.TARGET_GROUND && this instanceof L2PcInstance)
			{
				Point3D wp = ((L2PcInstance) this).getCurrentSkillWorldPosition();
				if (!region.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
					return false;
				}
			}
			else if (!region.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
			{
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
				return false;
			}
		}

		// Check if the caster owns the weapon needed
		if (!skill.getWeaponDependancy(this))
		{
			// Send a Server->Client packet ActionFailed to the L2PcInstance
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}

		// Check if the spell consumes an Item
		if (skill.getItemConsumeId() > 0 && getInventory() != null)
		{
			// Get the L2ItemInstance consumed by the spell
			L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());

			// Check if the caster owns enough consumed Item to cast
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				// Checked: when a summon skill failed, server show required consume item count
				if (skill.getSkillType() == L2SkillType.SUMMON)
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addNumber(skill.getItemConsume());
					sendPacket(sm);
					return false;
				}

				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NUMBER_INCORRECT));
				return false;
			}
		}

		return true;
	}

	/**
	 * Index according to skill id the current timestamp of use, overridden in L2PcInstance.
	 *
	 * @param skill
	 *            id
	 * @param reuse
	 *            delay
	 */
	public void addTimeStamp(L2Skill skill, long reuse)
	{
	}

	public void startFusionSkill(L2Character target, L2Skill skill)
	{
		if (skill.getSkillType() != L2SkillType.FUSION)
			return;

		if (_fusionSkill == null)
			_fusionSkill = new FusionSkill(this, target, skill);
	}

	/**
	 * Kill the L2Character.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set target to null and cancel Attack or Cast</li> <li>Stop movement</li> <li>Stop HP/MP/CP Regeneration task</li> <li>
	 * Stop all active skills effects in progress on the L2Character</li> <li>Send the Server->Client packet StatusUpdate with
	 * current HP and MP to all other L2PcInstance to inform</li> <li>Notify L2Character AI</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2Npc : Create a DecayTask to remove the corpse of the L2Npc after 7 seconds</li> <li>L2Attackable : Distribute rewards
	 * (EXP, SP, Drops...) and notify Quest Engine</li> <li>L2PcInstance : Apply Death Penalty, Manage gain/loss Karma and Item
	 * Drop</li><BR>
	 * <BR>
	 *
	 * @param killer
	 *            The L2Character who killed it
	 * @return true if successful.
	 */
	public boolean doDie(L2Character killer)
	{
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
				return false;

			// now reset currentHp to zero
			setCurrentHp(0);

			setIsDead(true);
		}

		// Set target to null and cancel Attack or Cast
		setTarget(null);

		// Stop movement
		stopMove(null);

		// Stop Regeneration task, and removes all current effects
		getStatus().stopHpMpRegeneration();
		stopAllEffectsExceptThoseThatLastThroughDeath();

		calculateRewards(killer);

		// Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
		broadcastStatusUpdate();

		// Notify L2Character AI
		if (hasAI())
			getAI().notifyEvent(CtrlEvent.EVT_DEAD, null);

		if (getWorldRegion() != null)
			getWorldRegion().onDeath(this);

		getAttackByList().clear();

		// If character is PhoenixBlessed a resurrection popup will show up
		if (this instanceof L2Summon)
		{
			if (((L2Summon) this).isPhoenixBlessed() && ((L2Summon) this).getOwner() != null)
				((L2Summon) this).getOwner().reviveRequest(((L2Summon) this).getOwner(), null, true);
		}
		else if (this instanceof L2PcInstance)
		{
			if (((L2Playable) this).isPhoenixBlessed())
				((L2PcInstance) this).reviveRequest(((L2PcInstance) this), null, false);
			else if (isAffected(CharEffectList.EFFECT_FLAG_CHARM_OF_COURAGE) && ((L2PcInstance) this).isInSiege())
				((L2PcInstance) this).reviveRequest(((L2PcInstance) this), null, false);
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

		return true;
	}

	public void deleteMe()
	{
		if (hasAI())
			getAI().stopAITask();
	}

	protected void calculateRewards(L2Character killer)
	{
	}

	/** Sets HP, MP and CP and revives the L2Character. */
	public void doRevive()
	{
		if (!isDead())
			return;

		if (!isTeleporting())
		{
			setIsPendingRevive(false);
			setIsDead(false);
			boolean restorefull = false;

			if (this instanceof L2Playable && ((L2Playable) this).isPhoenixBlessed())
			{
				restorefull = true;
				((L2Playable) this).stopPhoenixBlessing(null);
			}

			if (restorefull)
			{
				_status.setCurrentHp(getMaxHp());
				_status.setCurrentMp(getMaxMp());
			}
			else
				_status.setCurrentHp(getMaxHp() * PlayersConfig.RESPAWN_RESTORE_HP);

			// Start broadcast status
			broadcastPacket(new Revive(this));

			// Start paralyze task if it's a player
			if (this instanceof L2PcInstance)
			{
				final L2PcInstance player = ((L2PcInstance) this);

				// Schedule a paralyzed task to wait for the animation to finish
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
				{
					@Override
					public void run()
					{
						player.setIsParalyzed(false);
					}
				}, player.getAnimationTimer());
				setIsParalyzed(true);
			}

			if (getWorldRegion() != null)
				getWorldRegion().onRevive(this);
		}
		else
			setIsPendingRevive(true);
	}

	/**
	 * Revives the L2Character using skill.
	 *
	 * @param revivePower
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}

	/**
	 * @return the CharacterAI of the L2Character and if its null create a new one.
	 */
	public CharacterAI getAI()
	{
		CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new CharacterAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}

	public void setAI(CharacterAI newAI)
	{
		CharacterAI oldAI = getAI();
		if (oldAI != null && oldAI != newAI && oldAI instanceof AttackableAI)
			((AttackableAI) oldAI).stopAITask();

		_ai = newAI;
	}

	/**
	 * @return True if the L2Character has a CharacterAI.
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}

	/**
	 * @return True if the L2Character is RaidBoss or his minion.
	 */
	public boolean isRaid()
	{
		return _isRaid;
	}

	/**
	 * Set this Npc as a Raid instance.
	 *
	 * @param isRaid
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}

	/**
	 * @return True if the L2Character is minion.
	 */
	public boolean isMinion()
	{
		return false;
	}

	/**
	 * @return True if the L2Character is Raid minion.
	 */
	public boolean isRaidMinion()
	{
		return false;
	}

	/**
	 * @return a list of L2Character that attacked.
	 */
	public final Set<L2Character> getAttackByList()
	{
		if (_attackByList != null)
			return _attackByList;

		synchronized (this)
		{
			if (_attackByList == null)
				_attackByList = Sets.newSetFromMap(new WeakHashMap<L2Character, Boolean>());
		}
		return _attackByList;
	}

	public final L2Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	}

	public void setLastSimultaneousSkillCast(L2Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	}

	public final L2Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}

	public void setLastSkillCast(L2Skill skill)
	{
		_lastSkillCast = skill;
	}

	public final boolean isNoRndWalk()
	{
		return _isNoRndWalk;
	}

	public final void setIsNoRndWalk(boolean value)
	{
		_isNoRndWalk = value;
	}

	public final boolean isAfraid()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_FEAR);
	}

	public final boolean isConfused()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_CONFUSED);
	}

	public final boolean isMuted()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_MUTED);
	}

	public final boolean isPhysicalMuted()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_PHYSICAL_MUTED);
	}

	public final boolean isRooted()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_ROOTED);
	}

	public final boolean isSleeping()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_SLEEP);
	}

	public final boolean isStunned()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_STUNNED);
	}

	public final boolean isBetrayed()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_BETRAYED);
	}

	public final boolean isImmobileUntilAttacked()
	{
		return isAffected(CharEffectList.EFFECT_FLAG_MEDITATING);
	}

	/**
	 * @return True if the L2Character can't use its skills (ex : stun, sleep...).
	 */
	public final boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isStunned() || isImmobileUntilAttacked() || isSleeping() || isParalyzed();
	}

	/**
	 * @return True if the L2Character can't attack (stun, sleep, attackEndTime, fakeDeath, paralyse).
	 */
	public boolean isAttackingDisabled()
	{
		return isFlying() || isStunned() || isImmobileUntilAttacked() || isSleeping() || _attackEndTime > GameTimeController.getGameTicks() || isParalyzed() || isAlikeDead() || isCoreAIDisabled();
	}

	public final Calculator[] getCalculators()
	{
		return _calculators;
	}

	public final boolean isFlying()
	{
		return _isFlying;
	}

	public final void setIsFlying(boolean mode)
	{
		_isFlying = mode;
	}

	public boolean isImmobilized()
	{
		return _isImmobilized;
	}

	public void setIsImmobilized(boolean value)
	{
		_isImmobilized = value;
	}

	/**
	 * @return True if the L2Character is dead or use fake death.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}

	/**
	 * @return True if the L2Character is dead.
	 */
	public final boolean isDead()
	{
		return _isDead;
	}

	public final void setIsDead(boolean value)
	{
		_isDead = value;
	}

	/**
	 * @return True if the L2Character can't move (stun, root, sleep, overload, paralyzed).
	 */
	public boolean isMovementDisabled()
	{
		return isStunned() || isImmobileUntilAttacked() || isRooted() || isSleeping() || isOverloaded() || isParalyzed() || isImmobilized() || isAlikeDead() || isTeleporting();
	}

	/**
	 * @return True if the L2Character can be controlled by the player (confused, afraid).
	 */
	public final boolean isOutOfControl()
	{
		return isConfused() || isAfraid();
	}

	public final boolean isOverloaded()
	{
		return _isOverloaded;
	}

	public final void setIsOverloaded(boolean value)
	{
		_isOverloaded = value;
	}

	public final boolean isParalyzed()
	{
		return _isParalyzed || isAffected(CharEffectList.EFFECT_FLAG_PARALYZED);
	}

	public final void setIsParalyzed(boolean value)
	{
		_isParalyzed = value;
	}

	public final boolean isPendingRevive()
	{
		return isDead() && _isPendingRevive;
	}

	public final void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}

	/**
	 * Overriden in L2PcInstance.
	 *
	 * @return the L2Summon of the L2Character.
	 */
	public L2Summon getPet()
	{
		return null;
	}

	public final boolean isRiding()
	{
		return _isRiding;
	}

	public final void setIsRiding(boolean mode)
	{
		_isRiding = mode;
	}

	public final boolean isRunning()
	{
		return _isRunning;
	}

	public final void setIsRunning(boolean value)
	{
		_isRunning = value;
		if (getRunSpeed() != 0)
			broadcastPacket(new ChangeMoveType(this));

		if (this instanceof L2PcInstance)
			((L2PcInstance) this).broadcastUserInfo();
		else if (this instanceof L2Summon)
			((L2Summon) this).broadcastStatusUpdate();
		else if (this instanceof L2Npc)
		{
			Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
			for (L2PcInstance player : plrs)
			{
				if (player == null)
					continue;

				if (getRunSpeed() == 0)
					player.sendPacket(new ServerObjectInfo((L2Npc) this, player));
				else
					player.sendPacket(new NpcInfo((L2Npc) this, player));
			}
		}
	}

	/** Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
	public final void setRunning()
	{
		if (!isRunning())
			setIsRunning(true);
	}

	public final boolean isTeleporting()
	{
		return _isTeleporting;
	}

	public final void setIsTeleporting(boolean value)
	{
		_isTeleporting = value;
	}

	public void setIsInvul(boolean b)
	{
		_isInvul = b;
	}

	public boolean isInvul()
	{
		return _isInvul || _isTeleporting;
	}

	public void setIsMortal(boolean b)
	{
		_isMortal = b;
	}

	public boolean isMortal()
	{
		return _isMortal;
	}

	public boolean isUndead()
	{
		return false;
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new CharKnownList(this));
	}

	@Override
	public CharKnownList getKnownList()
	{
		return ((CharKnownList) super.getKnownList());
	}

	public void initCharStat()
	{
		_stat = new CharStat(this);
	}

	public CharStat getStat()
	{
		return _stat;
	}

	public final void setStat(CharStat value)
	{
		_stat = value;
	}

	public void initCharStatus()
	{
		_status = new CharStatus(this);
	}

	public CharStatus getStatus()
	{
		return _status;
	}

	public final void setStatus(CharStatus value)
	{
		_status = value;
	}

	@Override
	public void initPosition()
	{
		setObjectPosition(new CharPosition(this));
	}

	@Override
	public CharPosition getPosition()
	{
		return (CharPosition) super.getPosition();
	}

	public L2CharTemplate getTemplate()
	{
		return _template;
	}

	/**
	 * Set the template of the L2Character.<BR>
	 * <BR>
	 * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...). All of those
	 * properties are stored in a different template for each type of L2Character. Each template is loaded once in the server
	 * cache memory (reduce memory use). When a new instance of L2Character is spawned, server just create a link between the
	 * instance and the template This link is stored in <B>_template</B>
	 *
	 * @param template
	 *            The template to set up.
	 */
	protected final void setTemplate(L2CharTemplate template)
	{
		_template = template;
	}

	/**
	 * @return the Title of the L2Character.
	 */
	public final String getTitle()
	{
		return _title;
	}

	/**
	 * Set the Title of the L2Character. Concatens it if length > 16.
	 *
	 * @param value
	 *            The String to test.
	 */
	public final void setTitle(String value)
	{
		if (value == null)
			_title = "";
		else
			_title = value.length() > 16 ? value.substring(0, 15) : value;
	}

	/** Set the L2Character movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
	public final void setWalking()
	{
		if (isRunning())
			setIsRunning(false);
	}

	/**
	 * Task lauching the function onHitTimer().<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet
	 * ActionFailed (if attacker is a L2PcInstance)</li> <li>If attack isn't aborted, send a message system (critical hit,
	 * missed...) to attacker/target if they are L2PcInstance</li> <li>If attack isn't aborted and hit isn't missed, reduce HP of
	 * the target and calculate reflection damage to reduce HP of attacker if necessary</li> <li>if attack isn't aborted and hit
	 * isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li><BR>
	 * <BR>
	 */
	class HitTask implements Runnable
	{
		L2Character _hitTarget;
		int _damage;
		boolean _crit;
		boolean _miss;
		byte _shld;
		boolean _soulshot;

		public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
		{
			_hitTarget = target;
			_damage = damage;
			_crit = crit;
			_shld = shld;
			_miss = miss;
			_soulshot = soulshot;
		}

		@Override
		public void run()
		{
			try
			{
				onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld);
			}
			catch (Exception e)
			{
				_log.warn("Failed executing HitTask.", e);
			}
		}
	}

	/** Task lauching the magic skill phases */
	class MagicUseTask implements Runnable
	{
		L2Object[] targets;
		L2Skill skill;
		int hitTime;
		int coolTime;
		int phase;
		boolean simultaneously;
		int shots;

		public MagicUseTask(L2Object[] tgts, L2Skill s, int hit, int coolT, boolean simultaneous, int shot)
		{
			targets = tgts;
			skill = s;
			phase = 1;
			hitTime = hit;
			coolTime = coolT;
			simultaneously = simultaneous;
			shots = shot;
		}

		@Override
		public void run()
		{
			try
			{
				switch (phase)
				{
					case 1:
						onMagicLaunchedTimer(this);
						break;
					case 2:
						onMagicHitTimer(this);
						break;
					case 3:
						onMagicFinalizer(this);
						break;
					default:
						break;
				}
			}
			catch (Exception e)
			{
				_log.warn("Failed executing MagicUseTask.", e);
				if (simultaneously)
					setIsCastingSimultaneouslyNow(false);
				else
					setIsCastingNow(false);
			}
		}
	}

	/** Task launching the function useMagic() */
	private static class QueuedMagicUseTask implements Runnable
	{
		L2PcInstance _currPlayer;
		L2Skill _queuedSkill;
		boolean _isCtrlPressed;
		boolean _isShiftPressed;

		public QueuedMagicUseTask(L2PcInstance currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed)
		{
			_currPlayer = currPlayer;
			_queuedSkill = queuedSkill;
			_isCtrlPressed = isCtrlPressed;
			_isShiftPressed = isShiftPressed;
		}

		@Override
		public void run()
		{
			try
			{
				_currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
			}
			catch (Exception e)
			{
				_log.warn("Failed executing QueuedMagicUseTask.", e);
			}
		}
	}

	/** Task of AI notification */
	public class NotifyAITask implements Runnable
	{
		private final CtrlEvent _evt;

		NotifyAITask(CtrlEvent evt)
		{
			_evt = evt;
		}

		@Override
		public void run()
		{
			try
			{
				getAI().notifyEvent(_evt, null);
			}
			catch (Throwable t)
			{
				_log.warn(t.getLocalizedMessage(), t);
			}
		}
	}

	/** Task lauching the magic skill phases */
	class FlyToLocationTask implements Runnable
	{
		private final L2Object _tgt;
		private final L2Character _actor;
		private final L2Skill _skill;

		public FlyToLocationTask(L2Character actor, L2Object target, L2Skill skill)
		{
			_actor = actor;
			_tgt = target;
			_skill = skill;
		}

		@Override
		public void run()
		{
			try
			{
				FlyType _flyType;

				_flyType = FlyType.valueOf(_skill.getFlyType());

				broadcastPacket(new FlyToLocation(_actor, _tgt, _flyType));
				getPosition().setXYZ(_tgt.getX(), _tgt.getY(), _tgt.getZ());
			}
			catch (Exception e)
			{
				_log.warn("Failed executing FlyToLocationTask.", e);
			}
		}
	}

	// =========================================================
	/** Map 32 bits (0x0000) containing all abnormal effect in progress */
	private int _AbnormalEffects;

	protected CharEffectList _effects = new CharEffectList(this);

	// Method - Public
	/**
	 * Launch and add L2Effect (including Stack Group management) to L2Character and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
	 * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * Several same effect can't be used on a L2Character at the same time. Indeed, effects are not stackable and the last cast
	 * will replace the previous in progress. More, some effects belong to the same Stack Group (ex WindWald and Haste Potion). If
	 * 2 effects of a same group are used at the same time on a L2Character, only the more efficient (identified by its priority
	 * order) will be preserve.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Add the L2Effect to the L2Character _effects</li> <li>If this effect doesn't belong to a Stack Group, add its Funcs to
	 * the Calculator set of the L2Character (remove the old one if necessary)</li> <li>If this effect has higher priority in its
	 * Stack Group, add its Funcs to the Calculator set of the L2Character (remove previous stacked effect Funcs if necessary)</li>
	 * <li>If this effect has NOT higher priority in its Stack Group, set the effect to Not In Use</li> <li>Update active skills
	 * in progress icones on player client</li><BR>
	 *
	 * @param newEffect
	 */
	public void addEffect(L2Effect newEffect)
	{
		_effects.queueEffect(newEffect, false);
	}

	/**
	 * Stop and remove L2Effect (including Stack Group management) from L2Character and update client magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
	 * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * Several same effect can't be used on a L2Character at the same time. Indeed, effects are not stackable and the last cast
	 * will replace the previous in progress. More, some effects belong to the same Stack Group (ex WindWald and Haste Potion). If
	 * 2 effects of a same group are used at the same time on a L2Character, only the more efficient (identified by its priority
	 * order) will be preserve.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li> <li>If the L2Effect belongs to a
	 * not empty Stack Group, replace theses Funcs by next stacked effect Funcs</li> <li>Remove the L2Effect from _effects of the
	 * L2Character</li> <li>Update active skills in progress icones on player client</li><BR>
	 *
	 * @param effect
	 */
	public final void removeEffect(L2Effect effect)
	{
		_effects.queueEffect(effect, true);
	}

	public final void startAbnormalEffect(AbnormalEffect mask)
	{
		_AbnormalEffects |= mask.getMask();
		updateAbnormalEffect();
	}

	public final void startAbnormalEffect(int mask)
	{
		_AbnormalEffects |= mask;
		updateAbnormalEffect();
	}

	public final void stopAbnormalEffect(AbnormalEffect mask)
	{
		_AbnormalEffects &= ~mask.getMask();
		updateAbnormalEffect();
	}

	public final void stopAbnormalEffect(int mask)
	{
		_AbnormalEffects &= ~mask;
		updateAbnormalEffect();
	}

	/**
	 * Stop all active skills effects in progress on the L2Character.<BR>
	 * <BR>
	 */
	public void stopAllEffects()
	{
		_effects.stopAllEffects();
	}

	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effects.stopAllEffectsExceptThoseThatLastThroughDeath();
	}

	/**
	 * Confused
	 */
	public final void startConfused()
	{
		getAI().notifyEvent(CtrlEvent.EVT_CONFUSED);
		updateAbnormalEffect();
	}

	public final void stopConfused(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.CONFUSION);
		else
			removeEffect(effect);

		if (!(this instanceof L2PcInstance))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}

	/**
	 * Fake Death
	 */
	public final void startFakeDeath()
	{
		if (!(this instanceof L2PcInstance))
			return;

		((L2PcInstance) this).setIsFakeDeath(true);
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
	}

	public final void stopFakeDeath(boolean removeEffects)
	{
		if (!(this instanceof L2PcInstance))
			return;

		final L2PcInstance player = ((L2PcInstance) this);

		if (removeEffects)
			stopEffects(L2EffectType.FAKE_DEATH);

		// if this is a player instance, start the grace period for this character (grace from mobs only)!
		player.setIsFakeDeath(false);
		player.setRecentFakeDeath(true);

		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		broadcastPacket(new Revive(this));

		// Schedule a paralyzed task to wait for the animation to finish
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				player.setIsParalyzed(false);
			}
		}, player.getAnimationTimer());
		setIsParalyzed(true);
	}

	/**
	 * Fear
	 */
	public final void startFear()
	{
		getAI().notifyEvent(CtrlEvent.EVT_AFRAID);
		updateAbnormalEffect();
	}

	public final void stopFear(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.FEAR);
		updateAbnormalEffect();
	}

	/**
	 * ImmobileUntilAttacked
	 */
	public final void startImmobileUntilAttacked()
	{
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING, null);
		updateAbnormalEffect();
	}

	public final void stopImmobileUntilAttacked(L2Effect effect)
	{
		if (effect == null)
			stopEffects(L2EffectType.IMMOBILEUNTILATTACKED);
		else
		{
			removeEffect(effect);
			stopSkillEffects(effect.getSkill().getId());
		}

		getAI().notifyEvent(CtrlEvent.EVT_THINK, null);
		updateAbnormalEffect();
	}

	/**
	 * Muted
	 */
	public final void startMuted()
	{
		abortCast();
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}

	public final void stopMuted(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.MUTE);

		updateAbnormalEffect();
	}

	/**
	 * Paralize
	 */
	public final void startParalyze()
	{
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_PARALYZED);
	}

	public final void stopParalyze(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.PARALYZE);

		if (!(this instanceof L2PcInstance))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
	}

	/**
	 * PsychicalMuted
	 */
	public final void startPhysicalMuted()
	{
		getAI().notifyEvent(CtrlEvent.EVT_MUTED);
		updateAbnormalEffect();
	}

	public final void stopPhysicalMuted(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.PHYSICAL_MUTE);

		updateAbnormalEffect();
	}

	/**
	 * Root
	 */
	public final void startRooted()
	{
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_ROOTED);
		updateAbnormalEffect();
	}

	public final void stopRooting(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.ROOT);

		if (!(this instanceof L2PcInstance))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}

	/**
	 * Sleep
	 */
	public final void startSleeping()
	{
		/* Aborts any attacks/casts if slept */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_SLEEPING);
		updateAbnormalEffect();
	}

	public final void stopSleeping(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.SLEEP);

		if (!(this instanceof L2PcInstance))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}

	/**
	 * Stun
	 */
	public final void startStunning()
	{
		/* Aborts any attacks/casts if stunned */
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyEvent(CtrlEvent.EVT_STUNNED);

		if (!(this instanceof L2Summon))
			getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

		updateAbnormalEffect();
	}

	public final void stopStunning(boolean removeEffects)
	{
		if (removeEffects)
			stopEffects(L2EffectType.STUN);

		if (!(this instanceof L2PcInstance))
			getAI().notifyEvent(CtrlEvent.EVT_THINK);
		updateAbnormalEffect();
	}

	/**
	 * Stop and remove the L2Effects corresponding to the L2Skill Identifier and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
	 * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 *
	 * @param skillId
	 *            The L2Skill Identifier of the L2Effect to remove from _effects
	 */
	public final void stopSkillEffects(int skillId)
	{
		_effects.stopSkillEffects(skillId);
	}

	/**
	 * Stop and remove the L2Effects corresponding to the L2SkillType and update client magic icon.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
	 * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 *
	 * @param skillType
	 *            The L2SkillType of the L2Effect to remove from _effects
	 * @param negateLvl
	 */
	public final void stopSkillEffects(L2SkillType skillType, int negateLvl)
	{
		_effects.stopSkillEffects(skillType, negateLvl);
	}

	public final void stopSkillEffects(L2SkillType skillType)
	{
		_effects.stopSkillEffects(skillType, -1);
	}

	/**
	 * Stop and remove all L2Effect of the selected type (ex : BUFF, DMG_OVER_TIME...) from the L2Character and update client
	 * magic icone.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
	 * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li> <li>Remove the L2Effect from
	 * _effects of the L2Character</li> <li>Update active skills in progress icones on player client</li><BR>
	 * <BR>
	 *
	 * @param type
	 *            The type of effect to stop ((ex : BUFF, DMG_OVER_TIME...)
	 */
	public final void stopEffects(L2EffectType type)
	{
		_effects.stopEffects(type);
	}

	/**
	 * Exits all buffs effects of the skills with "removedOnAnyAction" set. Called on any action except movement (attack, cast).
	 */
	public final void stopEffectsOnAction()
	{
		_effects.stopEffectsOnAction();
	}

	/**
	 * Exits all buffs effects of the skills with "removedOnDamage" set. Called on decreasing HP and mana burn.
	 *
	 * @param awake
	 */
	public final void stopEffectsOnDamage(boolean awake)
	{
		_effects.stopEffectsOnDamage(awake);
	}

	/**
	 * <B><U> Overridden in</U> :</B><BR>
	 * <BR>
	 * <li>L2Npc</li> <li>L2PcInstance</li> <li>L2Summon</li> <li>L2DoorInstance</li><BR>
	 * <BR>
	 */
	public abstract void updateAbnormalEffect();

	/**
	 * Update active skills in progress (In Use and Not In Use because stacked) icones on client.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icone on the client.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the client of the player and not clients of all players in
	 * the party.</B></FONT><BR>
	 * <BR>
	 */
	public final void updateEffectIcons()
	{
		updateEffectIcons(false);
	}

	/**
	 * Updates Effect Icons for this character(palyer/summon) and his party if any<BR>
	 * Overridden in:<BR>
	 * L2PcInstance<BR>
	 * L2Summon<BR>
	 *
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// overridden
	}

	/**
	 * In Server->Client packet, each effect is represented by 1 bit of the map (ex : BLEEDING = 0x0001 (bit 1), SLEEP = 0x0080
	 * (bit 8)...). The map is calculated by applying a BINARY OR operation on each effect.
	 *
	 * @return a map of 16 bits (0x0000) containing all abnormal effect in progress for this L2Character.
	 */
	public int getAbnormalEffect()
	{
		int ae = _AbnormalEffects;
		if (isStunned())
			ae |= AbnormalEffect.STUN.getMask();
		if (isRooted())
			ae |= AbnormalEffect.ROOT.getMask();
		if (isSleeping())
			ae |= AbnormalEffect.SLEEP.getMask();
		if (isConfused())
			ae |= AbnormalEffect.FEAR.getMask();
		if (isAfraid())
			ae |= AbnormalEffect.FEAR.getMask();
		if (isMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isPhysicalMuted())
			ae |= AbnormalEffect.MUTED.getMask();
		if (isImmobileUntilAttacked())
			ae |= AbnormalEffect.FLOATING_ROOT.getMask();

		return ae;
	}

	/**
	 * Return all active skills effects in progress on the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in <B>_effects</B>. The Integer key of _effects is
	 * the L2Skill Identifier that has created the effect.<BR>
	 * <BR>
	 *
	 * @return A table containing all active skills effect in progress on the L2Character
	 */
	public final L2Effect[] getAllEffects()
	{
		return _effects.getAllEffects();
	}

	/**
	 * Return L2Effect in progress on the L2Character corresponding to the L2Skill Identifier.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
	 *
	 * @param skillId
	 *            The L2Skill Identifier of the L2Effect to return from the _effects
	 * @return The L2Effect corresponding to the L2Skill Identifier
	 */
	public final L2Effect getFirstEffect(int skillId)
	{
		return _effects.getFirstEffect(skillId);
	}

	/**
	 * Return the first L2Effect in progress on the L2Character created by the L2Skill.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
	 *
	 * @param skill
	 *            The L2Skill whose effect must be returned
	 * @return The first L2Effect created by the L2Skill
	 */
	public final L2Effect getFirstEffect(L2Skill skill)
	{
		return _effects.getFirstEffect(skill);
	}

	/**
	 * Return the first L2Effect in progress on the L2Character corresponding to the Effect Type (ex : BUFF, STUN, ROOT...).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect)
	 * <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
	 * <BR>
	 *
	 * @param tp
	 *            The Effect Type of skills whose effect must be returned
	 * @return The first L2Effect corresponding to the Effect Type
	 */
	public final L2Effect getFirstEffect(L2EffectType tp)
	{
		return _effects.getFirstEffect(tp);
	}

	// =========================================================
	// NEED TO ORGANIZE AND MOVE TO PROPER PLACE
	/** This class permit to the L2Character AI to obtain informations and uses L2Character method */
	public class AIAccessor
	{
		public AIAccessor()
		{
		}

		/**
		 * @return the L2Character managed by this Accessor AI.
		 */
		public L2Character getActor()
		{
			return L2Character.this;
		}

		/**
		 * Accessor to L2Character moveToLocation() method with an interaction area.
		 *
		 * @param x
		 * @param y
		 * @param z
		 * @param offset
		 */
		public void moveTo(int x, int y, int z, int offset)
		{
			moveToLocation(x, y, z, offset);
		}

		/**
		 * Accessor to L2Character moveToLocation() method without interaction area.
		 *
		 * @param x
		 * @param y
		 * @param z
		 */
		public void moveTo(int x, int y, int z)
		{
			moveToLocation(x, y, z, 0);
		}

		/**
		 * Accessor to L2Character stopMove() method.
		 *
		 * @param pos
		 *            The L2CharPosition position.
		 */
		public void stopMove(L2CharPosition pos)
		{
			L2Character.this.stopMove(pos);
		}

		/**
		 * Accessor to L2Character doAttack() method.
		 *
		 * @param target
		 *            The target to make checks on.
		 */
		public void doAttack(L2Character target)
		{
			L2Character.this.doAttack(target);
		}

		/**
		 * Accessor to L2Character doCast() method.
		 *
		 * @param skill
		 *            The skill object to launch.
		 */
		public void doCast(L2Skill skill)
		{
			L2Character.this.doCast(skill);
		}

		/**
		 * @param evt
		 *            An event which happens.
		 * @return a new NotifyAITask.
		 */
		public NotifyAITask newNotifyTask(CtrlEvent evt)
		{
			return new NotifyAITask(evt);
		}

		/**
		 * Cancel the AI.
		 */
		public void detachAI()
		{
			_ai = null;
		}
	}

	/**
	 * This class group all mouvement data.<BR>
	 * <BR>
	 * <B><U> Data</U> :</B><BR>
	 * <BR>
	 * <li>_moveTimestamp : Last time position update</li> <li>_xDestination, _yDestination, _zDestination : Position of the
	 * destination</li> <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of the origin</li> <li>_moveStartTime : Start time of
	 * the movement</li> <li>_ticksToMove : Nb of ticks between the start and the destination</li> <li>_xSpeedTicks, _ySpeedTicks
	 * : Speed in unit/ticks</li><BR>
	 * <BR>
	 */
	public static class MoveData
	{
		// when we retrieve x/y/z we use GameTimeControl.getGameTicks()
		// if we are moving, but move timestamp==gameticks, we don't need
		// to recalculate position
		public int _moveStartTime;
		public int _moveTimestamp; // last update
		public int _xDestination;
		public int _yDestination;
		public int _zDestination;
		public double _xAccurate; // otherwise there would be rounding errors
		public double _yAccurate;
		public double _zAccurate;
		public int _heading;

		public boolean disregardingGeodata;
		public int onGeodataPathIndex;
		public List<AbstractNodeLoc> geoPath;
		public int geoPathAccurateTx;
		public int geoPathAccurateTy;
		public int geoPathGtx;
		public int geoPathGty;
	}

	/** Table containing all skillId that are disabled */
	protected Map<Integer, Long> _disabledSkills;
	private boolean _allSkillsDisabled;

	/** Movement data of this L2Character */
	protected MoveData _move;

	/** Orientation of the L2Character */
	private int _heading;

	/** L2Charcater targeted by the L2Character */
	private L2Object _target;

	// set by the start of attack, in game ticks
	private int _attackEndTime;
	private int _attacking;
	private int _disableBowAttackEndTime;
	private int _castInterruptTime;

	/** Table of calculators containing all standard NPC calculator (ex : ACCURACY_COMBAT, EVASION_RATE */
	private static final Calculator[] NPC_STD_CALCULATOR;
	static
	{
		NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
	}

	protected CharacterAI _ai;

	/** Future Skill Cast */
	protected Future<?> _skillCast;
	protected Future<?> _skillCast2;

	/**
	 * Add a Func to the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>. Each Calculator (a calculator per state) own a table
	 * of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex :
	 * REGENERATE_HP_RATE...). To reduce cache memory use, L2Npcs who don't have skills share the same Calculator set called
	 * <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * That's why, if a L2Npc is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be
	 * create in its _calculators before addind new Func object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If _calculators is linked to NPC_STD_CALCULATOR, create a copy of NPC_STD_CALCULATOR in _calculators</li> <li>Add the
	 * Func object to _calculators</li><BR>
	 * <BR>
	 *
	 * @param f
	 *            The Func object to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFunc(Func f)
	{
		if (f == null)
			return;

		synchronized (_calculators)
		{
			// Check if Calculator set is linked to the standard Calculator set of NPC
			if (_calculators == NPC_STD_CALCULATOR)
			{
				// Create a copy of the standard NPC Calculator set
				_calculators = new Calculator[Stats.NUM_STATS];

				for (int i = 0; i < Stats.NUM_STATS; i++)
				{
					if (NPC_STD_CALCULATOR[i] != null)
						_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
				}
			}

			// Select the Calculator of the affected state in the Calculator set
			int stat = f.stat.ordinal();

			if (_calculators[stat] == null)
				_calculators[stat] = new Calculator();

			// Add the Func to the calculator corresponding to the state
			_calculators[stat].addFunc(f);
		}
	}

	/**
	 * Add a list of Funcs to the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>. Each Calculator (a calculator per state) own a table
	 * of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex :
	 * REGENERATE_HP_RATE...). <BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method is ONLY for L2PcInstance</B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Equip an item from inventory</li> <li>Learn a new passive skill</li> <li>Use an active skill</li><BR>
	 * <BR>
	 *
	 * @param funcs
	 *            The list of Func objects to add to the Calculator corresponding to the state affected
	 */
	public final void addStatFuncs(Func[] funcs)
	{
		FastList<Stats> modifiedStats = new FastList<>();

		for (Func f : funcs)
		{
			modifiedStats.add(f.stat);
			addStatFunc(f);
		}
		broadcastModifiedStats(modifiedStats);
	}

	/**
	 * Remove all Func objects with the selected owner from the Calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A L2Character owns a table of Calculators called <B>_calculators</B>. Each Calculator (a calculator per state) own a table
	 * of Func object. A Func object is a mathematic function that permit to calculate the modifier of a state (ex :
	 * REGENERATE_HP_RATE...). To reduce cache memory use, L2Npcs who don't have skills share the same Calculator set called
	 * <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * That's why, if a L2Npc is under a skill/spell effect that modify one of its state, a copy of the NPC_STD_CALCULATOR must be
	 * create in its _calculators before addind new Func object.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove all Func objects of the selected owner from _calculators</li><BR>
	 * <BR>
	 * <li>If L2Character is a L2Npc and _calculators is equal to NPC_STD_CALCULATOR, free cache memory and just create a link on
	 * NPC_STD_CALCULATOR in _calculators</li><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>Unequip an item from inventory</li> <li>Stop an active skill</li><BR>
	 * <BR>
	 *
	 * @param owner
	 *            The Object(Skill, Item...) that has created the effect
	 */
	public final void removeStatsOwner(Object owner)
	{
		FastList<Stats> modifiedStats = null;

		int i = 0;
		// Go through the Calculator set
		synchronized (_calculators)
		{
			for (Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Delete all Func objects of the selected owner
					if (modifiedStats != null)
						modifiedStats.addAll(calc.removeOwner(owner));
					else
						modifiedStats = calc.removeOwner(owner);

					if (calc.size() == 0)
						_calculators[i] = null;
				}
				i++;
			}

			// If possible, free the memory and just create a link on NPC_STD_CALCULATOR
			if (this instanceof L2Npc)
			{
				i = 0;
				for (; i < Stats.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
						break;
				}

				if (i >= Stats.NUM_STATS)
					_calculators = NPC_STD_CALCULATOR;
			}

			if (owner instanceof L2Effect)
			{
				if (!((L2Effect) owner).preventExitUpdate)
					broadcastModifiedStats(modifiedStats);
			}
			else
				broadcastModifiedStats(modifiedStats);
		}
	}

	private void broadcastModifiedStats(FastList<Stats> stats)
	{
		if (stats == null || stats.isEmpty())
			return;

		boolean broadcastFull = false;
		StatusUpdate su = null;

		if (this instanceof L2Summon && ((L2Summon) this).getOwner() != null)
			((L2Summon) this).updateAndBroadcastStatusAndInfos(1);
		else
		{
			for (Stats stat : stats)
			{
				if (stat == Stats.POWER_ATTACK_SPEED)
				{
					if (su == null)
						su = new StatusUpdate(this);

					su.addAttribute(StatusUpdate.ATK_SPD, getPAtkSpd());
				}
				else if (stat == Stats.MAGIC_ATTACK_SPEED)
				{
					if (su == null)
						su = new StatusUpdate(this);

					su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd());
				}
				else if (stat == Stats.MAX_HP && this instanceof L2Attackable)
				{
					if (su == null)
						su = new StatusUpdate(this);

					su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
				}
				else if (stat == Stats.RUN_SPEED)
					broadcastFull = true;
			}
		}

		if (this instanceof L2PcInstance)
		{
			if (broadcastFull)
				((L2PcInstance) this).updateAndBroadcastStatus(2);
			else
			{
				((L2PcInstance) this).updateAndBroadcastStatus(1);
				if (su != null)
					broadcastPacket(su);
			}
		}
		else if (this instanceof L2Npc)
		{
			if (broadcastFull)
			{
				Collection<L2PcInstance> plrs = getKnownList().getKnownPlayers().values();
				for (L2PcInstance player : plrs)
				{
					if (player == null)
						continue;

					if (getRunSpeed() == 0)
						player.sendPacket(new ServerObjectInfo((L2Npc) this, player));
					else
						player.sendPacket(new NpcInfo((L2Npc) this, player));
				}
			}
			else if (su != null)
				broadcastPacket(su);
		}
		else if (su != null)
			broadcastPacket(su);
	}

	/**
	 * @return the orientation of the L2Character.
	 */
	public final int getHeading()
	{
		return _heading;
	}

	/**
	 * Set the orientation of the L2Character.
	 *
	 * @param heading
	 */
	public final void setHeading(int heading)
	{
		_heading = heading;
	}

	public final int getXdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._xDestination;

		return getX();
	}

	public final int getYdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._yDestination;

		return getY();
	}

	public final int getZdestination()
	{
		MoveData m = _move;
		if (m != null)
			return m._zDestination;

		return getZ();
	}

	/**
	 * @return True if the L2Character is in combat.
	 */
	public boolean isInCombat()
	{
		return (getAI().getAttackTarget() != null || getAI().isAutoAttacking());
	}

	/**
	 * @return True if the L2Character is moving.
	 */
	public final boolean isMoving()
	{
		return _move != null;
	}

	/**
	 * @return True if the L2Character is travelling a calculated path.
	 */
	public final boolean isOnGeodataPath()
	{
		MoveData m = _move;
		if (m == null)
			return false;
		if (m.onGeodataPathIndex == -1)
			return false;
		if (m.onGeodataPathIndex == m.geoPath.size() - 1)
			return false;
		return true;
	}

	/**
	 * @return True if the L2Character is casting.
	 */
	public final boolean isCastingNow()
	{
		return _isCastingNow;
	}

	public void setIsCastingNow(boolean value)
	{
		_isCastingNow = value;
	}

	public final boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}

	public void setIsCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}

	/**
	 * @return True if the cast of the L2Character can be aborted.
	 */
	public final boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeController.getGameTicks();
	}

	/**
	 * @return True if the L2Character is attacking.
	 */
	public boolean isAttackingNow()
	{
		return _attackEndTime > GameTimeController.getGameTicks();
	}

	/**
	 * @return True if the L2Character has aborted its attack.
	 */
	public final boolean isAttackAborted()
	{
		return _attacking <= 0;
	}

	/**
	 * Abort the attack of the L2Character and send Server->Client ActionFailed packet.
	 */
	public final void abortAttack()
	{
		if (isAttackingNow())
		{
			_attacking = 0;
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	/**
	 * @return the body part (paperdoll slot) we are targeting right now.
	 */
	public final int getAttackingBodyPart()
	{
		return _attacking;
	}

	/**
	 * Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.<BR>
	 * <BR>
	 */
	public final void abortCast()
	{
		if (isCastingNow() || isCastingSimultaneouslyNow())
		{
			Future<?> future = _skillCast;
			// cancels the skill hit scheduled task
			if (future != null)
			{
				future.cancel(true);
				_skillCast = null;
			}
			future = _skillCast2;
			if (future != null)
			{
				future.cancel(true);
				_skillCast2 = null;
			}

			if (getFusionSkill() != null)
				getFusionSkill().onCastAbort();

			L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
			if (mog != null)
				mog.exit();

			if (_allSkillsDisabled)
				enableAllSkills(); // this remains for forced skill use, e.g. scroll of escape

			setIsCastingNow(false);
			setIsCastingSimultaneouslyNow(false);

			// safeguard for cannot be interrupt any more
			_castInterruptTime = 0;

			if (this instanceof L2PcInstance)
				getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING); // setting back previous intention

			broadcastPacket(new MagicSkillCanceld(getObjectId())); // broadcast packet to stop animations client-side
			sendPacket(ActionFailed.STATIC_PACKET); // send an "action failed" packet to the caster
		}
	}

	/**
	 * Update the position of the L2Character during a movement and return True if the movement is finished.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B>
	 * of the L2Character. The position of the start point and of the destination permit to estimated in function of the movement
	 * speed the time to achieve the destination.<BR>
	 * <BR>
	 * When the movement is started (ex : by MovetoLocation), this method will be called each 0.1 sec to estimate and update the
	 * L2Character position on the server. Note, that the current server position can differe from the current client position
	 * even if each movement is straight foward. That's why, client send regularly a Client->Server ValidatePosition packet to
	 * eventually correct the gap on the server. But, it's always the server position that is used in range calculation.<BR>
	 * <BR>
	 * At the end of the estimated movement time, the L2Character position is automatically set to the destination position even
	 * if the movement is not finished.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current Z position is obtained FROM THE CLIENT by the Client->Server
	 * ValidatePosition Packet. But x and y positions must be calculated to avoid that players try to modify their movement
	 * speed.</B></FONT><BR>
	 * <BR>
	 *
	 * @param gameTicks
	 *            Nb of ticks since the server start
	 * @return True if the movement is finished
	 */
	public boolean updatePosition(int gameTicks)
	{
		// Get movement data
		MoveData m = _move;

		if (m == null)
			return true;

		if (!isVisible())
		{
			_move = null;
			return true;
		}

		// Check if this is the first update
		if (m._moveTimestamp == 0)
		{
			m._moveTimestamp = m._moveStartTime;
			m._xAccurate = getX();
			m._yAccurate = getY();
		}

		// Check if the position has already been calculated
		if (m._moveTimestamp == gameTicks)
			return false;

		int xPrev = getX();
		int yPrev = getY();
		int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations

		double dx, dy, dz;
		if (MainConfig.COORD_SYNCHRONIZE == 1)
		// the only method that can modify x,y while moving (otherwise _move would/should be set null)
		{
			dx = m._xDestination - xPrev;
			dy = m._yDestination - yPrev;
		}
		else
		// otherwise we need saved temporary values to avoid rounding errors
		{
			dx = m._xDestination - m._xAccurate;
			dy = m._yDestination - m._yAccurate;
		}

		final boolean isFloating = isFlying() || isInsideZone(L2Character.ZONE_WATER);

		// Z coordinate will follow geodata or client values
		if (MainConfig.GEODATA > 0 && MainConfig.COORD_SYNCHRONIZE == 2 && !isFloating && !m.disregardingGeodata && GameTimeController.getGameTicks() % 10 == 0 // once
				// a
				// second
				// to
				// reduce
				// possible
				// cpu
				// load
				&& GeoData.getInstance().hasGeo(xPrev, yPrev))
		{
			short geoHeight = GeoData.getInstance().getSpawnHeight(xPrev, yPrev, zPrev - 30, zPrev + 30, null);
			dz = m._zDestination - geoHeight;
			// quite a big difference, compare to validatePosition packet
			if (this instanceof L2PcInstance && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) > 200 && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) < 1500)
			{
				dz = m._zDestination - zPrev; // allow diff
			}
			else if (isInCombat() && Math.abs(dz) > 200 && (dx * dx + dy * dy) < 40000) // allow mob to climb up to pcinstance
				dz = m._zDestination - zPrev; // climbing
			else
				zPrev = geoHeight;
		}
		else
			dz = m._zDestination - zPrev;

		double delta = dx * dx + dy * dy;
		if (delta < 10000 && (dz * dz > 2500) // close enough, allows error between client and server geodata if it cannot be
												// avoided
				&& !isFloating) // should not be applied on vertical movements in water or during flight
			delta = Math.sqrt(delta);
		else
			delta = Math.sqrt(delta + dz * dz);

		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = getStat().getMoveSpeed() * (gameTicks - m._moveTimestamp) / GameTimeController.TICKS_PER_SECOND;
			distFraction = distPassed / delta;
		}

		if (distFraction > 1) // already there
			// Set the position of the L2Character to the destination
			super.getPosition().setXYZ(m._xDestination, m._yDestination, m._zDestination);
		else
		{
			m._xAccurate += dx * distFraction;
			m._yAccurate += dy * distFraction;

			// Set the position of the L2Character to estimated after parcial move
			super.getPosition().setXYZ((int) (m._xAccurate), (int) (m._yAccurate), zPrev + (int) (dz * distFraction + 0.5));
		}
		revalidateZone(false);

		// Set the timer of last position update to now
		m._moveTimestamp = gameTicks;

		return (distFraction > 1);
	}

	public void revalidateZone(boolean force)
	{
		if (getWorldRegion() == null)
			return;

		// This function is called too often from movement code
		if (force)
			_zoneValidateCounter = 4;
		else
		{
			_zoneValidateCounter--;
			if (_zoneValidateCounter < 0)
				_zoneValidateCounter = 4;
			else
				return;
		}
		getWorldRegion().revalidateZones(this);
	}

	/**
	 * Stop movement of the L2Character (Called by AI Accessor only).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Delete movement data of the L2Character</li> <li>Set the current position (x,y,z), its current L2WorldRegion if
	 * necessary and its heading</li> <li>Remove the L2Object object from _gmList** of GmListTable</li> <li>Remove object from
	 * _knownObjects and _knownPlayer* of all surrounding L2WorldRegion L2Characters</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send Server->Client packet StopMove/StopRotation </B></FONT><BR>
	 * <BR>
	 *
	 * @param pos
	 */
	public void stopMove(L2CharPosition pos)
	{
		stopMove(pos, false);
	}

	public void stopMove(L2CharPosition pos, boolean updateKnownObjects)
	{
		// Delete movement data of the L2Character
		_move = null;

		// Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading
		// All data are contained in a L2CharPosition object
		if (pos != null)
		{
			getPosition().setXYZ(pos.x, pos.y, pos.z);
			setHeading(pos.heading);
			revalidateZone(true);
		}
		broadcastPacket(new StopMove(this));

		if (NPCConfig.MOVE_BASED_KNOWNLIST && updateKnownObjects)
			getKnownList().findObjects();
	}

	/**
	 * @return Returns the showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}

	/**
	 * @param showSummonAnimation
	 *            The showSummonAnimation to set.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}

	/**
	 * Target a L2Object (add the target to the L2Character _target, _knownObject and L2Character to _KnownObject of the
	 * L2Object).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * The L2Object (including L2Character) targeted is identified in <B>_target</B> of the L2Character<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Set the _target of L2Character to L2Object</li> <li>If necessary, add L2Object to _knownObject of the L2Character</li>
	 * <li>If necessary, add L2Character to _KnownObject of the L2Object</li> <li>If object==null, cancel Attak or Cast</li><BR>
	 * <BR>
	 * <B><U> Overridden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Remove the L2PcInstance from the old target _statusListener and add it to the new target if it was a
	 * L2Character</li><BR>
	 * <BR>
	 *
	 * @param object
	 *            L2object to target
	 */
	public void setTarget(L2Object object)
	{
		if (object != null)
		{
			if (!object.isVisible())
				object = null;
			else if (object != _target)
			{
				getKnownList().addKnownObject(object);
				object.getKnownList().addKnownObject(this);
			}
		}

		_target = object;
	}

	/**
	 * @return the identifier of the L2Object targeted or -1.
	 */
	public final int getTargetId()
	{
		if (_target != null)
			return _target.getObjectId();

		return -1;
	}

	/**
	 * @return the L2Object targeted or null.
	 */
	public final L2Object getTarget()
	{
		return _target;
	}

	/**
	 * Calculate movement data for a move to location action and add the L2Character to movingObjects of GameTimeController (only
	 * called by AI Accessor).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B>
	 * of the L2Character. The position of the start point and of the destination permit to estimated in function of the movement
	 * speed the time to achieve the destination.<BR>
	 * <BR>
	 * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController that will call the updatePosition
	 * method of those L2Character each 0.1s.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Get current position of the L2Character</li> <li>Calculate distance (dx,dy) between current position and destination
	 * including offset</li> <li>Create and Init a MoveData object</li> <li>Set the L2Character _move object to MoveData object</li>
	 * <li>Add the L2Character to movingObjects of the GameTimeController</li> <li>Create a task to notify the AI that L2Character
	 * arrives at a check point of the movement</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send Server->Client packet MoveToPawn/MoveToLocation
	 * </B></FONT><BR>
	 * <BR>
	 * <B><U> Example of use </U> :</B><BR>
	 * <BR>
	 * <li>AI : onIntentionMoveTo(L2CharPosition), onIntentionPickUp(L2Object), onIntentionInteract(L2Object)</li> <li>FollowTask</li>
	 * <BR>
	 * <BR>
	 *
	 * @param x
	 *            The X position of the destination
	 * @param y
	 *            The Y position of the destination
	 * @param z
	 *            The Y position of the destination
	 * @param offset
	 *            The size of the interaction area of the L2Character targeted
	 */
	protected void moveToLocation(int x, int y, int z, int offset)
	{
		// Get the Move Speed of the L2Charcater
		float speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled())
			return;

		// Get current position of the L2Character
		final int curX = super.getX();
		final int curY = super.getY();
		final int curZ = super.getZ();

		// Calculate distance (dx,dy) between current position and destination
		// TODO: improve Z axis move/follow support when dx,dy are small compared to dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.sqrt(dx * dx + dy * dy);

		final boolean verticalMovementOnly = isFlying() && distance == 0 && dz != 0;
		if (verticalMovementOnly)
			distance = Math.abs(dz);

		// make water move short and use no geodata checks for swimming chars
		// distance in a click can easily be over 3000
		if (MainConfig.GEODATA > 0 && isInsideZone(ZONE_WATER) && distance > 700)
		{
			double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.sqrt(dx * dx + dy * dy);
		}

		_log.debug("distance to target:" + distance);

		// Define movement angles needed
		// ^
		// | X (x,y)
		// | /
		// | /distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)

		double cos;
		double sin;

		// Check if a movement offset is defined or no distance to go through
		if (offset > 0 || distance < 1)
		{
			// approximation for moving closer when z coordinates are different
			// TODO: handle Z axis movement better
			offset -= Math.abs(dz);
			if (offset < 5)
				offset = 5;

			// If no distance to go through, the movement is canceled
			if (distance < 1 || distance - offset <= 0)
			{
				// Notify the AI that the L2Character is arrived at destination
				getAI().notifyEvent(CtrlEvent.EVT_ARRIVED);

				return;
			}
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;

			distance -= (offset - 5); // due to rounding error, we have to move a bit closer to be in range

			// Calculate the new destination with offset included
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calculate movement angles needed
			sin = dy / distance;
			cos = dx / distance;
		}

		// Create and Init a MoveData object
		MoveData m = new MoveData();

		// GEODATA MOVEMENT CHECKS AND PATHFINDING
		m.onGeodataPathIndex = -1; // Initialize not on geodata path
		m.disregardingGeodata = false;

		if (MainConfig.GEODATA > 0 && !isFlying() // flying chars not checked - even canSeeTarget doesn't work yet
				&& (!isInsideZone(ZONE_WATER) || isInsideZone(ZONE_SIEGE)) // swimming also not checked unless in siege zone - but
																			// distance is limited
				&& !(this instanceof L2NpcWalkerInstance)) // npc walkers not checked
		{
			final boolean isInVehicle = this instanceof L2PcInstance && ((L2PcInstance) this).getVehicle() != null;
			if (isInVehicle)
				m.disregardingGeodata = true;

			double originalDistance = distance;
			int originalX = x;
			int originalY = y;
			int originalZ = z;
			int gtx = (originalX - L2World.MAP_MIN_X) >> 4;
			int gty = (originalY - L2World.MAP_MIN_Y) >> 4;

			// Movement checks:
			// when geodata == 2, for all characters except mobs returning home (could be changed later to teleport if pathfinding
			// fails)
			// when geodata == 1, for l2playableinstance and l2riftinstance only
			if ((MainConfig.GEODATA == 2 && !(this instanceof L2Attackable && ((L2Attackable) this).isReturningToSpawnPoint())) || (this instanceof L2PcInstance && !(isInVehicle && distance > 1500)) || (this instanceof L2Summon && !(getAI().getIntention() == AI_INTENTION_FOLLOW)) // assuming
					// intention_follow
					// only when
					// following owner
					|| isAfraid() || this instanceof L2RiftInvaderInstance)
			{
				if (isOnGeodataPath())
				{
					try
					{
						if (gtx == _move.geoPathGtx && gty == _move.geoPathGty)
							return;

						_move.onGeodataPathIndex = -1; // Set not on geodata path
					}
					catch (NullPointerException e)
					{
						// nothing
					}
				}

				if (curX < L2World.MAP_MIN_X || curX > L2World.MAP_MAX_X || curY < L2World.MAP_MIN_Y || curY > L2World.MAP_MAX_Y)
				{
					// Temporary fix for character outside world region errors
					_log.warn("Character " + getName() + " outside world area, in coordinates x:" + curX + " y:" + curY);
					getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);

					if (this instanceof L2PcInstance)
						((L2PcInstance) this).logout();
					else if (this instanceof L2Summon)
						return; // prevention when summon get out of world coords, player will not loose him, unsummon handled
								// from pcinstance
					else
						onDecay();

					return;
				}
				Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z);
				// location different if destination wasn't reached (or just z coord is different)
				x = destiny.getX();
				y = destiny.getY();
				z = destiny.getZ();
				dx = x - curX;
				dy = y - curY;
				dz = z - curZ;
				distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt(dx * dx + dy * dy);
			}
			// Pathfinding checks. Only when geodata setting is 2, the LoS check gives shorter result
			// than the original movement was and the LoS gives a shorter distance than 2000
			// This way of detecting need for pathfinding could be changed.
			if (MainConfig.GEODATA == 2 && originalDistance - distance > 30 && distance < 2000 && !isAfraid())
			{
				// Path calculation -- overrides previous movement check
				if ((this instanceof L2Playable && !isInVehicle) || isMinion() || isInCombat())
				{
					m.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, this instanceof L2Playable);
					if (m.geoPath == null || m.geoPath.size() < 2) // No path found
					{
						// * Even though there's no path found (remember geonodes aren't perfect),
						// the mob is attacking and right now we set it so that the mob will go
						// after target anyway, is dz is small enough.
						// * With cellpathfinding this approach could be changed but would require taking
						// off the geonodes and some more checks.
						// * Summons will follow their masters no matter what.
						// * Currently minions also must move freely since AttackableAI commands
						// them to move along with their leader
						if (this instanceof L2PcInstance || (!(this instanceof L2Playable) && !isMinion() && Math.abs(z - curZ) > 140) || (this instanceof L2Summon && !((L2Summon) this).getFollowStatus()))
						{
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}

						m.disregardingGeodata = true;
						x = originalX;
						y = originalY;
						z = originalZ;
						distance = originalDistance;
					}
					else
					{
						m.onGeodataPathIndex = 0; // on first segment
						m.geoPathGtx = gtx;
						m.geoPathGty = gty;
						m.geoPathAccurateTx = originalX;
						m.geoPathAccurateTy = originalY;

						x = m.geoPath.get(m.onGeodataPathIndex).getX();
						y = m.geoPath.get(m.onGeodataPathIndex).getY();
						z = m.geoPath.get(m.onGeodataPathIndex).getZ();

						// check for doors in the route
						if (DoorData.getInstance().checkIfDoorsBetween(curX, curY, curZ, x, y, z))
						{
							m.geoPath = null;
							getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
							return;
						}
						for (int i = 0; i < m.geoPath.size() - 1; i++)
						{
							if (DoorData.getInstance().checkIfDoorsBetween(m.geoPath.get(i), m.geoPath.get(i + 1)))
							{
								m.geoPath = null;
								getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
								return;
							}
						}

						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt(dx * dx + dy * dy);
						sin = dy / distance;
						cos = dx / distance;
					}
				}
			}
			// If no distance to go through, the movement is canceled
			if (distance < 1 && (MainConfig.GEODATA == 2 || this instanceof L2Playable || this instanceof L2RiftInvaderInstance || isAfraid()))
			{
				if (this instanceof L2Summon)
					((L2Summon) this).setFollowStatus(false);
				getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
				return;
			}
		}

		// Apply Z distance for flying or swimming for correct timing calculations
		if ((isFlying() || isInsideZone(ZONE_WATER)) && !verticalMovementOnly)
			distance = Math.sqrt(distance * distance + dz * dz);

		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);
		m._xDestination = x;
		m._yDestination = y;
		m._zDestination = z; // this is what was requested from client

		// Calculate and set the heading of the L2Character
		m._heading = 0; // initial value for coordinate sync
		// Does not broke heading on vertical movements
		if (!verticalMovementOnly)
			setHeading(Util.calculateHeadingFrom(cos, sin));

		_log.debug("dist:" + distance + "speed:" + speed + " ttt:" + ticksToMove + " heading:" + getHeading());

		m._moveStartTime = GameTimeController.getGameTicks();

		// Set the L2Character _move object to MoveData object
		_move = m;

		// Add the L2Character to movingObjects of the GameTimeController
		GameTimeController.getInstance().registerMovingObject(this);

		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if (ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);
	}

	public boolean moveToNextRoutePoint()
	{
		if (!isOnGeodataPath())
		{
			// Cancel the move action
			_move = null;
			return false;
		}

		// Get the Move Speed of the L2Charcater
		float speed = getStat().getMoveSpeed();
		if (speed <= 0 || isMovementDisabled())
		{
			// Cancel the move action
			_move = null;
			return false;
		}

		MoveData md = _move;
		if (md == null)
			return false;

		// Create and Init a MoveData object
		MoveData m = new MoveData();

		// Update MoveData object
		m.onGeodataPathIndex = md.onGeodataPathIndex + 1; // next segment
		m.geoPath = md.geoPath;
		m.geoPathGtx = md.geoPathGtx;
		m.geoPathGty = md.geoPathGty;
		m.geoPathAccurateTx = md.geoPathAccurateTx;
		m.geoPathAccurateTy = md.geoPathAccurateTy;

		if (md.onGeodataPathIndex == md.geoPath.size() - 2)
		{
			m._xDestination = md.geoPathAccurateTx;
			m._yDestination = md.geoPathAccurateTy;
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		else
		{
			m._xDestination = md.geoPath.get(m.onGeodataPathIndex).getX();
			m._yDestination = md.geoPath.get(m.onGeodataPathIndex).getY();
			m._zDestination = md.geoPath.get(m.onGeodataPathIndex).getZ();
		}
		double dx = (m._xDestination - super.getX());
		double dy = (m._yDestination - super.getY());
		double distance = Math.sqrt(dx * dx + dy * dy);
		// Calculate and set the heading of the L2Character
		if (distance != 0)
			setHeading(Util.calculateHeadingFrom(getX(), getY(), m._xDestination, m._yDestination));

		// Caclulate the Nb of ticks between the current position and the destination
		// One tick added for rounding reasons
		int ticksToMove = 1 + (int) (GameTimeController.TICKS_PER_SECOND * distance / speed);

		m._heading = 0; // initial value for coordinate sync
		m._moveStartTime = GameTimeController.getGameTicks();

		_log.debug("time to target:" + ticksToMove);

		// Set the L2Character _move object to MoveData object
		_move = m;

		// Add the L2Character to movingObjects of the GameTimeController
		// The GameTimeController manage objects movement
		GameTimeController.getInstance().registerMovingObject(this);

		// Create a task to notify the AI that L2Character arrives at a check point of the movement
		if (ticksToMove * GameTimeController.MILLIS_IN_TICK > 3000)
			ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(CtrlEvent.EVT_ARRIVED_REVALIDATE), 2000);

		// Send a Server->Client packet MoveToLocation to the actor and all L2PcInstance in its _knownPlayers
		MoveToLocation msg = new MoveToLocation(this);
		broadcastPacket(msg);

		return true;
	}

	public boolean validateMovementHeading(int heading)
	{
		MoveData m = _move;

		if (m == null)
			return true;

		boolean result = true;
		if (m._heading != heading)
		{
			result = (m._heading == 0); // initial value or false
			m._heading = heading;
		}

		return result;
	}

	/**
	 * Return the squared distance between the current position of the L2Character and the given object.<BR>
	 * <BR>
	 *
	 * @param object
	 *            L2Object
	 * @return the squared distance
	 */
	public final double getDistanceSq(L2Object object)
	{
		return getDistanceSq(object.getX(), object.getY(), object.getZ());
	}

	/**
	 * Return the squared distance between the current position of the L2Character and the given x, y, z.<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param z
	 *            Z position of the target
	 * @return the squared distance
	 */
	public final double getDistanceSq(int x, int y, int z)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		return (dx * dx + dy * dy + dz * dz);
	}

	/**
	 * Return the squared plan distance between the current position of the L2Character and the given object.<BR>
	 * (check only x and y, not z)<BR>
	 * <BR>
	 *
	 * @param object
	 *            L2Object
	 * @return the squared plan distance
	 */
	public final double getPlanDistanceSq(L2Object object)
	{
		return getPlanDistanceSq(object.getX(), object.getY());
	}

	/**
	 * Return the squared plan distance between the current position of the L2Character and the given x, y, z.<BR>
	 * (check only x and y, not z)<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @return the squared plan distance
	 */
	public final double getPlanDistanceSq(int x, int y)
	{
		double dx = x - getX();
		double dy = y - getY();

		return (dx * dx + dy * dy);
	}

	/**
	 * Check if this object is inside the given radius around the given object. Warning: doesn't cover collision radius!<BR>
	 * <BR>
	 *
	 * @param object
	 *            the target
	 * @param radius
	 *            the radius around the target
	 * @param checkZ
	 *            should we check Z axis also
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(L2Object object, int radius, boolean checkZ, boolean strictCheck)
	{
		return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
	}

	/**
	 * Check if this object is inside the given plan radius around the given point. Warning: doesn't cover collision radius!<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param radius
	 *            the radius around the target
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck)
	{
		return isInsideRadius(x, y, 0, radius, false, strictCheck);
	}

	/**
	 * Check if this object is inside the given radius around the given point.<BR>
	 * <BR>
	 *
	 * @param x
	 *            X position of the target
	 * @param y
	 *            Y position of the target
	 * @param z
	 *            Z position of the target
	 * @param radius
	 *            the radius around the target
	 * @param checkZ
	 *            should we check Z axis also
	 * @param strictCheck
	 *            true if (distance < radius), false if (distance <= radius)
	 * @return true is the L2Character is inside the radius.
	 */
	public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck)
	{
		double dx = x - getX();
		double dy = y - getY();
		double dz = z - getZ();

		if (strictCheck)
		{
			if (checkZ)
				return (dx * dx + dy * dy + dz * dz) < radius * radius;

			return (dx * dx + dy * dy) < radius * radius;
		}

		if (checkZ)
			return (dx * dx + dy * dy + dz * dz) <= radius * radius;

		return (dx * dx + dy * dy) <= radius * radius;
	}

	/**
	 * Set _attacking corresponding to Attacking Body part to CHEST.<BR>
	 * <BR>
	 */
	public void setAttackingBodypart()
	{
		_attacking = Inventory.PAPERDOLL_CHEST;
	}

	/**
	 * @return True if arrows are available.
	 */
	protected boolean checkAndEquipArrows()
	{
		return true;
	}

	/**
	 * Add Exp and Sp to the L2Character.
	 *
	 * @param addToExp
	 *            An int value.
	 * @param addToSp
	 *            An int value.
	 */
	public void addExpAndSp(long addToExp, int addToSp)
	{
		// Dummy method (overridden by players and pets)
	}

	/**
	 * @return the active weapon instance (always equipped in the right hand).
	 */
	public abstract L2ItemInstance getActiveWeaponInstance();

	/**
	 * @return the active weapon item (always equipped in the right hand).
	 */
	public abstract L2Weapon getActiveWeaponItem();

	/**
	 * @return the secondary weapon instance (always equipped in the left hand).
	 */
	public abstract L2ItemInstance getSecondaryWeaponInstance();

	/**
	 * @return the secondary {@link L2Item} item (always equiped in the left hand).
	 */
	public abstract L2Item getSecondaryWeaponItem();

	/**
	 * Manage hit process (called by Hit Task).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send a Server->Client packet
	 * ActionFailed (if attacker is a L2PcInstance)</li> <li>If attack isn't aborted, send a message system (critical hit,
	 * missed...) to attacker/target if they are L2PcInstance</li> <li>If attack isn't aborted and hit isn't missed, reduce HP of
	 * the target and calculate reflection damage to reduce HP of attacker if necessary</li> <li>if attack isn't aborted and hit
	 * isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li><BR>
	 * <BR>
	 *
	 * @param target
	 *            The L2Character targeted
	 * @param damage
	 *            Nb of HP to reduce
	 * @param crit
	 *            True if hit is critical
	 * @param miss
	 *            True if hit is missed
	 * @param soulshot
	 *            True if SoulShot are charged
	 * @param shld
	 *            True if shield is efficient
	 */
	protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld)
	{
		// If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
		if (target == null || isAlikeDead())
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}

		if ((this instanceof L2Npc && target.isAlikeDead()) || target.isDead() || (!getKnownList().knowsObject(target) && !(this instanceof L2DoorInstance)))
		{
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);

			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (miss)
		{
			// Notify target AI
			if (target.hasAI())
				target.getAI().notifyEvent(CtrlEvent.EVT_EVADED, this);

			// ON_EVADED_HIT
			if (target.getChanceSkills() != null)
				target.getChanceSkills().onEvadedHit(this);

			if (target instanceof L2PcInstance)
				((L2PcInstance) target).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(this));
		}

		// If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance
		if (!isAttackAborted())
		{
			// Character will be petrified if attacking a raid that's more than 8 levels lower
			if (target.isRaid() && !NPCConfig.RAID_DISABLE_CURSE)
			{
				if (getLevel() > target.getLevel() + 8)
				{
					L2Skill skill = FrequentSkill.RAID_CURSE2.getSkill();
					if (skill != null)
					{
						// Send visual and skill effects. Caster is the victim.
						broadcastPacket(new MagicSkillUse(this, this, skill.getId(), skill.getLevel(), 300, 0));
						skill.getEffects(this, this);
					}

					damage = 0; // prevents messing up drop calculation
				}
			}

			// Send message about damage/crit or miss
			sendDamageMessage(target, damage, false, crit, miss);

			// If the target is a player, start AutoAttack
			if (target instanceof L2PcInstance)
				((L2PcInstance) target).getAI().clientStartAutoAttack();

			if (!miss && damage > 0)
			{
				L2Weapon weapon = getActiveWeaponItem();
				boolean isBow = (weapon != null && weapon.getItemType() == L2WeaponType.BOW);
				int reflectedDamage = 0;

				// Reflect damage system - do not reflect if weapon is a bow or target is invulnerable
				if (!isBow && !target.isInvul())
				{
					// quick fix for no drop from raid if boss attack high-level char with damage reflection
					if (!target.isRaid() || getActingPlayer() == null || getActingPlayer().getLevel() <= target.getLevel() + 8)
					{
						// Calculate reflection damage to reduce HP of attacker if necessary
						double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
						if (reflectPercent > 0)
						{
							reflectedDamage = (int) (reflectPercent / 100. * damage);

							// You can't kill someone from a reflect. If value > current HPs, make damages equal to current HP -
							// 1.
							int currentHp = (int) getCurrentHp();
							if (reflectedDamage >= currentHp)
								reflectedDamage = currentHp - 1;
						}
					}
				}

				// Reduce target HPs
				target.reduceCurrentHp(damage, this, null);

				// Reduce attacker HPs in case of a reflect.
				if (reflectedDamage > 0)
					reduceCurrentHp(reflectedDamage, target, true, false, null);

				if (!isBow) // Do not absorb if weapon is of type bow
				{
					// Absorb HP from the damage inflicted
					double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);

					if (absorbPercent > 0)
					{
						int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
						int absorbDamage = (int) (absorbPercent / 100. * damage);

						if (absorbDamage > maxCanAbsorb)
							absorbDamage = maxCanAbsorb; // Can't absord more than max hp

						if (absorbDamage > 0)
							setCurrentHp(getCurrentHp() + absorbDamage);
					}
				}

				// Notify AI with EVT_ATTACKED
				if (target.hasAI())
					target.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);

				getAI().clientStartAutoAttack();

				// Manage cast break of the target (calculating rate, sending message...)
				Formulas.calcCastBreak(target, damage);

				// Maybe launch chance skills on us
				if (_chanceSkills != null)
				{
					_chanceSkills.onHit(target, damage, false, crit);

					// Reflect triggers onHit
					if (reflectedDamage > 0)
						_chanceSkills.onHit(target, damage, true, false);
				}

				// Maybe launch chance skills on target
				if (target.getChanceSkills() != null)
					target.getChanceSkills().onHit(this, damage, true, crit);
			}

			// Launch weapon Special ability effect if available
			L2Weapon activeWeapon = getActiveWeaponItem();

			if (activeWeapon != null)
				activeWeapon.getSkillEffects(this, target, crit);

			return;
		}

		if (!isCastingNow() && !isCastingSimultaneouslyNow())
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
	}

	/**
	 * Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character.<BR>
	 * <BR>
	 */
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			// Abort the attack of the L2Character and send Server->Client ActionFailed packet
			abortAttack();

			if (this instanceof L2PcInstance)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED));
		}
	}

	/**
	 * Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character.<BR>
	 * <BR>
	 */
	public void breakCast()
	{
		// damage can only cancel magical skills
		if (isCastingNow() && canAbortCast() && getLastSkillCast() != null && getLastSkillCast().isMagic())
		{
			// Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
			abortCast();

			if (this instanceof L2PcInstance)
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTING_INTERRUPTED));
		}
	}

	/**
	 * Reduce the arrow number of the L2Character.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance</li><BR>
	 * <BR>
	 */
	protected void reduceArrowCount()
	{
		// default is to do nothing
	}

	/**
	 * Manage Forced attack (shift + select target).<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>If L2Character or target is in a town area, send a system message TARGET_IN_PEACEZONE a Server->Client packet
	 * ActionFailed</li> <li>If target is confused, send a Server->Client packet ActionFailed</li> <li>If L2Character is a
	 * L2ArtefactInstance, send a Server->Client packet ActionFailed</li> <li>Send a Server->Client packet MyTargetSelected to
	 * start attack and Notify AI with AI_INTENTION_ATTACK</li><BR>
	 * <BR>
	 *
	 * @param player
	 *            The L2PcInstance to attack
	 */
	@Override
	public void onForcedAttack(L2PcInstance player)
	{
		if (isInsidePeaceZone(player))
		{
			// If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE a Server->Client packet
			// ActionFailed
			player.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isInOlympiadMode() && player.getTarget() != null && player.getTarget() instanceof L2Playable)
		{
			L2PcInstance target = player.getTarget().getActingPlayer();
			if (target == null || (target.isInOlympiadMode() && (!player.isOlympiadStart() || player.getOlympiadGameId() != target.getOlympiadGameId())))
			{
				// if L2PcInstance is in Olympia and the match isn't already start, send a Server->Client packet ActionFailed
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		if (player.getTarget() != null && !player.getTarget().isAttackable() && !player.getAccessLevel().allowPeaceAttack())
		{
			// If target is not attackable, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (player.isConfused())
		{
			// If target is confused, send a Server->Client packet ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// GeoData Los Check or dz > 1000
		if (!GeoData.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(SystemMessageId.CANT_SEE_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Notify AI with AI_INTENTION_ATTACK
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
	}

	/**
	 * This method checks if the player given as argument can interact with the L2Npc.
	 *
	 * @param player
	 *            The player to test
	 * @return true if the player can interact with the L2Npc
	 */
	public boolean canInteract(L2PcInstance player)
	{
		// Can't interact while casting a spell.
		if (player.isCastingNow() || player.isCastingSimultaneouslyNow())
			return false;

		// Can't interact while died.
		if (player.isDead() || player.isFakeDeath())
			return false;

		// Can't interact sitted.
		if (player.isSitting())
			return false;

		// Can't interact in shop mode, or during a transaction or a request.
		if (player.getPrivateStoreType() != 0 || player.isProcessingTransaction())
			return false;

		// Can't interact if regular distance doesn't match.
		if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, true, false))
			return false;

		return true;
	}

	/**
	 * @param attacker
	 *            The attacker to test.
	 * @return True if inside peace zone.
	 */
	public boolean isInsidePeaceZone(L2PcInstance attacker)
	{
		return isInsidePeaceZone(attacker, this);
	}

	public static boolean isInsidePeaceZone(L2PcInstance attacker, L2Object target)
	{
		return (!attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((L2Object) attacker, target));
	}

	public static boolean isInsidePeaceZone(L2Object attacker, L2Object target)
	{
		if (target == null)
			return false;

		if (target instanceof L2Npc || attacker instanceof L2Npc)
			return false;

		if (PlayersConfig.KARMA_PLAYER_CAN_BE_KILLED_IN_PZ)
		{
			// allows red to be attacked and red to attack flagged players
			if (target.getActingPlayer() != null && target.getActingPlayer().getKarma() > 0)
				return false;

			if (attacker.getActingPlayer() != null && attacker.getActingPlayer().getKarma() > 0 && target.getActingPlayer() != null && target.getActingPlayer().getPvpFlag() > 0)
				return false;

			if (attacker instanceof L2Character && target instanceof L2Character)
				return (((L2Character) target).isInsideZone(ZONE_PEACE) || ((L2Character) attacker).isInsideZone(ZONE_PEACE));

			if (attacker instanceof L2Character)
				return (TownManager.getTown(target.getX(), target.getY(), target.getZ()) != null || ((L2Character) attacker).isInsideZone(ZONE_PEACE));
		}

		if (attacker instanceof L2Character && target instanceof L2Character)
			return (((L2Character) target).isInsideZone(ZONE_PEACE) || ((L2Character) attacker).isInsideZone(ZONE_PEACE));

		if (attacker instanceof L2Character)
			return (TownManager.getTown(target.getX(), target.getY(), target.getZ()) != null || ((L2Character) attacker).isInsideZone(ZONE_PEACE));

		return (TownManager.getTown(target.getX(), target.getY(), target.getZ()) != null || TownManager.getTown(attacker.getX(), attacker.getY(), attacker.getZ()) != null);
	}

	/**
	 * @return true if this character is inside an active grid.
	 */
	public boolean isInActiveRegion()
	{
		try
		{
			L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
			return ((region != null) && (region.isActive()));
		}
		catch (Exception e)
		{
			if (this instanceof L2PcInstance)
			{
				_log.warn("Player " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
				((L2PcInstance) this).sendMessage("Error with your coordinates! Please reboot your game fully!");
				((L2PcInstance) this).teleToLocation(80753, 145481, -3532, false); // Near Giran luxury shop
			}
			else
			{
				_log.warn("Object " + getName() + " at bad coords: (x: " + getX() + ", y: " + getY() + ", z: " + getZ() + ").");
				decayMe();
			}
			return false;
		}
	}

	/**
	 * @return True if the L2Character has a Party in progress.
	 */
	public boolean isInParty()
	{
		return false;
	}

	/**
	 * @return the L2Party object of the L2Character.
	 */
	public L2Party getParty()
	{
		return null;
	}

	/**
	 * @param target
	 *            The target to test.
	 * @param weapon
	 *            The wepaon to test.
	 * @return The Attack Speed of the L2Character (delay (in milliseconds) before next attack).
	 */
	public int calculateTimeBetweenAttacks(L2Character target, L2Weapon weapon)
	{
		double atkSpd = 0;
		if (weapon != null)
		{
			switch (weapon.getItemType())
			{
				case BOW:
					atkSpd = getStat().getPAtkSpd();
					return (int) (1500 * 345 / atkSpd);
				case DAGGER:
					atkSpd = getStat().getPAtkSpd();
					// atkSpd /= 1.15;
					break;
				default:
					atkSpd = getStat().getPAtkSpd();
			}
		}
		else
			atkSpd = getPAtkSpd();

		return Formulas.calcPAtkSpd(this, target, atkSpd);
	}

	public int calculateReuseTime(L2Character target, L2Weapon weapon)
	{
		if (weapon == null)
			return 0;

		// only bows should continue for now
		int reuse = weapon.getReuseDelay();
		if (reuse == 0)
			return 0;

		reuse *= getStat().getWeaponReuseModifier(target);
		double atkSpd = getStat().getPAtkSpd();
		switch (weapon.getItemType())
		{
			case BOW:
				return (int) (reuse * 345 / atkSpd);
			default:
				return (int) (reuse * 312 / atkSpd);
		}
	}

	/**
	 * @return True if the L2Character use a dual weapon.
	 */
	public boolean isUsingDualWeapon()
	{
		return false;
	}

	/**
	 * Add a skill to the L2Character _skills and its Func objects to the calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Replace oldSkill by newSkill or Add the newSkill</li> <li>If an old skill has been replaced, remove all its Func
	 * objects of L2Character calculator set</li> <li>Add Func objects of newSkill to the calculator set of the L2Character</li><BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li><BR>
	 * <BR>
	 *
	 * @param newSkill
	 *            The L2Skill to add to the L2Character
	 * @return The L2Skill replaced or null if just added a new L2Skill
	 */
	public L2Skill addSkill(L2Skill newSkill)
	{
		L2Skill oldSkill = null;

		if (newSkill != null)
		{
			// Replace oldSkill by newSkill or Add the newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);

			// If an old skill has been replaced, remove all its Func objects
			if (oldSkill != null)
			{
				// if skill came with another one, we should delete the other one too.
				if (oldSkill.triggerAnotherSkill())
					removeSkill(oldSkill.getTriggeredId(), true);

				removeStatsOwner(oldSkill);
			}
			// Add Func objects of newSkill to the calculator set of the L2Character
			addStatFuncs(newSkill.getStatFuncs(null, this));

			if (oldSkill != null && _chanceSkills != null)
				removeChanceSkill(oldSkill.getId());

			if (newSkill.isChance())
				addChanceTrigger(newSkill);
		}

		return oldSkill;
	}

	/**
	 * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills own by a L2Character are identified in <B>_skills</B><BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Remove the skill from the L2Character _skills</li> <li>Remove all its Func objects from the L2Character calculator set</li>
	 * <BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance : Save update in the character_skills table of the database</li><BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to remove from the L2Character
	 * @return The L2Skill removed
	 */
	public L2Skill removeSkill(L2Skill skill)
	{
		if (skill == null)
			return null;

		return removeSkill(skill.getId(), true);
	}

	public L2Skill removeSkill(L2Skill skill, boolean cancelEffect)
	{
		if (skill == null)
			return null;

		// Remove the skill from the L2Character _skills
		return removeSkill(skill.getId(), cancelEffect);
	}

	public L2Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}

	public L2Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove the skill from the L2Character _skills
		L2Skill oldSkill = _skills.remove(skillId);

		// Remove all its Func objects from the L2Character calculator set
		if (oldSkill != null)
		{
			// this is just a fail-safe againts buggers and gm dummies...
			if ((oldSkill.triggerAnotherSkill()) && oldSkill.getTriggeredId() > 0)
				removeSkill(oldSkill.getTriggeredId(), true);

			// Stop casting if this skill is used right now
			if (getLastSkillCast() != null && isCastingNow())
			{
				if (oldSkill.getId() == getLastSkillCast().getId())
					abortCast();
			}
			if (getLastSimultaneousSkillCast() != null && isCastingSimultaneouslyNow())
			{
				if (oldSkill.getId() == getLastSimultaneousSkillCast().getId())
					abortCast();
			}

			if (cancelEffect || oldSkill.isToggle())
			{
				removeStatsOwner(oldSkill);
				stopSkillEffects(oldSkill.getId());
			}

			if (oldSkill.isChance() && _chanceSkills != null)
				removeChanceSkill(oldSkill.getId());
		}

		return oldSkill;
	}

	public void removeChanceSkill(int id)
	{
		if (_chanceSkills == null)
			return;

		synchronized (_chanceSkills)
		{
			for (IChanceSkillTrigger trigger : _chanceSkills.keySet())
			{
				if (!(trigger instanceof L2Skill))
					continue;
				if (((L2Skill) trigger).getId() == id)
					_chanceSkills.remove(trigger);
			}
		}
	}

	public void addChanceTrigger(IChanceSkillTrigger trigger)
	{
		if (_chanceSkills == null)
		{
			synchronized (this)
			{
				if (_chanceSkills == null)
					_chanceSkills = new ChanceSkillList(this);
			}
		}
		_chanceSkills.put(trigger, trigger.getTriggeredChanceCondition());
	}

	public void removeChanceEffect(EffectChanceSkillTrigger effect)
	{
		if (_chanceSkills == null)
			return;
		_chanceSkills.remove(effect);
	}

	public void onStartChanceEffect()
	{
		if (_chanceSkills == null)
			return;

		_chanceSkills.onStart();
	}

	public void onActionTimeChanceEffect()
	{
		if (_chanceSkills == null)
			return;

		_chanceSkills.onActionTime();
	}

	public void onExitChanceEffect()
	{
		if (_chanceSkills == null)
			return;

		_chanceSkills.onExit();
	}

	/**
	 * @return A skill array fed with all skills that L2Character owns.
	 */
	public final L2Skill[] getAllSkills()
	{
		if (_skills == null)
			return new L2Skill[0];

		return _skills.values(new L2Skill[0]);
	}

	public ChanceSkillList getChanceSkills()
	{
		return _chanceSkills;
	}

	/**
	 * Return the level of a skill owned by the L2Character.
	 *
	 * @param skillId
	 *            The identifier of the L2Skill whose level must be returned
	 * @return The level of the L2Skill identified by skillId
	 */
	public int getSkillLevel(int skillId)
	{
		final L2Skill skill = getKnownSkill(skillId);
		if (skill == null)
			return -1;

		return skill.getLevel();
	}

	/**
	 * @param skillId
	 *            The identifier of the L2Skill to check the knowledge
	 * @return True if the skill is known by the L2Character.
	 */
	public final L2Skill getKnownSkill(int skillId)
	{
		if (_skills == null)
			return null;

		return _skills.get(skillId);
	}

	/**
	 * Return the number of skills of type(Buff, Debuff, HEAL_PERCENT, MANAHEAL_PERCENT) affecting this L2Character.<BR>
	 * <BR>
	 *
	 * @return The number of Buffs affecting this L2Character
	 */
	public int getBuffCount()
	{
		return _effects.getBuffCount();
	}

	public int getDanceCount()
	{
		return _effects.getDanceCount();
	}

	/**
	 * Manage the magic skill launching task (MP, HP, Item consummation...) and display the magic skill animation on client.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Send a Server->Client packet MagicSkillLaunched (to display magic skill animation) to all L2PcInstance of L2Charcater
	 * _knownPlayers</li> <li>Consumme MP, HP and Item if necessary</li> <li>Send a Server->Client packet StatusUpdate with MP
	 * modification to the L2PcInstance</li> <li>Launch the magic skill in order to calculate its effects</li> <li>If the skill
	 * type is PDAM, notify the AI of the target with AI_INTENTION_ATTACK</li> <li>Notify the AI of the L2Character with
	 * EVT_FINISH_CASTING</li><BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A magic skill casting MUST BE in progress</B></FONT><BR>
	 * <BR>
	 *
	 * @param mut
	 */
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		final L2Skill skill = mut.skill;
		L2Object[] targets = mut.targets;

		if (skill == null || targets == null)
		{
			abortCast();
			return;
		}

		if (targets.length == 0)
		{
			switch (skill.getTargetType())
			{
			// only AURA-type skills can be cast without target
				case TARGET_AURA:
				case TARGET_FRONT_AURA:
				case TARGET_BEHIND_AURA:
					break;
				default:
					abortCast();
					return;
			}
		}

		// Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
			escapeRange = skill.getEffectRange();
		else if (skill.getCastRange() < 0 && skill.getSkillRadius() > 80)
			escapeRange = skill.getSkillRadius();

		if (targets.length > 0 && escapeRange > 0)
		{
			int _skiprange = 0;
			int _skipgeo = 0;
			int _skippeace = 0;
			List<L2Character> targetList = new FastList<>(targets.length);
			for (L2Object target : targets)
			{
				if (target instanceof L2Character)
				{
					if (!Util.checkIfInRange(escapeRange, this, target, true))
					{
						_skiprange++;
						continue;
					}
					if (skill.getSkillRadius() > 0 && skill.isOffensive() && MainConfig.GEODATA > 0 && !GeoData.getInstance().canSeeTarget(this, target))
					{
						_skipgeo++;
						continue;
					}
					if (skill.isOffensive())
					{
						if (this instanceof L2PcInstance)
						{
							if (((L2Character) target).isInsidePeaceZone((L2PcInstance) this))
							{
								_skippeace++;
								continue;
							}
						}
						else
						{
							if (isInsidePeaceZone(this, target))
							{
								_skippeace++;
								continue;
							}
						}
					}
					targetList.add((L2Character) target);
				}
			}
			if (targetList.isEmpty())
			{
				if (this instanceof L2PcInstance)
				{
					if (_skiprange > 0)
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED));
					else if (_skipgeo > 0)
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
					else if (_skippeace > 0)
						sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
				}
				abortCast();
				return;
			}
			mut.targets = targetList.toArray(new L2Character[targetList.size()]);
		}

		// Ensure that a cast is in progress
		// Check if player is using fake death.
		// Potions can be used while faking death.
		if ((mut.simultaneously && !isCastingSimultaneouslyNow()) || (!mut.simultaneously && !isCastingNow()) || (isAlikeDead() && !skill.isPotion()))
		{
			// now cancels both, simultaneous and normal
			getAI().notifyEvent(CtrlEvent.EVT_CANCEL);
			return;
		}

		mut.phase = 2;
		if (mut.hitTime == 0)
			onMagicHitTimer(mut);
		else
			_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 400);
	}

	/*
	 * Runs in the end of skill casting
	 */
	public void onMagicHitTimer(MagicUseTask mut)
	{
		final L2Skill skill = mut.skill;
		final L2Object[] targets = mut.targets;

		if (skill == null || targets == null)
		{
			abortCast();
			return;
		}

		if (getFusionSkill() != null)
		{
			if (mut.simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			getFusionSkill().onCastAbort();
			notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}
		L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
		if (mog != null)
		{
			if (mut.simultaneously)
			{
				_skillCast2 = null;
				setIsCastingSimultaneouslyNow(false);
			}
			else
			{
				_skillCast = null;
				setIsCastingNow(false);
			}
			mog.exit();
			notifyQuestEventSkillFinished(skill, targets[0]);
			return;
		}

		try
		{
			// Go through targets table
			for (L2Object tgt : targets)
			{
				if (tgt instanceof L2Playable)
				{
					L2Character target = (L2Character) tgt;

					if (skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.FUSION || skill.getSkillType() == L2SkillType.SEED)
					{
						SystemMessage smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT);
						smsg.addSkillName(skill);
						target.sendPacket(smsg);
					}

					if (this instanceof L2PcInstance && target instanceof L2Summon)
						((L2Summon) target).updateAndBroadcastStatus(1);
				}
			}

			StatusUpdate su = new StatusUpdate(this);
			boolean isSendStatus = false;

			// Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other
			// L2PcInstance to inform
			double mpConsume = getStat().getMpConsume(skill);

			if (mpConsume > 0)
			{
				if (mpConsume > getCurrentMp())
				{
					sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
					abortCast();
					return;
				}

				getStatus().reduceMp(mpConsume);
				su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
				isSendStatus = true;
			}

			// Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other
			// L2PcInstance to inform
			if (skill.getHpConsume() > 0)
			{
				double consumeHp;

				consumeHp = calcStat(Stats.HP_CONSUME_RATE, skill.getHpConsume(), null, null);
				if (consumeHp + 1 >= getCurrentHp())
					consumeHp = getCurrentHp() - 1.0;

				getStatus().reduceHp(consumeHp, this, true);

				su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
				isSendStatus = true;
			}

			// Send a Server->Client packet StatusUpdate with MP modification to the L2PcInstance
			if (isSendStatus)
				sendPacket(su);

			if (this instanceof L2PcInstance)
			{
				int charges = ((L2PcInstance) this).getCharges();
				// check for charges
				if (skill.getMaxCharges() == 0 && charges < skill.getNumCharges())
				{
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
					sm.addSkillName(skill);
					sendPacket(sm);
					abortCast();
					return;
				}
				// generate charges if any
				if (skill.getNumCharges() > 0)
				{
					if (skill.getMaxCharges() > 0)
						((L2PcInstance) this).increaseCharges(skill.getNumCharges(), skill.getMaxCharges());
					else
						((L2PcInstance) this).decreaseCharges(skill.getNumCharges());
				}
			}

			// Launch the magic skill in order to calculate its effects
			callSkill(mut.skill, mut.targets);
		}
		catch (NullPointerException e)
		{
			_log.warn(e.getLocalizedMessage(), e);
		}

		mut.phase = 3;
		if (mut.hitTime == 0 || mut.coolTime == 0)
			onMagicFinalizer(mut);
		else
		{
			if (mut.simultaneously)
				_skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, mut.coolTime);
			else
				_skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, mut.coolTime);
		}
	}

	/*
	 * Runs after skill hitTime+coolTime
	 */
	public void onMagicFinalizer(MagicUseTask mut)
	{
		if (mut.simultaneously)
		{
			_skillCast2 = null;
			setIsCastingSimultaneouslyNow(false);
			return;
		}

		_skillCast = null;
		setIsCastingNow(false);
		_castInterruptTime = 0;

		final L2Skill skill = mut.skill;
		final L2Object target = mut.targets.length > 0 ? mut.targets[0] : null;

		// Attack target after skill use
		if (skill.nextActionIsAttack() && getTarget() instanceof L2Character && getTarget() != this && getTarget() == target && getTarget().isAttackable())
		{
			if (getAI() == null || getAI().getNextIntention() == null || getAI().getNextIntention().getCtrlIntention() != CtrlIntention.AI_INTENTION_MOVE_TO)
				getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
		}

		if (skill.isOffensive() && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK))
			getAI().clientStartAutoAttack();

		// Notify the AI of the L2Character with EVT_FINISH_CASTING
		getAI().notifyEvent(CtrlEvent.EVT_FINISH_CASTING);

		notifyQuestEventSkillFinished(skill, target);

		/*
		 * If character is a player, then wipe their current cast state and check if a skill is queued. If there is a queued
		 * skill, launch it and wipe the queue.
		 */
		if (this instanceof L2PcInstance)
		{
			L2PcInstance currPlayer = (L2PcInstance) this;
			SkillDat queuedSkill = currPlayer.getQueuedSkill();

			currPlayer.setCurrentSkill(null, false, false);

			if (queuedSkill != null)
			{
				currPlayer.setQueuedSkill(null, false, false);
				ThreadPoolManager.getInstance().executeTask(new QueuedMagicUseTask(currPlayer, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
			}
		}
	}

	// Quest event ON_SPELL_FINISHED
	protected void notifyQuestEventSkillFinished(L2Skill skill, L2Object target)
	{
	}

	public Map<Integer, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}

	/**
	 * Enable a skill (remove it from _disabledSkills of the L2Character).<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their skillId in <B>_disabledSkills</B> of the L2Character <BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to enable
	 */
	public void enableSkill(L2Skill skill)
	{
		if (skill == null || _disabledSkills == null)
			return;

		_disabledSkills.remove(Integer.valueOf(skill.getReuseHashCode()));
	}

	/**
	 * Disable this skill id for the duration of the delay in milliseconds.
	 *
	 * @param skill
	 * @param delay
	 *            (seconds * 1000)
	 */
	public void disableSkill(L2Skill skill, long delay)
	{
		if (skill == null)
			return;

		if (_disabledSkills == null)
			_disabledSkills = Collections.synchronizedMap(new FastMap<Integer, Long>());

		_disabledSkills.put(Integer.valueOf(skill.getReuseHashCode()), delay > 10 ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}

	/**
	 * Check if a skill is disabled.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their reuse hashcodes in <B>_disabledSkills</B> of the L2Character <BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to check
	 * @return true if the skill is currently disabled.
	 */
	public boolean isSkillDisabled(L2Skill skill)
	{
		if (skill == null)
			return true;

		return isSkillDisabled(skill.getReuseHashCode());
	}

	/**
	 * Check if a skill is disabled.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * All skills disabled are identified by their reuse hashcodes in <B>_disabledSkills</B> of the L2Character <BR>
	 * <BR>
	 *
	 * @param reuseHashcode
	 *            The reuse hashcode of the skillId/level to check
	 * @return true if the skill is currently disabled.
	 */
	public boolean isSkillDisabled(int reuseHashcode)
	{
		if (isAllSkillsDisabled())
			return true;

		if (_disabledSkills == null)
			return false;

		final Long timeStamp = _disabledSkills.get(Integer.valueOf(reuseHashcode));
		if (timeStamp == null)
			return false;

		if (timeStamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(Integer.valueOf(reuseHashcode));
			return false;
		}

		return true;
	}

	/**
	 * Disable all skills (set _allSkillsDisabled to True).<BR>
	 * <BR>
	 */
	public void disableAllSkills()
	{
		_log.debug("All skills disabled for {}", toString());
		_allSkillsDisabled = true;
	}

	/**
	 * Enable all skills (set _allSkillsDisabled to False).<BR>
	 * <BR>
	 */
	public void enableAllSkills()
	{
		_log.debug("All skills enabled for {}", toString());
		_allSkillsDisabled = false;
	}

	/**
	 * Launch the magic skill and calculate its effects on each target contained in the targets table.<BR>
	 * <BR>
	 *
	 * @param skill
	 *            The L2Skill to use
	 * @param targets
	 *            The table of L2Object targets
	 */
	public void callSkill(L2Skill skill, L2Object[] targets)
	{
		try
		{
			// Get the skill handler corresponding to the skill type (PDAM, MDAM, SWEEP...) started in gameserver
			ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
			L2Weapon activeWeapon = getActiveWeaponItem();

			// Check if the toggle skill effects are already in progress on the L2Character
			if (skill.isToggle() && getFirstEffect(skill.getId()) != null)
				return;

			// Initial checks
			for (L2Object trg : targets)
			{
				if (trg instanceof L2Character)
				{
					// Set some values inside target's instance for later use
					L2Character target = (L2Character) trg;

					if (!NPCConfig.RAID_DISABLE_CURSE)
					{
						// Raidboss curse.
						L2Character targetsAttackTarget = null;
						L2Character targetsCastTarget = null;

						if (target.hasAI())
						{
							targetsAttackTarget = target.getAI().getAttackTarget();
							targetsCastTarget = target.getAI().getCastTarget();
						}

						if ((target.isRaid() && getLevel() > target.getLevel() + 8) || (!skill.isOffensive() && targetsAttackTarget != null && targetsAttackTarget.isRaid() && targetsAttackTarget.getAttackByList().contains(target) && getLevel() > targetsAttackTarget.getLevel() + 8) || (!skill.isOffensive() && targetsCastTarget != null && targetsCastTarget.isRaid() && targetsCastTarget.getAttackByList().contains(target) && getLevel() > targetsCastTarget.getLevel() + 8))
						{
							L2Skill curse = FrequentSkill.RAID_CURSE.getSkill();
							if (curse != null)
							{
								// Send visual and skill effects. Caster is the victim.
								broadcastPacket(new MagicSkillUse(this, this, curse.getId(), curse.getLevel(), 300, 0));
								curse.getEffects(this, this);
							}
							return;
						}
					}

					// Check if over-hit is possible
					if (skill.isOverhit())
					{
						if (target instanceof L2Attackable)
							((L2Attackable) target).overhitEnabled(true);
					}

					// crafting does not trigger any chance skills
					switch (skill.getSkillType())
					{
						case COMMON_CRAFT:
						case DWARVEN_CRAFT:
							break;
						default:
							// Launch weapon Special ability skill effect if available
							if (activeWeapon != null && !target.isDead())
							{
								if (activeWeapon.getSkillEffects(this, target, skill).length > 0 && this instanceof L2PcInstance)
								{
									SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED);
									sm.addSkillName(skill);
									sendPacket(sm);
								}
							}

							// Maybe launch chance skills on us
							if (_chanceSkills != null)
								_chanceSkills.onSkillHit(target, false, skill.isMagic(), skill.isOffensive(), skill.getElement());
							// Maybe launch chance skills on target
							if (target.getChanceSkills() != null)
								target.getChanceSkills().onSkillHit(this, true, skill.isMagic(), skill.isOffensive(), skill.getElement());
					}
				}
			}

			// Launch the magic skill and calculate its effects
			if (handler != null)
				handler.useSkill(this, skill, targets);
			else
				skill.useSkill(this, targets);

			L2PcInstance player = getActingPlayer();
			if (player != null)
			{
				for (L2Object target : targets)
				{
					// EVT_ATTACKED and PvPStatus
					if (target instanceof L2Character)
					{
						if (skill.isOffensive())
						{
							if (target instanceof L2Playable)
							{
								// Signets are a special case, casted on target_self but don't harm self
								if (skill.getSkillType() != L2SkillType.SIGNET && skill.getSkillType() != L2SkillType.SIGNET_CASTTIME)
								{
									((L2Character) target).getAI().clientStartAutoAttack();

									// attack of the own pet does not flag player
									if (player.getPet() != target)
										player.updatePvPStatus((L2Character) target);
								}
							}
							else if (target instanceof L2Attackable)
							{
								switch (skill.getId())
								{
									case 51: // Lure
									case 511: // Temptation
										break;
									default:
										// add attacker into list
										((L2Character) target).addAttackerToAttackByList(this);
								}
							}
							// notify target AI about the attack
							if (((L2Character) target).hasAI())
							{
								switch (skill.getSkillType())
								{
									case AGGREDUCE:
									case AGGREDUCE_CHAR:
									case AGGREMOVE:
										break;
									default:
										((L2Character) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
								}
							}
						}
						else
						{
							if (target instanceof L2PcInstance)
							{
								// Casting non offensive skill on player with pvp flag set or with karma
								if (!(target.equals(this) || target.equals(player)) && (((L2PcInstance) target).getPvpFlag() > 0 || ((L2PcInstance) target).getKarma() > 0))
									player.updatePvPStatus();
							}
							else if (target instanceof L2Attackable)
							{
								switch (skill.getSkillType())
								{
									case SUMMON:
									case BEAST_FEED:
									case UNLOCK:
									case UNLOCK_SPECIAL:
									case DELUXE_KEY_UNLOCK:
										break;
									default:
										player.updatePvPStatus();
								}
							}
						}
					}
				}

				// Mobs in range 1000 see spell
				Collection<L2Object> objs = player.getKnownList().getKnownObjects().values();
				for (L2Object spMob : objs)
				{
					if (spMob instanceof L2Npc)
					{
						L2Npc npcMob = (L2Npc) spMob;

						if ((npcMob.isInsideRadius(player, 1000, true, true)) && (npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE) != null))
							for (Quest quest : npcMob.getTemplate().getEventQuests(Quest.QuestEventType.ON_SKILL_SEE))
								quest.notifySkillSee(npcMob, player, skill, targets, this instanceof L2Summon);
					}
				}
			}

			// Notify AI
			if (skill.isOffensive())
			{
				switch (skill.getSkillType())
				{
					case AGGREDUCE:
					case AGGREDUCE_CHAR:
					case AGGREMOVE:
						break;
					default:
						for (L2Object target : targets)
						{
							// notify target AI about the attack
							if (target instanceof L2Character && ((L2Character) target).hasAI())
								((L2Character) target).getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, this);
						}
						break;
				}
			}
		}
		catch (Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": callSkill() failed.", e);
		}
	}

	/**
	 * @param target
	 *            Target to check.
	 * @return True if the L2Character is behind the target and can't be seen.
	 */
	public boolean isBehind(L2Object target)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 60;

		if (target == null)
			return false;

		if (target instanceof L2Character)
		{
			L2Character target1 = (L2Character) target;
			angleChar = Util.calculateAngleFrom(this, target1);
			angleTarget = Util.convertHeadingToDegree(target1.getHeading());
			angleDiff = angleChar - angleTarget;

			if (angleDiff <= -360 + maxAngleDiff)
				angleDiff += 360;

			if (angleDiff >= 360 - maxAngleDiff)
				angleDiff -= 360;

			if (Math.abs(angleDiff) <= maxAngleDiff)
			{
				_log.debug("Char " + getName() + " is behind " + target.getName());

				return true;
			}
		}
		else
		{
			_log.debug("isBehindTarget's target not an Character.");
		}
		return false;
	}

	public boolean isBehindTarget()
	{
		return isBehind(getTarget());
	}

	/**
	 * @param target
	 *            Target to check.
	 * @return True if the target is facing the L2Character.
	 */
	public boolean isInFrontOf(L2Character target)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff = 60;
		if (target == null)
			return false;

		angleTarget = Util.calculateAngleFrom(target, this);
		angleChar = Util.convertHeadingToDegree(target.getHeading());
		angleDiff = angleChar - angleTarget;

		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;

		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;

		if (Math.abs(angleDiff) <= maxAngleDiff)
			return true;

		return false;
	}

	/**
	 * @param target
	 *            Target to check.
	 * @param maxAngle
	 *            The angle to check.
	 * @return true if target is in front of L2Character (shield def etc)
	 */
	public boolean isFacing(L2Object target, int maxAngle)
	{
		double angleChar, angleTarget, angleDiff, maxAngleDiff;
		if (target == null)
			return false;

		maxAngleDiff = maxAngle / 2;
		angleTarget = Util.calculateAngleFrom(this, target);
		angleChar = Util.convertHeadingToDegree(getHeading());
		angleDiff = angleChar - angleTarget;

		if (angleDiff <= -360 + maxAngleDiff)
			angleDiff += 360;

		if (angleDiff >= 360 - maxAngleDiff)
			angleDiff -= 360;

		if (Math.abs(angleDiff) <= maxAngleDiff)
			return true;

		return false;
	}

	public boolean isInFrontOfTarget()
	{
		L2Object target = getTarget();
		if (target instanceof L2Character)
			return isInFrontOf((L2Character) target);

		return false;
	}

	/**
	 * @return the level modifier (overriden in summons).
	 */
	public double getLevelMod()
	{
		return 1;
	}

	public final void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}

	/**
	 * Sets _isCastingNow to true and _castInterruptTime is calculated from end time (ticks)
	 *
	 * @param newSkillCastEndTick
	 */
	public final void forceIsCasting(int newSkillCastEndTick)
	{
		setIsCastingNow(true);
		// for interrupt -400 ms
		_castInterruptTime = newSkillCastEndTick - 4;
	}

	/**
	 * @param target
	 *            Target to check.
	 * @return a Random Damage in function of the weapon.
	 */
	public final int getRandomDamage(L2Character target)
	{
		L2Weapon weaponItem = getActiveWeaponItem();

		if (weaponItem == null)
			return 5 + (int) Math.sqrt(getLevel());

		return weaponItem.getRandomDamage();
	}

	@Override
	public String toString()
	{
		return "mob " + getObjectId();
	}

	public int getAttackEndTime()
	{
		return _attackEndTime;
	}

	/**
	 * @return the level of the L2Character.
	 */
	public abstract int getLevel();

	// =========================================================
	// Stat - NEED TO REMOVE ONCE L2CHARSTAT IS COMPLETE
	// Property - Public
	public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill)
	{
		return getStat().calcStat(stat, init, target, skill);
	}

	// Property - Public
	public int getCON()
	{
		return getStat().getCON();
	}

	public int getDEX()
	{
		return getStat().getDEX();
	}

	public int getINT()
	{
		return getStat().getINT();
	}

	public int getMEN()
	{
		return getStat().getMEN();
	}

	public int getSTR()
	{
		return getStat().getSTR();
	}

	public int getWIT()
	{
		return getStat().getWIT();
	}

	public int getAccuracy()
	{
		return getStat().getAccuracy();
	}

	public final float getAttackSpeedMultiplier()
	{
		return getStat().getAttackSpeedMultiplier();
	}

	public int getCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getCriticalHit(target, skill);
	}

	public int getEvasionRate(L2Character target)
	{
		return getStat().getEvasionRate(target);
	}

	public int getMDef(L2Character target, L2Skill skill)
	{
		return getStat().getMDef(target, skill);
	}

	public int getPDef(L2Character target)
	{
		return getStat().getPDef(target);
	}

	public final int getShldDef()
	{
		return getStat().getShldDef();
	}

	public final int getPhysicalAttackRange()
	{
		return getStat().getPhysicalAttackRange();
	}

	public final int getMagicalAttackRange(L2Skill skill)
	{
		return getStat().getMagicalAttackRange(skill);
	}

	public int getPAtk(L2Character target)
	{
		return getStat().getPAtk(target);
	}

	public int getPAtkSpd()
	{
		return getStat().getPAtkSpd();
	}

	public int getMAtk(L2Character target, L2Skill skill)
	{
		return getStat().getMAtk(target, skill);
	}

	public int getMAtkSpd()
	{
		return getStat().getMAtkSpd();
	}

	public final int getMCriticalHit(L2Character target, L2Skill skill)
	{
		return getStat().getMCriticalHit(target, skill);
	}

	public int getMaxMp()
	{
		return getStat().getMaxMp();
	}

	public int getMaxHp()
	{
		return getStat().getMaxHp();
	}

	public final int getMaxCp()
	{
		return getStat().getMaxCp();
	}

	public double getMReuseRate(L2Skill skill)
	{
		return getStat().getMReuseRate(skill);
	}

	public float getMovementSpeedMultiplier()
	{
		return getStat().getMovementSpeedMultiplier();
	}

	public double getPAtkAnimals(L2Character target)
	{
		return getStat().getPAtkAnimals(target);
	}

	public double getPAtkDragons(L2Character target)
	{
		return getStat().getPAtkDragons(target);
	}

	public double getPAtkInsects(L2Character target)
	{
		return getStat().getPAtkInsects(target);
	}

	public double getPAtkMonsters(L2Character target)
	{
		return getStat().getPAtkMonsters(target);
	}

	public double getPAtkPlants(L2Character target)
	{
		return getStat().getPAtkPlants(target);
	}

	public double getPAtkGiants(L2Character target)
	{
		return getStat().getPAtkGiants(target);
	}

	public double getPAtkMagicCreatures(L2Character target)
	{
		return getStat().getPAtkMagicCreatures(target);
	}

	public double getPDefAnimals(L2Character target)
	{
		return getStat().getPDefAnimals(target);
	}

	public double getPDefDragons(L2Character target)
	{
		return getStat().getPDefDragons(target);
	}

	public double getPDefInsects(L2Character target)
	{
		return getStat().getPDefInsects(target);
	}

	public double getPDefMonsters(L2Character target)
	{
		return getStat().getPDefMonsters(target);
	}

	public double getPDefPlants(L2Character target)
	{
		return getStat().getPDefPlants(target);
	}

	public double getPDefGiants(L2Character target)
	{
		return getStat().getPDefGiants(target);
	}

	public double getPDefMagicCreatures(L2Character target)
	{
		return getStat().getPDefMagicCreatures(target);
	}

	public int getRunSpeed()
	{
		return getStat().getRunSpeed();
	}

	public final int getWalkSpeed()
	{
		return getStat().getWalkSpeed();
	}

	// =========================================================
	// Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
	// Method - Public
	public void addStatusListener(L2Character object)
	{
		getStatus().addStatusListener(object);
	}

	public void reduceCurrentHp(double i, L2Character attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, true, false, skill);
	}

	public void reduceCurrentHpByDOT(double i, L2Character attacker, L2Skill skill)
	{
		reduceCurrentHp(i, attacker, !skill.isToggle(), true, skill);
	}

	public void reduceCurrentHp(double i, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (NPCConfig.CHAMPION_ENABLE && isChampion() && NPCConfig.CHAMPION_HP != 0)
			getStatus().reduceHp(i / NPCConfig.CHAMPION_HP, attacker, awake, isDOT, false);
		else
			getStatus().reduceHp(i, attacker, awake, isDOT, false);
	}

	public void reduceCurrentMp(double i)
	{
		getStatus().reduceMp(i);
	}

	public void removeStatusListener(L2Character object)
	{
		getStatus().removeStatusListener(object);
	}

	protected void stopHpMpRegeneration()
	{
		getStatus().stopHpMpRegeneration();
	}

	// Property - Public
	public final double getCurrentCp()
	{
		return getStatus().getCurrentCp();
	}

	public final void setCurrentCp(Double newCp)
	{
		setCurrentCp((double) newCp);
	}

	public final void setCurrentCp(double newCp)
	{
		getStatus().setCurrentCp(newCp);
	}

	public final double getCurrentHp()
	{
		return getStatus().getCurrentHp();
	}

	public final void setCurrentHp(double newHp)
	{
		getStatus().setCurrentHp(newHp);
	}

	public final void setCurrentHpMp(double newHp, double newMp)
	{
		getStatus().setCurrentHpMp(newHp, newMp);
	}

	public final double getCurrentMp()
	{
		return getStatus().getCurrentMp();
	}

	public final void setCurrentMp(Double newMp)
	{
		setCurrentMp((double) newMp);
	}

	public final void setCurrentMp(double newMp)
	{
		getStatus().setCurrentMp(newMp);
	}

	// =========================================================

	public void setAiClass(String aiClass)
	{
		_aiClass = aiClass;
	}

	public String getAiClass()
	{
		return _aiClass;
	}

	public void setChampion(boolean champ)
	{
		_champion = champ;
	}

	public boolean isChampion()
	{
		return _champion;
	}

	/**
	 * Send system message about damage.<BR>
	 * <BR>
	 * <B><U> Overriden in </U> :</B><BR>
	 * <BR>
	 * <li>L2PcInstance <li>L2SummonInstance <li>L2PetInstance</li><BR>
	 * <BR>
	 *
	 * @param target
	 * @param damage
	 * @param mcrit
	 * @param pcrit
	 * @param miss
	 */
	public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
	}

	public FusionSkill getFusionSkill()
	{
		return _fusionSkill;
	}

	public void setFusionSkill(FusionSkill fb)
	{
		_fusionSkill = fb;
	}

	public int getAttackElementValue(byte attackAttribute)
	{
		return getStat().getAttackElementValue(attackAttribute);
	}

	public int getDefenseElementValue(byte defenseAttribute)
	{
		return getStat().getDefenseElementValue(defenseAttribute);
	}

	/**
	 * Check if target is affected with special buff
	 *
	 * @see CharEffectList#isAffected(int)
	 * @param flag
	 *            int
	 * @return boolean
	 */
	public boolean isAffected(int flag)
	{
		return _effects.isAffected(flag);
	}

	/**
	 * Check player max buff count
	 *
	 * @return max buff count
	 */
	public int getMaxBuffCount()
	{
		return PlayersConfig.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
	}

	/**
	 * @return a multiplier based on weapon random damage.
	 */
	public final double getRandomDamageMultiplier()
	{
		L2Weapon activeWeapon = getActiveWeaponItem();
		int random;

		if (activeWeapon != null)
			random = activeWeapon.getRandomDamage();
		else
			random = 5 + (int) Math.sqrt(getLevel());

		return (1 + ((double) Rnd.get(0 - random, random) / 100));
	}

	public void disableCoreAI(boolean val)
	{
		_AIdisabled = val;
	}

	public boolean isCoreAIDisabled()
	{
		return _AIdisabled;
	}

	/** Task for potion and herb queue */
	private static class UsePotionTask implements Runnable
	{
		private final L2Character _activeChar;
		private final L2Skill _skill;

		UsePotionTask(L2Character activeChar, L2Skill skill)
		{
			_activeChar = activeChar;
			_skill = skill;
		}

		@Override
		public void run()
		{
			try
			{
				_activeChar.doSimultaneousCast(_skill);
			}
			catch (Exception e)
			{
				_log.warn(e.getLocalizedMessage(), e);
			}
		}
	}

	private int _PremiumService;

	public void setPremiumService(int PS)
	{
		_PremiumService = PS;
	}

	public int getPremiumService()
	{
		return _PremiumService;
	}
}
