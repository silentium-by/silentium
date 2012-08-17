/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.configs.ClansConfig;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.ClanTable;

public final class RequestStartPledgeWar extends L2GameClientPacket
{
	private String _pledgeName;

	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		final L2Clan _clan = player.getClan();
		if (_clan == null)
			return;

		if (_clan.getLevel() < 3 || _clan.getMembersCount() < ClansConfig.ALT_CLAN_MEMBERS_FOR_WAR)
		{
			player.sendPacket(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
			return;
		}

		if (!player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}

		final L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);
		if (clan == null)
		{
			player.sendPacket(SystemMessageId.CLAN_WAR_CANNOT_DECLARED_CLAN_NOT_EXIST);
			return;
		}

		if (_clan.getAllyId() == clan.getAllyId() && _clan.getAllyId() != 0)
		{
			player.sendPacket(SystemMessageId.CLAN_WAR_AGAINST_A_ALLIED_CLAN_NOT_WORK);
			return;
		}

		if (clan.getLevel() < 3 || clan.getMembersCount() < ClansConfig.ALT_CLAN_MEMBERS_FOR_WAR)
		{
			player.sendPacket(SystemMessageId.CLAN_WAR_DECLARED_IF_CLAN_LVL3_OR_15_MEMBER);
			return;
		}

		if (_clan.isAtWarWith(clan.getClanId()))
		{
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ALREADY_AT_WAR_WITH_S1_WAIT_5_DAYS).addString(clan.getName()));
			return;
		}

		ClanTable.getInstance().storeclanswars(player.getClanId(), clan.getClanId());

		for (L2PcInstance member : clan.getOnlineMembers(0))
			member.broadcastUserInfo();

		for (L2PcInstance member : _clan.getOnlineMembers(0))
			member.broadcastUserInfo();
	}
}