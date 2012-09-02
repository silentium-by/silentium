/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.geo.pathfinding.AbstractNodeLoc;
import silentium.gameserver.geo.pathfinding.PathFinding;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;

import java.util.List;

public class AdminPathNode implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_pn_info", "admin_show_path", "admin_path_debug", "admin_show_pn", "admin_find_path", };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		switch (command) {
			case "admin_pn_info":
				final String[] info = PathFinding.getInstance().getStat();
				if (info == null)
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Not supported");
				else
					for (final String msg : info)
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", msg);
				break;
			case "admin_show_path":

				break;
			case "admin_path_debug":

				break;
			case "admin_show_pn":

				break;
			case "admin_find_path":
				if (MainConfig.GEODATA < 2) {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "PathFinding has not been enabled.");
					return true;
				}

				if (activeChar.getTarget() != null) {
					final List<AbstractNodeLoc> path = PathFinding.getInstance().findPath(activeChar.getX(), activeChar.getY(), (short) activeChar.getZ(), activeChar.getTarget().getX(), activeChar.getTarget().getY(), (short) activeChar.getTarget().getZ(), true);
					if (path == null) {
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "No Route!");
						return true;
					}

					for (final AbstractNodeLoc a : path)
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "x:" + a.getX() + " y:" + a.getY() + " z:" + a.getZ());
				} else
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "No Target!");
				break;
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}