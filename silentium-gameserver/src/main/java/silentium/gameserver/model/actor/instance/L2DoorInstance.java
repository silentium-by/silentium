/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.ai.CharacterAI;
import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.ai.DoorAI;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.knownlist.DoorKnownList;
import silentium.gameserver.model.actor.stat.DoorStat;
import silentium.gameserver.model.actor.status.DoorStatus;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.model.entity.ClanHall;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.ConfirmDlg;
import silentium.gameserver.network.serverpackets.DoorInfo;
import silentium.gameserver.network.serverpackets.DoorStatusUpdate;
import silentium.gameserver.network.serverpackets.MyTargetSelected;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.network.serverpackets.ValidateLocation;
import silentium.gameserver.templates.chars.L2CharTemplate;
import silentium.gameserver.templates.item.L2Weapon;

public class L2DoorInstance extends L2Character
{
	protected static final Logger log = LoggerFactory.getLogger(L2DoorInstance.class.getName());

	/** The castle index in the array of L2Castle this L2Npc belongs to */
	private int _castleIndex = -2;
	private int _mapRegion = -1;

	// when door is closed, the dimensions are
	private int _rangeXMin = 0;
	private int _rangeYMin = 0;
	private int _rangeZMin = 0;
	private int _rangeXMax = 0;
	private int _rangeYMax = 0;
	private int _rangeZMax = 0;

	// these variables assist in see-through calculation only
	private int _A = 0;
	private int _B = 0;
	private int _C = 0;
	private int _D = 0;

	protected final int _doorId;
	protected final String _name;
	private boolean _open;
	private final boolean _unlockable;
	private boolean _isWall = false; // False by default

	private ClanHall _clanHall;

	protected int _autoActionDelay = -1;
	private ScheduledFuture<?> _autoActionTask;

	/** This class may be created only by L2Character and only for AI */
	public class AIAccessor extends L2Character.AIAccessor
	{
		protected AIAccessor()
		{
		}

		@Override
		public L2DoorInstance getActor()
		{
			return L2DoorInstance.this;
		}

		@Override
		public void moveTo(int x, int y, int z, int offset)
		{
		}

		@Override
		public void moveTo(int x, int y, int z)
		{
		}

		@Override
		public void stopMove(L2CharPosition pos)
		{
		}

		@Override
		public void doAttack(L2Character target)
		{
		}

		@Override
		public void doCast(L2Skill skill)
		{
		}
	}

