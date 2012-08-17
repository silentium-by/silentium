/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.network.serverpackets.ExQuestInfo;
import silentium.gameserver.templates.chars.L2NpcTemplate;

/**
 * @author LBaldi
 */
public class L2AdventurerInstance extends L2NpcInstance
{
	public L2AdventurerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("raidInfo"))
		{
			int bossLevel = Integer.parseInt(command.substring(9).trim());
			String filename = StaticHtmPath.AdventurerGuildsmanHtmPath + "raid_info/info.htm";
			if (bossLevel != 0)
				filename = StaticHtmPath.AdventurerGuildsmanHtmPath + "raid_info/level" + bossLevel + ".htm";

			showChatWindow(player, filename);
		}
		else if (command.equalsIgnoreCase("questlist"))
			player.sendPacket(ExQuestInfo.STATIC_PACKET);
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return StaticHtmPath.AdventurerGuildsmanHtmPath + pom + ".htm";
	}
}