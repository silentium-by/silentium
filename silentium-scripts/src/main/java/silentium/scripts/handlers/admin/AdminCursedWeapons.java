/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.commons.utils.StringUtil;
import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.CursedWeaponsManager;
import silentium.gameserver.model.CursedWeapon;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;

import java.util.Collection;
import java.util.StringTokenizer;

/**
 * This class handles following admin commands: - cw_info = displays cursed weapon status - cw_remove = removes a cursed weapon from the world,
 * item id or name must be provided - cw_add = adds a cursed weapon into the world, item id or name must be provided. Target will be the weilder
 * - cw_goto = teleports GM to the specified cursed weapon - cw_reload = reloads instance manager
 */
public class AdminCursedWeapons implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_cw_info", "admin_cw_remove", "admin_cw_goto", "admin_cw_reload", "admin_cw_add", "admin_cw_info_menu" };

	private int itemId;

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		final CursedWeaponsManager cwm = CursedWeaponsManager.getInstance();
		int id = 0;

		final StringTokenizer st = new StringTokenizer(command);
		st.nextToken();

		if (command.startsWith("admin_cw_info")) {
			if (!command.contains("menu")) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "====== Cursed Weapons: ======");
				for (final CursedWeapon cw : cwm.getCursedWeapons()) {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "> " + cw.getName() + " (" + cw.getItemId() + ')');
					if (cw.isActivated()) {
						final L2PcInstance pl = cw.getPlayer();
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "  Player holding: " + (pl == null ? "null" : pl.getName()));
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "    Player karma: " + cw.getPlayerKarma());
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "    Time Remaining: " + cw.getTimeLeft() / 60000 + " min.");
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "    Kills : " + cw.getNbKills());
					} else if (cw.isDropped()) {
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "  Lying on the ground.");
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "    Time Remaining: " + cw.getTimeLeft() / 60000 + " min.");
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "    Kills : " + cw.getNbKills());
					} else
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "  Don't exist in the world.");

					activeChar.sendPacket(SystemMessageId.FRIEND_LIST_FOOTER);
				}
			} else {
				final Collection<CursedWeapon> cws = cwm.getCursedWeapons();
				final StringBuilder replyMSG = new StringBuilder(cws.size() * 300);
				final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
				adminReply.setFile(StaticHtmPath.AdminHtmPath + "cwinfo.htm");
				for (final CursedWeapon cw : cwm.getCursedWeapons()) {
					itemId = cw.getItemId();

					StringUtil.append(replyMSG, "<table width=280><tr><td>Name:</td><td>", cw.getName(), "</td></tr>");

					if (cw.isActivated()) {
						final L2PcInstance pl = cw.getPlayer();
						StringUtil.append(replyMSG, "<tr><td>Weilder:</td><td>", pl == null ? "null" : pl.getName(), "</td></tr>" + "<tr><td>Karma:</td><td>", String.valueOf(cw.getPlayerKarma()), "</td></tr>" + "<tr><td>Kills:</td><td>", String.valueOf(cw.getPlayerPkKills()), "/", String.valueOf(cw.getNbKills()), "</td></tr>" + "<tr><td>Time remaining:</td><td>", String.valueOf(cw.getTimeLeft() / 60000), " min.</td></tr>" + "<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ", String.valueOf(itemId), "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>"
								+ "<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ", String.valueOf(itemId), "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>");
					} else if (cw.isDropped()) {
						StringUtil.append(replyMSG, "<tr><td>Position:</td><td>Lying on the ground</td></tr>" + "<tr><td>Time remaining:</td><td>", String.valueOf(cw.getTimeLeft() / 60000), " min.</td></tr>" + "<tr><td>Kills:</td><td>", String.valueOf(cw.getNbKills()), "</td></tr>" + "<tr><td><button value=\"Remove\" action=\"bypass -h admin_cw_remove ", String.valueOf(itemId), "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td>" + "<td><button value=\"Go\" action=\"bypass -h admin_cw_goto ", String.valueOf(itemId), "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td></tr>");
					} else {
						StringUtil.append(replyMSG, "<tr><td>Position:</td><td>Doesn't exist.</td></tr>" + "<tr><td><button value=\"Give to Target\" action=\"bypass -h admin_cw_add ", String.valueOf(itemId), "\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\"></td><td></td></tr>");
					}

					replyMSG.append("</table><br>");
				}
				adminReply.replace("%cwinfo%", replyMSG.toString());
				activeChar.sendPacket(adminReply);
			}
		} else if (command.startsWith("admin_cw_reload"))
			cwm.reload();
		else {
			CursedWeapon cw = null;
			try {
				String parameter = st.nextToken();
				if (parameter.matches("[0-9]*"))
					id = Integer.parseInt(parameter);
				else {
					parameter = parameter.replace('_', ' ');
					for (final CursedWeapon cwp : cwm.getCursedWeapons()) {
						if (cwp.getName().toLowerCase().contains(parameter.toLowerCase())) {
							id = cwp.getItemId();
							break;
						}
					}
				}
				cw = cwm.getCursedWeapon(id);
				if (cw == null) {
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Unknown cursed weapon ID.");
					return false;
				}

				if (command.startsWith("admin_cw_remove "))
					cw.endOfLife();
				else if (command.startsWith("admin_cw_goto "))
					cw.goTo(activeChar);
				else if (command.startsWith("admin_cw_add")) {
					if (cw.isActive())
						activeChar.sendChatMessage(0, Say2.ALL, "SYS", "This cursed weapon is already active.");
					else {
						final L2Object target = activeChar.getTarget();
						if (target instanceof L2PcInstance)
							((L2PcInstance) target).addItem("AdminCursedWeaponAdd", id, 1, target, true);
						else
							activeChar.addItem("AdminCursedWeaponAdd", id, 1, activeChar, true);

						// Start the Life Task
						cw.setEndTime(System.currentTimeMillis() + cw.getDuration() * 60000L);
						cw.reActivate(false);
					}
				} else
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Unknown command.");
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //cw_remove|//cw_goto|//cw_add <itemid|name>");
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}