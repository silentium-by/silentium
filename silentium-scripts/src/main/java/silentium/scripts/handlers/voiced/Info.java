/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.voiced;

import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.handler.IVoicedCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.serverpackets.NpcHtmlMessage;

public class Info implements IVoicedCommandHandler {
	private static final String[] VOICED_COMMANDS = { "info" };

	@Override
	public boolean useVoicedCommand(final String command, final L2PcInstance activeChar, final String target) {

		if (command.startsWith("info")) {
			final NpcHtmlMessage html = new NpcHtmlMessage(activeChar.getObjectId());
			html.setFile(StaticHtmPath.NpcHtmPath + "info.htm", activeChar);
			html.replace("%name%", activeChar.getName());
			activeChar.sendPacket(html);
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList() {
		return VOICED_COMMANDS;
	}
}