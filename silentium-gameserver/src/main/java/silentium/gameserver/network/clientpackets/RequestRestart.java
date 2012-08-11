/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.SevenSignsFestival;
import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.L2GameClient.GameClientState;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.CharSelectInfo;
import silentium.gameserver.network.serverpackets.RestartResponse;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.taskmanager.AttackStanceTaskManager;

public final class RequestRestart extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}

	@Override
	protected void runImpl()
	{
		final L2PcInstance player = getClient().getActiveChar();
		if (player == null)
			return;

		if (player.getActiveEnchantItem() != null)
		{
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if (player.isLocked())
		{
			log.warn(player.getName() + " tried to restart during class change.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if (player.isInsideZone(L2Character.ZONE_NORESTART))
		{
			player.sendPacket(SystemMessageId.NO_RESTART_HERE);
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if (player.getPrivateStoreType() != 0)
		{
			player.sendMessage("You can't restart while trading.");
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		if (AttackStanceTaskManager.getInstance().getAttackStanceTask(player) && !player.isGM())
		{
			log.debug(player.getName() + " tried to restart while fighting.");

			player.sendPacket(SystemMessageId.CANT_RESTART_WHILE_FIGHTING);
			sendPacket(RestartResponse.valueOf(false));
			return;
		}

		// Prevent player from restarting if they are a festival participant and it is in progress,
		// otherwise notify party members that the player is not longer a participant.
		if (player.isFestivalParticipant())
		{
			if (SevenSignsFestival.getInstance().isFestivalInitialized())
			{
				player.sendMessage("You can't restart while you are a participant in a festival.");
				sendPacket(RestartResponse.valueOf(false));
				return;
			}
			final L2Party playerParty = player.getParty();

			if (playerParty != null)
				player.getParty().broadcastToPartyMembers(SystemMessage.sendString(player.getName() + " has been removed from the upcoming festival."));
		}

		// Remove player from Boss Zone
		player.removeFromBossZone();

		final L2GameClient client = getClient();

		// detach the client from the char so that the connection isnt closed in the deleteMe
		player.setClient(null);

		// removing player from the world
		player.deleteMe();

		client.setActiveChar(null);
		client.setState(GameClientState.AUTHED);

		sendPacket(RestartResponse.valueOf(true));

		// send char list
		final CharSelectInfo cl = new CharSelectInfo(client.getAccountName(), client.getSessionId().playOkID1);
		sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}
}
