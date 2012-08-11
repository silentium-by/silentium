/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q276_TotemOfTheHestui extends Quest
{
	private final static String qn = "Q276_TotemOfTheHestui";

	// NPC
	private static final int TANAPI = 30571;

	// Items
	private static final int KASHA_PARASITE = 1480;
	private static final int KASHA_CRYSTAL = 1481;
	private static final int HESTUIS_TOTEM = 1500;
	private static final int LEATHER_PANTS = 29;

	public Q276_TotemOfTheHestui(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { KASHA_PARASITE, KASHA_CRYSTAL };

		addStartNpc(TANAPI);
		addTalkId(TANAPI);

		addKillId(20479, 27044);
	}

	public static void main(String[] args)
	{
		new Q276_TotemOfTheHestui(276, "Q276_TotemOfTheHestui", "Totem of the Hestui");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30571-03.htm"))
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
				if (player.getRace().ordinal() == 3)
				{
					if (player.getLevel() >= 15 && player.getLevel() <= 21)
						htmltext = "30571-02.htm";
					else
					{
						htmltext = "30571-01.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30571-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(KASHA_CRYSTAL) == 0)
					htmltext = "30571-04.htm";
				else
				{
					htmltext = "30571-05.htm";
					st.takeItems(KASHA_CRYSTAL, -1);
					st.takeItems(KASHA_PARASITE, -1);
					st.giveItems(HESTUIS_TOTEM, 1);
					st.giveItems(LEATHER_PANTS, 1);
					st.exitQuest(true);
					st.playSound(QuestState.SOUND_FINISH);
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

		if (st.getInt("cond") == 1 && !st.hasQuestItems(KASHA_CRYSTAL))
		{
			switch (npc.getNpcId())
			{
				case 20479:
					int count = st.getQuestItemsCount(KASHA_PARASITE);
					int random = Rnd.get(100);

					if ((count >= 70 && random < 90) || (count >= 65 && random < 75) || (count >= 60 && random < 60) || (count >= 52 && random < 45) || (count >= 50 && random < 30))
					{
						st.addSpawn(27044, npc);
						st.takeItems(KASHA_PARASITE, count);
					}
					else
					{
						st.playSound(QuestState.SOUND_ITEMGET);
						st.giveItems(KASHA_PARASITE, 1);
					}
					break;

				case 27044:
					st.playSound(QuestState.SOUND_MIDDLE);
					st.giveItems(KASHA_CRYSTAL, 1);
					st.set("cond", "2");
					break;
			}
		}
		return null;
	}
}