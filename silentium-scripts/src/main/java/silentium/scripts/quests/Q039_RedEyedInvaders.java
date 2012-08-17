/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import java.util.HashMap;
import java.util.Map;

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q039_RedEyedInvaders extends Quest implements ScriptFile
{
	private final static String qn = "Q039_RedEyedInvaders";

	// NPCs
	private static final int BABENCO = 30334;
	private static final int BATHIS = 30332;

	// Mobs
	private static final int M_LIZARDMAN = 20919;
	private static final int M_LIZARDMAN_SCOUT = 20920;
	private static final int M_LIZARDMAN_GUARD = 20921;
	private static final int ARANEID = 20925;

	// Items
	private static final int BLACK_BONE_NECKLACE = 7178;
	private static final int RED_BONE_NECKLACE = 7179;
	private static final int INCENSE_POUCH = 7180;
	private static final int GEM_OF_MAILLE = 7181;

	// First droplist
	private static final Map<Integer, int[]> FIRST_DP = new HashMap<>();

	{
		FIRST_DP.put(M_LIZARDMAN_GUARD, new int[] { RED_BONE_NECKLACE, 100, BLACK_BONE_NECKLACE, 3, 33 });
		FIRST_DP.put(M_LIZARDMAN, new int[] { BLACK_BONE_NECKLACE, 100, RED_BONE_NECKLACE, 3, 50 });
		FIRST_DP.put(M_LIZARDMAN_SCOUT, new int[] { BLACK_BONE_NECKLACE, 100, RED_BONE_NECKLACE, 3, 50 });
	}

	// Second droplist
	private static final Map<Integer, int[]> SECOND_DP = new HashMap<>();

	{
		SECOND_DP.put(ARANEID, new int[] { GEM_OF_MAILLE, 30, INCENSE_POUCH, 5, 25 });
		SECOND_DP.put(M_LIZARDMAN_GUARD, new int[] { INCENSE_POUCH, 30, GEM_OF_MAILLE, 5, 25 });
		SECOND_DP.put(M_LIZARDMAN_SCOUT, new int[] { INCENSE_POUCH, 30, GEM_OF_MAILLE, 5, 25 });
	}

	// Rewards
	private static final int GREEN_COLORED_LURE_HG = 6521;
	private static final int BABY_DUCK_RODE = 6529;
	private static final int FISHING_SHOT_NG = 6535;

	public Q039_RedEyedInvaders(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { BLACK_BONE_NECKLACE, RED_BONE_NECKLACE, INCENSE_POUCH, GEM_OF_MAILLE };

		addStartNpc(BABENCO);
		addTalkId(BABENCO, BATHIS);

		addKillId(M_LIZARDMAN, M_LIZARDMAN_SCOUT, M_LIZARDMAN_GUARD, ARANEID);
	}

	public static void onLoad()
	{
		new Q039_RedEyedInvaders(39, "Q039_RedEyedInvaders", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30334-1.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30332-1.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30332-3.htm"))
		{
			st.set("cond", "4");
			st.takeItems(BLACK_BONE_NECKLACE, -1);
			st.takeItems(RED_BONE_NECKLACE, -1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30332-5.htm"))
		{
			st.takeItems(INCENSE_POUCH, -1);
			st.takeItems(GEM_OF_MAILLE, -1);
			st.giveItems(GREEN_COLORED_LURE_HG, 60);
			st.giveItems(BABY_DUCK_RODE, 1);
			st.giveItems(FISHING_SHOT_NG, 500);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 20 && player.getLevel() <= 28)
					htmltext = "30334-0.htm";
				else
				{
					htmltext = "30334-2.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case BABENCO:
						if (cond >= 1)
							htmltext = "30334-3.htm";
						break;

					case BATHIS:
						if (cond == 1)
							htmltext = "30332-0.htm";
						else if (cond == 2)
							htmltext = "30332-2a.htm";
						else if (cond == 3)
							htmltext = "30332-2.htm";
						else if (cond == 4)
							htmltext = "30332-3a.htm";
						else if (cond == 5)
							htmltext = "30332-4.htm";
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
		int npcId = npc.getNpcId();

		L2PcInstance partyMember = getRandomPartyMember(player, npc, "2");
		if (partyMember != null && npcId != ARANEID)
		{
			drop(partyMember, FIRST_DP.get(npcId));
			return null;
		}

		partyMember = getRandomPartyMember(player, npc, "4");
		if (partyMember != null && npcId != M_LIZARDMAN)
			drop(partyMember, SECOND_DP.get(npcId));
		return null;
	}

	private void drop(L2PcInstance player, int[] list)
	{
		int item = list[0];
		int max = list[1];
		int item2 = list[2];
		int cond = list[3];
		int dropRate = list[4];

		QuestState st = player.getQuestState(qn);

		int count = st.getQuestItemsCount(item);
		int chance = (int) (dropRate * MainConfig.RATE_QUEST_DROP);
		int numItems = chance / 100;
		chance = chance % 100;

		if (Rnd.get(100) <= chance)
			numItems++;

		if (count + numItems >= max)
			numItems = max - count;

		if (numItems > 0)
		{
			st.giveItems(item, numItems);
			if (st.getQuestItemsCount(item) == max && st.getQuestItemsCount(item2) == max)
			{
				st.set("cond", String.valueOf(cond));
				st.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				st.playSound(QuestState.SOUND_ITEMGET);
		}
	}
}