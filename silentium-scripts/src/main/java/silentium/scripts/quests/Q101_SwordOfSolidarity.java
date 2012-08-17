/*
 * This program is free software: you can redistribute it &&/or modify it under the terms of the GNU General Public License as published by the
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

/**
 * @author Demon
 */

public class Q101_SwordOfSolidarity extends Quest implements ScriptFile
{
	private final static String qn = "Q101_SwordOfSolidarity";

	private static final int ROIENS_LETTER_ID = 796;
	private static final int HOWTOGO_RUINS_ID = 937;
	private static final int BROKEN_SWORD_HANDLE_ID = 739;
	private static final int BROKEN_BLADE_BOTTOM_ID = 740;
	private static final int BROKEN_BLADE_TOP_ID = 741;
	private static final int ALLTRANS_NOTE_ID = 742;
	private static final int SWORD_OF_SOLIDARITY_ID = 738;

	public Q101_SwordOfSolidarity(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(30008);
		addTalkId(30008, 30283);
		addKillId(20361, 20362);
		questItemIds = new int[] { ALLTRANS_NOTE_ID, HOWTOGO_RUINS_ID, BROKEN_BLADE_TOP_ID, BROKEN_BLADE_BOTTOM_ID, ROIENS_LETTER_ID, BROKEN_SWORD_HANDLE_ID };
	}

	public static void onLoad()
	{
		new Q101_SwordOfSolidarity(101, "Q101_SwordOfSolidarity", "Sword Of Solidarity");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event == "30008-04.htm")
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound("ItemSound.quest_accept");
			st.giveItems(ROIENS_LETTER_ID, 1);
		}
		else if (event == "30283-02.htm")
		{
			st.set("cond", "2");
			st.takeItems(ROIENS_LETTER_ID, st.getQuestItemsCount(ROIENS_LETTER_ID));
			st.giveItems(HOWTOGO_RUINS_ID, 1);
		}
		else if (event == "30283-07.htm")
		{
			st.takeItems(BROKEN_SWORD_HANDLE_ID, -1);
			st.giveItems(SWORD_OF_SOLIDARITY_ID, 1);
			st.set("cond", "0");
			st.setState(QuestState.COMPLETED);
			st.playSound("ItemSound.quest_finish");
			st.set("onlyone", "1");
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

		int npcId = npc.getNpcId();
		int id = st.getState();
		if (id == QuestState.CREATED)
		{
			st.set("cond", "0");
			st.set("onlyone", "0");
		}
		if (npcId == 30008 && st.getInt("cond") == 0 && st.getInt("onlyone") == 0)
		{
			if (player.getRace().ordinal() != 0)
				htmltext = "30008-00.htm";
			else if (player.getLevel() >= 9)
			{
				htmltext = "30008-02.htm";
				return htmltext;
			}
			else
			{
				htmltext = "30008-08.htm";
				st.exitQuest(true);
			}
		}
		if (id == QuestState.STARTED)
		{
			if (npcId == 30008 && st.getInt("cond") == 1 && (st.getQuestItemsCount(ROIENS_LETTER_ID) == 1))
				htmltext = "30008-05.htm";
			else if (npcId == 30008 && st.getInt("cond") >= 2 && st.getQuestItemsCount(ROIENS_LETTER_ID) == 0 && st.getQuestItemsCount(ALLTRANS_NOTE_ID) == 0)
			{
				if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) > 0 && st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) > 0)
					htmltext = "30008-12.htm";
				if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) + st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) <= 1)
					htmltext = "30008-11.htm";
				if (st.getQuestItemsCount(BROKEN_SWORD_HANDLE_ID) > 0)
					htmltext = "30008-07.htm";
				if (st.getQuestItemsCount(HOWTOGO_RUINS_ID) == 1)
					htmltext = "30008-10.htm";
			}
			else if (npcId == 30008 && st.getInt("cond") == 4 && st.getQuestItemsCount(ROIENS_LETTER_ID) == 0 && st.getQuestItemsCount(ALLTRANS_NOTE_ID) > 0)
			{
				htmltext = "30008-06.htm";
				st.set("cond", "5");
				st.takeItems(ALLTRANS_NOTE_ID, st.getQuestItemsCount(ALLTRANS_NOTE_ID));
				st.giveItems(BROKEN_SWORD_HANDLE_ID, 1);
			}
			else if (npcId == 30283 && st.getInt("cond") == 1 && st.getQuestItemsCount(ROIENS_LETTER_ID) > 0)
				htmltext = "30283-01.htm";
			else if (npcId == 30283 && st.getInt("cond") >= 2 && st.getQuestItemsCount(ROIENS_LETTER_ID) == 0 && st.getQuestItemsCount(HOWTOGO_RUINS_ID) > 0)
			{
				if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) + st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) == 1)
					htmltext = "30283-08.htm";
				if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) + st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) == 0)
					htmltext = "30283-03.htm";
				if (st.getQuestItemsCount(BROKEN_BLADE_TOP_ID) > 0 && st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID) > 0)
				{
					htmltext = "30283-04.htm";
					st.set("cond", "4");
					st.takeItems(HOWTOGO_RUINS_ID, st.getQuestItemsCount(HOWTOGO_RUINS_ID));
					st.takeItems(BROKEN_BLADE_TOP_ID, st.getQuestItemsCount(BROKEN_BLADE_TOP_ID));
					st.takeItems(BROKEN_BLADE_BOTTOM_ID, st.getQuestItemsCount(BROKEN_BLADE_BOTTOM_ID));
					st.giveItems(ALLTRANS_NOTE_ID, 1);
				}
			}
			else if (npcId == 30283 && st.getInt("cond") == 4 && st.getQuestItemsCount(ALLTRANS_NOTE_ID) > 0)
				htmltext = "30283-05.htm";
			else if (npcId == 30283 && st.getInt("cond") == 5 && st.getQuestItemsCount(BROKEN_SWORD_HANDLE_ID) > 0)
				htmltext = "30283-06.htm";
		}
		else if (npcId == 30008 && st.getInt("cond") == 0 && st.getInt("onlyone") == 1)
			htmltext = "<html><body>This quest has already been completed.</body></html>";

		return htmltext;
	}
}