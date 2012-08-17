/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q360_PlunderTheirSupplies extends Quest implements ScriptFile
{
	private static final String qn = "Q360_PlunderTheirSupplies";

	// NPC
	private static final int COLEMAN = 30873;

	// Items
	private static final int SUPPLY_ITEM = 5872;
	private static final int SUSPICIOUS_DOCUMENT = 5871;
	private static final int RECIPE_OF_SUPPLY = 5870;

	public Q360_PlunderTheirSupplies(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { RECIPE_OF_SUPPLY, SUPPLY_ITEM, SUSPICIOUS_DOCUMENT };

		addStartNpc(COLEMAN);
		addTalkId(COLEMAN);

		addKillId(20666, 20669);
	}

	public static void onLoad()
	{
		new Q360_PlunderTheirSupplies(360, "Q360_PlunderTheirSupplies", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30873-2.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30873-6.htm"))
		{
			st.takeItems(SUPPLY_ITEM, -1);
			st.takeItems(SUSPICIOUS_DOCUMENT, -1);
			st.takeItems(RECIPE_OF_SUPPLY, -1);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getLevel() >= 52 && player.getLevel() <= 59)
					htmltext = "30873-0.htm";
				else
				{
					htmltext = "30873-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(SUPPLY_ITEM) == 0)
					htmltext = "30873-3.htm";
				else
				{
					htmltext = "30873-5.htm";

					int reward = 6000 + st.getQuestItemsCount(SUPPLY_ITEM) * 100 + st.getQuestItemsCount(RECIPE_OF_SUPPLY) * 6000;
					st.takeItems(SUPPLY_ITEM, -1);
					st.takeItems(RECIPE_OF_SUPPLY, -1);
					st.rewardItems(57, reward);
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

		int chance = Rnd.get(10);
		if (chance == 9)
		{
			st.giveItems(SUSPICIOUS_DOCUMENT, 1);

			if (st.getQuestItemsCount(SUSPICIOUS_DOCUMENT) == 5)
			{
				st.takeItems(SUSPICIOUS_DOCUMENT, 5);
				st.giveItems(RECIPE_OF_SUPPLY, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				st.playSound(QuestState.SOUND_ITEMGET);
		}
		else if (chance < 6)
		{
			st.giveItems(SUPPLY_ITEM, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}