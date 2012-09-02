/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.AutoSpawnHandler;
import silentium.gameserver.model.AutoSpawnHandler.AutoSpawnInstance;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.tables.SpawnTable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin Command Handler for Mammon NPCs
 *
 * @author Tempy
 */
public class AdminMammon implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_mammon_find", "admin_mammon_respawn", "admin_list_spawns", "admin_msg" };

	private final boolean _isSealValidation = SevenSigns.getInstance().isSealValidationPeriod();

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		int npcId = 0;
		int teleportIndex = -1;
		final AutoSpawnInstance blackSpawnInst = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_BLACKSMITH_ID, false);
		final AutoSpawnInstance merchSpawnInst = AutoSpawnHandler.getInstance().getAutoSpawnInstance(SevenSigns.MAMMON_MERCHANT_ID, false);

		if (command.startsWith("admin_mammon_find")) {
			try {
				if (command.length() > 17)
					teleportIndex = Integer.parseInt(command.substring(18));
			} catch (Exception NumberFormatException) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Usage: //mammon_find [teleportIndex] (where 1 = Blacksmith, 2 = Merchant)");
			}

			if (!_isSealValidation) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The competition period is currently in effect.");
				return true;
			}
			if (blackSpawnInst != null) {
				final L2Npc[] blackInst = blackSpawnInst.getNPCInstanceList();
				if (blackInst.length > 0) {
					final int x1 = blackInst[0].getX();
					final int y1 = blackInst[0].getY();
					final int z1 = blackInst[0].getZ();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Blacksmith of Mammon: " + x1 + ' ' + y1 + ' ' + z1);
					if (teleportIndex == 1)
						activeChar.teleToLocation(x1, y1, z1, true);
				}
			} else
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Blacksmith of Mammon isn't registered for spawn.");
			if (merchSpawnInst != null) {
				final L2Npc[] merchInst = merchSpawnInst.getNPCInstanceList();
				if (merchInst.length > 0) {
					final int x2 = merchInst[0].getX();
					final int y2 = merchInst[0].getY();
					final int z2 = merchInst[0].getZ();
					activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Merchant of Mammon: " + x2 + ' ' + y2 + ' ' + z2);
					if (teleportIndex == 2)
						activeChar.teleToLocation(x2, y2, z2, true);
				}
			} else
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Merchant of Mammon isn't registered for spawn.");
		} else if (command.startsWith("admin_mammon_respawn")) {
			if (!_isSealValidation) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The competition period is currently in effect.");
				return true;
			}
			if (merchSpawnInst != null) {
				final long merchRespawn = AutoSpawnHandler.getInstance().getTimeToNextSpawn(merchSpawnInst);
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The Merchant of Mammon will respawn in " + merchRespawn / 60000 + " minute(s).");
			} else
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Merchant of Mammon isn't registered for spawn.");
			if (blackSpawnInst != null) {
				final long blackRespawn = AutoSpawnHandler.getInstance().getTimeToNextSpawn(blackSpawnInst);
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "The Blacksmith of Mammon will respawn in " + blackRespawn / 60000 + " minute(s).");
			} else
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Blacksmith of Mammon isn't registered for spawn.");
		} else if (command.startsWith("admin_list_spawns")) {
			try {
				final String[] params = command.split(" ");
				final Pattern pattern = Pattern.compile("[0-9]*");
				final Matcher regexp = pattern.matcher(params[1]);
				if (regexp.matches())
					npcId = Integer.parseInt(params[1]);
				else {
					params[1] = params[1].replace('_', ' ');
					npcId = NpcTable.getInstance().getTemplateByName(params[1]).getNpcId();
				}
				if (params.length > 2)
					teleportIndex = Integer.parseInt(params[2]);
			} catch (Exception e) {
				activeChar.sendPacket(SystemMessage.sendString("Command format is //list_spawns <npcId|npc_name> [tele_index]"));
			}
			SpawnTable.getInstance().findNPCInstances(activeChar, npcId, teleportIndex, false);
		}
		// Used for testing SystemMessage IDs - Use //msg <ID>
		else if (command.startsWith("admin_msg")) {
			try {
				activeChar.sendPacket(SystemMessage.getSystemMessage(Integer.parseInt(command.substring(10).trim())));
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Command format: //msg <SYSTEM_MSG_ID>");
				return false;
			}
		}

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}