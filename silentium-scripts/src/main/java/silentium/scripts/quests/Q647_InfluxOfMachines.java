/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q647_InfluxOfMachines extends Quest implements ScriptFile
{
	private final static String qn = "Q647_InfluxOfMachines";

	// Item
	private static final int DESTROYED_GOLEM_SHARD = 8100;

	// NPC
	private static final int Gutenhagen = 32069;

	// Low B-grade weapons recipes
	private static final int recipes[] = { 4963, 4964, 4965, 4966, 4967, 4968, 4969, 4970, 4971, 4972 };

	public Q647_InfluxOfMachines(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { DESTROYED_GOLEM_SHARD };

		addStartNpc(Gutenhagen);
		addTalkId(Gutenhagen);

		for (int i = 22052; i < 22079; i++)
			addKillId(i);
	}

	public static void onLoad()
	{
		new Q647_InfluxOfMachines(647, "Q647_InfluxOfMachines", "Influx of Machines");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("32069-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("32069-06.htm"))
		{
			if (st.getQuestItemsCount(DESTROYED_GOLEM_SHARD) >= 500)
			{
				st.takeItems(DESTROYED_GOLEM_SHARD, -1);
				st.giveItems(recipes[Rnd.get(recipes.length)], 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
				htmltext = "32069-04.htm";
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 46 && player.getLevel() <= 54)
					htmltext = "32069-01.htm";
				else
				{
					htmltext = "32069-03.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "32069-04.htm";
				else if (cond == 2)
				{
					if (st.getQuestItemsCount(DESTROYED_GOLEM_SHARD) >= 500)
						htmltext = "32069-05.htm";
					else
						st.set("cond", "1");
				}

				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		int count = st.getQuestItemsCount(DESTROYED_GOLEM_SHARD);
		int chance = (int) (30 * MainConfig.RATE_QUEST_DROP);
		int numItems = chance / 100;
		chance = chance % 100;

		if (Rnd.get(100) < chance)
			numItems++;

		if (numItems > 0)
		{
			if (count + numItems >= 500)
			{
				st.set("cond", "2");
				st.playSound(QuestState.SOUND_MIDDLE);
				numItems = 500 - count;
			}
			else
				st.playSound(QuestState.SOUND_ITEMGET);

			st.giveItems(DESTROYED_GOLEM_SHARD, numItems);
		}

		return null;
	}
}