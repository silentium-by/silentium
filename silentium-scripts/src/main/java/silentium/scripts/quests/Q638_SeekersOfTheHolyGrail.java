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

public class Q638_SeekersOfTheHolyGrail extends Quest implements ScriptFile
{
	private static final String qn = "Q638_SeekersOfTheHolyGrail";

	// NPC
	private static final int INNOCENTIN = 31328;

	// Item
	private static final int TOTEM = 8068;

	public Q638_SeekersOfTheHolyGrail(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { TOTEM };

		addStartNpc(INNOCENTIN);
		addTalkId(INNOCENTIN);

		for (int i = 22138; i < 22175; i++)
			addKillId(i);
	}

	public static void onLoad()
	{
		new Q638_SeekersOfTheHolyGrail(638, "Q638_SeekersOfTheHolyGrail", "Seekers of the Holy Grail");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31328-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31328-06.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getLevel() >= 73)
					htmltext = "31328-01.htm";
				else
				{
					htmltext = "31328-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(TOTEM) >= 2000)
				{
					htmltext = "31328-03.htm";
					st.takeItems(TOTEM, 2000);

					int chance = Rnd.get(3);
					if (chance == 0)
						st.rewardItems(959, 1);
					else if (chance == 1)
						st.rewardItems(960, 1);
					else
						st.rewardItems(57, 3576000);

					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					htmltext = "31328-04.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		int chance = (int) (30 * MainConfig.RATE_QUEST_DROP);
		int numItems = chance / 100;
		chance = chance % 100;

		if (Rnd.get(100) < chance)
			numItems++;

		if (numItems > 0)
		{
			st.giveItems(TOTEM, numItems);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}