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

public class Q103_SpiritOfCraftsman extends Quest implements ScriptFile
{
	private final static String qn = "Q103_SpiritOfCraftsman";

	private static final int KAROYDS_LETTER_ID = 968;
	private static final int CECKTINONS_VOUCHER1_ID = 969;
	private static final int CECKTINONS_VOUCHER2_ID = 970;
	private static final int BONE_FRAGMENT1_ID = 1107;
	private static final int SOUL_CATCHER_ID = 971;
	private static final int PRESERVE_OIL_ID = 972;
	private static final int ZOMBIE_HEAD_ID = 973;
	private static final int STEELBENDERS_HEAD_ID = 974;
	private static final int BLOODSABER_ID = 975;

	public Q103_SpiritOfCraftsman(int questId, String name, String descr)
	{
		super(questId, name, descr);
		addStartNpc(30307);
		addTalkId(30307, 30132, 30144);
		addKillId(20015, 20020, 20455, 20517, 20518);
		questItemIds = new int[] { KAROYDS_LETTER_ID, CECKTINONS_VOUCHER1_ID, CECKTINONS_VOUCHER2_ID, BONE_FRAGMENT1_ID, SOUL_CATCHER_ID, PRESERVE_OIL_ID, ZOMBIE_HEAD_ID, STEELBENDERS_HEAD_ID };
	}

