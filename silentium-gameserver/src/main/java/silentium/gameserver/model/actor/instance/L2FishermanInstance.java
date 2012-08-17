/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.actor.instance;

import silentium.gameserver.data.html.StaticHtmPath;
import silentium.gameserver.data.xml.SkillTreeData;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.L2SkillLearn;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.AcquireSkillList;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;

public class L2FishermanInstance extends L2MerchantInstance
{
	public L2FishermanInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom = "";

		if (val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return StaticHtmPath.FishermanHtmPath + pom + ".htm";
	}

	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		if (command.startsWith("FishSkillList"))
		{
			player.setSkillLearningClassId(player.getClassId());
			showFishSkillList(player);
		}
		else
			super.onBypassFeedback(player, command);
	}

	public static void showFishSkillList(L2PcInstance player)
	{
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.SkillType.Fishing);
		boolean empty = true;

		for (L2SkillLearn sl : SkillTreeData.getInstance().getAvailableFishingDwarvenCraftSkills(player))
		{
			L2Skill sk = SkillTable.getInstance().getInfo(sl.getId(), sl.getLevel());
			if (sk == null)
				continue;

			asl.addSkill(sl.getId(), sl.getLevel(), sl.getLevel(), sl.getSpCost(), 1);
			empty = false;
		}

		if (empty)
		{
			int minlevel = SkillTreeData.getInstance().getMinLevelForNewFishingDwarvenCraftSkill(player);

			if (minlevel > 0)
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DO_NOT_HAVE_FURTHER_SKILLS_TO_LEARN_S1).addNumber(minlevel));
			else
				player.sendPacket(SystemMessageId.NO_MORE_SKILLS_TO_LEARN);
		}
		else
			player.sendPacket(asl);

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}