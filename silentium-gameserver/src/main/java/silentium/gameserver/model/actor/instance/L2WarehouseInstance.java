/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import java.util.Map;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.itemcontainer.PcFreight;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.EnchantResult;
import silentium.gameserver.network.serverpackets.PackageToList;
import silentium.gameserver.network.serverpackets.WareHouseDepositList;
import silentium.gameserver.network.serverpackets.WareHouseWithdrawalList;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public final class L2WarehouseInstance extends L2NpcInstance
{
	public L2WarehouseInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public boolean isWarehouse()
	{
		return true;
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";
		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return StaticHtmPath.WarehouseHtmPath + pom + ".htm";
	}

	private static void showRetrieveWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());

		if (player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
			return;
		}

		player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.PRIVATE));
	}

	private static void showDepositWindow(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		player.setActiveWarehouse(player.getWarehouse());
		player.tempInventoryDisable();

		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.PRIVATE));
	}

	private static void showDepositWindowClan(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if (player.getClan() != null)
		{
			if (player.getClan().getLevel() == 0)
				player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
			else
			{
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.tempInventoryDisable();
				player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.CLAN));
			}
		}
	}

	private static void showWithdrawWindowClan(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		if ((player.getClanPrivileges() & L2Clan.CP_CL_VIEW_WAREHOUSE) != L2Clan.CP_CL_VIEW_WAREHOUSE)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_CLAN_WAREHOUSE);
			return;
		}

		if (player.getClan().getLevel() == 0)
			player.sendPacket(SystemMessageId.ONLY_LEVEL_1_CLAN_OR_HIGHER_CAN_USE_WAREHOUSE);
		else
		{
			player.setActiveWarehouse(player.getClan().getWarehouse());
			player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
		}
	}

	private void showWithdrawWindowFreight(L2PcInstance player)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		PcFreight freight = player.getFreight();

		if (freight != null)
		{
			if (freight.getSize() > 0)
			{
				if (PlayersConfig.ALT_GAME_FREIGHTS)
					freight.setActiveLocation(0);
				else
					freight.setActiveLocation(getWorldRegion().hashCode());

				player.setActiveWarehouse(freight);
				player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.FREIGHT));
			}
			else
				player.sendPacket(SystemMessageId.NO_ITEM_DEPOSITED_IN_WH);
		}
	}

	private static void showDepositWindowFreight(L2PcInstance player)
	{
		// No other chars in the account of this player
		if (player.getAccountChars().isEmpty())
			player.sendPacket(SystemMessageId.CHARACTER_DOES_NOT_EXIST);
		// One or more chars other than this player for this account
		else
		{
			Map<Integer, String> chars = player.getAccountChars();

			if (chars.size() < 1)
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}

			player.sendPacket(new PackageToList(chars));
		}
	}

	private void showDepositWindowFreight(L2PcInstance player, int obj_Id)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);

		PcFreight freight = player.getDepositedFreight(obj_Id);

		if (PlayersConfig.ALT_GAME_FREIGHTS)
			freight.setActiveLocation(0);
		else
			freight.setActiveLocation(getWorldRegion().hashCode());

		player.setActiveWarehouse(freight);
		player.tempInventoryDisable();
		player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.FREIGHT));
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}

		if (player.getActiveEnchantItem() != null)
		{
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}

		if (command.startsWith("WithdrawP"))
			showRetrieveWindow(player);
		else if (command.equals("DepositP"))
			showDepositWindow(player);
		else if (command.equals("WithdrawC"))
			showWithdrawWindowClan(player);
		else if (command.equals("DepositC"))
			showDepositWindowClan(player);
		else if (command.startsWith("WithdrawF"))
		{
			if (MainConfig.ALLOW_FREIGHT)
				showWithdrawWindowFreight(player);
		}
		else if (command.startsWith("DepositF"))
		{
			if (MainConfig.ALLOW_FREIGHT)
				showDepositWindowFreight(player);
		}
		else if (command.startsWith("FreightChar"))
		{
			if (MainConfig.ALLOW_FREIGHT)
			{
				int startOfId = command.lastIndexOf("_") + 1;
				String id = command.substring(startOfId);
				showDepositWindowFreight(player, Integer.parseInt(id));
			}
		}
		else
			super.onBypassFeedback(player, command);
	}
}