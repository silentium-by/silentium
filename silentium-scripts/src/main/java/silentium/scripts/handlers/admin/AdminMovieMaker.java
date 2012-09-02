/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.instancemanager.MovieMakerManager;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;

/**
 * @author KKnD
 */
public class AdminMovieMaker implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_addseq", "admin_playseqq", "admin_delsequence", "admin_editsequence", "admin_addsequence", "admin_playsequence", "admin_movie", "admin_updatesequence", "admin_broadcast", "admin_playmovie", "admin_broadmovie" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		if ("admin_movie".equals(command)) {
			MovieMakerManager.getInstance().mainHtm(activeChar);
		} else if (command.startsWith("admin_playseqq")) {
			try {
				MovieMakerManager.getInstance().playSequence(Integer.parseInt(command.substring(15)), activeChar);
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		} else if ("admin_addseq".equals(command)) {
			MovieMakerManager.getInstance().addSequence(activeChar);
		} else if (command.startsWith("admin_delsequence")) {
			try {
				MovieMakerManager.getInstance().deleteSequence(Integer.parseInt(command.substring(18)), activeChar);
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		} else if (command.startsWith("admin_broadcast")) {
			try {
				MovieMakerManager.getInstance().broadcastSequence(Integer.parseInt(command.substring(16)), activeChar);
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		} else if ("admin_playmovie".equals(command)) {
			MovieMakerManager.getInstance().playMovie(0, activeChar);
		} else if ("admin_broadmovie".equals(command)) {
			MovieMakerManager.getInstance().playMovie(1, activeChar);
		} else if (command.startsWith("admin_editsequence")) {
			try {
				MovieMakerManager.getInstance().editSequence(Integer.parseInt(command.substring(19)), activeChar);
			} catch (Exception e) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "You entered an invalid sequence id.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}
		} else {
			final String[] args = command.split(" ");
			if (args.length < 10) {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Some arguments are missing.");
				return false;
			}

			int targ = 0;
			if (activeChar.getTarget() != null)
				targ = activeChar.getTarget().getObjectId();
			else {
				activeChar.sendChatMessage(0, Say2.ALL, "SYS", "Target for camera is missing.");
				MovieMakerManager.getInstance().mainHtm(activeChar);
				return false;
			}

			if (command.startsWith("admin_addsequence")) {
				MovieMakerManager.getInstance().addSequence(activeChar, Integer.parseInt(args[1]), targ, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
			} else if (command.startsWith("admin_playsequence")) {
				MovieMakerManager.getInstance().playSequence(activeChar, targ, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]));
			} else if (command.startsWith("admin_updatesequence")) {
				MovieMakerManager.getInstance().updateSequence(activeChar, Integer.parseInt(args[1]), targ, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
			}
		}
		return true;
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}