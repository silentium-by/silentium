/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */

package silentium.gameserver.network.serverpackets;

import java.util.Random;

import silentium.gameserver.configs.CustomConfig;
import silentium.gameserver.model.L2World;

/**
 * @author Z3r0C00L
 */

public final class SendStatus extends L2GameServerPacket
{
	private int online_players = 0;
	private int max_online = 0;
	private int online_priv_store = 0;
	private float priv_store_factor = 0;

	@Override
	protected final void writeImpl()
	{
		Random ppc = new Random();
		online_players = L2World.getInstance().getAllPlayersCount() + CustomConfig.RWHO_ONLINE_INCREMENT;

		if (online_players > CustomConfig.RWHO_MAX_ONLINE)
			CustomConfig.RWHO_MAX_ONLINE = online_players;
		max_online = CustomConfig.RWHO_MAX_ONLINE;
		priv_store_factor = CustomConfig.RWHO_PRIV_STORE_FACTOR;
		online_players = L2World.getInstance().getAllPlayersCount() + L2World.getInstance().getAllPlayersCount() * CustomConfig.RWHO_ONLINE_INCREMENT / 100 + CustomConfig.RWHO_FORCE_INC;
		online_priv_store = (int) (online_players * priv_store_factor / 100);

		writeC(0x00); // Packet ID
		writeD(0x01); // World ID
		writeD(max_online); // Max Online
		writeD(online_players); // Current Online
		writeD(online_players); // Current Online
		writeD(online_priv_store); // Priv.Sotre Chars

		if (CustomConfig.RWHO_SEND_TRASH)
		{
			writeH(0x30);
			writeH(0x2C);
			writeH(0x36);
			writeH(0x2C);

			if (CustomConfig.RWHO_ARRAY[12] == CustomConfig.RWHO_KEEP_STAT)
			{
				int z;
				z = ppc.nextInt(6);
				if (z == 0)
				{
					z += 2;
				}
				for (int x = 0; x < 8; x++)
				{
					if (x == 4)
					{
						CustomConfig.RWHO_ARRAY[x] = 44;
					}
					else
					{
						CustomConfig.RWHO_ARRAY[x] = 51 + ppc.nextInt(z);
					}
				}
				CustomConfig.RWHO_ARRAY[11] = 37265 + ppc.nextInt(z * 2 + 3);
				CustomConfig.RWHO_ARRAY[8] = 51 + ppc.nextInt(z);
				z = 36224 + ppc.nextInt(z * 2);
				CustomConfig.RWHO_ARRAY[9] = z;
				CustomConfig.RWHO_ARRAY[10] = z;
				CustomConfig.RWHO_ARRAY[12] = 1;
			}

			for (int z = 0; z < 8; z++)
			{
				if (z == 3)
					CustomConfig.RWHO_ARRAY[z] -= 1;
				writeH(CustomConfig.RWHO_ARRAY[z]);
			}
			writeD(CustomConfig.RWHO_ARRAY[8]);
			writeD(CustomConfig.RWHO_ARRAY[9]);
			writeD(CustomConfig.RWHO_ARRAY[10]);
			writeD(CustomConfig.RWHO_ARRAY[11]);
			CustomConfig.RWHO_ARRAY[12]++;
			writeD(0x00);
			writeD(0x02);
		}
	}
}