/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.entity;

import gnu.trove.map.hash.TIntIntHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.CastleUpdater;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.configs.ClansConfig;
import silentium.gameserver.data.xml.DoorData;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.CastleManorManager;
import silentium.gameserver.instancemanager.CastleManorManager.CropProcure;
import silentium.gameserver.instancemanager.CastleManorManager.SeedProduction;
import silentium.gameserver.instancemanager.ZoneManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2Manor;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2ArtefactInstance;
import silentium.gameserver.model.actor.instance.L2DoorInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.model.zone.type.L2CastleTeleportZone;
import silentium.gameserver.model.zone.type.L2CastleZone;
import silentium.gameserver.model.zone.type.L2SiegeZone;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.PlaySound;
import silentium.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.ClanTable;

public class Castle
{
	protected static final Logger _log = LoggerFactory.getLogger(Castle.class.getName());

	private List<CropProcure> _procure = new ArrayList<>();
	private List<SeedProduction> _production = new ArrayList<>();
	private List<CropProcure> _procureNext = new ArrayList<>();
	private List<SeedProduction> _productionNext = new ArrayList<>();
	private boolean _isNextPeriodApproved = false;

	private static final String CASTLE_MANOR_DELETE_PRODUCTION = "DELETE FROM castle_manor_production WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PRODUCTION_PERIOD = "DELETE FROM castle_manor_production WHERE castle_id=? AND period=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE = "DELETE FROM castle_manor_procure WHERE castle_id=?;";
	private static final String CASTLE_MANOR_DELETE_PROCURE_PERIOD = "DELETE FROM castle_manor_procure WHERE castle_id=? AND period=?;";
	private static final String CASTLE_UPDATE_CROP = "UPDATE castle_manor_procure SET can_buy=? WHERE crop_id=? AND castle_id=? AND period=?";
	private static final String CASTLE_UPDATE_SEED = "UPDATE castle_manor_production SET can_produce=? WHERE seed_id=? AND castle_id=? AND period=?";

	private int _castleId = 0;
	private final List<L2DoorInstance> _doors = new ArrayList<>();
	private String _name = "";
	private int _ownerId = 0;
	private Siege _siege = null;
	private Calendar _siegeDate;
	private boolean _isTimeRegistrationOver = true;
	private Calendar _siegeTimeRegistrationEndDate;
	private int _taxPercent = 0;
	private double _taxRate = 0;
	private long _treasury = 0;
	private L2SiegeZone _zone = null;
	private L2CastleZone _castleZone = null;
	private L2CastleTeleportZone _teleZone;
	private L2Clan _formerOwner = null;
	private final List<L2ArtefactInstance> _artefacts = new ArrayList<>(1);
	private final TIntIntHashMap _engrave = new TIntIntHashMap(1);

	public Castle(int castleId)
	{
		_castleId = castleId;
		load();
	}

