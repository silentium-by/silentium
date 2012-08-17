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

public class Q003_WillTheSealBeBroken extends Quest implements ScriptFile
{
	private static final String qn = "Q003_WillTheSealBeBroken";

	private final static int TALLOTH = 30141;
	private final static int[] MONSTERS = { 20031, 20041, 20046, 20048, 20052, 20057 };

	private final static int ONYX_BEAST_EYE = 1081;
	private final static int TAINT_STONE = 1082;
	private final static int SUCCUBUS_BLOOD = 1083;
	private final static int SCROLL_ENCHANT_ARMOR_D = 956;

	public Q003_WillTheSealBeBroken(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { ONYX_BEAST_EYE, TAINT_STONE, SUCCUBUS_BLOOD };
		addStartNpc(TALLOTH);
		addTalkId(TALLOTH);

		for (int monster : MONSTERS)
			addKillId(monster);
	}

	public static void onLoad()
	{
		new Q003_WillTheSealBeBroken(3, "Q003_WillTheSealBeBroken", "Will the Seal be Broken?");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30141-03.htm"))
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
				if (player.getRace().ordinal() != 2)
				{
					htmltext = "30141-00.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= 16 && player.getLevel() <= 26)
					htmltext = "30141-02.htm";
				else
				{
					htmltext = "30141-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(ONYX_BEAST_EYE) > 0 && st.getQuestItemsCount(TAINT_STONE) > 0 && st.getQuestItemsCount(SUCCUBUS_BLOOD) > 0)
				{
					htmltext = "30141-06.htm";
					st.takeItems(ONYX_BEAST_EYE, 1);
					st.takeItems(TAINT_STONE, 1);
					st.takeItems(SUCCUBUS_BLOOD, 1);
					st.giveItems(SCROLL_ENCHANT_ARMOR_D, 1);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(false);
				}
				else
					htmltext = "30141-04.htm";
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
			switch (npc.getNpcId())
			{
				case 20031:
					if (!st.hasQuestItems(ONYX_BEAST_EYE))
					{
						st.giveItems(ONYX_BEAST_EYE, 1);

						if (st.hasQuestItems(TAINT_STONE) && st.hasQuestItems(SUCCUBUS_BLOOD))
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case 20041:
				case 20046:
					if (!st.hasQuestItems(TAINT_STONE))
					{
						st.giveItems(TAINT_STONE, 1);

						if (st.hasQuestItems(ONYX_BEAST_EYE) && st.hasQuestItems(SUCCUBUS_BLOOD))
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case 20048:
				case 20052:
				case 20057:
					if (!st.hasQuestItems(SUCCUBUS_BLOOD))
					{
						st.giveItems(SUCCUBUS_BLOOD, 1);

						if (st.hasQuestItems(ONYX_BEAST_EYE) && st.hasQuestItems(TAINT_STONE))
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
			}
		}
		return null;
	}
}