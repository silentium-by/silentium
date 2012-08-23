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

import java.util.HashMap;
import java.util.Map;

public class Q628_HuntOfTheGoldenRamMercenaryForce extends Quest implements ScriptFile {
	private static final String qn = "Q628_HuntOfTheGoldenRamMercenaryForce";

	// NPCs
	private static final int KAHMAN = 31554;

	// Items
	private static final int CHITIN = 7248;
	private static final int CHITIN2 = 7249;
	private static final int RECRUIT = 7246;
	private static final int SOLDIER = 7247;

	// Chance to drop a qItem
	private static final Map<Integer, Integer> chances = new HashMap<>();

	static {
		chances.put(21508, 25);
		chances.put(21509, 21);
		chances.put(21510, 26);
		chances.put(21511, 26);
		chances.put(21512, 37);
		chances.put(21513, 25);
		chances.put(21514, 21);
		chances.put(21515, 25);
		chances.put(21516, 26);
		chances.put(21517, 37);
	}

	public Q628_HuntOfTheGoldenRamMercenaryForce(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { CHITIN, CHITIN2, RECRUIT, SOLDIER };

		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);

		addKillId(21508, 21509, 21510, 21511, 21512, 21513, 21514, 21515, 21516, 21517);
	}

	public static void onLoad() {
		new Q628_HuntOfTheGoldenRamMercenaryForce(628, "Q628_HuntOfTheGoldenRamMercenaryForce", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31554-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31554-03a.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(CHITIN) >= 100 && st.getInt("cond") == 1) // Giving Recruit Medals
			{
				st.set("cond", "2");
				st.takeItems(CHITIN, -1);
				st.giveItems(RECRUIT, 1);
				htmltext = "31554-04.htm";
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if ("31554-07.htm".equalsIgnoreCase(event)) // Cancel Quest
		{
			st.playSound(QuestState.SOUND_GIVEUP);
			st.exitQuest(true);
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		final QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 66 && player.getLevel() <= 78)
					htmltext = "31554-01.htm";
				else {
					htmltext = "31554-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1) {
					htmltext = st.getQuestItemsCount(CHITIN) >= 100 ? "31554-03.htm" : "31554-03a.htm";
				} else if (cond == 2) {
					if (st.getQuestItemsCount(CHITIN) >= 100 && st.getQuestItemsCount(CHITIN2) >= 100) {
						htmltext = "31554-05.htm";
						st.takeItems(CHITIN, -1);
						st.takeItems(CHITIN2, -1);
						st.takeItems(RECRUIT, 1);
						st.giveItems(SOLDIER, 1);
						st.set("cond", "3");
						st.playSound(QuestState.SOUND_FINISH);
					} else
						htmltext = !st.hasQuestItems(CHITIN) && !st.hasQuestItems(CHITIN2) ? "31554-04b.htm" : "31554-04a.htm";
				} else if (cond == 3)
					htmltext = "31554-05a.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		// Pickup a random member in the player's party and verify if he got the quest.
		final L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		final QuestState st = partyMember.getQuestState(qn);

		final int cond = st.getInt("cond");

		// Don't go further if cond 3 is found (end's quest statut).
		if (cond == 3)
			return null;

		final int npcId = npc.getNpcId();

		int chance = (int) (chances.get(npcId) * MainConfig.RATE_QUEST_DROP);
		int numItems = chance / 100;
		chance %= 100;

		if (Rnd.get(100) < chance)
			numItems++;

		int rewardId = 0;

		switch (npcId) {
			case 21508:
			case 21509:
			case 21510:
			case 21511:
			case 21512:
				if (cond == 1 || cond == 2)
					rewardId = CHITIN;
				break;

			case 21513:
			case 21514:
			case 21515:
			case 21516:
			case 21517:
				if (cond == 2)
					rewardId = CHITIN2;
				break;
		}

		if (numItems > 0 && rewardId != 0) {
			final int prevItems = st.getQuestItemsCount(rewardId);
			if (prevItems < 100) {
				if (prevItems + numItems >= 100) {
					numItems = 100 - prevItems;
					st.playSound(QuestState.SOUND_MIDDLE);
				} else
					st.playSound(QuestState.SOUND_ITEMGET);

				st.giveItems(rewardId, numItems);
			}
		}

		return null;
	}
}