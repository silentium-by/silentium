/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.templates.item.L2Weapon;

public class GMViewWarehouseWithdrawList extends L2GameServerPacket
{
	private final L2ItemInstance[] _items;
	private final String _playerName;
	private L2PcInstance _activeChar;
	private final int _money;

	public GMViewWarehouseWithdrawList(L2PcInstance cha)
	{
		_activeChar = cha;
		_items = _activeChar.getWarehouse().getItems();
		_playerName = _activeChar.getName();
		_money = _activeChar.getWarehouse().getAdena();
	}

	public GMViewWarehouseWithdrawList(L2Clan clan)
	{
		_playerName = clan.getLeaderName();
		_items = clan.getWarehouse().getItems();
		_money = clan.getWarehouse().getAdena();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x95);
		writeS(_playerName);
		writeD(_money);
		writeH(_items.length);

		for (L2ItemInstance item : _items)
		{
			writeH(item.getItem().getType1());

			writeD(item.getObjectId());
			writeD(item.getItemId());
			writeD(item.getCount());
			writeH(item.getItem().getType2());
			writeH(item.getCustomType1());
			writeD(item.getItem().getBodyPart());
			writeH(item.getEnchantLevel());
			writeH(item.isWeapon() ? ((L2Weapon) item.getItem()).getSoulShotCount() : 0x00);
			writeH(item.isWeapon() ? ((L2Weapon) item.getItem()).getSpiritShotCount() : 0x00);
			writeD(item.getObjectId());
			writeD((item.isWeapon() && item.isAugmented()) ? 0x0000FFFF & item.getAugmentation().getAugmentationId() : 0);
			writeD((item.isWeapon() && item.isAugmented()) ? item.getAugmentation().getAugmentationId() >> 16 : 0);
		}
	}
}