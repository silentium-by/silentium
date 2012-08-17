/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.tables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.NPCConfig;
import silentium.gameserver.instancemanager.DayNightSpawnManager;
import silentium.gameserver.model.L2Spawn;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.templates.chars.L2NpcTemplate;

import com.google.common.collect.Sets;

/**
 * @author Nightmare
 */
public class SpawnTable
{
	private static Logger _log = LoggerFactory.getLogger(SpawnTable.class.getName());

	private final Set<L2Spawn> _spawntable = Sets.newCopyOnWriteArraySet();
	private int _npcSpawnCount;

	public static SpawnTable getInstance()
	{
		return SingletonHolder._instance;
	}

	protected SpawnTable()
	{
		if (!MainConfig.ALT_DEV_NO_SPAWNS)
			fillSpawnTable();
	}

	public Set<L2Spawn> getSpawnTable()
	{
		return _spawntable;
	}

	private void fillSpawnTable()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM spawnlist");
			ResultSet rset = statement.executeQuery();

			L2Spawn spawnDat;
			L2NpcTemplate template1;

			while (rset.next())
			{
				template1 = NpcTable.getInstance().getTemplate(rset.getInt("npc_templateid"));
				if (template1 != null)
				{
					if (template1.isType("L2SiegeGuard"))
					{
						// Don't spawn guards, they're spawned during castle sieges.
					}
					else if (template1.isType("L2RaidBoss"))
					{
						// Don't spawn raidbosses ; raidbosses are supposed to be loaded in another table !
						_log.warn("SpawnTable: A RB (id: " + template1 + ") has been found in regular spawnlist. Please move it in raidboss_spawnlist.");
					}
					else if (!NPCConfig.ALLOW_CLASS_MASTERS && template1.isType("L2ClassMaster"))
					{
						// Dont' spawn class masters (if config is setuped to false).
					}
					else if (!NPCConfig.WYVERN_ALLOW_UPGRADER && template1.isType("L2WyvernManager"))
					{
						// Dont' spawn wyvern managers (if config is setuped to false).
					}
					else
					{
						spawnDat = new L2Spawn(template1);
						spawnDat.setLocx(rset.getInt("locx"));
						spawnDat.setLocy(rset.getInt("locy"));
						spawnDat.setLocz(rset.getInt("locz"));
						spawnDat.setHeading(rset.getInt("heading"));
						spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

						switch (rset.getInt("periodOfDay"))
						{
							case 0: // default
								spawnDat.init();
								_npcSpawnCount++;
								break;
							case 1: // Day
								DayNightSpawnManager.getInstance().addDayCreature(spawnDat);
								_npcSpawnCount++;
								break;
							case 2: // Night
								DayNightSpawnManager.getInstance().addNightCreature(spawnDat);
								_npcSpawnCount++;
								break;
						}

						_spawntable.add(spawnDat);
					}
				}
				else
				{
					_log.warn("SpawnTable: Data missing in NPC table for ID: " + rset.getInt("npc_templateid") + ".");
				}
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warn("SpawnTable: Spawn could not be initialized: " + e);
		}

		_log.info("SpawnTable: Loaded " + _spawntable.size() + " Npc Spawn Locations.");

		_log.info("SpawnTable: Spawning completed, total number of NPCs in the world: " + _npcSpawnCount);
	}

	public void addNewSpawn(L2Spawn spawn, boolean storeInDb)
	{
		_spawntable.add(spawn);

		if (storeInDb)
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement = con.prepareStatement("INSERT INTO spawnlist (npc_templateid,locx,locy,locz,heading,respawn_delay) values(?,?,?,?,?,?)");
				statement.setInt(1, spawn.getNpcId());
				statement.setInt(2, spawn.getLocx());
				statement.setInt(3, spawn.getLocy());
				statement.setInt(4, spawn.getLocz());
				statement.setInt(5, spawn.getHeading());
				statement.setInt(6, spawn.getRespawnDelay() / 1000);
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				// problem with storing spawn
				_log.warn("SpawnTable: Could not store spawn in the DB:" + e);
			}
		}
	}

	public void deleteSpawn(L2Spawn spawn, boolean updateDb)
	{
		if (!_spawntable.remove(spawn))
			return;

		if (updateDb)
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement = con.prepareStatement("DELETE FROM spawnlist WHERE locx=? AND locy=? AND locz=? AND npc_templateid=? AND heading=?");
				statement.setInt(1, spawn.getLocx());
				statement.setInt(2, spawn.getLocy());
				statement.setInt(3, spawn.getLocz());
				statement.setInt(4, spawn.getNpcId());
				statement.setInt(5, spawn.getHeading());
				statement.execute();
				statement.close();
			}
			catch (Exception e)
			{
				// problem with deleting spawn
				_log.warn("SpawnTable: Spawn " + spawn + " could not be removed from DB: " + e);
			}
		}
	}

	// just wrapper
	public void reloadAll()
	{
		_spawntable.clear();
		fillSpawnTable();
	}

	/**
	 * Get all spawns of a NPC.
	 * 
	 * @param activeChar
	 *            The player who requested that action.
	 * @param npcId
	 *            : ID of the NPC to find.
	 * @param teleportIndex
	 * @param showposition
	 */
	public void findNPCInstances(L2PcInstance activeChar, int npcId, int teleportIndex, boolean showposition)
	{
		int index = 0;
		for (L2Spawn spawn : _spawntable)
		{
			if (npcId == spawn.getNpcId())
			{
				index++;
				L2Npc _npc = spawn.getLastSpawn();
				if (teleportIndex > -1)
				{
					if (teleportIndex == index)
					{
						if (showposition && _npc != null)
							activeChar.teleToLocation(_npc.getX(), _npc.getY(), _npc.getZ(), true);
						else
							activeChar.teleToLocation(spawn.getLocx(), spawn.getLocy(), spawn.getLocz(), true);
					}
				}
				else
				{
					if (showposition && _npc != null)
						activeChar.sendMessage(index + " - " + spawn.getTemplate().getName() + " (" + spawn + "): " + _npc.getX() + " " + _npc.getY() + " " + _npc.getZ());
					else
						activeChar.sendMessage(index + " - " + spawn.getTemplate().getName() + " (" + spawn + "): " + spawn.getLocx() + " " + spawn.getLocy() + " " + spawn.getLocz());
				}
			}
		}

		if (index == 0)
			activeChar.sendMessage("No current spawns found.");
	}

	private static class SingletonHolder
	{
		protected static final SpawnTable _instance = new SpawnTable();
	}
}
