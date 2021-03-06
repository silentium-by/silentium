/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver;

import java.util.List;

import javolution.util.FastList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.instancemanager.ItemsOnGroundManager;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2World;
import silentium.gameserver.templates.item.L2EtcItemType;

public class ItemsAutoDestroy
{
	protected static final Logger _log = LoggerFactory.getLogger(ItemsAutoDestroy.class.getName());
	protected List<L2ItemInstance> _items = null;
	protected static long _sleep;

	protected ItemsAutoDestroy()
	{
		_log.info("Initializing ItemsAutoDestroy.");
		_items = new FastList<>();
		_sleep = MainConfig.AUTODESTROY_ITEM_AFTER * 1000;

		if (_sleep == 0) // it should not happend as it is not called when AUTODESTROY_ITEM_AFTER = 0 but we never know..
			_sleep = 3600000;

		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckItemsForDestroy(), 5000, 5000);
	}

	public static ItemsAutoDestroy getInstance()
	{
		return SingletonHolder._instance;
	}

	public synchronized void addItem(L2ItemInstance item)
	{
		item.setDropTime(System.currentTimeMillis());
		_items.add(item);
	}

	public synchronized void removeItems()
	{
		_log.debug("[ItemsAutoDestroy] : " + _items.size() + " items to check.");

		if (_items.isEmpty())
			return;

		long curtime = System.currentTimeMillis();
		for (L2ItemInstance item : _items)
		{
			if (item == null || item.getDropTime() == 0 || item.getLocation() != L2ItemInstance.ItemLocation.VOID)
				_items.remove(item);
			else
			{
				if (item.getItemType() == L2EtcItemType.HERB)
				{
					if ((curtime - item.getDropTime()) > MainConfig.HERB_AUTO_DESTROY_TIME)
					{
						L2World.getInstance().removeVisibleObject(item, item.getWorldRegion());
						L2World.getInstance().removeObject(item);
						_items.remove(item);

						if (MainConfig.SAVE_DROPPED_ITEM)
							ItemsOnGroundManager.getInstance().removeObject(item);
					}
				}
				else if ((curtime - item.getDropTime()) > _sleep)
				{
					L2World.getInstance().removeVisibleObject(item, item.getWorldRegion());
					L2World.getInstance().removeObject(item);
					_items.remove(item);

					if (MainConfig.SAVE_DROPPED_ITEM)
						ItemsOnGroundManager.getInstance().removeObject(item);
				}
			}
		}

		_log.debug("[ItemsAutoDestroy] : " + _items.size() + " items remaining.");
	}

	protected class CheckItemsForDestroy extends Thread
	{
		@Override
		public void run()
		{
			removeItems();
		}
	}

	private static class SingletonHolder
	{
		protected static final ItemsAutoDestroy _instance = new ItemsAutoDestroy();
	}
}
