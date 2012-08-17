/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.data.crest.CrestCache;
import silentium.gameserver.data.crest.CrestCache.CrestType;
import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;

/**
 * Format : chdb c (id) 0xD0 h (subid) 0x11 d data size b raw data
 * 
 * @author -Wooden-
 */
public final class RequestExSetPledgeCrestLarge extends L2GameClientPacket
{
	private int _length;
	private byte[] _data;

	@Override
	protected void readImpl()
	{
		_length = readD();
		_data = new byte[_length];
		readB(_data);
	}

	@Override
	protected void runImpl()
	{
		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2Clan clan = activeChar.getClan();
		if (clan == null)
			return;

		if (_length < 0)
		{
			activeChar.sendMessage("File transfer error.");
			return;
		}

		if (_length > 2176)
		{
			activeChar.sendMessage("The insignia file size is greater than 2176 bytes.");
			return;
		}

		boolean updated = false;
		int crestLargeId = -1;
		if ((activeChar.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if (_length == 0 || _data == null)
			{
				if (clan.getCrestLargeId() == 0)
					return;

				crestLargeId = 0;
				activeChar.sendMessage("The insignia has been removed.");
				updated = true;
			}
			else
			{
				if (!clan.hasCastle() && !clan.hasHideout())
				{
					activeChar.sendMessage("Only a clan that owns a clan hall or castle can have their crest displayed.");
					return;
				}

				crestLargeId = IdFactory.getInstance().getNextId();
				if (!CrestCache.saveCrest(CrestType.PLEDGE_LARGE, crestLargeId, _data))
				{
					log.info("Error saving large crest for clan " + clan.getName() + " [" + clan.getClanId() + "]");
					return;
				}

				activeChar.sendPacket(SystemMessageId.CLAN_EMBLEM_WAS_SUCCESSFULLY_REGISTERED);
				updated = true;
			}
		}

		if (updated && crestLargeId != -1)
			clan.changeLargeCrest(crestLargeId);
	}
}
