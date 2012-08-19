/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.Announcements;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;

import java.util.Collection;

/**
 * This class handles following admin commands: - announce text = announces text to all players - list_announcements = show menu -
 * reload_announcements = reloads announcements from txt file - announce_announcements = announce all stored announcements to all players -
 * add_announcement text = adds text to startup announcements - del_announcement id = deletes announcement with respective id
 */
public class AdminAnnouncements implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_list_announcements", "admin_reload_announcements", "admin_announce_announcements", "admin_add_announcement", "admin_del_announcement", "admin_announce", "admin_announce_menu" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_list_announcements".equals(command)) {
			Announcements.getInstance().listAnnouncements(activeChar);
		} else if ("admin_reload_announcements".equals(command)) {
			Announcements.getInstance().loadAnnouncements();
			Announcements.getInstance().listAnnouncements(activeChar);
		} else if (command.startsWith("admin_announce_menu")) {
			Announcements.handleAnnounce(command, 20);
			Announcements.getInstance().listAnnouncements(activeChar);
		} else if ("admin_announce_announcements".equals(command)) {
			final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();

			for (final L2PcInstance player : pls)
				Announcements.getInstance().showAnnouncements(player);

			Announcements.getInstance().listAnnouncements(activeChar);
		} else if (command.startsWith("admin_add_announcement")) {
			// FIXME the player can send only 16 chars (if you try to send more it sends null), remove this function or not?
			if (!"admin_add_announcement".equals(command)) {
				try {
					final String val = command.substring(23);
					Announcements.getInstance().addAnnouncement(val);
					Announcements.getInstance().listAnnouncements(activeChar);
				} catch (StringIndexOutOfBoundsException e) {
				}// ignore errors
			}
		} else if (command.startsWith("admin_del_announcement")) {
			try {
				final int val = Integer.parseInt(command.substring(23));
				Announcements.getInstance().delAnnouncement(val);
				Announcements.getInstance().listAnnouncements(activeChar);
			} catch (StringIndexOutOfBoundsException e) {
			}
		}
		// Command is admin announce
		else if (command.startsWith("admin_announce")) {
			// Call method from another class
			Announcements.handleAnnounce(command, 15);
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}