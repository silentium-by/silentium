/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import java.util.List;

import javolution.util.FastList;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.templates.item.L2Item;

public class SellList extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;
	private final int _money;
	private final List<L2ItemInstance> _selllist = new FastList<>();

	public SellList(L2PcInstance player)
	{
		_activeChar = player;
		_money = _activeChar.getAdena();
	}

	@Override
	public final void runImpl()
	{
		for (L2ItemInstance item : _activeChar.getInventory().getItems())
		{
			if (!item.isEquipped() && item.getItem().isSellable() && (_activeChar.getPet() == null || item.getObjectId() != _activeChar.getPet().getControlItemId()))
				_selllist.add(item);
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x10);
		writeD(_money);
		writeD(0x00);
		writeH(_selllist.size());

		L2Item item;
		for (L2ItemInstance temp : _selllist)
		{
			if (temp == null || temp.getItem() == null)
				continue;

			item = temp.getItem();

			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(temp.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeD(item.getBodyPart());
			writeH(temp.getEnchantLevel());
			writeH(temp.getCustomType2());
			writeH(0x00);
			writeD(item.getReferencePrice() / 2);
		}
	}
}