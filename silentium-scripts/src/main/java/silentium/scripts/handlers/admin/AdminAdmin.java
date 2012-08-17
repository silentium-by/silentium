/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import java.util.StringTokenizer;

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
import silentium.gameserver.tables.GmListTable;
import silentium.gameserver.tables.ItemTable;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.tables.SkillTable;

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
public class AdminAdmin implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS = { "admin_admin", "admin_admin1", "admin_admin2", "admin_admin3", "admin_admin4", "admin_gmliston", "admin_gmlistoff", "admin_silence", "admin_tradeoff", "admin_reload" };

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("admin_admin"))
			showMainPage(activeChar, command);
		else if (command.startsWith("admin_gmliston"))
		{
			GmListTable.getInstance().showGm(activeChar);
			activeChar.sendMessage("Registered into GMList.");
		}
		else if (command.startsWith("admin_gmlistoff"))
		{
			GmListTable.getInstance().hideGm(activeChar);
			activeChar.sendMessage("Removed from GMList.");
		}
		else if (command.startsWith("admin_silence"))
		{
			if (activeChar.isInRefusalMode()) // already in message refusal mode
			{
				activeChar.setInRefusalMode(false);
				activeChar.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
			}
			else
			{
				activeChar.setInRefusalMode(true);
				activeChar.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
			}
		}
		else if (command.startsWith("admin_tradeoff"))
		{
			try
			{
				String mode = command.substring(15);
				if (mode.equalsIgnoreCase("on"))
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
				else if (mode.equalsIgnoreCase("off"))
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
			}
			catch (Exception ex)
			{
				if (activeChar.getTradeRefusal())
				{
					activeChar.setTradeRefusal(false);
					activeChar.sendMessage("Trade refusal disabled");
				}
				else
				{
					activeChar.setTradeRefusal(true);
					activeChar.sendMessage("Trade refusal enabled");
				}
			}
		}
		else if (command.startsWith("admin_reload"))
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			try
			{
				String type = st.nextToken();
				if (type.startsWith("acar"))
				{
					AdminCommandAccessRightsData.getInstance().reload();
					activeChar.sendMessage("Admin commands rights have been reloaded.");
				}
				else if (type.startsWith("config"))
				{
					ConfigEngine.init();
					activeChar.sendMessage("Configs files have been reloaded.");
				}
				else if (type.startsWith("crest"))
				{
					CrestCache.load();
					activeChar.sendMessage("Crests have been reloaded.");
				}
				else if (type.startsWith("door"))
				{
					DoorData.getInstance().reload();
					activeChar.sendMessage("Doors instance has been reloaded.");
				}
				else if (type.startsWith("htm"))
				{
					HtmCache.getInstance().reload();
					activeChar.sendMessage("The HTM cache has been reloaded.");
				}
				else if (type.startsWith("item"))
				{
					ItemTable.getInstance().reload();
					activeChar.sendMessage("Items' templates have been reloaded.");
				}
				else if (type.equals("multisell"))
				{
					L2Multisell.getInstance().reload();
					activeChar.sendMessage("The multisell instance has been reloaded.");
				}
				else if (type.equals("npc"))
				{
					NpcTable.getInstance().reloadAllNpc();
					activeChar.sendMessage("NPCs templates have been reloaded.");
				}
				else if (type.startsWith("npcwalker"))
				{
					NpcWalkerRoutesData.getInstance().reload();
					activeChar.sendMessage("NPCwalkers' routes have been reloaded.");
				}
				else if (type.startsWith("skill"))
				{
					SkillTable.getInstance().reload();
					activeChar.sendMessage("Skills' XMLs have been reloaded.");
				}
				else if (type.startsWith("teleport"))
				{
					TeleportLocationData.getInstance().reload();
					activeChar.sendMessage("The teleport location table has been reloaded.");
				}
				else if (type.startsWith("zone"))
				{
					ZoneManager.getInstance().reload();
					activeChar.sendMessage("Zones have been reloaded.");
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage : //reload <acar|config|crest|door|htm|item|multisell>");
				activeChar.sendMessage("Usage : //reload <npc|npcwalker|quest|scripts|skill|teleport|zone>");
			}
		}

		return true;
	}

	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}

	private static void showMainPage(L2PcInstance activeChar, String command)
	{
		int mode = 0;
		String filename = null;
		try
		{
			mode = Integer.parseInt(command.substring(11));
		}
		catch (Exception e)
		{
		}

		switch (mode)
		{
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