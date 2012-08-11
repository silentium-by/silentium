/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import javolution.util.FastSet;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.model.ItemRequest;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.TradeList;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.utils.Util;

public final class RequestPrivateStoreBuy extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of one item

	private int _storePlayerId;
	private FastSet<ItemRequest> _items = null;

	@Override
	protected void readImpl()
	{
		_storePlayerId = readD();
		int count = readD();
		if (count <= 0 || count > PlayersConfig.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;

		_items = new FastSet<>();

		for (int i = 0; i < count; i++)
		{
			int objectId = readD();
			long cnt = readD();
			int price = readD();

			if (objectId < 1 || cnt < 1 || price < 0)
			{
				_items = null;
				return;
			}

			_items.add(new ItemRequest(objectId, (int) cnt, price));
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

		L2PcInstance storePlayer = L2World.getInstance().getPlayer(_storePlayerId);
		if (storePlayer == null)
			return;

		if (player.isCursedWeaponEquipped())
			return;

		if (!player.isInsideRadius(storePlayer, 150, true, false))
			return;

		if (!(storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_SELL || storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL))
			return;

		TradeList storeList = storePlayer.getSellList();
		if (storeList == null)
			return;

		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}

		if (storePlayer.getPrivateStoreType() == L2PcInstance.STORE_PRIVATE_PACKAGE_SELL)
		{
			if (storeList.getItemCount() > _items.size())
			{
				Util.handleIllegalPlayerAction(getClient().getActiveChar(), "[RequestPrivateStoreBuy] " + player.getName() + " tried to buy less items than sold in package.", MainConfig.DEFAULT_PUNISH);
				return;
			}
		}

		int result = storeList.privateStoreBuy(player, _items);
		if (result > 0)
		{
			if (result > 1)
				log.warn("PrivateStore buy has failed due to invalid list or request. Player: " + player.getName() + ", Private store of: " + storePlayer.getName());
			return;
		}

		if (storeList.getItemCount() == 0)
		{
			storePlayer.setPrivateStoreType(L2PcInstance.STORE_PRIVATE_NONE);
			storePlayer.broadcastUserInfo();
		}
	}
}
