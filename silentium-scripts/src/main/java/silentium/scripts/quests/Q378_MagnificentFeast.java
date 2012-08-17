/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import java.util.HashMap;
import java.util.Map;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q378_MagnificentFeast extends Quest implements ScriptFile
{
	private static final String qn = "Q378_MagnificentFeast";

	// NPC
	private final static int RANSPO = 30594;

	// Items
	private final static int WINE_15 = 5956;
	private final static int WINE_30 = 5957;
	private final static int WINE_60 = 5958;
	private final static int MUSICALS_SCORE = 4421;
	private final static int JSALAD_RECIPE = 1455;
	private final static int JSAUCE_RECIPE = 1456;
	private final static int JSTEAK_RECIPE = 1457;
	private final static int RITRON_DESSERT = 5959;

	// Rewards
	private static final Map<String, int[]> Reward_list = new HashMap<>();

	{
		Reward_list.put("9", new int[] { 847, 1, 5700 });
		Reward_list.put("10", new int[] { 846, 2, 0 });
		Reward_list.put("12", new int[] { 909, 1, 25400 });
		Reward_list.put("17", new int[] { 846, 2, 1200 });
		Reward_list.put("18", new int[] { 879, 1, 6900 });
		Reward_list.put("20", new int[] { 890, 2, 8500 });
		Reward_list.put("33", new int[] { 879, 1, 8100 });
		Reward_list.put("34", new int[] { 910, 1, 0 });
		Reward_list.put("36", new int[] { 848, 1, 2200 });
	}

	public Q378_MagnificentFeast(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(RANSPO);
		addTalkId(RANSPO);
	}

	public static void onLoad()
	{
		new Q378_MagnificentFeast(378, "Q378_MagnificentFeast", "Magnificent Feast");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30594-2.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30594-4a.htm"))
		{
			if (st.getQuestItemsCount(WINE_15) >= 1)
			{
				st.set("cond", "2");
				st.set("score", "1");
				st.takeItems(WINE_15, 1);
				st.playSound(QuestState.SOUND_MIDDLE);

			}
			else
				htmltext = "30594-4.htm";
		}
		else if (event.equalsIgnoreCase("30594-4b.htm"))
		{
			if (st.getQuestItemsCount(WINE_30) >= 1)
			{
				st.set("cond", "2");
				st.set("score", "2");
				st.takeItems(WINE_30, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				htmltext = "30594-4.htm";
		}
		else if (event.equalsIgnoreCase("30594-4c.htm"))
		{
			if (st.getQuestItemsCount(WINE_60) >= 1)
			{
				st.set("cond", "2");
				st.set("score", "4");
				st.takeItems(WINE_60, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				htmltext = "30594-4.htm";
		}
		else if (event.equalsIgnoreCase("30594-6.htm"))
		{
			if (st.getQuestItemsCount(MUSICALS_SCORE) >= 1)
			{
				st.set("cond", "3");
				st.takeItems(MUSICALS_SCORE, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				htmltext = "30594-5.htm";
		}
		else
		{
			int score = st.getInt("score");
			if (event.equalsIgnoreCase("30594-8a.htm"))
			{
				if (st.getQuestItemsCount(JSALAD_RECIPE) >= 1)
				{
					st.set("cond", "4");
					st.takeItems(JSALAD_RECIPE, 1);
					st.playSound(QuestState.SOUND_MIDDLE);
					st.set("score", String.valueOf(score + 8));
				}
				else
					htmltext = "30594-8.htm";
			}
			else if (event.equalsIgnoreCase("30594-8b.htm"))
			{
				if (st.getQuestItemsCount(JSAUCE_RECIPE) >= 1)
				{
					st.set("cond", "4");
					st.takeItems(JSAUCE_RECIPE, 1);
					st.playSound(QuestState.SOUND_MIDDLE);
					st.set("score", String.valueOf(score + 16));
				}
				else
					htmltext = "30594-8.htm";
			}
			else if (event.equalsIgnoreCase("30594-8c.htm"))
			{
				if (st.getQuestItemsCount(JSTEAK_RECIPE) >= 1)
				{
					st.set("cond", "4");
					st.takeItems(JSTEAK_RECIPE, 1);
					st.playSound(QuestState.SOUND_MIDDLE);
					st.set("score", String.valueOf(score + 32));
				}
				else
					htmltext = "30594-8.htm";
			}
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
				if (player.getLevel() >= 20 && player.getLevel() <= 30)
					htmltext = "30594-1.htm";
				else
				{
					st.exitQuest(true);
					htmltext = "30594-0.htm";
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30594-3.htm";
				else if (cond == 2)
				{
					if (st.getQuestItemsCount(MUSICALS_SCORE) >= 1)
						htmltext = "30594-5a.htm";
					else
						htmltext = "30594-5.htm";
				}
				else if (cond == 3)
					htmltext = "30594-7.htm";
				else if (cond == 4)
				{
					String score = st.get("score");
					if (Reward_list.containsKey(score) && st.getQuestItemsCount(RITRON_DESSERT) >= 1)
					{
						htmltext = "30594-10.htm";

						st.takeItems(RITRON_DESSERT, 1);
						st.giveItems(Reward_list.get(score)[0], Reward_list.get(score)[1]);

						int adena = Reward_list.get(score)[2];
						if (adena > 0)
							st.rewardItems(57, adena);

						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(true);
					}
					else
						htmltext = "30594-9.htm";
				}
		}

		return htmltext;
	}
}