	public static void onLoad()
	{
		new Q103_SpiritOfCraftsman(103, "Q103_SpiritOfCraftsman", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event == "30307-05.htm")
		{
			st.giveItems(KAROYDS_LETTER_ID, 1);
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound("ItemSound.quest_accept");
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		int id = st.getState();
		int npcId = npc.getNpcId();

		if (id == QuestState.CREATED)
		{
			st.set("cond", "0");
			st.set("onlyone", "0");
		}
		if (npcId == 30307 && st.getInt("cond") == 0 && st.getInt("onlyone") == 0)
		{
			if (player.getRace().ordinal() != 2)
				htmltext = "30307-00.htm";
			else if (player.getLevel() >= 10)
			{
				htmltext = "30307-03.htm";
				return htmltext;
			}
			else
			{
				htmltext = "30307-02.htm";
				st.exitQuest(true);
			}
		}
		else if (id == QuestState.STARTED)
		{
			if (npcId == 30307 && st.getInt("cond") >= 1 && (st.getQuestItemsCount(KAROYDS_LETTER_ID) >= 1 || st.getQuestItemsCount(CECKTINONS_VOUCHER1_ID) >= 1 || st.getQuestItemsCount(CECKTINONS_VOUCHER2_ID) >= 1))
				htmltext = "30307-06.htm";
			else if (npcId == 30132 && st.getInt("cond") == 1 && st.getQuestItemsCount(KAROYDS_LETTER_ID) == 1)
			{
				htmltext = "30132-01.htm";
				st.set("cond", "2");
				st.takeItems(KAROYDS_LETTER_ID, 1);
				st.giveItems(CECKTINONS_VOUCHER1_ID, 1);
			}
			else if (npcId == 30132 && st.getInt("cond") >= 2 && (st.getQuestItemsCount(CECKTINONS_VOUCHER1_ID) >= 1 || st.getQuestItemsCount(CECKTINONS_VOUCHER2_ID) >= 1))
				htmltext = "30132-02.htm";
			else if (npcId == 30144 && st.getInt("cond") == 2 && st.getQuestItemsCount(CECKTINONS_VOUCHER1_ID) >= 1)
			{
				htmltext = "30144-01.htm";
				st.set("cond", "3");
				st.takeItems(CECKTINONS_VOUCHER1_ID, 1);
				st.giveItems(CECKTINONS_VOUCHER2_ID, 1);
			}
			else if (npcId == 30144 && st.getInt("cond") == 3 && st.getQuestItemsCount(CECKTINONS_VOUCHER2_ID) >= 1 && st.getQuestItemsCount(BONE_FRAGMENT1_ID) < 10)
				htmltext = "30144-02.htm";
			else if (npcId == 30144 && st.getInt("cond") == 4 && st.getQuestItemsCount(CECKTINONS_VOUCHER2_ID) == 1 && st.getQuestItemsCount(BONE_FRAGMENT1_ID) >= 10)
			{
				htmltext = "30144-03.htm";
				st.set("cond", "5");
				st.takeItems(CECKTINONS_VOUCHER2_ID, 1);
				st.takeItems(BONE_FRAGMENT1_ID, 10);
				st.giveItems(SOUL_CATCHER_ID, 1);
			}
			else if (npcId == 30144 && st.getInt("cond") == 5 && st.getQuestItemsCount(SOUL_CATCHER_ID) == 1)
				htmltext = "30144-04.htm";
			else if (npcId == 30132 && st.getInt("cond") == 5 && st.getQuestItemsCount(SOUL_CATCHER_ID) == 1)
			{
				htmltext = "30132-03.htm";
				st.set("cond", "6");
				st.takeItems(SOUL_CATCHER_ID, 1);
				st.giveItems(PRESERVE_OIL_ID, 1);
			}
			else if (npcId == 30132 && st.getInt("cond") == 6 && st.getQuestItemsCount(PRESERVE_OIL_ID) == 1 && st.getQuestItemsCount(ZOMBIE_HEAD_ID) == 0 && st.getQuestItemsCount(STEELBENDERS_HEAD_ID) == 0)
				htmltext = "30132-04.htm";
			else if (npcId == 30132 && st.getInt("cond") == 7 && st.getQuestItemsCount(ZOMBIE_HEAD_ID) == 1)
			{
				htmltext = "30132-05.htm";
				st.set("cond", "8");
				st.takeItems(ZOMBIE_HEAD_ID, 1);
				st.giveItems(STEELBENDERS_HEAD_ID, 1);
			}
			else if (npcId == 30132 && st.getInt("cond") == 8 && st.getQuestItemsCount(STEELBENDERS_HEAD_ID) == 1)
				htmltext = "30132-06.htm";
			else if (npcId == 30307 && st.getInt("cond") == 8 && st.getQuestItemsCount(STEELBENDERS_HEAD_ID) == 1)
				htmltext = "30307-07.htm";
			st.takeItems(STEELBENDERS_HEAD_ID, 1);
			st.giveItems(BLOODSABER_ID, 1);
			st.set("cond", "0");
			st.setState(QuestState.COMPLETED);
			st.playSound("ItemSound.quest_finish");
			st.set("onlyone", "1");
		}
		else if (npcId == 30307 && st.getInt("cond") == 0 && st.getInt("onlyone") == 1)
			htmltext = "<html><body>This quest has already been completed.</body></html>";

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(qn);
		int npcId = npc.getNpcId();
		if (st == null)
			return null;

		if (npcId == 20517 || npcId == 20518 || npcId == 20455)
		{
			int bones = st.getQuestItemsCount(BONE_FRAGMENT1_ID);
			if (st.getQuestItemsCount(CECKTINONS_VOUCHER2_ID) == 1 && bones < 10)
			{
				int chance = 30;
				if (st.getRandom(100) <= chance)
				{
					if (bones < 10)
					{
						st.playSound("ItemSound.quest_itemget");
						st.giveItems(BONE_FRAGMENT1_ID, 1);
					}
					else
					{
						st.playSound("ItemSound.quest_middle");
						st.set("cond", "4");
					}
				}
			}
		}
		else if (npcId == 20015 || npcId == 20020)
		{
			if (st.getQuestItemsCount(PRESERVE_OIL_ID) == 1)
			{
				if (st.getRandom(10) < 3)
				{
					st.set("cond", "7");
					st.giveItems(ZOMBIE_HEAD_ID, 1);
					st.playSound("ItemSound.quest_middle");
					st.takeItems(PRESERVE_OIL_ID, 1);
				}
			}
		}

		return null;
	}
}