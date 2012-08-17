/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.model.L2Spawn;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public class SiegeGuardManager
{
	private static Logger _log = LoggerFactory.getLogger(SiegeGuardManager.class.getName());

	private final Castle _castle;
	private final List<L2Spawn> _siegeGuardSpawn = new FastList<>();

	public SiegeGuardManager(Castle castle)
	{
		_castle = castle;
	}

	/**
	 * Add a guard on activeChar's position.
	 * 
	 * @param activeChar
	 *            The position used.
	 * @param npcId
	 *            The templte to spawn.
	 */
	public void addSiegeGuard(L2PcInstance activeChar, int npcId)
	{
		if (activeChar == null)
			return;

		addSiegeGuard(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}

	/**
	 * Add guard following regular coordinates.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param npcId
	 *            The templte to spawn.
	 */
	public void addSiegeGuard(int x, int y, int z, int heading, int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 0);
	}

	/**
	 * Hire merc.
	 * 
	 * @param activeChar
	 * @param npcId
	 *            The templte to spawn.
	 */
	public void hireMerc(L2PcInstance activeChar, int npcId)
	{
		if (activeChar == null)
			return;

		hireMerc(activeChar.getX(), activeChar.getY(), activeChar.getZ(), activeChar.getHeading(), npcId);
	}

	/**
	 * Hire merc.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param npcId
	 */
	public void hireMerc(int x, int y, int z, int heading, int npcId)
	{
		saveSiegeGuard(x, y, z, heading, npcId, 1);
	}

	/**
	 * Remove a single mercenary, identified by the npcId and location. Presumably, this is used when a castle lord picks up a previously dropped
	 * ticket
	 * 
	 * @param npcId
	 * @param x
	 * @param y
	 * @param z
	 */
	public void removeMerc(int npcId, int x, int y, int z)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM castle_siege_guards WHERE npcId = ? AND x = ? AND y = ? AND z = ? AND isHired = 1");
			statement.setInt(1, npcId);
			statement.setInt(2, x);
			statement.setInt(3, y);
			statement.setInt(4, z);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Error deleting hired siege guard at " + x + ',' + y + ',' + z + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Remove mercs.<BR>
	 * <BR>
	 */
	public void removeMercs()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("Delete From castle_siege_guards Where castleId = ? And isHired = 1");
			statement.setInt(1, getCastle().getCastleId());
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Error deleting hired siege guard for castle " + getCastle().getName() + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Spawn guards.
	 */
	public void spawnSiegeGuard()
	{
		try
		{
			int hiredCount = 0;
			int hiredMax = MercTicketManager.getInstance().getMaxAllowedMerc(_castle.getCastleId());
			boolean isHired = (getCastle().getOwnerId() > 0) ? true : false;

			loadSiegeGuard();

			for (L2Spawn spawn : getSiegeGuardSpawn())
			{
				if (spawn != null)
				{
					spawn.init();
					if (isHired)
					{
						spawn.stopRespawn();
						if (++hiredCount > hiredMax)
							return;
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Error spawning siege guards for castle " + getCastle().getName(), e);
		}
	}

	/**
	 * Unspawn guards.<BR>
	 * <BR>
	 */
	public void unspawnSiegeGuard()
	{
		for (L2Spawn spawn : getSiegeGuardSpawn())
		{
			if (spawn == null)
				continue;

			spawn.stopRespawn();
			spawn.getLastSpawn().doDie(spawn.getLastSpawn());
		}

		getSiegeGuardSpawn().clear();
	}

	/**
	 * Load guards. If castle is owned by a clan, then don't spawn default guards
	 */
	private void loadSiegeGuard()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM castle_siege_guards Where castleId = ? And isHired = ?");
			statement.setInt(1, getCastle().getCastleId());
			statement.setInt(2, (getCastle().getOwnerId() > 0 ? 1 : 0));

			ResultSet rs = statement.executeQuery();

			L2Spawn spawn1;
			L2NpcTemplate template1;

			while (rs.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rs.getInt("npcId"));
				if (template1 != null)
				{
					spawn1 = new L2Spawn(template1);
					spawn1.setLocx(rs.getInt("x"));
					spawn1.setLocy(rs.getInt("y"));
					spawn1.setLocz(rs.getInt("z"));
					spawn1.setHeading(rs.getInt("heading"));
					spawn1.setRespawnDelay(rs.getInt("respawnDelay"));

					_siegeGuardSpawn.add(spawn1);
				}
				else
				{
					_log.warn("Missing npc data in npc table for id: " + rs.getInt("npcId"));
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Error loading siege guard for castle " + getCastle().getName() + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Save guards.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param npcId
	 * @param isHire
	 */
	private void saveSiegeGuard(int x, int y, int z, int heading, int npcId, int isHire)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO castle_siege_guards (castleId, npcId, x, y, z, heading, respawnDelay, isHired) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, getCastle().getCastleId());
			statement.setInt(2, npcId);
			statement.setInt(3, x);
			statement.setInt(4, y);
			statement.setInt(5, z);
			statement.setInt(6, heading);
			statement.setInt(7, (isHire == 1 ? 0 : 600));
			statement.setInt(8, isHire);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Error adding siege guard for castle " + getCastle().getName() + ": " + e.getMessage(), e);
		}
	}

	public final Castle getCastle()
	{
		return _castle;
	}

	public final List<L2Spawn> getSiegeGuardSpawn()
	{
		return _siegeGuardSpawn;
	}
}
