/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import java.util.Map;

import silentium.gameserver.model.itemcontainer.Inventory;

/**
 * <font color="red">This packet still need rework. The Freya structure is different. Feets and bows aren't shown, and the fact
 * getFromList() returns 0 is wrong.</font><br>
 * <br>
 * Calls the wearlist ("try on" option), and sends items in good paperdoll slot.
 * 
 * @author Gnacik, Tk
 */
public class ShopPreviewInfo extends L2GameServerPacket
{
	private final Map<Integer, Integer> _itemlist;

	public ShopPreviewInfo(Map<Integer, Integer> itemlist)
	{
		_itemlist = itemlist;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xf0);
		writeD(Inventory.PAPERDOLL_TOTALSLOTS);
		// Slots
		writeD(getFromList(Inventory.PAPERDOLL_UNDER));
		writeD(getFromList(Inventory.PAPERDOLL_REAR));
		writeD(getFromList(Inventory.PAPERDOLL_LEAR));
		writeD(getFromList(Inventory.PAPERDOLL_NECK));
		writeD(getFromList(Inventory.PAPERDOLL_RFINGER));
		writeD(getFromList(Inventory.PAPERDOLL_LFINGER));
		writeD(getFromList(Inventory.PAPERDOLL_HEAD));
		writeD(getFromList(Inventory.PAPERDOLL_RHAND)); // good
		writeD(getFromList(Inventory.PAPERDOLL_LHAND)); // good
		writeD(getFromList(Inventory.PAPERDOLL_GLOVES)); // good
		writeD(getFromList(Inventory.PAPERDOLL_CHEST)); // good
		writeD(getFromList(Inventory.PAPERDOLL_LEGS)); // good
		writeD(getFromList(Inventory.PAPERDOLL_FEET)); // show nothing
		writeD(getFromList(Inventory.PAPERDOLL_BACK));
		writeD(getFromList(Inventory.PAPERDOLL_FACE)); // good, or seems so
		writeD(getFromList(Inventory.PAPERDOLL_HAIR));
		writeD(getFromList(Inventory.PAPERDOLL_HAIRALL));
	}

	private int getFromList(int key)
	{
		return ((_itemlist.get(key) != null) ? _itemlist.get(key) : 0);
	}
}