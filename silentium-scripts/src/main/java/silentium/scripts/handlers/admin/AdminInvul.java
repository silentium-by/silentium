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
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;

/**
 * This class handles following admin commands: - invul = turns invulnerability on/off
 */
public class AdminInvul implements IAdminCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(AdminInvul.class.getName());
	private static final String[] ADMIN_COMMANDS = { "admin_invul", "admin_setinvul" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_invul".equals(command))
			handleInvul(activeChar);
		if ("admin_setinvul".equals(command)) {
			final L2Object target = activeChar.getTarget();
			if (target instanceof L2PcInstance)
				handleInvul((L2PcInstance) target);
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}

	private static void handleInvul(final L2PcInstance activeChar) {
		final String text;
		if (activeChar.isInvul()) {
			activeChar.setIsInvul(false);
			text = activeChar.getName() + " is now mortal.";

			_log.info("GM: Gm removed invul mode from character " + activeChar.getName() + '(' + activeChar.getObjectId() + ')');
		} else {
			activeChar.setIsInvul(true);
			text = activeChar.getName() + " is now invulnerable.";

			_log.info("GM: Gm activated invul mode for character " + activeChar.getName() + '(' + activeChar.getObjectId() + ')');
		}
		activeChar.sendChatMessage(0, Say2.ALL, "SYS", text);
	}
}
