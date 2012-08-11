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

public class Q295_DreamingOfTheSkies extends Quest
{
	private static final String qn = "Q295_DreamingOfTheSkies";

	// NPC
	private static final int ARIN = 30536;

	// Item
	private static final int FLOATING_STONE = 1492;

	// Reward
	private static final int RING_OF_FIREFLY = 1509;

	// Monster
	private static final int MAGICAL_WEAVER = 20153;

	public Q295_DreamingOfTheSkies(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { FLOATING_STONE };

		addStartNpc(ARIN);
		addTalkId(ARIN);
		addKillId(MAGICAL_WEAVER);
	}

	public static void main(String[] args)
	{
		new Q295_DreamingOfTheSkies(295, "Q295_DreamingOfTheSkies", "Dreaming of the Skies");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30536-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getLevel() >= 11 && player.getLevel() <= 15)
					htmltext = "30536-02.htm";
				else
				{
					htmltext = "30536-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(FLOATING_STONE) < 50)
					htmltext = "30536-04.htm";
				else if (st.getQuestItemsCount(RING_OF_FIREFLY) == 0)
				{
					htmltext = "30536-05.htm";
					st.takeItems(FLOATING_STONE, -1);
					st.giveItems(RING_OF_FIREFLY, 1);
					st.addExpAndSp(0, 500);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				}
				else
				{
					htmltext = "30536-06.htm";
					st.takeItems(FLOATING_STONE, -1);
					st.rewardItems(57, 2400);
					st.addExpAndSp(0, 500);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				}
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
			if (st.dropQuestItems(FLOATING_STONE, 1, 2, 50, 250000))
				st.set("cond", "2");

		return null;
	}
}