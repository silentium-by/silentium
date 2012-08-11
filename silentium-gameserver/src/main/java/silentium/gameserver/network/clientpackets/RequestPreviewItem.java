/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.TradeController;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2MercManagerInstance;
import silentium.gameserver.model.actor.instance.L2MerchantInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.ShopPreviewInfo;
import silentium.gameserver.network.serverpackets.UserInfo;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.utils.Util;

/**
 ** @author Gnacik
 */
public final class RequestPreviewItem extends L2GameClientPacket
{
	protected L2PcInstance _activeChar;
	private Map<Integer, Integer> _itemList;
	@SuppressWarnings("unused")
	private int _unk;
	private int _listId;
	private int _count;
	private int[] _items;

	private class RemoveWearItemsTask implements Runnable
	{
		private final L2PcInstance activeChar;

		protected RemoveWearItemsTask(L2PcInstance player)
		{
			activeChar = player;
		}

		@Override
		public void run()
		{
			try
			{
				activeChar.sendPacket(SystemMessageId.NO_LONGER_TRYING_ON);
				activeChar.sendPacket(new UserInfo(activeChar));
			}
			catch (Exception e)
			{
				log.error("", e);
			}
		}
	}

	@Override
	protected void readImpl()
	{
		_unk = readD();
		_listId = readD();
		_count = readD();

		if (_count < 0)
			_count = 0;
		if (_count > 100)
			return; // prevent too long lists

		// Create _items table that will contain all ItemID to Wear
		_items = new int[_count];

		// Fill _items table with all ItemID to Wear
		for (int i = 0; i < _count; i++)
			_items[i] = readD();
	}

	@Override
	protected void runImpl()
	{
		if (_items == null)
			return;

		if (_count < 1 || _listId >= 4000000)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Get the current player and return if null
		_activeChar = getClient().getActiveChar();
		if (_activeChar == null)
			return;

		// If Alternate rule Karma punishment is set to true, forbid Wear to player with Karma
		if (!PlayersConfig.KARMA_PLAYER_CAN_SHOP && _activeChar.getKarma() > 0)
			return;

		// Check current target of the player and the INTERACTION_DISTANCE
		L2Object target = _activeChar.getTarget();
		if (!_activeChar.isGM() && (target == null || !(target instanceof L2MerchantInstance || target instanceof L2MercManagerInstance) || !_activeChar.isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false)))
			return;

		// Get the current merchant targeted by the player
		final L2MerchantInstance merchant = (target instanceof L2MerchantInstance) ? (L2MerchantInstance) target : null;
		if (merchant == null)
		{
			log.warn(getClass().getName() + " Null merchant!");
			return;
		}

		final List<L2TradeList> lists = TradeController.getInstance().getBuyListByNpcId(merchant.getNpcId());
		if (lists == null)
		{
			Util.handleIllegalPlayerAction(_activeChar, _activeChar.getName() + " of account " + _activeChar.getAccountName() + " sent a false BuyList list_id " + _listId, MainConfig.DEFAULT_PUNISH);
			return;
		}

		L2TradeList list = null;
		for (L2TradeList tradeList : lists)
		{
			if (tradeList.getListId() == _listId)
				list = tradeList;
		}

		if (list == null)
		{
			Util.handleIllegalPlayerAction(_activeChar, _activeChar.getName() + " of account " + _activeChar.getAccountName() + " sent a false BuyList list_id " + _listId, MainConfig.DEFAULT_PUNISH);
			return;
		}

		int totalPrice = 0;
		_listId = list.getListId();
		_itemList = new HashMap<>();

		for (int i = 0; i < _count; i++)
		{
			int itemId = _items[i];

			if (!list.containsItemId(itemId))
			{
				Util.handleIllegalPlayerAction(_activeChar, _activeChar.getName() + " of account " + _activeChar.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + itemId, MainConfig.DEFAULT_PUNISH);
				return;
			}

			final L2Item template = ItemTable.getInstance().getTemplate(itemId);
			if (template == null)
				continue;

			final int slot = Inventory.getPaperdollIndex(template.getBodyPart());
			if (slot < 0)
				continue;

			if (_itemList.containsKey(slot))
			{
				_activeChar.sendPacket(SystemMessageId.YOU_CAN_NOT_TRY_THOSE_ITEMS_ON_AT_THE_SAME_TIME);
				return;
			}
			_itemList.put(slot, itemId);

			totalPrice += MainConfig.WEAR_PRICE;
			if (totalPrice > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(_activeChar, _activeChar.getName() + " of account " + _activeChar.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", MainConfig.DEFAULT_PUNISH);
				return;
			}
		}

		// Charge buyer and add tax to castle treasury if not owned by npc clan because a Try On is not Free
		if (totalPrice < 0 || !_activeChar.reduceAdena("Wear", totalPrice, _activeChar.getCurrentFolkNPC(), true))
		{
			_activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}

		if (!_itemList.isEmpty())
		{
			_activeChar.sendPacket(new ShopPreviewInfo(_itemList));

			// Schedule task
			ThreadPoolManager.getInstance().scheduleGeneral(new RemoveWearItemsTask(_activeChar), MainConfig.WEAR_DELAY * 1000);
		}
	}
}
