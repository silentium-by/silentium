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

public class Q375_WhisperOfDreams_Part2 extends Quest
{
	private static final String qn = "Q375_WhisperOfDreams_Part2";

	// NPCs
	private static final int MANAKIA = 30515;

	// Monsters
	private static final int KARIK = 20629;
	private static final int CAVE_HOWLER = 20624;

	// Items
	private static final int MYSTERIOUS_STONE = 5887;
	private static final int KARIK_HORN = 5888;
	private static final int CAVE_HOWLER_SKULL = 5889;

	// Rewards : A grade robe recipes
	private static final int[] REWARDS = { 5346, 5348, 5350, 5352, 5354 };

	public Q375_WhisperOfDreams_Part2(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { KARIK_HORN, CAVE_HOWLER_SKULL };

		addStartNpc(MANAKIA);
		addTalkId(MANAKIA);

		addKillId(KARIK, CAVE_HOWLER);
	}

	public static void main(String[] args)
	{
		new Q375_WhisperOfDreams_Part2(375, "Q375_WhisperOfDreams_Part2", "Whisper of Dreams, Part 2");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		// Manakia
		if (event.equalsIgnoreCase("30515-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.takeItems(MYSTERIOUS_STONE, 1);
		}
		else if (event.equalsIgnoreCase("30515-07.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (st.hasQuestItems(MYSTERIOUS_STONE))
				{
					if (player.getLevel() < 60 || player.getLevel() > 74)
					{
						htmltext = "30515-01.htm";
						st.exitQuest(true);
					}
					else
						htmltext = "30515-02.htm";
				}
				else
				{
					htmltext = "30515-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(KARIK_HORN) >= 100 && st.getQuestItemsCount(CAVE_HOWLER_SKULL) >= 100)
				{
					htmltext = "30515-05.htm";
					st.takeItems(KARIK_HORN, 100);
					st.takeItems(CAVE_HOWLER_SKULL, 100);
					st.giveItems(REWARDS[Rnd.get(5)], 1);
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					htmltext = "30515-04.htm";
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		// Drop horn or skull to anyone.
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		switch (npc.getNpcId())
		{
			case KARIK:
				st.dropQuestItems(KARIK_HORN, 1, 100, 500000);
				break;

			case CAVE_HOWLER:
				st.dropQuestItems(CAVE_HOWLER_SKULL, 1, 100, 800000);
				break;
		}

		return null;
	}
}