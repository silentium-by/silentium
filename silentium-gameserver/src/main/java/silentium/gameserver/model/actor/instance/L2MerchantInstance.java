/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.gameserver.TradeController;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.model.L2Multisell;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.network.serverpackets.*;
import silentium.gameserver.templates.chars.L2NpcTemplate;

import java.util.StringTokenizer;

/**
 * L2Merchant type, it got buy/sell methods && bypasses.<br>
 * It is used as extends for classes such as L2Fisherman, L2CastleChamberlain, etc.
 */
public class L2MerchantInstance extends L2NpcInstance
{
	public L2MerchantInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return StaticHtmPath.MerchantHtmPath + pom + ".htm";
	}

	private static void showWearWindow(L2PcInstance player, int val)
	{
		player.tempInventoryDisable();

		_log.trace("Showing wearlist.");

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if (list != null)
			player.sendPacket(new ShopPreviewList(list, player.getAdena(), player.getExpertiseIndex()));
		else
		{
			_log.warn("no buylist with id:" + val);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}

	protected final void showBuyWindow(L2PcInstance player, int val)
	{
		double taxRate = 0;

		if (getIsInTown())
			taxRate = getCastle().getTaxRate();

		player.tempInventoryDisable();

		_log.trace("Showing buylist.");

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if (list != null && list.getNpcId().equals(String.valueOf(getNpcId())))
			player.sendPacket(new BuyList(list, player.getAdena(), taxRate));
		else
		{
			_log.warn(player.getName() + " attempted to buy from GM shop.");
			_log.warn("buylist id:" + val);
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void showSellWindow(L2PcInstance player)
	{
		_log.trace("Showing sellList.");

		player.sendPacket(new SellList(player));

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command

		if (actualCommand.equalsIgnoreCase("Buy"))
		{
			if (st.countTokens() < 1)
				return;

			int val = Integer.parseInt(st.nextToken());
			showBuyWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Sell"))
		{
			showSellWindow(player);
		}
		else if (actualCommand.equalsIgnoreCase("Wear") && MainConfig.ALLOW_WEAR)
		{
			if (st.countTokens() < 1)
				return;

			int val = Integer.parseInt(st.nextToken());
			showWearWindow(player, val);
		}
		else if (actualCommand.equalsIgnoreCase("Multisell"))
		{
			if (st.countTokens() < 1)
				return;

			int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().separateAndSend(val, player, false, getCastle().getTaxRate());
		}
		else if (actualCommand.equalsIgnoreCase("Multisell_Shadow"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

			if (player.getLevel() < 40)
				html.setFile(StaticHtmPath.CommonHtmPath + "shadow_item-lowlevel.htm");
			else if (player.getLevel() >= 40 && player.getLevel() < 46)
				html.setFile(StaticHtmPath.CommonHtmPath + "shadow_item_mi_c.htm");
			else if (player.getLevel() >= 46 && player.getLevel() < 52)
				html.setFile(StaticHtmPath.CommonHtmPath + "shadow_item_hi_c.htm");
			else if (player.getLevel() >= 52)
				html.setFile(StaticHtmPath.CommonHtmPath + "shadow_item_b.htm");

			html.replace("%objectId%", String.valueOf(getObjectId()));
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("Exc_Multisell"))
		{
			if (st.countTokens() < 1)
				return;

			int val = Integer.parseInt(st.nextToken());
			L2Multisell.getInstance().separateAndSend(val, player, true, getCastle().getTaxRate());
		}
		else
			super.onBypassFeedback(player, command);
	}
}
