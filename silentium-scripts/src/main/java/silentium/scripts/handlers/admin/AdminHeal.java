/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;

/**
 * This class handles following admin commands: - heal = restores HP/MP/CP on target, name or radius
 */
public class AdminHeal implements IAdminCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(AdminHeal.class.getName());
	private static final String[] ADMIN_COMMANDS = { "admin_heal" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_heal".equals(command))
			handleRes(activeChar);
		else if (command.startsWith("admin_heal")) {
			try {
				final String healTarget = command.substring(11);
				handleRes(activeChar, healTarget);
			} catch (StringIndexOutOfBoundsException e) {
				if (MainConfig.DEVELOPER)
					System.out.println("Heal error: " + e);
				activeChar.sendMessage("Incorrect target/radius specified.");
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void handleRes(final L2PcInstance activeChar) {
		handleRes(activeChar, null);
	}

	private static void handleRes(final L2PcInstance activeChar, final String player) {
		L2Object obj = activeChar.getTarget();
		if (player != null) {
			final L2PcInstance plyr = L2World.getInstance().getPlayer(player);

			if (plyr != null)
				obj = plyr;
			else {
				try {
					final int radius = Integer.parseInt(player);
					for (final L2Object object : activeChar.getKnownList().getKnownObjects().values()) {
						if (object instanceof L2Character) {
							final L2Character character = (L2Character) object;
							character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
							if (object instanceof L2PcInstance)
								character.setCurrentCp(character.getMaxCp());
						}
					}
					activeChar.sendMessage("Healed within " + radius + " unit radius.");
					return;
				} catch (NumberFormatException nbe) {
				}
			}
		}

		if (obj == null)
			obj = activeChar;

		if (obj instanceof L2Character) {
			final L2Character target = (L2Character) obj;
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());

			if (target instanceof L2PcInstance)
				target.setCurrentCp(target.getMaxCp());

			_log.info("GM: " + activeChar.getName() + '(' + activeChar.getObjectId() + ") healed character " + target.getName());
		} else
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
	}
}