	@Override
	public CharacterAI getAI()
	{
		CharacterAI ai = _ai; // copy handle
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new DoorAI(new AIAccessor());
				return _ai;
			}
		}
		return ai;
	}

	class CloseTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				onClose();
			}
			catch (Throwable e)
			{
				log.error("", e);
			}
		}
	}

	/**
	 * Manages the auto open and closing of a door.
	 */
	class AutoOpenClose implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				String doorAction;

				if (!getOpen())
				{
					doorAction = "opened";
					openMe();
				}
				else
				{
					doorAction = "closed";
					closeMe();
				}

				log.debug("Auto " + doorAction + " door ID " + _doorId + " (" + _name + ") for " + (_autoActionDelay / 60000) + " minute(s).");
			}
			catch (Exception e)
			{
				log.warn("Could not auto open/close door ID " + _doorId + " (" + _name + ")");
			}
		}
	}

	public L2DoorInstance(int objectId, L2CharTemplate template, int doorId, String name, boolean unlockable)
	{
		super(objectId, template);
		_doorId = doorId;
		_name = name;
		_unlockable = unlockable;
	}

	@Override
	public void initKnownList()
	{
		setKnownList(new DoorKnownList(this));
	}

	@Override
	public final DoorKnownList getKnownList()
	{
		return (DoorKnownList) super.getKnownList();
	}

	@Override
	public void initCharStat()
	{
		setStat(new DoorStat(this));
	}

	@Override
	public final DoorStat getStat()
	{
		return (DoorStat) super.getStat();
	}

	@Override
	public void initCharStatus()
	{
		setStatus(new DoorStatus(this));
	}

	@Override
	public final DoorStatus getStatus()
	{
		return (DoorStatus) super.getStatus();
	}

	public final boolean isUnlockable()
	{
		return _unlockable;
	}

	@Override
	public final int getLevel()
	{
		return 1;
	}

	/**
	 * @return Returns the doorId.
	 */
	public int getDoorId()
	{
		return _doorId;
	}

	/**
	 * @return Returns the open.
	 */
	public boolean getOpen()
	{
		return _open;
	}

	/**
	 * @param open
	 *            The open to set.
	 */
	public void setOpen(boolean open)
	{
		_open = open;
	}

	/**
	 * Sets the delay for automatic opening/closing of this door instance.<BR>
	 * <B>Note:</B> A value of -1 cancels the auto open/close task.
	 * 
	 * @param actionDelay
	 *            Delay in milliseconds.
	 */
	public void setAutoActionDelay(int actionDelay)
	{
		if (_autoActionDelay == actionDelay)
			return;

		if (actionDelay > -1)
		{
			AutoOpenClose ao = new AutoOpenClose();
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(ao, actionDelay, actionDelay);
		}
		else
		{
			if (_autoActionTask != null)
				_autoActionTask.cancel(false);
		}

		_autoActionDelay = actionDelay;
	}

	public int getDamage()
	{
		int dmg = 6 - (int) Math.ceil(getCurrentHp() / getMaxHp() * 6);
		if (dmg > 6)
			return 6;
		if (dmg < 0)
			return 0;
		return dmg;
	}

	public final Castle getCastle()
	{
		if (_castleIndex < 0)
			_castleIndex = CastleManager.getInstance().getCastleIndex(this);
		if (_castleIndex < 0)
			return null;
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}

	public void setClanHall(ClanHall clanhall)
	{
		_clanHall = clanhall;
	}

	public ClanHall getClanHall()
	{
		return _clanHall;
	}

	public boolean isEnemy()
	{
		if (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress())
			return true;
		return false;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		if (isUnlockable())
			return true;

		// Doors can`t be attacked by NPCs
		if (!(attacker instanceof L2Playable))
			return false;

		// Attackable during siege by attacker only
		boolean isCastle = (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());
		L2PcInstance actingPlayer = attacker.getActingPlayer();

		if (isCastle)
		{
			L2Clan clan = actingPlayer.getClan();
			if (clan != null && clan.getClanId() == getCastle().getOwnerId())
				return false;
		}

		return isCastle;
	}

	public boolean isAttackable(L2Character attacker)
	{
		return isAutoAttackable(attacker);
	}

	@Override
	public void updateAbnormalEffect()
	{
	}

	public int getDistanceToWatchObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
			return 0;

		return 6000;
	}

	/**
	 * @param object
	 *            The object to check on. If L2PcInstance : 9000, otherwise 0.
	 * @return The distance after which the object must be remove from _knownObject according to the type of the object.
	 */
	public int getDistanceToForgetObject(L2Object object)
	{
		if (!(object instanceof L2PcInstance))
			return 0;

		return 9000;
	}

	/**
	 * Return null.<BR>
	 * <BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

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

			// Send a Server->Client packet MyTargetSelected to the L2PcInstance player
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));

			player.sendPacket(new DoorStatusUpdate(this));

			// Send a Server->Client packet ValidateLocation to correct the L2Npc position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if (isAutoAttackable(player))
			{
				if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			}
			else if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false))
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			else if (player.getClan() != null && getClanHall() != null && player.getClanId() == getClanHall().getOwnerId())
			{
				player.gatesRequest(this);
				if (!getOpen())
					player.sendPacket(new ConfirmDlg(1140));
				else
					player.sendPacket(new ConfirmDlg(1141));

				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
				// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
				player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	@Override
	public void onActionShift(L2GameClient client)
	{
		L2PcInstance player = client.getActiveChar();
		if (player == null)
			return;

		if (player.getAccessLevel().isGm())
		{
			player.setTarget(this);
			player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));

			if (isAutoAttackable(player))
				player.sendPacket(new DoorStatusUpdate(this));

			NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile(StaticHtmPath.AdminHtmPath + "infos/doorinfo.htm", player);

			html.replace("%class%", getClass().getSimpleName());
			html.replace("%hp%", String.valueOf((int) getCurrentHp()));
			html.replace("%hpmax%", String.valueOf(getMaxHp()));
			html.replace("%objid%", String.valueOf(getObjectId()));
			html.replace("%doorid%", String.valueOf(getDoorId()));

			html.replace("%minx%", String.valueOf(getXMin()));
			html.replace("%miny%", String.valueOf(getYMin()));
			html.replace("%minz%", String.valueOf(getZMin()));

			html.replace("%maxx%", String.valueOf(getXMax()));
			html.replace("%maxy%", String.valueOf(getYMax()));
			html.replace("%maxz%", String.valueOf(getZMax()));
			html.replace("%unlock%", isUnlockable() ? "<font color=00FF00>YES<font>" : "<font color=FF0000>NO</font>");
			html.replace("%isWall%", isWall() ? "<font color=00FF00>YES<font>" : "<font color=FF0000>NO</font>");

			player.sendPacket(html);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void broadcastStatusUpdate()
	{
		Collection<L2PcInstance> knownPlayers = getKnownList().getKnownPlayers().values();
		if (knownPlayers == null || knownPlayers.isEmpty())
			return;

		DoorStatusUpdate su = new DoorStatusUpdate(this);
		for (L2PcInstance player : knownPlayers)
			player.sendPacket(su);
	}

	public void onOpen()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new CloseTask(), 60000);
	}

	public void onClose()
	{
		closeMe();
	}

	public final void closeMe()
	{
		setOpen(false);
		broadcastStatusUpdate();
	}

	public final void openMe()
	{
		setOpen(true);
		broadcastStatusUpdate();
	}

	@Override
	public String toString()
	{
		return "door " + _doorId;
	}

	public String getDoorName()
	{
		return _name;
	}

	public int getXMin()
	{
		return _rangeXMin;
	}

	public int getYMin()
	{
		return _rangeYMin;
	}

	public int getZMin()
	{
		return _rangeZMin;
	}

	public int getXMax()
	{
		return _rangeXMax;
	}

	public int getYMax()
	{
		return _rangeYMax;
	}

	public int getZMax()
	{
		return _rangeZMax;
	}

	public void setRange(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax)
	{
		_rangeXMin = xMin;
		_rangeYMin = yMin;
		_rangeZMin = zMin;

		_rangeXMax = xMax;
		_rangeYMax = yMax;
		_rangeZMax = zMax;

		_A = _rangeYMax * (_rangeZMax - _rangeZMin) + _rangeYMin * (_rangeZMin - _rangeZMax);
		_B = _rangeZMin * (_rangeXMax - _rangeXMin) + _rangeZMax * (_rangeXMin - _rangeXMax);
		_C = _rangeXMin * (_rangeYMax - _rangeYMin) + _rangeXMin * (_rangeYMin - _rangeYMax);
		_D = -1 * (_rangeXMin * (_rangeYMax * _rangeZMax - _rangeYMin * _rangeZMax) + _rangeXMax * (_rangeYMin * _rangeZMin - _rangeYMin * _rangeZMax) + _rangeXMin * (_rangeYMin * _rangeZMax - _rangeYMax * _rangeZMin));
	}

	public int getMapRegion()
	{
		return _mapRegion;
	}

	public void setMapRegion(int region)
	{
		_mapRegion = region;
	}

	public Collection<L2SiegeGuardInstance> getKnownSiegeGuards()
	{
		FastList<L2SiegeGuardInstance> result = new FastList<>();

		for (L2Object obj : getKnownList().getKnownObjects().values())
		{
			if (obj instanceof L2SiegeGuardInstance)
				result.add((L2SiegeGuardInstance) obj);
		}
		return result;
	}

	public int getA()
	{
		return _A;
	}

	public int getB()
	{
		return _B;
	}

	public int getC()
	{
		return _C;
	}

	public int getD()
	{
		return _D;
	}

	public void setIsWall(boolean isWall)
	{
		_isWall = isWall;
	}

	public boolean isWall()
	{
		return _isWall;
	}

	@Override
	public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (isWall() && !(attacker instanceof L2SiegeSummonInstance))
			return;

		if (!(getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress()))
			return;

		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}

	@Override
	public void reduceCurrentHpByDOT(double i, L2Character attacker, L2Skill skill)
	{
		// doors can't be damaged by DOTs
	}

	@Override
	public boolean doDie(L2Character killer)
	{
		if (!super.doDie(killer))
			return false;

		boolean isCastle = (getCastle() != null && getCastle().getCastleId() > 0 && getCastle().getSiege().getIsInProgress());
		if (isCastle)
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTLE_GATE_BROKEN_DOWN));

		return true;
	}

	@Override
	public void sendInfo(L2PcInstance activeChar)
	{
		activeChar.sendPacket(new DoorInfo(this));
		activeChar.sendPacket(new DoorStatusUpdate(this));
	}
}
