/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.ItemContainer;
import silentium.gameserver.model.itemcontainer.PcFreight;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.InventoryUpdate;
import silentium.gameserver.network.serverpackets.ItemList;
import silentium.gameserver.network.serverpackets.StatusUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;

/**
 * @author -Wooden-
 */
public final class RequestPackageSend extends L2GameClientPacket
{
	private List<Item> _items = null;
	private int _objectID;

	@Override
	protected void readImpl()
	{
		_objectID = readD();
		int count = readD();

		if (count < 0 || count > PlayersConfig.MAX_ITEM_IN_PACKET)
			return;

		_items = new ArrayList<>(count);

		for (int i = 0; i < count; i++)
		{
			int id = readD(); // this is some id sent in PackageSendableList
			int cnt = readD();
			_items.add(new Item(id, cnt));
		}
	}

	@Override
	protected void runImpl()
	{
		if (_items == null || _items.isEmpty() || !MainConfig.ALLOW_FREIGHT)
			return;

		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		// player attempts to send freight to the different account
		if (!player.getAccountChars().containsKey(_objectID))
			return;

		final PcFreight freight = player.getDepositedFreight(_objectID);
		player.setActiveWarehouse(freight);

		final ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
			return;

		final L2Npc manager = player.getCurrentFolkNPC();
		if ((manager == null || !player.isInsideRadius(manager, L2Npc.INTERACTION_DISTANCE, false, false)) && !player.isGM())
			return;

		if (warehouse instanceof PcFreight && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			return;
		}

		// Alt game - Karma punishment
		if (!PlayersConfig.KARMA_PLAYER_CAN_USE_WH && player.getKarma() > 0)
			return;

		// Freight price from config or normal price per item slot (30)
		int fee = _items.size() * PlayersConfig.ALT_GAME_FREIGHT_PRICE;
		int currentAdena = player.getAdena();
		int slots = 0;

		for (Item i : _items)
		{
			int objectId = i.id;
			int count = i.count;

			// Check validity of requested item
			L2ItemInstance item = player.checkItemManipulation(objectId, count, "deposit");
			if (item == null)
			{
				log.warn("Error depositing a warehouse object for char " + player.getName() + " (validity check)");
				i.id = 0;
				i.count = 0;
				continue;
			}

			if (!item.isTradable() || item.isQuestItem())
				return;

			// Calculate needed adena and slots
			if (item.getItemId() == 57)
				currentAdena -= count;

			if (!item.isStackable())
				slots += count;
			else if (warehouse.getItemByItemId(item.getItemId()) == null)
				slots++;
		}

		// Item Max Limit Check
		if (!warehouse.validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			return;
		}

		// Check if enough adena and charge the fee
		if (currentAdena < fee || !player.reduceAdena("Warehouse", fee, player.getCurrentFolkNPC(), false))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}

		// Proceed to the transfer
		InventoryUpdate playerIU = MainConfig.FORCE_INVENTORY_UPDATE ? null : new InventoryUpdate();
		for (Item i : _items)
		{
			int objectId = i.id;
			int count = i.count;

			// check for an invalid item
			if (objectId == 0 && count == 0)
				continue;

			L2ItemInstance oldItem = player.getInventory().getItemByObjectId(objectId);
			if (oldItem == null)
			{
				log.warn("Error depositing a warehouse object for char " + player.getName() + " (olditem == null)");
				continue;
			}

			if (oldItem.isHeroItem())
				continue;

			L2ItemInstance newItem = player.getInventory().transferItem("Warehouse", objectId, count, warehouse, player, player.getCurrentFolkNPC());
			if (newItem == null)
			{
				log.warn("Error depositing a warehouse object for char " + player.getName() + " (newitem == null)");
				continue;
			}

			if (playerIU != null)
			{
				if (oldItem.getCount() > 0 && oldItem != newItem)
					playerIU.addModifiedItem(oldItem);
				else
					playerIU.addRemovedItem(oldItem);
			}
		}

		// Send updated item list to the player
		if (playerIU != null)
			player.sendPacket(playerIU);
		else
			player.sendPacket(new ItemList(player, false));

		// Update current load status on player
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
	}

	private class Item
	{
		public int id;
		public int count;

		public Item(int i, int c)
		{
			id = i;
			count = c;
		}
	}
}
