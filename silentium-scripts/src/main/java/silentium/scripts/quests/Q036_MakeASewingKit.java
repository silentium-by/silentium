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

public class Q036_MakeASewingKit extends Quest implements ScriptFile
{
	private static final String qn = "Q036_MakeASewingKit";

	// NPC
	private static final int FERRIS = 30847;

	// Monster
	private static final int IRON_GOLEM = 20566;

	// Items
	private static final int REINFORCED_STEEL = 7163;
	private static final int ARTISANS_FRAME = 1891;
	private static final int ORIHARUKON = 1893;

	// Reward
	private static final int SEWING_KIT = 7078;

	public Q036_MakeASewingKit(int id, String name, String descr)
	{
		super(id, name, descr);
		questItemIds = new int[] { REINFORCED_STEEL };

		addStartNpc(FERRIS);
		addTalkId(FERRIS);

		addKillId(IRON_GOLEM);
	}

	public static void onLoad()
	{
		new Q036_MakeASewingKit(36, "Q036_MakeASewingKit", "Make a Sewing Kit");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equals("30847-1.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equals("30847-3.htm"))
		{
			st.takeItems(REINFORCED_STEEL, 5);
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equals("30847-5.htm"))
		{
			if (st.getQuestItemsCount(ORIHARUKON) >= 10 && st.getQuestItemsCount(ARTISANS_FRAME) >= 10)
			{
				st.takeItems(ORIHARUKON, 10);
				st.takeItems(ARTISANS_FRAME, 10);
				st.giveItems(SEWING_KIT, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "30847-4a.htm";
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
				if (player.getLevel() >= 60)
				{
					QuestState fwear = player.getQuestState("Q037_MakeFormalWear");
					if (fwear != null && fwear.getInt("cond") == 6)
						htmltext = "30847-0.htm";
					else
					{
						htmltext = "30847-0a.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30847-0b.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30847-1a.htm";
				else if (cond == 2)
					htmltext = "30847-2.htm";
				else if (cond == 3)
				{
					if (st.getQuestItemsCount(ORIHARUKON) < 10 || st.getQuestItemsCount(ARTISANS_FRAME) < 10)
						htmltext = "30847-4a.htm";
					else
						htmltext = "30847-4.htm";
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
			if (st.dropAlwaysQuestItems(REINFORCED_STEEL, 1, 5))
				st.set("cond", "2");

		return null;
	}
}