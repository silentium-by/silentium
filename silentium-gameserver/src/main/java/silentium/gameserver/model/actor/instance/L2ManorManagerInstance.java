/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.instancemanager.CastleManorManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.BuyListSeed;
import silentium.gameserver.network.serverpackets.ExShowCropInfo;
import silentium.gameserver.network.serverpackets.ExShowCropSetting;
import silentium.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import silentium.gameserver.network.serverpackets.ExShowProcureCropDetail;
import silentium.gameserver.network.serverpackets.ExShowSeedInfo;
import silentium.gameserver.network.serverpackets.ExShowSeedSetting;
import silentium.gameserver.network.serverpackets.ExShowSellCropList;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public class L2ManorManagerInstance extends L2MerchantInstance
{
	public L2ManorManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		final L2Npc manager = player.getCurrentFolkNPC();
		final boolean isCastle = manager instanceof L2CastleChamberlainInstance;
		if (!(manager instanceof L2ManorManagerInstance || isCastle))
			return;

		if (command.startsWith("manor_menu_select"))
		{
			final Castle castle = manager.getCastle();
			if (isCastle)
			{
				if (player.getClan() == null || castle.getOwnerId() != player.getClanId() || (player.getClanPrivileges() & L2Clan.CP_CS_MANOR_ADMIN) != L2Clan.CP_CS_MANOR_ADMIN)
				{
					manager.showChatWindow(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-noprivs.htm");
					return;
				}
				if (castle.getSiege().getIsInProgress())
				{
					manager.showChatWindow(player, StaticHtmPath.ChamberlainHtmPath + "chamberlain-busy.htm");
					return;
				}
			}

			if (CastleManorManager.getInstance().isUnderMaintenance())
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.sendPacket(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
				return;
			}

			final StringTokenizer st = new StringTokenizer(command, "&");
			final int ask = Integer.parseInt(st.nextToken().split("=")[1]);
			final int state = Integer.parseInt(st.nextToken().split("=")[1]);
			final int time = Integer.parseInt(st.nextToken().split("=")[1]);
			final int castleId = (state < 0) ? castle.getCastleId() : state;

			switch (ask)
			{
				case 1: // Seed purchase
					if (isCastle)
						break;
					if (castleId != getCastle().getCastleId())
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR).addString(manager.getCastle().getName()));
					else
						player.sendPacket(new BuyListSeed(player.getAdena(), castleId, castle.getSeedProduction(CastleManorManager.PERIOD_CURRENT)));
					break;
				case 2: // Crop sales
					if (isCastle)
						break;
					player.sendPacket(new ExShowSellCropList(player, castleId, castle.getCropProcure(CastleManorManager.PERIOD_CURRENT)));
					break;
				case 3: // Current seeds (Manor info)
					if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowSeedInfo(castleId, null));
					else
						player.sendPacket(new ExShowSeedInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
					break;
				case 4: // Current crops (Manor info)
					if (time == 1 && !CastleManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
						player.sendPacket(new ExShowCropInfo(castleId, null));
					else
						player.sendPacket(new ExShowCropInfo(castleId, CastleManager.getInstance().getCastleById(castleId).getCropProcure(time)));
					break;
				case 5: // Basic info (Manor info)
					player.sendPacket(new ExShowManorDefaultInfo());
					break;
				case 6: // Buy harvester
					if (isCastle)
						break;
					((L2MerchantInstance) manager).showBuyWindow(player, 300000 + manager.getNpcId());
					break;
				case 7: // Edit seed setup
					if (!isCastle)
						break;
					if (castle.isNextPeriodApproved())
						player.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM);
					else
						player.sendPacket(new ExShowSeedSetting(castle.getCastleId()));
					break;
				case 8: // Edit crop setup
					if (!isCastle)
						break;
					if (castle.isNextPeriodApproved())
						player.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM);
					else
						player.sendPacket(new ExShowCropSetting(castle.getCastleId()));
					break;
				case 9: // Edit sales (Crop sales)
					player.sendPacket(new ExShowProcureCropDetail(state));
					break;
				default:
					return;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return StaticHtmPath.ManorManagerHtmPath + "manager.htm";
	}

	@Override
	public void showChatWindow(L2PcInstance player)
	{
		if (CastleManorManager.getInstance().isDisabled())
		{
			showChatWindow(player, StaticHtmPath.NpcHtmPath + "npcdefault.htm");
			return;
		}

		if (!player.isGM() && getCastle() != null && getCastle().getCastleId() > 0 && player.getClan() != null && getCastle().getOwnerId() == player.getClanId() && player.isClanLeader())
			showChatWindow(player, StaticHtmPath.ManorManagerHtmPath + "manager-lord.htm");
		else
			showChatWindow(player, StaticHtmPath.ManorManagerHtmPath + "manager.htm");
	}
}
