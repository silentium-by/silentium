/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q152_ShardsOfGolem extends Quest implements ScriptFile
{
	private final static String qn = "Q152_ShardsOfGolem";

	// Items
	private static final int HARRYS_RECEIPT1 = 1008;
	private static final int HARRYS_RECEIPT2 = 1009;
	private static final int GOLEM_SHARD = 1010;
	private static final int TOOL_BOX = 1011;

	// Reward
	private static final int WOODEN_BP = 23;

	// NPCs
	private static final int HARRIS = 30035;
	private static final int ALTRAN = 30283;

	// Mob
	private static final int STONE_GOLEM = 20016;

	public Q152_ShardsOfGolem(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { HARRYS_RECEIPT1, HARRYS_RECEIPT2, GOLEM_SHARD, TOOL_BOX };

		addStartNpc(HARRIS);
		addTalkId(HARRIS, ALTRAN);

		addKillId(STONE_GOLEM);
	}

	public static void onLoad()
	{
		new Q152_ShardsOfGolem(152, "Q152_ShardsOfGolem", "Shards of Golem");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30035-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(HARRYS_RECEIPT1, 1);
		}
		else if (event.equalsIgnoreCase("30283-02.htm"))
		{
			st.set("cond", "2");
			st.takeItems(HARRYS_RECEIPT1, -1);
			st.giveItems(HARRYS_RECEIPT2, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 10 && player.getLevel() <= 17)
					htmltext = "30035-01.htm";
				else
				{
					htmltext = "30035-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case HARRIS:
						if (cond >= 1 && cond <= 3)
							htmltext = "30035-03.htm";
						else if (cond == 4 && st.getQuestItemsCount(TOOL_BOX) == 1)
						{
							htmltext = "30035-04.htm";
							st.takeItems(TOOL_BOX, -1);
							st.takeItems(HARRYS_RECEIPT2, -1);
							st.giveItems(WOODEN_BP, 1);
							st.addExpAndSp(5000, 0);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;

					case ALTRAN:
						if (cond == 1)
							htmltext = "30283-01.htm";
						else if (cond == 2)
							htmltext = "30283-03.htm";
						else if (cond == 3)
						{
							if (st.getQuestItemsCount(GOLEM_SHARD) >= 5 && st.getQuestItemsCount(TOOL_BOX) == 0)
							{
								st.set("cond", "4");
								htmltext = "30283-04.htm";
								st.takeItems(GOLEM_SHARD, -1);
								st.giveItems(TOOL_BOX, 1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						}
						else if (cond == 4)
							htmltext = "30283-05.htm";
						break;
				}
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

		if (st.getInt("cond") == 2)
			if (st.dropQuestItems(GOLEM_SHARD, 1, 5, 300000))
				st.set("cond", "3");

		return null;
	}
}