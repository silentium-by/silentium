/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.model.L2TradeList.L2TradeItem;
import silentium.gameserver.tables.ItemTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class TradeController
{
	private static Logger _log = LoggerFactory.getLogger(TradeController.class.getName());

	private int _nextListId;
	private final Map<Integer, L2TradeList> _lists = new HashMap<>();

	public static TradeController getInstance()
	{
		return SingletonHolder._instance;
	}

	protected TradeController()
	{
		_lists.clear();
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement1 = con.prepareStatement("SELECT shop_id, npc_id FROM merchant_shopids");
			ResultSet rset1 = statement1.executeQuery();

			int itemId, price, maxCount, currentCount, time;
			long saveTimer;
			PreparedStatement statement = con.prepareStatement("SELECT item_id, price, shop_id, order, count, currentCount, time, savetimer FROM merchant_buylists WHERE shop_id=? ORDER BY order ASC");
			while (rset1.next())
			{
				statement.setString(1, String.valueOf(rset1.getInt("shop_id")));
				ResultSet rset = statement.executeQuery();
				statement.clearParameters();

				int shopId = rset1.getInt("shop_id");
				L2TradeList buy1 = new L2TradeList(shopId);

				while (rset.next())
				{
					itemId = rset.getInt("item_id");
					price = rset.getInt("price");
					maxCount = rset.getInt("count");
					currentCount = rset.getInt("currentCount");
					time = rset.getInt("time");
					saveTimer = rset.getLong("saveTimer");

					L2TradeItem item = new L2TradeItem(shopId, itemId);
					if (ItemTable.getInstance().getTemplate(itemId) == null)
					{
						_log.warn("Skipping itemId: " + itemId + " on buylistId: " + buy1.getListId() + ", missing data for that item.");
						continue;
					}

					if (price <= -1)
						price = ItemTable.getInstance().getTemplate(itemId).getReferencePrice();

					if (_log.isDebugEnabled())
					{
						// debug
						double diff = ((double) (price)) / ItemTable.getInstance().getTemplate(itemId).getReferencePrice();
						if (diff < 0.8 || diff > 1.2)
						{
							_log.error("PRICING DEBUG: TradeListId: " + buy1.getListId() + " -  ItemId: " + itemId + " (" + ItemTable.getInstance().getTemplate(itemId).getName() + ") diff: " + diff + " - Price: " + price + " - Reference: " + ItemTable.getInstance().getTemplate(itemId).getReferencePrice());
						}
					}

					item.setPrice(price);

					item.setRestoreDelay(time);
					item.setNextRestoreTime(saveTimer);
					item.setMaxCount(maxCount);

					if (currentCount > -1)
						item.setCurrentCount(currentCount);
					else
						item.setCurrentCount(maxCount);

					buy1.addItem(item);
				}

				buy1.setNpcId(rset1.getString("npc_id"));
				_lists.put(buy1.getListId(), buy1);
				_nextListId = Math.max(_nextListId, buy1.getListId() + 1);

				rset.close();
			}
			statement.close();
			rset1.close();
			statement1.close();

			_log.info("TradeController: Loaded " + _lists.size() + " Buylists.");
		}
		catch (Exception e)
		{
			// problem with initializing spawn, go to next one
			_log.warn("TradeController: Buylists could not be initialized: " + e.getMessage(), e);
		}
	}

	public L2TradeList getBuyList(int listId)
	{
		return _lists.get(listId);
	}

	public List<L2TradeList> getBuyListByNpcId(int npcId)
	{
		List<L2TradeList> lists = new ArrayList<>();
		Collection<L2TradeList> values = _lists.values();

		for (L2TradeList list : values)
		{
			String tradeNpcId = list.getNpcId();
			if (tradeNpcId.startsWith("gm"))
				continue;

			if (npcId == Integer.parseInt(tradeNpcId))
				lists.add(list);
		}
		return lists;
	}

	public void dataCountStore()
	{
		int listId;
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("UPDATE merchant_buylists SET currentCount=? WHERE item_id=? AND shop_id=?");
			for (L2TradeList list : _lists.values())
			{
				if (list.hasLimitedStockItem())
				{
					listId = list.getListId();

					for (L2TradeItem item : list.getItems())
					{
						int currentCount;
						if (item.hasLimitedStock() && (currentCount = item.getCurrentCount()) < item.getMaxCount())
						{
							statement.setInt(1, currentCount);
							statement.setInt(2, item.getItemId());
							statement.setInt(3, listId);
							statement.executeUpdate();
							statement.clearParameters();
						}
					}
				}
			}
			statement.close();
		}
		catch (Exception e)
		{
			_log.error("TradeController: Could not store Count Item: " + e.getMessage(), e);
		}
	}

	public synchronized int getNextId()
	{
		return _nextListId++;
	}

	private static class SingletonHolder
	{
		protected static final TradeController _instance = new TradeController();
	}
}
