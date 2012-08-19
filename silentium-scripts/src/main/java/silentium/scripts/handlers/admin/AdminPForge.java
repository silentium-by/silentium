/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import javolution.text.TextBuilder;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.AdminForgePacket;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;

import java.util.StringTokenizer;

/**
 * This class handles commands for gm to forge packets
 *
 * @author Maktakien
 */
public class AdminPForge implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_forge", "admin_forge2", "admin_forge3" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_forge".equals(command))
			showMainPage(activeChar);
		else if (command.startsWith("admin_forge2")) {
			try {
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				final String format = st.nextToken();
				showPage2(activeChar, format);
			} catch (Exception ex) {
				activeChar.sendMessage("Usage: //forge2 format");
			}
		} else if (command.startsWith("admin_forge3")) {
			try {
				final StringTokenizer st = new StringTokenizer(command);
				st.nextToken();
				String format = st.nextToken();
				boolean broadcast = false;

				if ("broadcast".equals(format.toLowerCase())) {
					format = st.nextToken();
					broadcast = true;
				}

				final AdminForgePacket sp = new AdminForgePacket();
				for (int i = 0; i < format.length(); i++) {
					String val = st.nextToken();
					if ("$objid".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getObjectId());
					} else if ("$tobjid".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getTarget().getObjectId());
					} else if ("$bobjid".equals(val.toLowerCase())) {
						if (activeChar.getBoat() != null) {
							val = String.valueOf(activeChar.getBoat().getObjectId());
						}
					} else if ("$clanid".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getCharId());
					} else if ("$allyid".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getAllyId());
					} else if ("$tclanid".equals(val.toLowerCase())) {
						val = String.valueOf(((L2PcInstance) activeChar.getTarget()).getCharId());
					} else if ("$tallyid".equals(val.toLowerCase())) {
						val = String.valueOf(((L2PcInstance) activeChar.getTarget()).getAllyId());
					} else if ("$x".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getX());
					} else if ("$y".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getY());
					} else if ("$z".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getZ());
					} else if ("$heading".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getHeading());
					} else if ("$tx".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getTarget().getX());
					} else if ("$ty".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getTarget().getY());
					} else if ("$tz".equals(val.toLowerCase())) {
						val = String.valueOf(activeChar.getTarget().getZ());
					} else if ("$theading".equals(val.toLowerCase())) {
						val = String.valueOf(((L2PcInstance) activeChar.getTarget()).getHeading());
					}

					sp.addPart(format.getBytes()[i], val);
				}

				if (broadcast)
					activeChar.broadcastPacket(sp);
				else
					activeChar.sendPacket(sp);

				showPage3(activeChar, format, command);
			} catch (Exception ex) {
				activeChar.sendMessage("Usage: //forge or //forge2 format");
			}
		}
		return true;
	}

	private static void showMainPage(final L2PcInstance activeChar) {
		AdminHelpPage.showHelpPage(activeChar, "pforge1.htm");
	}

	private static void showPage2(final L2PcInstance activeChar, final String format) {
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "pforge2.htm");
		adminReply.replace("%format%", format);
		final TextBuilder replyMSG = new TextBuilder();
		for (int i = 0; i < format.length(); i++)
			replyMSG.append(format.charAt(i)).append(" : <edit var=\"v").append(i).append("\" width=100><br1>");
		adminReply.replace("%valueditors%", replyMSG.toString());
		replyMSG.clear();
		for (int i = 0; i < format.length(); i++)
			replyMSG.append(" \\$v").append(i);
		adminReply.replace("%send%", replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private static void showPage3(final L2PcInstance activeChar, final String format, final String command) {
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		adminReply.setFile(StaticHtmPath.AdminHtmPath + "pforge3.htm");
		adminReply.replace("%format%", format);
		adminReply.replace("%command%", command);
		activeChar.sendPacket(adminReply);
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}