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

public class Q035_FindGlitteringJewelry extends Quest implements ScriptFile
{
	private static final String qn = "Q035_FindGlitteringJewelry";

	// NPCs
	private static final int ELLIE = 30091;
	private static final int FELTON = 30879;

	// Monster
	private static final int ALLIGATOR = 20135;

	// Items
	private static final int ROUGH_JEWEL = 7162;
	private static final int ORIHARUKON = 1893;
	private static final int SILVER_NUGGET = 1873;
	private static final int THONS = 4044;

	// Reward
	private static final int JEWEL_BOX = 7077;

	public Q035_FindGlitteringJewelry(int id, String name, String descr)
	{
		super(id, name, descr);
		questItemIds = new int[] { ROUGH_JEWEL };

		addStartNpc(ELLIE);
		addTalkId(ELLIE, FELTON);

		addKillId(ALLIGATOR);
	}

	public static void onLoad()
	{
		new Q035_FindGlitteringJewelry(35, "Q035_FindGlitteringJewelry", "Find Glittering Jewelry");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equals("30091-1.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equals("30879-1.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equals("30091-3.htm"))
		{
			st.takeItems(ROUGH_JEWEL, 10);
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equals("30091-5.htm"))
		{
			if (st.getQuestItemsCount(ORIHARUKON) >= 5 && st.getQuestItemsCount(SILVER_NUGGET) >= 500 && st.getQuestItemsCount(THONS) >= 150)
			{
				st.takeItems(ORIHARUKON, 5);
				st.takeItems(SILVER_NUGGET, 500);
				st.takeItems(THONS, 150);
				st.giveItems(JEWEL_BOX, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "30091-4a.htm";
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
						htmltext = "30091-0.htm";
					else
					{
						htmltext = "30091-0a.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30091-0b.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case ELLIE:
						if (cond == 1 || cond == 2)
							htmltext = "30091-1a.htm";
						else if (cond == 3)
							htmltext = "30091-2.htm";
						else if (cond == 4)
						{
							if (st.getQuestItemsCount(ORIHARUKON) >= 5 && st.getQuestItemsCount(SILVER_NUGGET) >= 500 && st.getQuestItemsCount(THONS) >= 150)
								htmltext = "30091-4.htm";
							else
								htmltext = "30091-4a.htm";
						}
						break;

					case FELTON:
						if (cond == 1)
							htmltext = "30879-0.htm";
						else if (cond >= 2)
							htmltext = "30879-1a.htm";
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
			if (st.dropAlwaysQuestItems(ROUGH_JEWEL, 1, 10))
				st.set("cond", "3");

		return null;
	}
}