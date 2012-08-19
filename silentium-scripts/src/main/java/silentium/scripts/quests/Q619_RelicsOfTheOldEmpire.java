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

public class Q619_RelicsOfTheOldEmpire extends Quest implements ScriptFile {
	private static final String qn = "Q619_RelicsOfTheOldEmpire";

	// NPC
	private static final int GHOST_OF_ADVENTURER = 31538;

	// Items
	private static final int RELICS = 7254;
	private static final int ENTRANCE = 7075;

	// Rewards ; all S grade weapons recipe (60%)
	private static final int[] RCP_REWARDS = { 6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580 };

	public Q619_RelicsOfTheOldEmpire(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { RELICS };

		addStartNpc(GHOST_OF_ADVENTURER);
		addTalkId(GHOST_OF_ADVENTURER);

		for (int id = 21396; id <= 21434; id++)
			// IT monsters
			addKillId(id);

		// monsters at IT entrance
		addKillId(21798, 21799, 21800);

		for (int id = 18120; id <= 18256; id++)
			// Sepulchers monsters
			addKillId(id);
	}

	public static void onLoad() {
		new Q619_RelicsOfTheOldEmpire(619, "Q619_RelicsOfTheOldEmpire", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31538-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31538-09.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(RELICS) >= 1000) {
				htmltext = "31538-09.htm";
				st.takeItems(RELICS, 1000);
				st.giveItems(RCP_REWARDS[Rnd.get(RCP_REWARDS.length)], 1);
			} else
				htmltext = "31538-06.htm";
		} else if ("31538-10.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}
		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 74)
					htmltext = "31538-01.htm";
				else {
					htmltext = "31538-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				final int relics = st.getQuestItemsCount(RELICS);
				final int entrance = st.getQuestItemsCount(ENTRANCE);

				if (cond == 1 && relics >= 1000)
					htmltext = "31538-04.htm";
				else htmltext = entrance >= 1 ? "31538-06.htm" : "31538-07.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember == null)
			return null;

		final QuestState st = partyMember.getQuestState(qn);
		if (st.isStarted()) {
			int numItems = (int) (100 * MainConfig.RATE_QUEST_DROP / 100);
			final int chance = (int) (100 * MainConfig.RATE_QUEST_DROP % 100);

			if (Rnd.get(100) < chance)
				numItems++;

			if (numItems > 0) {
				st.giveItems(RELICS, numItems);
				st.playSound(QuestState.SOUND_ITEMGET);
			}

			if (Rnd.get(100) < 5 * MainConfig.RATE_QUEST_DROP) {
				st.giveItems(ENTRANCE, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		return null;
	}
}