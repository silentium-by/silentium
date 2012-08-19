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
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>admin_unblockip</li>
 * </ul>
 */
public class AdminUnblockIp implements IAdminCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(AdminUnblockIp.class.getName());
	private static final String[] ADMIN_COMMANDS = { "admin_unblockip" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if (command.startsWith("admin_unblockip ")) {
			try {
				final String ipAddress = command.substring(16);
				if (unblockIp(ipAddress, activeChar))
					activeChar.sendMessage("Removed IP " + ipAddress + " from blocklist!");
			} catch (StringIndexOutOfBoundsException e) {
				// Send syntax to the user
				activeChar.sendMessage("Usage mode: //unblockip <ip>");
			}
		}

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static boolean unblockIp(final String ipAddress, final L2PcInstance activeChar) {
		_log.warn("IP removed by GM " + activeChar.getName());
		return true;
	}
}
