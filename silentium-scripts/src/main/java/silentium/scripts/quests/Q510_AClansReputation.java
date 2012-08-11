/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import silentium.gameserver.network.serverpackets.SystemMessage;

public class Q510_AClansReputation extends Quest
{
	private final static String qn = "Q510_AClansReputation";

	// NPC
	private static final int Valdis = 31331;

	// Quest Item
	private static final int Claw = 8767;

	// Reward
	private static final int CLAN_POINTS_REWARD = 50; // Quantity of points

	public Q510_AClansReputation(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { Claw };

		addStartNpc(Valdis);
		addTalkId(Valdis);

		addKillId(22215, 22216, 22217);
	}

	public static void main(String[] args)
	{
		new Q510_AClansReputation(510, "Q510_AClansReputation", "A Clan's Reputation");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31331-3.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31331-6.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		L2Clan clan = player.getClan();

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (!player.isClanLeader())
				{
					st.exitQuest(true);
					htmltext = "31331-0.htm";
				}
				else if (clan.getLevel() < 5)
				{
					st.exitQuest(true);
					htmltext = "31331-0.htm";
				}
				else
					htmltext = "31331-1.htm";
				break;

			case QuestState.STARTED:
				if (st.getInt("cond") == 1)
				{
					int count = st.getQuestItemsCount(Claw);
					if (count < 1)
						htmltext = "31331-4.htm";
					else if (count >= 1)
					{
						htmltext = "31331-7.htm";
						st.takeItems(Claw, -1);
						int reward = (CLAN_POINTS_REWARD * count);

						clan.addReputationScore(reward);
						player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CLAN_QUEST_COMPLETED_AND_S1_POINTS_GAINED).addNumber(reward));
						clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
					}
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		// Retrieve the qS of the clan leader.
		QuestState st = getClanLeaderQuestState(player, 1600);
		if (st == null)
			return null;

		if (st.isStarted())
		{
			st.giveItems(Claw, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		return null;
	}
}