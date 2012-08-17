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

public class Q271_ProofOfValor extends Quest implements ScriptFile
{
	private final static String qn = "Q271_ProofOfValor";

	// Items
	private static final int KASHA_WOLF_FANG = 1473;
	private static final int NECKLACE_OF_VALOR = 1507;
	private static final int NECKLACE_OF_COURAGE = 1506;

	// NPC
	private static final int RUKAIN = 30577;

	// Mob
	private static final int KASHA_WOLF = 20475;

	public Q271_ProofOfValor(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { KASHA_WOLF_FANG };

		addStartNpc(RUKAIN);
		addTalkId(RUKAIN);

		addKillId(KASHA_WOLF);
	}

	public static void onLoad()
	{
		new Q271_ProofOfValor(271, "Q271_ProofOfValor", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30577-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);

			if (st.getQuestItemsCount(NECKLACE_OF_COURAGE) >= 1 || st.getQuestItemsCount(NECKLACE_OF_VALOR) >= 1)
				htmltext = "30577-07.htm";
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
				if (player.getRace().ordinal() == 3)
				{
					if (player.getLevel() >= 4 && player.getLevel() <= 8)
					{
						// Different HTM if you are repeating the quest.
						if (st.getQuestItemsCount(NECKLACE_OF_COURAGE) >= 1 || st.getQuestItemsCount(NECKLACE_OF_VALOR) >= 1)
							htmltext = "30577-06.htm";
						else
							htmltext = "30577-02.htm";
					}
					else
					{
						htmltext = "30577-01.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30577-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
				{
					if (st.getQuestItemsCount(NECKLACE_OF_COURAGE) >= 1 || st.getQuestItemsCount(NECKLACE_OF_VALOR) >= 1)
						htmltext = "30577-07.htm";
					else
						htmltext = "30577-04.htm";
				}
				else if (cond == 2)
				{
					htmltext = "30577-05.htm";
					st.takeItems(KASHA_WOLF_FANG, -1);

					if (Rnd.get(100) <= 10)
						st.giveItems(NECKLACE_OF_VALOR, 1);
					else
						st.giveItems(NECKLACE_OF_COURAGE, 1);

					st.unset("cond"); // Reset cond

					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
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

		if (st.getInt("cond") == 1)
		{
			int count = st.getQuestItemsCount(KASHA_WOLF_FANG);
			int chance = (int) (125 * MainConfig.RATE_QUEST_DROP);
			int numItems = chance / 100;
			chance = chance % 100;

			if (Rnd.get(100) <= chance)
				numItems++;

			if (numItems > 0)
			{
				if (count + numItems >= 50)
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
					numItems = 50 - count;
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);

				st.giveItems(KASHA_WOLF_FANG, numItems);
			}
		}

		return null;
	}
}