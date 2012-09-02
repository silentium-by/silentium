/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;

import java.util.StringTokenizer;

public class AdminRideWyvern implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_ride", "admin_unride", };

	private int _petRideId;

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if (command.startsWith("admin_ride")) {
			// command disabled if CW is worn. Warn user.
			if (activeChar.isCursedWeaponEquipped()) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You can't use //ride owning a Cursed Weapon.");
				return false;
			}

			String mount = "";
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command

			if (st.hasMoreTokens()) {
				mount = st.nextToken();

				switch (mount) {
					case "wyvern":
					case "2":
						_petRideId = 12621;
						break;
					case "strider":
					case "1":
						_petRideId = 12526;
						break;
					default:
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Parameter '" + mount + "' isn't recognized for that command.");
						return false;
				}
			} else {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You must enter a parameter for that command.");
				return false;
			}

			// If code reached that place, it means _petRideId has been filled.
			if (activeChar.isMounted())
				activeChar.dismount();
			else if (activeChar.getPet() != null)
				activeChar.getPet().unSummon(activeChar);

			activeChar.mount(_petRideId, 0, false);
		} else if ("admin_unride".equals(command))
			activeChar.dismount();

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}