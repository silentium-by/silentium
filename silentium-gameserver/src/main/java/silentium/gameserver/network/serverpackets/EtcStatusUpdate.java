/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.serverpackets;

import silentium.gameserver.model.CharEffectList;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Luca Baldi
 */
public class EtcStatusUpdate extends L2GameServerPacket
{
	private final L2PcInstance _activeChar;

	public EtcStatusUpdate(L2PcInstance activeChar)
	{
		_activeChar = activeChar;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0xF3);
		writeD(_activeChar.getCharges());
		writeD(_activeChar.getWeightPenalty());
		writeD((_activeChar.isInRefusalMode() || _activeChar.isChatBanned()) ? 1 : 0);
		writeD(_activeChar.isInsideZone(L2Character.ZONE_DANGERAREA) ? 1 : 0);
		writeD(_activeChar.getExpertisePenalty());
		writeD(_activeChar.isAffected(CharEffectList.EFFECT_FLAG_CHARM_OF_COURAGE) ? 1 : 0);
		writeD(_activeChar.getDeathPenaltyBuffLevel());
	}
}