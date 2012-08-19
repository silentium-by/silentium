/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.handlers.admin;

import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.handler.IAdminCommandHandler;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * This class handles teleport admin commands
 */
public class AdminTeleport implements IAdminCommandHandler {
	private static final String[] ADMIN_COMMANDS = { "admin_runmod", "admin_instant_move", "admin_tele", "admin_tele_areas", "admin_goto", "admin_teleportto", // deprecated
			"admin_recall", "admin_recall_party", "admin_recall_clan", "admin_move_to", "admin_sendhome" };

	@Override
	public boolean useAdminCommand(final String command, final L2PcInstance activeChar) {
		// runmod
		if ("admin_runmod".equals(command) || "admin_instant_move".equals(command))
			activeChar.setTeleMode(1);
		if ("admin_runmod tele".equals(command))
			activeChar.setTeleMode(2);
		if ("admin_runmod norm".equals(command))
			activeChar.setTeleMode(0);

		// teleport via panels
		if ("admin_tele".equals(command))
			AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
		if ("admin_tele_areas".equals(command))
			AdminHelpPage.showHelpPage(activeChar, "tele/other.htm");

		// recalls / goto types
		if (command.startsWith("admin_goto") || command.startsWith("admin_teleportto")) {
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1) {
				st.nextToken();
				final String plyr = st.nextToken();
				final L2PcInstance player = L2World.getInstance().getPlayer(plyr);
				if (player == null) {
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}

				teleportToCharacter(activeChar, player);
			}
		} else if (command.startsWith("admin_recall ")) {
			try {
				final String targetName = command.substring(13);
				final L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null) {
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}

				teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_recall_party")) {
			try {
				final String targetName = command.substring(19);
				final L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null) {
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}

				if (player.isInParty()) {
					for (final L2PcInstance pm : player.getParty().getPartyMembers())
						teleportCharacter(pm, activeChar.getX(), activeChar.getY(), activeChar.getZ());

					activeChar.sendMessage("You recall " + player.getName() + "'s party.");
				} else {
					activeChar.sendMessage("You recall " + player.getName() + ", but he isn't in a party.");
					teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
				}
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_recall_clan")) {
			try {
				final String targetName = command.substring(18);
				final L2PcInstance player = L2World.getInstance().getPlayer(targetName);
				if (player == null) {
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}

				final L2Clan clan = player.getClan();
				if (clan != null) {
					final L2PcInstance[] members = clan.getOnlineMembers(0);
					for (final L2PcInstance member : members)
						teleportCharacter(member, activeChar.getX(), activeChar.getY(), activeChar.getZ());

					activeChar.sendMessage("You recall " + player.getName() + "'s clan.");
				} else {
					activeChar.sendMessage("You recall " + player.getName() + ", but he isn't a clan member.");
					teleportCharacter(player, activeChar.getX(), activeChar.getY(), activeChar.getZ());
				}
			} catch (StringIndexOutOfBoundsException e) {
			}
		} else if (command.startsWith("admin_move_to")) {
			try {
				final String val = command.substring(14);
				teleportTo(activeChar, val);
			} catch (Exception e) {
				// Case of empty or missing coordinates
				AdminHelpPage.showHelpPage(activeChar, "teleports.htm");
			}
		} else if (command.startsWith("admin_sendhome")) {
			final StringTokenizer st = new StringTokenizer(command);
			if (st.countTokens() > 1) {
				st.nextToken();
				final String plyr = st.nextToken();
				final L2PcInstance player = L2World.getInstance().getPlayer(plyr);
				if (player == null) {
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return false;
				}

				sendHome(player);
			} else {
				final L2Object target = activeChar.getTarget();
				L2PcInstance player = null;

				// if target isn't a player, select yourself as target
				player = target instanceof L2PcInstance ? (L2PcInstance) target : activeChar;

				sendHome(player);
			}
		}
		return true;
	}

	private static void sendHome(final L2PcInstance player) {
		player.teleToLocation(MapRegionData.TeleportWhereType.Town);
		player.setIsIn7sDungeon(false);
		player.sendMessage("A GM sent you at nearest town.");
	}

	private static void teleportTo(final L2PcInstance activeChar, final String Cords) {
		try {
			final StringTokenizer st = new StringTokenizer(Cords);
			final String x1 = st.nextToken();
			final int x = Integer.parseInt(x1);
			final String y1 = st.nextToken();
			final int y = Integer.parseInt(y1);
			final String z1 = st.nextToken();
			final int z = Integer.parseInt(z1);

			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			activeChar.teleToLocation(x, y, z, false);

			activeChar.sendMessage("You have been teleported to " + Cords + '.');
		} catch (NoSuchElementException nsee) {
			activeChar.sendMessage("Coordinates you entered as parameter [" + Cords + "] are wrong.");
		}
	}

	private static void teleportCharacter(final L2PcInstance player, final int x, final int y, final int z) {
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		player.teleToLocation(x, y, z, true);
		player.sendMessage("A GM is teleporting you.");
	}

	private static void teleportToCharacter(final L2PcInstance activeChar, final L2PcInstance target) {
		if (target.getObjectId() == activeChar.getObjectId())
			activeChar.sendPacket(SystemMessageId.CANNOT_USE_ON_YOURSELF);
		else {
			final int x = target.getX();
			final int y = target.getY();
			final int z = target.getZ();

			activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
			activeChar.teleToLocation(x, y, z, true);
			activeChar.sendMessage("You have teleported to " + target.getName() + '.');
		}
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}