/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.instancemanager;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.SevenSigns;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2ClanMember;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.Castle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CastleManager
{
	protected static final Logger _log = LoggerFactory.getLogger(CastleManager.class.getName());

	public static final CastleManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private List<Castle> _castles;
	private static final int _castleCirclets[] = { 0, 6838, 6835, 6839, 6837, 6840, 6834, 6836, 8182, 8183 };

	protected CastleManager()
	{
	}

	public final int findNearestCastleIndex(L2Object obj)
	{
		int index = getCastleIndex(obj);
		if (index < 0)
		{
			double closestDistance = 99999999;
			double distance;
			Castle castle;
			for (int i = 0; i < _castles.size(); i++)
			{
				castle = _castles.get(i);
				if (castle == null)
					continue;
				distance = castle.getDistance(obj);
				if (closestDistance > distance)
				{
					closestDistance = distance;
					index = i;
				}
			}
		}
		return index;
	}

	public final void load()
	{
		_castles = new ArrayList<>();

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT id FROM castle ORDER BY id");
			ResultSet rs = statement.executeQuery();

			while (rs.next())
				_castles.add(new Castle(rs.getInt("id")));

			statement.close();

			_log.info("CastleManager: Loaded " + _castles.size() + " castles.");
		}
		catch (Exception e)
		{
			_log.warn("Exception: loadCastleData(): " + e.getMessage(), e);
		}
	}

	public final Castle getCastleById(int castleId)
	{
		for (Castle temp : _castles)
		{
			if (temp.getCastleId() == castleId)
				return temp;
		}
		return null;
	}

	public final Castle getCastleByOwner(L2Clan clan)
	{
		for (Castle temp : _castles)
		{
			if (temp.getOwnerId() == clan.getClanId())
				return temp;
		}
		return null;
	}

	public final Castle getCastle(String name)
	{
		for (Castle temp : _castles)
		{
			if (temp.getName().equalsIgnoreCase(name.trim()))
				return temp;
		}
		return null;
	}

	public final Castle getCastle(int x, int y, int z)
	{
		for (Castle temp : _castles)
		{
			if (temp.checkIfInZone(x, y, z))
				return temp;
		}
		return null;
	}

	public final Castle getCastle(L2Object activeObject)
	{
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final int getCastleIndex(int castleId)
	{
		Castle castle;
		for (int i = 0; i < _castles.size(); i++)
		{
			castle = _castles.get(i);
			if (castle != null && castle.getCastleId() == castleId)
				return i;
		}
		return -1;
	}

	public final int getCastleIndex(L2Object activeObject)
	{
		return getCastleIndex(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final int getCastleIndex(int x, int y, int z)
	{
		Castle castle;
		for (int i = 0; i < _castles.size(); i++)
		{
			castle = _castles.get(i);
			if (castle != null && castle.checkIfInZone(x, y, z))
				return i;
		}
		return -1;
	}

	public final List<Castle> getCastles()
	{
		return _castles;
	}

	public final void validateTaxes(int sealStrifeOwner)
	{
		int maxTax;
		switch (sealStrifeOwner)
		{
			case SevenSigns.CABAL_DUSK:
				maxTax = 5;
				break;
			case SevenSigns.CABAL_DAWN:
				maxTax = 25;
				break;
			default: // no owner
				maxTax = 15;
				break;
		}

		for (Castle castle : _castles)
			if (castle.getTaxPercent() > maxTax)
				castle.setTaxPercent(maxTax);
	}

	int _castleId = 1; // from this castle

	public int getCirclet()
	{
		return getCircletByCastleId(_castleId);
	}

	public int getCircletByCastleId(int castleId)
	{
		if (castleId > 0 && castleId < 10)
			return _castleCirclets[castleId];

		return 0;
	}

	// remove this castle's circlets from the clan
	public void removeCirclet(L2Clan clan, int castleId)
	{
		for (L2ClanMember member : clan.getMembers())
			removeCircletsAndCrown(member, castleId);
	}

	public void removeCircletsAndCrown(L2ClanMember member, int castleId)
	{
		if (member == null)
			return;

		L2PcInstance player = member.getPlayerInstance();
		int circletId = getCircletByCastleId(castleId);

		// online player actions
		if (player != null)
		{
			// Circlets removal for all members
			L2ItemInstance circlet = player.getInventory().getItemByItemId(circletId);
			if (circlet != null)
			{
				if (circlet.isEquipped())
					player.getInventory().unEquipItemInSlot(circlet.getLocationSlot());

				player.destroyItemByItemId("CastleCircletRemoval", circletId, 1, player, true);
			}

			// If the actual checked player is the clan leader, check for crown
			if (player.isClanLeader())
			{
				L2ItemInstance crown = player.getInventory().getItemByItemId(6841);
				if (crown != null)
				{
					if (crown.isEquipped())
						player.getInventory().unEquipItemInSlot(crown.getLocationSlot());

					player.destroyItemByItemId("CastleCrownRemoval", 6841, 1, player, true);
				}
			}
			return;
		}

		// offline player actions ; remove all circlets / crowns
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id = ? AND item_id IN (?, 6841)");
			statement.setInt(1, member.getObjectId());
			statement.setInt(2, circletId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("Failed to remove castle circlets && crowns for offline player " + member.getName() + ": " + e.getMessage(), e);
		}
	}

	private static class SingletonHolder
	{
		protected static final CastleManager _instance = new CastleManager();
	}
}
