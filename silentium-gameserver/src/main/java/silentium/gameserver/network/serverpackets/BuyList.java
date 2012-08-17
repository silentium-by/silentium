/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import java.util.Collection;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.model.L2TradeList.L2TradeItem;

public final class BuyList extends L2GameServerPacket
{
	private final int _listId, _money;
	private final Collection<L2TradeItem> _list;
	private double _taxRate = 0;

	public BuyList(L2TradeList list, int currentMoney, double taxRate)
	{
		_listId = list.getListId();
		_list = list.getItems();
		_money = currentMoney;
		_taxRate = taxRate;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x11);
		writeD(_money);
		writeD(_listId);
		writeH(_list.size());

		for (L2TradeItem item : _list)
		{
			if (item != null && (item.getCurrentCount() > 0 || !item.hasLimitedStock()))
			{
				writeH(item.getTemplate().getType1());
				writeD(item.getItemId());
				writeD(item.getItemId());
				writeD(item.getCurrentCount() < 0 ? 0 : item.getCurrentCount());
				writeH(item.getTemplate().getType2());
				writeH(0x00); // TODO: L2ItemInstance getCustomType1()
				writeD(item.getTemplate().getBodyPart());
				writeH(0x00); // TODO: L2ItemInstance getEnchantLevel()
				writeH(0x00); // TODO: L2ItemInstance getCustomType2()
				writeH(0x00);

				if (item.getItemId() >= 3960 && item.getItemId() <= 4026)
					writeD((int) (item.getPrice() * MainConfig.RATE_SIEGE_GUARDS_PRICE * (1 + _taxRate)));
				else
					writeD((int) (item.getPrice() * (1 + _taxRate)));
			}
		}
	}
}