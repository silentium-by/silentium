/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.configs.ConfigEngine;
import silentium.gameserver.data.crest.CrestCache;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.xml.AdminCommandAccessRightsData;
import silentium.gameserver.data.xml.DoorData;
import silentium.gameserver.data.xml.NpcWalkerRoutesData;
import silentium.gameserver.data.xml.TeleportLocationData;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.ZoneManager;
import silentium.gameserver.model.L2Multisell;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.tables.GmListTable;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.tables.SkillTable;

import java.util.StringTokenizer;

/**
 * This class handles following admin commands:<br>
 * <br>
 * - admin|admin1/admin2/admin3/admin4 = slots for the starting admin menus<br>
 * - gmliston/gmlistoff = includes/excludes active character from /gmlist results<br>
 * - silence = toggles private messages acceptance mode<br>
 * - tradeoff = toggles trade acceptance mode<br>
 * - reload = reloads specified component from multisell|skill|npc|htm|item|instancemanager<br>
 * - saveolymp = saves olympiad state manually<br>
 * - script_load = loads following script. MUSTN'T be used instead of //reload quest !<br>
 * - manualhero = cycles olympiad and calculate new heroes.
 */
public class AdminAdmin implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_admin", "admin_admin1", "admin_admin2", "admin_admin3", "admin_admin4", "admin_gmliston", "admin_gmlistoff", "admin_silence", "admin_tradeoff", "admin_reload" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if (command.startsWith("admin_admin"))
			showMainPage(activeChar, command);
		else if (command.startsWith("admin_gmliston")) {
			GmListTable.getInstance().showGm(activeChar);
			activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Registered into GMList.");
		} else if (command.startsWith("admin_gmlistoff")) {
			GmListTable.getInstance().hideGm(activeChar);
			activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Removed from GMList.");
		} else if (command.startsWith("admin_silence")) {
			if (activeChar.isInRefusalMode()) // already in message refusal mode
			{
				activeChar.setInRefusalMode(false);
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			} else {
				activeChar.setInRefusalMode(true);
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
		} else if (command.startsWith("admin_tradeoff")) {
			try {
				final String mode = command.substring(15);
				if ("on".equalsIgnoreCase(mode)) {
					activeChar.setTradeRefusal(true);
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Trade refusal enabled");
				} else if ("off".equalsIgnoreCase(mode)) {
					activeChar.setTradeRefusal(false);
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Trade refusal disabled");
				}
			} catch (Exception ex) {
				if (activeChar.getTradeRefusal()) {
					activeChar.setTradeRefusal(false);
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Trade refusal disabled");
				} else {
					activeChar.setTradeRefusal(true);
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Trade refusal enabled");
				}
			}
		} else if (command.startsWith("admin_reload")) {
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			try {
				final String type = st.nextToken();
				if (type.startsWith("acar")) {
					AdminCommandAccessRightsData.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Admin commands rights have been reloaded.");
				} else if (type.startsWith("config")) {
					ConfigEngine.init();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Configs files have been reloaded.");
				} else if (type.startsWith("crest")) {
					CrestCache.load();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Crests have been reloaded.");
				} else if (type.startsWith("door")) {
					DoorData.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Doors instance has been reloaded.");
				} else if (type.startsWith("htm")) {
					HtmCache.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The HTM cache has been reloaded.");
				} else if (type.startsWith("item")) {
					ItemTable.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Items' templates have been reloaded.");
				} else if ("multisell".equals(type)) {
					L2Multisell.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The multisell instance has been reloaded.");
				} else if ("npc".equals(type)) {
					NpcTable.getInstance().reloadAllNpc();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "NPCs templates have been reloaded.");
				} else if (type.startsWith("npcwalker")) {
					NpcWalkerRoutesData.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "NPCwalkers' routes have been reloaded.");
				} else if (type.startsWith("skill")) {
					SkillTable.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Skills' XMLs have been reloaded.");
				} else if (type.startsWith("teleport")) {
					TeleportLocationData.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The teleport location table has been reloaded.");
				} else if (type.startsWith("zone")) {
					ZoneManager.getInstance().reload();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Zones have been reloaded.");
				}
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage : //reload <acar|config|crest|door|htm|item|multisell>");
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage : //reload <npc|npcwalker|quest|scripts|skill|teleport|zone>");
			}
		}

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void showMainPage(final L2PcInstance activeChar, final String command) {
		int mode = 0;
		String filename = null;
		try {
			mode = Integer.parseInt(command.substring(11));
		} catch (Exception e) {
		}

		switch (mode) {
			case 1:
				filename = "main";
				break;
			case 2:
				filename = "game";
				break;
			case 3:
				filename = "effects";
				break;
			case 4:
				filename = "server";
				break;
			default:
				filename = "main";
				break;
		}
		AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");
	}
}