/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.HennaData;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.templates.item.L2Henna;
import silentium.gameserver.utils.Util;

/**
 * format cd
 */
public final class RequestHennaEquip extends L2GameClientPacket
{
	private int _symbolId;

	@Override
	protected void readImpl()
	{
		_symbolId = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		final L2Henna henna = HennaData.getInstance().getTemplate(_symbolId);
		if (henna == null)
			return;

		if (!henna.isForThisClass(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
			Util.handleIllegalPlayerAction(activeChar, activeChar.getName() + " of account " + activeChar.getAccountName() + " tried to add a forbidden henna.", MainConfig.DEFAULT_PUNISH);
			return;
		}

		int _count = 0;

		try
		{
			_count = activeChar.getInventory().getItemByItemId(henna.getDyeId()).getCount();
		}
		catch (Exception e)
		{
		}

		if (activeChar.getHennaEmptySlots() == 0)
		{
			activeChar.sendPacket(SystemMessageId.SYMBOLS_FULL);
			return;
		}

		if (_count >= L2Henna.getAmountDyeRequire() && activeChar.getAdena() >= henna.getPrice() && activeChar.addHenna(henna))
		{
			activeChar.destroyItemByItemId("Henna", henna.getDyeId(), L2Henna.getAmountDyeRequire(), activeChar, true);

			// reduceAdena sends a message aswell.
			activeChar.reduceAdena("Henna", henna.getPrice(), activeChar.getCurrentFolkNPC(), true);

			activeChar.sendPacket(SystemMessageId.SYMBOL_ADDED);
		}
		else
			activeChar.sendPacket(SystemMessageId.CANT_DRAW_SYMBOL);
	}
}