/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q053_LinnaeusSpecialBait extends Quest
{
	private static final String qn = "Q053_LinnaeusSpecialBait";

	// NPC
	private final static int LINNAEUS = 31577;

	// Item
	private final static int CRIMSON_DRAKE_HEART = 7624;

	// Reward
	private final static int FLAMING_FISHING_LURE = 7613;

	// Monster
	private final static int CRIMSON_DRAKE = 20670;

	public Q053_LinnaeusSpecialBait(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { CRIMSON_DRAKE_HEART };

		addStartNpc(LINNAEUS);
		addTalkId(LINNAEUS);

		addKillId(CRIMSON_DRAKE);
	}

	public static void main(String[] args)
	{
		new Q053_LinnaeusSpecialBait(53, "Q053_LinnaeusSpecialBait", "Linnaues' Special Bait");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31577-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31577-07.htm") && st.getQuestItemsCount(CRIMSON_DRAKE_HEART) == 100)
		{
			htmltext = "31577-06.htm";
			st.rewardItems(FLAMING_FISHING_LURE, 4);
			st.takeItems(CRIMSON_DRAKE_HEART, 100);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 60 && player.getLevel() <= 62)
					htmltext = "31577-01.htm";
				else
				{
					htmltext = "31577-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(CRIMSON_DRAKE_HEART) == 100)
					htmltext = "31577-04.htm";
				else
					htmltext = "31577-05.htm";
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 1)
			if (st.dropQuestItems(CRIMSON_DRAKE_HEART, 1, 100, 300000))
				st.set("cond", "2");

		return null;
	}
}