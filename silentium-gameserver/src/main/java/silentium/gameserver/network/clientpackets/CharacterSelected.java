/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import silentium.gameserver.model.CharSelectInfoPackage;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.L2GameClient.GameClientState;
import silentium.gameserver.network.serverpackets.CharSelected;
import silentium.gameserver.network.serverpackets.SignsSky;
import silentium.gameserver.tables.CharNameTable;

public class CharacterSelected extends L2GameClientPacket
{
	// cd
	private int _charSlot;

	@SuppressWarnings("unused")
	private int _unk1; // new in C4
	@SuppressWarnings("unused")
	private int _unk2; // new in C4
	@SuppressWarnings("unused")
	private int _unk3; // new in C4
	@SuppressWarnings("unused")
	private int _unk4; // new in C4

	@Override
	protected void readImpl()
	{
		_charSlot = readD();
		_unk1 = readH();
		_unk2 = readD();
		_unk3 = readD();
		_unk4 = readD();
	}

	@Override
	protected void runImpl()
	{
		final L2GameClient client = getClient();
		if (!client.getFloodProtectors().getCharacterSelect().tryPerformAction("characterSelect"))
			return;

		// we should always be able to acquire the lock
		// but if we cant lock then nothing should be done (ie repeated packet)
		if (client.getActiveCharLock().tryLock())
		{
			try
			{
				// should always be null
				// but if not then this is repeated packet and nothing should be done here
				if (client.getActiveChar() == null)
				{
					final CharSelectInfoPackage info = client.getCharSelection(_charSlot);
					if (info == null)
						return;

					// Selected character is banned. Acts like if nothing occured...
					if (info.getAccessLevel() < 0)
						return;

					// The L2PcInstance must be created here, so that it can be attached to the L2GameClient
					log.trace("Selected slot: " + _charSlot);

					// Load up character from disk
					final L2PcInstance cha = client.loadCharFromDisk(_charSlot);
					if (cha == null)
						return;

					CharNameTable.getInstance().addName(cha);

					cha.setClient(client);
					client.setActiveChar(cha);
					cha.setOnlineStatus(true, true);
					cha.restoreNewbieState();

					sendPacket(new SignsSky());

					client.setState(GameClientState.IN_GAME);
					CharSelected cs = new CharSelected(cha, client.getSessionId().playOkID1);
					sendPacket(cs);
				}
			}
			finally
			{
				client.getActiveCharLock().unlock();
			}
		}
	}
}