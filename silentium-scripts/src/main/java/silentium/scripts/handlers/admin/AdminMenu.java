/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;

public class AdminMenu implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_char_manage", "admin_teleport_character_to_menu" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_char_manage".equals(command))
			showMainPage(activeChar);
		else if (command.startsWith("admin_teleport_character_to_menu")) {
			final String[] data = command.split(" ");
			if (data.length == 5) {
				final String playerName = data[1];
				final L2PcInstance player = L2World.getInstance().getPlayer(playerName);
				if (player != null)
					teleportCharacter(player, Integer.parseInt(data[2]), Integer.parseInt(data[3]), Integer.parseInt(data[4]), activeChar);
			}
			showMainPage(activeChar);
		}

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void teleportCharacter(final L2PcInstance player, final int x, final int y, final int z, final L2PcInstance activeChar) {
		if (player != null) {
			player.sendMessage("A GM is teleporting you.");
			player.teleToLocation(x, y, z, true);
		}
		showMainPage(activeChar);
	}

	private static void showMainPage(final L2PcInstance activeChar) {
		AdminHelpPage.showHelpPage(activeChar, "charmanage.htm");
	}
}