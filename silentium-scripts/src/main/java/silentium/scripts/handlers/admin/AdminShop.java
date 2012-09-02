/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.TradeController;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2TradeList;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.BuyList;

/**
 * This class handles following admin commands: - gmshop = shows menu - buy id = shows shop with respective id
 */
public class AdminShop implements IAdminCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(AdminShop.class.getName());

	private static final String[] ADMIN_COMMANDS = { "admin_buy", "admin_gmshop" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if (command.startsWith("admin_buy")) {
			try {
				handleBuyRequest(activeChar, command.substring(10));
			} catch (IndexOutOfBoundsException e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Please specify buylist.");
			}
		} else if ("admin_gmshop".equals(command))
			AdminHelpPage.showHelpPage(activeChar, "gmshops.htm");

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void handleBuyRequest(final L2PcInstance activeChar, final String command) {
		int val = -1;
		try {
			val = Integer.parseInt(command);
		} catch (Exception e) {
			_log.warn("admin buylist failed:" + command);
		}

		final L2TradeList list = TradeController.getInstance().getBuyList(val);

		if (list != null) {
			activeChar.sendPacket(new BuyList(list, activeChar.getAdena(), 0));

			_log.info("GM: " + activeChar.getName() + '(' + activeChar.getObjectId() + ") opened GM shop id " + val);
		} else
			_log.warn("no buylist with id:" + val);

		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
