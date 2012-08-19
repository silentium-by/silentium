/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.voiced;

import silentium.gameserver.handler.IVoicedCommandHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author lord_rex
 */
public class ExpGain implements IVoicedCommandHandler {
	private final String[] _voicedCommands = { "expon", "xpon", "expoff", "xpoff" };

	@Override
	public boolean useVoicedCommand(final String command, final L2PcInstance activeChar, final String target) {
		if ("expon".equalsIgnoreCase(command) || "xpon".equalsIgnoreCase(command)) {
			activeChar.setExpOn(true);
			activeChar.sendMessage("You can gain Experience by killing mobs.");
		} else if ("expoff".equalsIgnoreCase(command) || "xpoff".equalsIgnoreCase(command)) {
			activeChar.setExpOn(false);
			activeChar.sendMessage("You can not gain Experience by killing mobs.");
		}
		return true;
	}

	@Override
	public String[] getVoicedCommandList() {
		return _voicedCommands;
	}
}
