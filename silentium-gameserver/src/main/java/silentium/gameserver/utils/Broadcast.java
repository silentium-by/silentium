/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.utils;

import java.util.Collection;

import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.clientpackets.Say2;
import silentium.gameserver.network.serverpackets.CreatureSay;
import silentium.gameserver.network.serverpackets.L2GameServerPacket;

public final class Broadcast
{
	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character that have the Character targetted.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to
	 * send Server->Client Packet<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use
	 * method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 *
	 * @param character
	 *            The character to make checks on.
	 * @param mov
	 *            The packet to send.
	 */
	public static void toPlayersTargettingMyself(L2Character character, L2GameServerPacket mov)
	{
		final Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player == null || player.getTarget() != character)
				continue;

			player.sendPacket(mov);
		}
	}

	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to
	 * send Server->Client Packet<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use
	 * method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 *
	 * @param character
	 *            The character to make checks on.
	 * @param mov
	 *            The packet to send.
	 */
	public static void toKnownPlayers(L2Character character, L2GameServerPacket mov)
	{
		final Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player == null)
				continue;

			player.sendPacket(mov);
		}
	}

	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers (in the specified radius) of the L2Character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just needs to go through _knownPlayers to
	 * send Server->Client Packet and check the distance between the targets.<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use
	 * method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 *
	 * @param character
	 *            The character to make checks on.
	 * @param mov
	 *            The packet to send.
	 * @param radius
	 *            The given radius.
	 */
	public static void toKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius)
	{
		if (radius < 0)
			radius = 1500;

		final Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player != null && character.isInsideRadius(player, radius, false, false))
				player.sendPacket(mov);
		}
	}

	/**
	 * Send a packet to all L2PcInstance in the _KnownPlayers of the L2Character and to the specified character.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * L2PcInstance in the detection area of the L2Character are identified in <B>_knownPlayers</B>.<BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _knownPlayers to
	 * send Server->Client Packet<BR>
	 * <BR>
	 *
	 * @param character
	 *            The character to make checks on.
	 * @param mov
	 *            The packet to send.
	 */
	public static void toSelfAndKnownPlayers(L2Character character, L2GameServerPacket mov)
	{
		if (character instanceof L2PcInstance)
			character.sendPacket(mov);

		toKnownPlayers(character, mov);
	}

	public static void toSelfAndKnownPlayersInRadius(L2Character character, L2GameServerPacket mov, int radius)
	{
		if (radius < 0)
			radius = 600;

		if (character instanceof L2PcInstance)
			character.sendPacket(mov);

		final Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player != null && character.isInsideRadius(player, radius, false, false))
				player.sendPacket(mov);
		}
	}

	public static void toSelfAndKnownPlayersInRadiusSq(L2Character character, L2GameServerPacket mov, int radiusSq)
	{
		if (radiusSq < 0)
			radiusSq = 360000;

		if (character instanceof L2PcInstance)
			character.sendPacket(mov);

		final Collection<L2PcInstance> plrs = character.getKnownList().getKnownPlayers().values();
		for (L2PcInstance player : plrs)
		{
			if (player != null && character.getDistanceSq(player) <= radiusSq)
				player.sendPacket(mov);
		}
	}

	/**
	 * Send a packet to all L2PcInstance present in the world.<BR>
	 * <BR>
	 * <B><U> Concept</U> :</B><BR>
	 * In order to inform other players of state modification on the L2Character, server just need to go through _allPlayers to
	 * send Server->Client Packet<BR>
	 * <BR>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packet to this L2Character (to do this use
	 * method toSelfAndKnownPlayers)</B></FONT><BR>
	 * <BR>
	 *
	 * @param mov
	 *            The packet to send.
	 */
	public static void toAllOnlinePlayers(L2GameServerPacket mov)
	{
		final Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
		for (L2PcInstance onlinePlayer : pls)
		{
			if (onlinePlayer != null && onlinePlayer.isOnline())
				onlinePlayer.sendPacket(mov);
		}
	}

	public static void announceToOnlinePlayers(String text)
	{
		toAllOnlinePlayers(new CreatureSay(0, Say2.ANNOUNCEMENT, "", text));
	}
}