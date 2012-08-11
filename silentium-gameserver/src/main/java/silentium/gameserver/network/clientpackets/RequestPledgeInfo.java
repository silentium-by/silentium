/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.PledgeInfo;
import silentium.gameserver.tables.ClanTable;

public final class RequestPledgeInfo extends L2GameClientPacket
{
	private int _clanId;

	@Override
	protected void readImpl()
	{
		_clanId = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		final L2Clan clan = ClanTable.getInstance().getClan(_clanId);
		if (clan == null)
		{
			log.warn("Clan data for clanId " + _clanId + " is missing for: " + activeChar.getName());
			return; // we have no clan data ?!? should not happen
		}

		activeChar.sendPacket(new PledgeInfo(clan));
	}

	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
