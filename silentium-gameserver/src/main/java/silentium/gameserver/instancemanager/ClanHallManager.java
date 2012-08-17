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
import java.util.Map;

import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.entity.Auction;
import silentium.gameserver.model.entity.ClanHall;
import silentium.gameserver.model.zone.type.L2ClanHallZone;
import silentium.gameserver.tables.ClanTable;

/**
 * @author Steuf
 */
public class ClanHallManager
{
	protected static final Logger _log = LoggerFactory.getLogger(ClanHallManager.class.getName());

	private final Map<Integer, ClanHall> _clanHall;
	private final Map<Integer, ClanHall> _freeClanHall;
	private boolean _loaded = false;

	public static ClanHallManager getInstance()
	{
		return SingletonHolder._instance;
	}

	public boolean loaded()
	{
		return _loaded;
	}

	protected ClanHallManager()
	{
		_clanHall = new FastMap<>();
		_freeClanHall = new FastMap<>();
		load();
	}

	/** Load All Clan Hall */
	private final void load()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			int id, ownerId, lease, grade = 0;
			String Name, Desc, Location;
			long paidUntil = 0;
			boolean paid = false;

			PreparedStatement statement = con.prepareStatement("SELECT * FROM clanhall ORDER BY id");
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				id = rs.getInt("id");
				Name = rs.getString("name");
				ownerId = rs.getInt("ownerId");
				lease = rs.getInt("lease");
				Desc = rs.getString("desc");
				Location = rs.getString("location");
				paidUntil = rs.getLong("paidUntil");
				grade = rs.getInt("Grade");
				paid = rs.getBoolean("paid");

				ClanHall ch = new ClanHall(id, Name, ownerId, lease, Desc, Location, paidUntil, grade, paid);

				if (ownerId > 0)
				{
					final L2Clan owner = ClanTable.getInstance().getClan(ownerId);
					if (owner != null)
					{
						_clanHall.put(id, ch);
						owner.setHasHideout(id);
						continue;
					}
					ch.free();
				}
				_freeClanHall.put(id, ch);

				Auction auc = AuctionManager.getInstance().getAuction(id);
				if (auc == null && lease > 0)
					AuctionManager.getInstance().initNPC(id);
			}
			statement.close();
			_log.info("ClanHallManager: Loaded " + getClanHalls().size() + " clan halls.");
			_log.info("ClanHallManager: Loaded " + getFreeClanHalls().size() + " free clan halls.");
			_loaded = true;
		}
		catch (Exception e)
		{
			_log.warn("Exception: ClanHallManager.load(): " + e.getMessage(), e);
		}
	}

	/**
	 * @return Map with all free ClanHalls
	 */
	public final Map<Integer, ClanHall> getFreeClanHalls()
	{
		return _freeClanHall;
	}

	/**
	 * @return Map with all ClanHalls that have owner
	 */
	public final Map<Integer, ClanHall> getClanHalls()
	{
		return _clanHall;
	}

	/**
	 * @param chId
	 *            the clanHall id to check.
	 * @return true if the clanHall is free.
	 */
	public final boolean isFree(int chId)
	{
		return _freeClanHall.containsKey(chId);
	}

	/**
	 * Free a ClanHall
	 * 
	 * @param chId
	 *            the id of clanHall to release.
	 */
	public final synchronized void setFree(int chId)
	{
		_freeClanHall.put(chId, _clanHall.get(chId));
		ClanTable.getInstance().getClan(_freeClanHall.get(chId).getOwnerId()).setHasHideout(0);
		_freeClanHall.get(chId).free();
		_clanHall.remove(chId);
	}

	/**
	 * Set owner status for a clan hall.
	 * 
	 * @param chId
	 *            the clanHall id to make checks on.
	 * @param clan
	 *            the new clan owner.
	 */
	public final synchronized void setOwner(int chId, L2Clan clan)
	{
		if (!_clanHall.containsKey(chId))
		{
			_clanHall.put(chId, _freeClanHall.get(chId));
			_freeClanHall.remove(chId);
		}
		else
			_clanHall.get(chId).free();

		ClanTable.getInstance().getClan(clan.getClanId()).setHasHideout(chId);
		_clanHall.get(chId).setOwner(clan);
	}

	/**
	 * @param clanHallId
	 *            the id to use.
	 * @return a clanHall by its id.
	 */
	public final ClanHall getClanHallById(int clanHallId)
	{
		if (_clanHall.containsKey(clanHallId))
			return _clanHall.get(clanHallId);

		if (_freeClanHall.containsKey(clanHallId))
			return _freeClanHall.get(clanHallId);

		_log.warn("ClanHall (id: " + clanHallId + ") isn't found in clanhall table.");
		return null;
	}

	public final ClanHall getNearbyClanHall(int x, int y, int maxDist)
	{
		L2ClanHallZone zone = null;

		for (Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet())
		{
			zone = ch.getValue().getZone();
			if (zone != null && zone.getDistanceToZone(x, y) < maxDist)
				return ch.getValue();
		}
		for (Map.Entry<Integer, ClanHall> ch : _freeClanHall.entrySet())
		{
			zone = ch.getValue().getZone();
			if (zone != null && zone.getDistanceToZone(x, y) < maxDist)
				return ch.getValue();
		}
		return null;
	}

	/**
	 * @param clan
	 *            the clan to use.
	 * @return a clanHall by its owner.
	 */
	public final ClanHall getClanHallByOwner(L2Clan clan)
	{
		for (Map.Entry<Integer, ClanHall> ch : _clanHall.entrySet())
		{
			if (clan.getClanId() == ch.getValue().getOwnerId())
				return ch.getValue();
		}
		return null;
	}

	private static class SingletonHolder
	{
		protected static final ClanHallManager _instance = new ClanHallManager();
	}
}
