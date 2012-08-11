/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import java.util.List;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.TradeController;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.model.L2TradeList.L2TradeItem;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2MerchantInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ItemList;
import silentium.gameserver.network.serverpackets.StatusUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.templates.item.L2Item;
import silentium.gameserver.utils.Util;

public final class RequestBuyItem extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 8; // length of the one item

	private int _listId;
	private Item[] _items = null;

	@Override
	protected void readImpl()
	{
		_listId = readD();
		int count = readD();
		if (count <= 0 || count > PlayersConfig.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;

		_items = new Item[count];
		for (int i = 0; i < count; i++)
		{
			int itemId = readD();
			int cnt = readD();
			if (itemId < 1 || cnt < 1)
			{
				_items = null;
				return;
			}
			_items[i] = new Item(itemId, cnt);
		}
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (_items == null)
			return;

		// Alt game - Karma punishment
		if (!PlayersConfig.KARMA_PLAYER_CAN_SHOP && player.getKarma() > 0)
			return;

		L2Npc merchant = null;
		if (!player.isGM())
		{
			merchant = (player.getTarget() instanceof L2MerchantInstance) ? (L2Npc) player.getTarget() : null;
			if (merchant == null || !merchant.canInteract(player))
				return;
		}

		L2TradeList list = null;
		double taxRate = 0;

		if (merchant != null)
		{
			List<L2TradeList> lists = null;
			if (merchant instanceof L2MerchantInstance)
			{
				lists = TradeController.getInstance().getBuyListByNpcId(((L2MerchantInstance) merchant).getNpcId());
				taxRate = merchant.getCastle().getTaxRate();
			}

			if (!player.isGM())
			{
				if (lists == null)
				{
					Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId, MainConfig.DEFAULT_PUNISH);
					return;
				}
				for (L2TradeList tradeList : lists)
				{
					if (tradeList.getListId() == _listId)
						list = tradeList;
				}
			}
			else
				list = TradeController.getInstance().getBuyList(_listId);
		}
		else
			list = TradeController.getInstance().getBuyList(_listId);

		if (list == null)
		{
			Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId, MainConfig.DEFAULT_PUNISH);
			return;
		}
		_listId = list.getListId();

		int subTotal = 0;

		// Check for buylist validity and calculates summary values
		long slots = 0;
		long weight = 0;
		for (Item i : _items)
		{
			int price = -1;

			L2TradeItem tradeItem = list.getItemById(i.getItemId());
			if (tradeItem == null)
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getItemId(), MainConfig.DEFAULT_PUNISH);
				return;
			}

			L2Item template = ItemTable.getInstance().getTemplate(i.getItemId());
			if (template == null)
				continue;

			if (!template.isStackable() && i.getCount() > 1)
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried to purchase invalid quantity of items at the same time.", MainConfig.DEFAULT_PUNISH);
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
				return;
			}

			price = list.getPriceForItemId(i.getItemId());
			if (i.getItemId() >= 3960 && i.getItemId() <= 4026)
				price *= MainConfig.RATE_SIEGE_GUARDS_PRICE;

			if (price < 0)
				return;

			if (price == 0 && !player.isGM())
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried buy item for 0 adena.", MainConfig.DEFAULT_PUNISH);
				return;
			}

			if (tradeItem.hasLimitedStock())
			{
				// trying to buy more then available
				if (i.getCount() > tradeItem.getCurrentCount())
					return;
			}

			if ((Integer.MAX_VALUE / i.getCount()) < price)
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", MainConfig.DEFAULT_PUNISH);
				return;
			}

			// first calculate price per item with tax, then multiply by count
			price = (int) (price * (1 + taxRate));
			subTotal += i.getCount() * price;
			if (subTotal > Integer.MAX_VALUE)
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " tried to purchase over " + Integer.MAX_VALUE + " adena worth of goods.", MainConfig.DEFAULT_PUNISH);
				return;
			}

			weight += i.getCount() * template.getWeight();
			if (!template.isStackable())
				slots += i.getCount();
			else if (player.getInventory().getItemByItemId(i.getItemId()) == null)
				slots++;
		}

		if (!player.isGM() && (weight > Integer.MAX_VALUE || weight < 0 || !player.getInventory().validateWeight((int) weight)))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}

		if (!player.isGM() && (slots > Integer.MAX_VALUE || slots < 0 || !player.getInventory().validateCapacity((int) slots)))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}

		// Charge buyer and add tax to castle treasury if not owned by npc clan
		if ((subTotal < 0) || !player.reduceAdena("Buy", subTotal, player.getCurrentFolkNPC(), false))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		// Proceed the purchase
		for (Item i : _items)
		{
			L2TradeItem tradeItem = list.getItemById(i.getItemId());
			if (tradeItem == null)
			{
				Util.handleIllegalPlayerAction(player, player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + _listId + " and item_id " + i.getItemId(), MainConfig.DEFAULT_PUNISH);
				continue;
			}

			if (tradeItem.hasLimitedStock())
			{
				if (tradeItem.decreaseCount(i.getCount()))
					player.getInventory().addItem("Buy", i.getItemId(), i.getCount(), player, merchant);
			}
			else
				player.getInventory().addItem("Buy", i.getItemId(), i.getCount(), player, merchant);
		}

		// add to castle treasury
		if (merchant instanceof L2MerchantInstance)
			((L2MerchantInstance) merchant).getCastle().addToTreasury((int) (subTotal * taxRate));

		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ItemList(player, true));
	}

	private static class Item
	{
		private final int _itemId, _count;

		public Item(int id, int num)
		{
			_itemId = id;
			_count = num;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getCount()
		{
			return _count;
		}
	}
}