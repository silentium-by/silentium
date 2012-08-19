/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.RaidBossSpawnManager;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Spawn;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.tables.SpawnTable;

/**
 * This class handles following admin commands: - delete = deletes target
 */
public class AdminDelete implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_delete" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_delete".equals(command))
			handleDelete(activeChar);

		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void handleDelete(final L2PcInstance activeChar) {
		final L2Object obj = activeChar.getTarget();
		if (obj instanceof L2Npc) {
			final L2Npc target = (L2Npc) obj;
			target.deleteMe();

			final L2Spawn spawn = target.getSpawn();
			if (spawn != null) {
				spawn.stopRespawn();

				if (RaidBossSpawnManager.getInstance().isDefined(spawn.getNpcId()))
					RaidBossSpawnManager.getInstance().deleteSpawn(spawn, true);
				else
					SpawnTable.getInstance().deleteSpawn(spawn, true);
			}

			activeChar.sendMessage("Deleted " + target.getName() + " from " + target.getObjectId() + '.');
		} else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}
}