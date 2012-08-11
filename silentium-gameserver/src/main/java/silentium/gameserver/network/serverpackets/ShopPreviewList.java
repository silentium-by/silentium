/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import java.util.Collection;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.model.L2TradeList.L2TradeItem;
import silentium.gameserver.templates.item.L2Item;

public class ShopPreviewList extends L2GameServerPacket
{
	private final int _listId, _money;
	private int _expertise;
	private final Collection<L2TradeItem> _list;

	public ShopPreviewList(L2TradeList list, int currentMoney, int expertiseIndex)
	{
		_listId = list.getListId();
		_list = list.getItems();
		_money = currentMoney;
		_expertise = expertiseIndex;
	}

	public ShopPreviewList(Collection<L2TradeItem> lst, int listId, int currentMoney)
	{
		_listId = listId;
		_list = lst;
		_money = currentMoney;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xef);
		writeC(0xc0); // ?
		writeC(0x13); // ?
		writeC(0x00); // ?
		writeC(0x00); // ?
		writeD(_money); // current money
		writeD(_listId);

		int newlength = 0;
		for (L2TradeItem item : _list)
		{
			if (item.getTemplate().getCrystalType() <= _expertise && item.getTemplate().isEquipable())
				newlength++;
		}
		writeH(newlength);

		for (L2TradeItem item : _list)
		{
			if (item.getTemplate().getCrystalType() <= _expertise && item.getTemplate().isEquipable())
			{
				writeD(item.getItemId());
				writeH(item.getTemplate().getType2()); // item type2

				if (item.getTemplate().getType1() != L2Item.TYPE1_ITEM_QUESTITEM_ADENA)
					writeH(item.getTemplate().getBodyPart()); // slot
				else
					writeH(0x00); // slot

				writeD(MainConfig.WEAR_PRICE);
			}
		}
	}
}