	public synchronized void engrave(L2Clan clan, L2Object target)
	{
		if (!_artefacts.contains(target))
			return;

		_engrave.put(target.getObjectId(), clan.getClanId());

		if (_engrave.size() == _artefacts.size())
		{
			for (L2ArtefactInstance art : _artefacts)
			{
				if (_engrave.get(art.getObjectId()) != clan.getClanId())
					return;
			}
			_engrave.clear();
			setOwner(clan);

			// "Clan X engraved the ruler" message
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_ENGRAVED_RULER);
			sm.addString(clan.getName());
			getSiege().announceToPlayer(sm, true);
		}
		else
		{
			// "Clan X engraved the ruler" message
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_S1_ENGRAVED_RULER);
			sm.addString(clan.getName());
			getSiege().announceToPlayer(sm, true);
		}
	}

	/**
	 * Add amount to castle's treasury (warehouse).
	 * 
	 * @param amount
	 *            The amount to add.
	 */
	public void addToTreasury(int amount)
	{
		if (getOwnerId() <= 0)
			return;

		if (_name.equalsIgnoreCase("Schuttgart") || _name.equalsIgnoreCase("Goddard"))
		{
			Castle rune = CastleManager.getInstance().getCastle("rune");
			if (rune != null)
			{
				int runeTax = (int) (amount * rune.getTaxRate());
				if (rune.getOwnerId() > 0)
					rune.addToTreasury(runeTax);
				amount -= runeTax;
			}
		}

		if (!_name.equalsIgnoreCase("aden") && !_name.equalsIgnoreCase("Rune") && !_name.equalsIgnoreCase("Schuttgart") && !_name.equalsIgnoreCase("Goddard")) // If
																																								// current
																																								// castle
																																								// instance
																																								// is
																																								// not
																																								// Aden,
																																								// Rune,
																																								// Goddard
																																								// or
																																								// Schuttgart.
		{
			Castle aden = CastleManager.getInstance().getCastle("aden");
			if (aden != null)
			{
				int adenTax = (int) (amount * aden.getTaxRate()); // Find out what Aden gets from the current castle instance's
																	// income
				if (aden.getOwnerId() > 0)
					aden.addToTreasury(adenTax); // Only bother to really add the tax to the treasury if not npc owned

				amount -= adenTax; // Subtract Aden's income from current castle instance's income
			}
		}

		addToTreasuryNoTax(amount);
	}

	/**
	 * Add amount to castle instance's treasury (warehouse), no tax paying.
	 * 
	 * @param amount
	 *            The amount of adenas to add to treasury.
	 * @return true if successful.
	 */
	public boolean addToTreasuryNoTax(long amount)
	{
		if (getOwnerId() <= 0)
			return false;

		if (amount < 0)
		{
			amount *= -1;
			if (_treasury < amount)
				return false;
			_treasury -= amount;
		}
		else
		{
			if (_treasury + amount > Integer.MAX_VALUE)
				_treasury = Integer.MAX_VALUE;
			else
				_treasury += amount;
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET treasury = ? WHERE id = ?");
			statement.setLong(1, getTreasury());
			statement.setInt(2, getCastleId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
		}
		return true;
	}

	/**
	 * Move non clan members off castle area and to nearest town.
	 */
	public void banishForeigners()
	{
		getCastleZone().banishForeigners(getOwnerId());
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return true if object is inside the zone
	 */
	public boolean checkIfInZone(int x, int y, int z)
	{
		return getZone().isInsideZone(x, y, z);
	}

	public L2SiegeZone getZone()
	{
		if (_zone == null)
		{
			for (L2SiegeZone zone : ZoneManager.getInstance().getAllZones(L2SiegeZone.class))
			{
				if (zone.getSiegeObjectId() == getCastleId())
				{
					_zone = zone;
					break;
				}
			}
		}
		return _zone;
	}

	public L2CastleZone getCastleZone()
	{
		if (_castleZone == null)
		{
			for (L2CastleZone zone : ZoneManager.getInstance().getAllZones(L2CastleZone.class))
			{
				if (zone.getCastleId() == getCastleId())
				{
					_castleZone = zone;
					break;
				}
			}
		}
		return _castleZone;
	}

	public L2CastleTeleportZone getTeleZone()
	{
		if (_teleZone == null)
		{
			for (L2CastleTeleportZone zone : ZoneManager.getInstance().getAllZones(L2CastleTeleportZone.class))
			{
				if (zone.getCastleId() == getCastleId())
				{
					_teleZone = zone;
					break;
				}
			}
		}
		return _teleZone;
	}

	public void oustAllPlayers()
	{
		getTeleZone().oustAllPlayers();
	}

	/**
	 * Get the object distance to this castle zone.
	 * 
	 * @param obj
	 *            The L2Object to make tests on.
	 * @return the distance between the L2Object and the zone.
	 */
	public double getDistance(L2Object obj)
	{
		return getZone().getDistanceToZone(obj);
	}

	public void closeDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, false);
	}

	public void openDoor(L2PcInstance activeChar, int doorId)
	{
		openCloseDoor(activeChar, doorId, true);
	}

	public void openCloseDoor(L2PcInstance activeChar, int doorId, boolean open)
	{
		if (activeChar.getClanId() != getOwnerId())
			return;

		L2DoorInstance door = getDoor(doorId);
		if (door != null)
		{
			if (open)
				door.openMe();
			else
				door.closeMe();
		}
	}

	/**
	 * This method is used to begin removing all castle upgrades.
	 */
	public void removeUpgrade()
	{
		removeDoorUpgrade();
	}

	/**
	 * This method setup the castle owner.
	 * 
	 * @param clan
	 *            The clan who will own the castle.
	 */
	public void setOwner(L2Clan clan)
	{
		// Act only if castle owner is different of NPC, or if old owner is different of new owner.
		if (getOwnerId() > 0 && (clan == null || clan.getClanId() != getOwnerId()))
		{
			// Try to find clan instance of the old owner.
			L2Clan oldOwner = ClanTable.getInstance().getClan(getOwnerId());
			if (oldOwner != null)
			{
				// Set the former owner.
				if (_formerOwner == null)
					_formerOwner = oldOwner;

				// Dismount the old leader if he was riding a wyvern.
				L2PcInstance oldLeader = oldOwner.getLeader().getPlayerInstance();
				if (oldLeader != null)
				{
					if (oldLeader.getMountType() == 2)
						oldLeader.dismount();
				}

				// Unset castle flag for old owner clan.
				oldOwner.setHasCastle(0);
			}
		}

		// Update database.
		updateOwnerInDB(clan);

		// If siege is in progress, mid victory phase of siege.
		if (getSiege().getIsInProgress())
		{
			getSiege().midVictory();

			// "There is a new castle Lord" message when the castle change of hands. Message sent for both sides.
			getSiege().announceToPlayer(SystemMessage.getSystemMessage(SystemMessageId.NEW_CASTLE_LORD), true);
		}
	}

	/**
	 * Remove the castle owner. This method is only used by admin command.
	 * 
	 * @param clan
	 *            The clan which is victim of the command.
	 **/
	public void removeOwner(L2Clan clan)
	{
		if (clan != null)
		{
			_formerOwner = clan;

			clan.setHasCastle(0);
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
		}

		updateOwnerInDB(null);

		if (getSiege().getIsInProgress())
			getSiege().midVictory();
		else
		{
			// Remove circlets only if a siege isn't in progress, and if an owner exists
			if (ClansConfig.REMOVE_CASTLE_CIRCLETS)
				if (_formerOwner != null)
					CastleManager.getInstance().removeCirclet(_formerOwner, getCastleId());
		}
	}

	/**
	 * This method updates the castle tax rate.
	 * 
	 * @param activeChar
	 *            Sends informative messages to that character (success or fail).
	 * @param taxPercent
	 *            The new tax rate to apply.
	 */
	public void setTaxPercent(L2PcInstance activeChar, int taxPercent)
	{
		int maxTax;
		switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
		{
			case SevenSigns.CABAL_DAWN:
				maxTax = 25;
				break;
			case SevenSigns.CABAL_DUSK:
				maxTax = 5;
				break;
			default: // no owner
				maxTax = 15;
		}

		if (taxPercent < 0 || taxPercent > maxTax)
		{
			activeChar.sendMessage("Tax value must be between 0 and " + maxTax + ".");
			return;
		}

		setTaxPercent(taxPercent);
		activeChar.sendMessage(getName() + " castle tax changed to " + taxPercent + "%.");
	}

	public void setTaxPercent(int taxPercent)
	{
		_taxPercent = taxPercent;
		_taxRate = _taxPercent / 100.0;

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE castle SET taxPercent = ? WHERE id = ?");
			statement.setInt(1, taxPercent);
			statement.setInt(2, getCastleId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * Respawn all doors on castle grounds. Doors HPs are set to their maximum.
	 */
	public void spawnDoor()
	{
		spawnDoor(false);
	}

	/**
	 * Respawn all doors on castle grounds.
	 * 
	 * @param isDoorWeak
	 *            if true, spawn doors with 50% max HPs.
	 */
	public void spawnDoor(boolean isDoorWeak)
	{
		for (L2DoorInstance door : _doors)
		{
			if (door.isDead())
			{
				door.doRevive();
				if (isDoorWeak)
				{
					door.setCurrentHp(door.getMaxHp() / 2);
				}
				else
				{
					door.setCurrentHp(door.getMaxHp());
				}
			}
			
			if (door.getOpen())
			{
				door.closeMe();
			}
		}
		loadDoorUpgrade(); // Check for any upgrade the doors may have
	}

	// This method upgrade door
	public void upgradeDoor(int doorId, int hp, int pDef, int mDef)
	{
		L2DoorInstance door = getDoor(doorId);
		if (door == null)
			return;

		door.setCurrentHp(door.getMaxHp() + hp);
		saveDoorUpgrade(doorId, hp, pDef, mDef);
	}

	// This method loads castle
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle WHERE id = ?");
			statement.setInt(1, getCastleId());
			ResultSet rs = statement.executeQuery();

			while (rs.next())
			{
				_name = rs.getString("name");
				_siegeDate = Calendar.getInstance();
				_siegeDate.setTimeInMillis(rs.getLong("siegeDate"));
				_siegeTimeRegistrationEndDate = Calendar.getInstance();
				_siegeTimeRegistrationEndDate.setTimeInMillis(rs.getLong("regTimeEnd"));
				_isTimeRegistrationOver = rs.getBoolean("regTimeOver");
				_taxPercent = rs.getInt("taxPercent");
				_treasury = rs.getLong("treasury");
			}
			rs.close();
			statement.close();

			_taxRate = _taxPercent / 100.0;

			statement = con.prepareStatement("SELECT clan_id FROM clan_data WHERE hasCastle = ?");
			statement.setInt(1, getCastleId());
			rs = statement.executeQuery();

			while (rs.next())
				_ownerId = rs.getInt("clan_id");

			if (getOwnerId() > 0)
			{
				// Try to find clan instance
				L2Clan clan = ClanTable.getInstance().getClan(getOwnerId());

				// Schedule owner tasks to start running
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000);
			}
			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Exception: loadCastleData(): " + e.getMessage(), e);
		}
	}

	// This method loads castle door upgrade data from database
	private void loadDoorUpgrade()
	{
		final StringBuilder sb = new StringBuilder();
		for (L2DoorInstance door : getDoors())
		{
			sb.append(",");
			sb.append(door.getDoorId());
		}

		String finalCommand = sb.toString().substring(1);

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_doorupgrade WHERE doorId IN (?)");
			statement.setString(1, finalCommand);
			ResultSet rs = statement.executeQuery();

			while (rs.next())
				upgradeDoor(rs.getInt("id"), rs.getInt("hp"), rs.getInt("pDef"), rs.getInt("mDef"));

			rs.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Exception: loadCastleDoorUpgrade(): " + e.getMessage(), e);
		}
	}

	private void removeDoorUpgrade()
	{
		final StringBuilder sb = new StringBuilder();
		for (L2DoorInstance door : getDoors())
		{
			sb.append(",");
			sb.append(door.getDoorId());
		}

		String finalCommand = sb.toString().substring(1);

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM castle_doorupgrade WHERE doorId IN (?)");
			statement.setString(1, finalCommand);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Exception: removeDoorUpgrade(): " + e.getMessage(), e);
		}
	}

	private static void saveDoorUpgrade(int doorId, int hp, int pDef, int mDef)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO castle_doorupgrade (doorId, hp, pDef, mDef) values (?,?,?,?)");
			statement.setInt(1, doorId);
			statement.setInt(2, hp);
			statement.setInt(3, pDef);
			statement.setInt(4, mDef);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Exception: saveDoorUpgrade(int doorId, int hp, int pDef, int mDef): " + e.getMessage(), e);
		}
	}

	private void updateOwnerInDB(L2Clan clan)
	{
		if (clan != null)
			_ownerId = clan.getClanId(); // Update owner id property
		else
		{
			_ownerId = 0; // Remove owner
			resetManor();
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET hasCastle=0 WHERE hasCastle=?");
			statement.setInt(1, getCastleId());
			statement.execute();
			statement.close();

			statement = con.prepareStatement("UPDATE clan_data SET hasCastle=? WHERE clan_id=?");
			statement.setInt(1, getCastleId());
			statement.setInt(2, getOwnerId());
			statement.execute();
			statement.close();

			// Announce to clan memebers
			if (clan != null)
			{
				clan.setHasCastle(getCastleId()); // Set has castle flag for new owner
				clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
				clan.broadcastToOnlineMembers(new PlaySound(1, "Siege_Victory", 0, 0, 0, 0, 0));
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(clan, 1), 3600000); // Schedule owner tasks to
																										// start running
			}
		}
		catch (Exception e)
		{
			_log.warn("Exception: updateOwnerInDB(L2Clan clan): " + e.getMessage(), e);
		}
	}

	public final int getCastleId()
	{
		return _castleId;
	}

	public final L2DoorInstance getDoor(int doorId)
	{
		if (doorId <= 0)
			return null;

		for (L2DoorInstance door : getDoors())
		{
			if (door.getDoorId() == doorId)
				return door;
		}
		return null;
	}

	public final List<L2DoorInstance> getDoors()
	{
		return _doors;
	}

	public final String getName()
	{
		return _name;
	}

	public final int getOwnerId()
	{
		return _ownerId;
	}

	public final Siege getSiege()
	{
		if (_siege == null)
			_siege = new Siege(new Castle[] { this });
		return _siege;
	}

	public final Calendar getSiegeDate()
	{
		return _siegeDate;
	}

	public boolean getIsTimeRegistrationOver()
	{
		return _isTimeRegistrationOver;
	}

	public void setIsTimeRegistrationOver(boolean val)
	{
		_isTimeRegistrationOver = val;
	}

	public Calendar getTimeRegistrationOverDate()
	{
		if (_siegeTimeRegistrationEndDate == null)
			_siegeTimeRegistrationEndDate = Calendar.getInstance();
		return _siegeTimeRegistrationEndDate;
	}

	public final int getTaxPercent()
	{
		return _taxPercent;
	}

	public final double getTaxRate()
	{
		return _taxRate;
	}

	public final long getTreasury()
	{
		return _treasury;
	}

	public List<SeedProduction> getSeedProduction(int period)
	{
		return (period == CastleManorManager.PERIOD_CURRENT ? _production : _productionNext);
	}

	public List<CropProcure> getCropProcure(int period)
	{
		return (period == CastleManorManager.PERIOD_CURRENT ? _procure : _procureNext);
	}

	public void setSeedProduction(List<SeedProduction> seed, int period)
	{
		if (period == CastleManorManager.PERIOD_CURRENT)
			_production = seed;
		else
			_productionNext = seed;
	}

	public void setCropProcure(List<CropProcure> crop, int period)
	{
		if (period == CastleManorManager.PERIOD_CURRENT)
			_procure = crop;
		else
			_procureNext = crop;
	}

	public SeedProduction getSeed(int seedId, int period)
	{
		for (SeedProduction seed : getSeedProduction(period))
		{
			if (seed.getId() == seedId)
				return seed;
		}
		return null;
	}

	public CropProcure getCrop(int cropId, int period)
	{
		for (CropProcure crop : getCropProcure(period))
		{
			if (crop.getId() == cropId)
				return crop;
		}
		return null;
	}

	public long getManorCost(int period)
	{
		List<CropProcure> procure;
		List<SeedProduction> production;

		if (period == CastleManorManager.PERIOD_CURRENT)
		{
			procure = _procure;
			production = _production;
		}
		else
		{
			procure = _procureNext;
			production = _productionNext;
		}

		long total = 0;
		if (production != null)
		{
			for (SeedProduction seed : production)
				total += L2Manor.getInstance().getSeedBuyPrice(seed.getId()) * seed.getStartProduce();
		}

		if (procure != null)
		{
			for (CropProcure crop : procure)
				total += crop.getPrice() * crop.getStartAmount();
		}
		return total;
	}

	/**
	 * Save manor production data.
	 */
	public void saveSeedData()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION);
			statement.setInt(1, getCastleId());
			statement.execute();
			statement.close();

			if (_production != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_production.size()];
				for (SeedProduction s : _production)
					values[count++] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_CURRENT + ")";

				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
						query += "," + values[i];

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			if (_productionNext != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[_productionNext.size()];
				for (SeedProduction s : _productionNext)
					values[count++] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + CastleManorManager.PERIOD_NEXT + ")";

				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
						query += "," + values[i];

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Save manor production data for specified period.
	 * 
	 * @param period
	 *            The period number.
	 */
	public void saveSeedData(int period)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_MANOR_DELETE_PRODUCTION_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();

			List<SeedProduction> prod = getSeedProduction(period);
			if (prod != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_production VALUES ";
				String values[] = new String[prod.size()];
				for (SeedProduction s : prod)
					values[count++] = "(" + getCastleId() + "," + s.getId() + "," + s.getCanProduce() + "," + s.getStartProduce() + "," + s.getPrice() + "," + period + ")";

				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
						query += "," + values[i];

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Save crop procure data.
	 */
	public void saveCropData()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE);
			statement.setInt(1, getCastleId());
			statement.execute();
			statement.close();

			if (_procure != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procure.size()];
				for (CropProcure cp : _procure)
					values[count++] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_CURRENT + ")";

				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
						query += "," + values[i];

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}

			if (_procureNext != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[_procureNext.size()];
				for (CropProcure cp : _procureNext)
					values[count++] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + CastleManorManager.PERIOD_NEXT + ")";

				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
						query += "," + values[i];

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Save crop procure data for specified period.
	 * 
	 * @param period
	 *            The period number.
	 */
	public void saveCropData(int period)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_MANOR_DELETE_PROCURE_PERIOD);
			statement.setInt(1, getCastleId());
			statement.setInt(2, period);
			statement.execute();
			statement.close();

			List<CropProcure> proc = getCropProcure(period);
			if (proc != null)
			{
				int count = 0;
				String query = "INSERT INTO castle_manor_procure VALUES ";
				String values[] = new String[proc.size()];

				for (CropProcure cp : proc)
					values[count++] = "(" + getCastleId() + "," + cp.getId() + "," + cp.getAmount() + "," + cp.getStartAmount() + "," + cp.getPrice() + "," + cp.getReward() + "," + period + ")";

				if (values.length > 0)
				{
					query += values[0];
					for (int i = 1; i < values.length; i++)
						query += "," + values[i];

					statement = con.prepareStatement(query);
					statement.execute();
					statement.close();
				}
			}
		}
		catch (Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Update crop informations.
	 * 
	 * @param cropId
	 * @param amount
	 * @param period
	 */
	public void updateCrop(int cropId, int amount, int period)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_UPDATE_CROP);
			statement.setInt(1, amount);
			statement.setInt(2, cropId);
			statement.setInt(3, getCastleId());
			statement.setInt(4, period);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.info("Error adding crop data for castle " + getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Update seeds informations.
	 * 
	 * @param seedId
	 * @param amount
	 * @param period
	 */
	public void updateSeed(int seedId, int amount, int period)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(CASTLE_UPDATE_SEED);
			statement.setInt(1, amount);
			statement.setInt(2, seedId);
			statement.setInt(3, getCastleId());
			statement.setInt(4, period);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.info("Error adding seed production data for castle " + getName() + ": " + e.getMessage());
		}
	}

	public boolean isNextPeriodApproved()
	{
		return _isNextPeriodApproved;
	}

	public void setNextPeriodApproved(boolean val)
	{
		_isNextPeriodApproved = val;
	}

	public void updateClansReputation()
	{
		SystemMessage msg;

		if (_formerOwner != null)
		{
			if (_formerOwner != ClanTable.getInstance().getClan(getOwnerId()))
			{
				int maxreward = Math.max(0, _formerOwner.getReputationScore());
				_formerOwner.takeReputationScore(1000);

				// Defenders fail
				msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_WAS_DEFEATED_IN_SIEGE_AND_LOST_S1_REPUTATION_POINTS);
				msg.addNumber(1000);
				_formerOwner.broadcastToOnlineMembers(msg);

				L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
				if (owner != null)
				{
					owner.addReputationScore(Math.min(1000, maxreward));

					// Attackers succeed over defenders
					msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_VICTORIOUS_IN_SIEGE_AND_GAINED_S1_REPUTATION_POINTS);
					msg.addNumber(1000);
					owner.broadcastToOnlineMembers(msg);
				}
			}
			else
			{
				_formerOwner.addReputationScore(500);

				// Draw
				msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_VICTORIOUS_IN_SIEGE_AND_GAINED_S1_REPUTATION_POINTS);
				msg.addNumber(500);
				_formerOwner.broadcastToOnlineMembers(msg);
			}
		}
		else
		{
			L2Clan owner = ClanTable.getInstance().getClan(getOwnerId());
			if (owner != null)
			{
				owner.addReputationScore(1000);

				// Attackers win over NPCs
				msg = SystemMessage.getSystemMessage(SystemMessageId.CLAN_VICTORIOUS_IN_SIEGE_AND_GAINED_S1_REPUTATION_POINTS);
				msg.addNumber(1000);
				owner.broadcastToOnlineMembers(msg);
			}
		}
	}

	/**
	 * Register an artefact instance.
	 * 
	 * @param artefact
	 *            The instance to register in _artefacts List.
	 */
	public void registerArtefact(L2ArtefactInstance artefact)
	{
		_artefacts.add(artefact);
	}

	/**
	 * @return the artefacts List.
	 */
	public List<L2ArtefactInstance> getArtefacts()
	{
		return _artefacts;
	}

	public void resetManor()
	{
		setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_CURRENT);
		setCropProcure(new ArrayList<CropProcure>(), CastleManorManager.PERIOD_NEXT);
		setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_CURRENT);
		setSeedProduction(new ArrayList<SeedProduction>(), CastleManorManager.PERIOD_NEXT);

		if (ClansConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
		{
			saveCropData();
			saveSeedData();
		}
	}

	/**
	 * @return the initial castle owner. Used to clean crowns && others castles related functions.
	 */
	public L2Clan getInitialCastleOwner()
	{
		return _formerOwner;
	}
}
