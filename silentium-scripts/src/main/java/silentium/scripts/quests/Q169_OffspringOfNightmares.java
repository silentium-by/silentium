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

public class Q169_OffspringOfNightmares extends Quest implements ScriptFile
{
	private final static String qn = "Q169_OffspringOfNightmares";

	// Items
	private static final int CRACKED_SKULL = 1030;
	private static final int PERFECT_SKULL = 1031;
	private static final int BONE_GAITERS = 31;

	// NPC
	private static final int VLASTY = 30145;

	public Q169_OffspringOfNightmares(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { CRACKED_SKULL, PERFECT_SKULL };

		addStartNpc(VLASTY);
		addTalkId(VLASTY);

		addKillId(20105, 20025);
	}

	public static void onLoad()
	{
		new Q169_OffspringOfNightmares(169, "Q169_OffspringOfNightmares", "Offspring of Nightmares");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30145-04.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30145-08.htm"))
		{
			int reward = 17000 + (st.getQuestItemsCount(CRACKED_SKULL) * 20);
			st.takeItems(PERFECT_SKULL, -1);
			st.takeItems(CRACKED_SKULL, -1);
			st.giveItems(BONE_GAITERS, 1);
			st.rewardItems(57, reward);
			st.exitQuest(false);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getRace().ordinal() == 2)
				{
					if (player.getLevel() >= 15 && player.getLevel() <= 20)
						htmltext = "30145-03.htm";
					else
					{
						htmltext = "30145-02.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30145-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
				{
					if (st.getQuestItemsCount(CRACKED_SKULL) >= 1)
						htmltext = "30145-06.htm";
					else
						htmltext = "30145-05.htm";
				}
				else if (cond == 2)
					htmltext = "30145-07.htm";
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

		if (st.isStarted())
		{
			int chance = Rnd.get(10);
			if (st.getInt("cond") == 1 && chance == 0)
			{
				st.set("cond", "2");
				st.giveItems(PERFECT_SKULL, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else if (chance > 6)
			{
				st.giveItems(CRACKED_SKULL, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}