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

public class Q296_TarantulasSpiderSilk extends Quest implements ScriptFile
{
	private static final String qn = "Q296_TarantulasSpiderSilk";

	// NPCs
	private static final int MION = 30519;
	private static final int DEFENTER_NATHAN = 30548;

	// Quest Items
	private static final int TARANTULA_SPIDER_SILK = 1493;
	private static final int TARANTULA_SPINNERETTE = 1494;

	// Items
	private static final int RING_OF_RACCOON = 1508;
	private static final int RING_OF_FIREFLY = 1509;

	// Monsters
	private static final int HUNTER_TARANTULA = 20403;
	private static final int PLUNDER_TARANTULA = 20508;

	public Q296_TarantulasSpiderSilk(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { TARANTULA_SPIDER_SILK, TARANTULA_SPINNERETTE };

		addStartNpc(MION);
		addTalkId(MION, DEFENTER_NATHAN);
		addKillId(HUNTER_TARANTULA, PLUNDER_TARANTULA);
	}

	public static void onLoad()
	{
		new Q296_TarantulasSpiderSilk(296, "Q296_TarantulasSpiderSilk", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30519-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30519-06.htm"))
		{
			st.takeItems(TARANTULA_SPIDER_SILK, -1);
			st.takeItems(TARANTULA_SPINNERETTE, -1);
			st.exitQuest(true);
			st.playSound(QuestState.SOUND_FINISH);
		}
		else if (event.equalsIgnoreCase("30548-02.htm"))
		{
			if (st.getQuestItemsCount(TARANTULA_SPINNERETTE) >= 1)
			{
				htmltext = "30548-03.htm";
				st.takeItems(TARANTULA_SPINNERETTE, 1);
				st.giveItems(TARANTULA_SPIDER_SILK, 15 + Rnd.get(10));
			}
		}
		else if (event.equalsIgnoreCase("30519-09.htm"))
			st.exitQuest(true);

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
				if (player.getLevel() >= 15 && player.getLevel() <= 21)
				{
					if (st.getQuestItemsCount(RING_OF_RACCOON) == 1 || st.getQuestItemsCount(RING_OF_FIREFLY) == 1)
						htmltext = "30519-02.htm";
					else
						htmltext = "30519-08.htm";
				}
				else
				{
					htmltext = "30519-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int count = st.getQuestItemsCount(TARANTULA_SPIDER_SILK);
				switch (npc.getNpcId())
				{
					case MION:
						if (count == 0)
							htmltext = "30519-04.htm";
						else
						{
							htmltext = "30519-05.htm";
							st.takeItems(TARANTULA_SPIDER_SILK, count);
							st.rewardItems(57, count * 30);
						}
						break;

					case DEFENTER_NATHAN:
						htmltext = "30548-01.htm";
						break;
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

		if (st.isStarted())
		{
			int n = Rnd.get(100);
			if (n < 5)
			{
				st.giveItems(TARANTULA_SPINNERETTE, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else if (n < 55)
			{
				st.giveItems(TARANTULA_SPIDER_SILK, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}