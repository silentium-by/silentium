/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.GameTimeController;
import silentium.gameserver.LoginServerThread;
import silentium.gameserver.Shutdown;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.gameserverpackets.ServerStatus;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;

public class AdminMaintenance implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_server",

			"admin_server_shutdown", "admin_server_restart", "admin_server_abort",

			"admin_server_gm_only", "admin_server_all", "admin_server_max_player", };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_server".equals(command))
			sendHtmlForm(activeChar);
		else if (command.startsWith("admin_server_shutdown")) {
			try {
				final int val = Integer.parseInt(command.substring(22));
				serverShutdown(activeChar, val, false);
			} catch (StringIndexOutOfBoundsException e) {
				sendHtmlForm(activeChar);
			}
		} else if (command.startsWith("admin_server_restart")) {
			try {
				final int val = Integer.parseInt(command.substring(21));
				serverShutdown(activeChar, val, true);
			} catch (StringIndexOutOfBoundsException e) {
				sendHtmlForm(activeChar);
			}
		} else if (command.startsWith("admin_server_abort")) {
			serverAbort(activeChar);
		} else if ("admin_server_gm_only".equals(command)) {
			gmOnly();
			activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Server is now GMonly");
			sendHtmlForm(activeChar);
		} else if ("admin_server_all".equals(command)) {
			allowToAll();
			activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Server isn't GMonly anymore");
			sendHtmlForm(activeChar);
		} else if (command.startsWith("admin_server_max_player")) {
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1) {
				st.nextToken();
				final String number = st.nextToken();
				try {
					LoginServerThread.getInstance().setMaxPlayer(Integer.parseInt(number));
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "maxPlayer set to " + number);
					sendHtmlForm(activeChar);
				} catch (NumberFormatException e) {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Max players must be a number.");
				}
			} else
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Format is server_max_player <max>");
		}
		return true;
	}

	private static void sendHtmlForm(final L2PcInstance activeChar) {
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		final int t = GameTimeController.getInstance().getGameTime();
		final int h = t / 60;
		final int m = t % 60;
		final SimpleDateFormat format = new SimpleDateFormat("h:mm a");
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, h);
		cal.set(Calendar.MINUTE, m);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "maintenance.htm");
		adminReply.replace("%count%", String.valueOf(L2World.getInstance().getAllPlayersCount()));
		adminReply.replace("%used%", String.valueOf(Math.round((int) ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576))));
		adminReply.replace("%server_name%", LoginServerThread.getInstance().getServerName());
		adminReply.replace("%status%", LoginServerThread.getInstance().getStatusString());
		adminReply.replace("%max_players%", String.valueOf(LoginServerThread.getInstance().getMaxPlayer()));
		adminReply.replace("%time%", String.valueOf(format.format(cal.getTime())));
		activeChar.sendPacket(adminReply);
	}

	private static void allowToAll() {
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_AUTO);
		MainConfig.SERVER_GMONLY = false;
	}

	private static void gmOnly() {
		LoginServerThread.getInstance().setServerStatus(ServerStatus.STATUS_GM_ONLY);
		MainConfig.SERVER_GMONLY = true;
	}

	private static void serverShutdown(final L2PcInstance activeChar, final int seconds, final boolean restart) {
		Shutdown.getInstance().startShutdown(activeChar, null, seconds, restart);
	}

	private static void serverAbort(final L2PcInstance activeChar) {
		Shutdown.getInstance().abort(activeChar);
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}