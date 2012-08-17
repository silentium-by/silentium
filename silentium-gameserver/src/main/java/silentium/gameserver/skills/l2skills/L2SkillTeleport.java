/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.l2skills;

import silentium.gameserver.data.xml.MapRegionData;
import silentium.gameserver.instancemanager.GrandBossManager;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.Location;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.TvTEvent;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.templates.StatsSet;
import silentium.gameserver.templates.skills.L2SkillType;

public class L2SkillTeleport extends L2Skill
{
	private final String _recallType;
	private final Location _loc;

	public L2SkillTeleport(StatsSet set)
	{
		super(set);

		_recallType = set.getString("recallType", "");
		String coords = set.getString("teleCoords", null);
		if (coords != null)
		{
			String[] valuesSplit = coords.split(",");
			_loc = new Location(Integer.parseInt(valuesSplit[0]), Integer.parseInt(valuesSplit[1]), Integer.parseInt(valuesSplit[2]));
		}
		else
			_loc = null;
	}

	@Override
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if (activeChar instanceof L2PcInstance)
		{
			// Thanks nbd
			if (!TvTEvent.onEscapeUse(((L2PcInstance) activeChar).getObjectId()))
			{
				activeChar.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			// Check invalid states.
			if (activeChar.isAfraid() || ((L2PcInstance) activeChar).isInOlympiadMode() || (GrandBossManager.getInstance().getZone(activeChar) != null && !activeChar.isGM()))
				return;
		}

		for (L2Character target : (L2Character[]) targets)
		{
			if (target == null)
				return;

			if (target instanceof L2PcInstance)
			{
				L2PcInstance targetChar = (L2PcInstance) target;

				// Check invalid states.
				if (targetChar.isFestivalParticipant() || targetChar.isInJail() || targetChar.isInDuel())
					continue;

				if (targetChar != activeChar)
				{
					if (targetChar.isInOlympiadMode())
						continue;

					if (GrandBossManager.getInstance().getZone(targetChar) != null)
						continue;
				}
			}

			Location loc = null;
			if (getSkillType() == L2SkillType.TELEPORT)
			{
				if (_loc != null)
				{
					if (!(target instanceof L2PcInstance) || !target.isFlying())
						loc = _loc;
				}
			}
			else
			{
				if (_recallType.equalsIgnoreCase("Castle"))
					loc = MapRegionData.getInstance().getTeleToLocation(target, MapRegionData.TeleportWhereType.Castle);
				else if (_recallType.equalsIgnoreCase("ClanHall"))
					loc = MapRegionData.getInstance().getTeleToLocation(target, MapRegionData.TeleportWhereType.ClanHall);
				else
					loc = MapRegionData.getInstance().getTeleToLocation(target, MapRegionData.TeleportWhereType.Town);
			}

			if (loc != null)
			{
				if (target instanceof L2PcInstance)
					((L2PcInstance) target).setIsIn7sDungeon(false);

				target.teleToLocation(loc, true);
			}
		}
	}
}