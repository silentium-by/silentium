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

public class Q328_SenseForBusiness extends Quest implements ScriptFile {
	private static final String qn = "Q328_SenseForBusiness";

	// NPC
	private static final int SARIEN = 30436;

	// Items
	private static final int MONSTER_EYE_LENS = 1366;
	private static final int MONSTER_EYE_CARCASS = 1347;
	private static final int BASILISK_GIZZARD = 1348;

	public Q328_SenseForBusiness(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { MONSTER_EYE_LENS, MONSTER_EYE_CARCASS, BASILISK_GIZZARD };

		addStartNpc(SARIEN);
		addTalkId(SARIEN);

		addKillId(20055, 20059, 20067, 20068, 20070, 20072);
	}

	public static void onLoad() {
		new Q328_SenseForBusiness(328, "Q328_SenseForBusiness", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30436-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30436-06.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getLevel() >= 21 && player.getLevel() <= 32)
					htmltext = "30436-02.htm";
				else {
					htmltext = "30436-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int carcasses = st.getQuestItemsCount(MONSTER_EYE_CARCASS);
				final int lenses = st.getQuestItemsCount(MONSTER_EYE_LENS);
				final int gizzards = st.getQuestItemsCount(BASILISK_GIZZARD);
				final int all = carcasses + lenses + gizzards;

				if (all == 0)
					htmltext = "30436-04.htm";
				else {
					htmltext = "30436-05.htm";
					final int reward = 25 * carcasses + 1000 * lenses + 60 * gizzards + (all >= 10 ? 618 : 0);
					st.takeItems(MONSTER_EYE_CARCASS, -1);
					st.takeItems(MONSTER_EYE_LENS, -1);
					st.takeItems(BASILISK_GIZZARD, -1);
					st.rewardItems(57, reward);
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted()) {
			final int chance = Rnd.get(100);
			switch (npc.getNpcId()) {
				case 20055:
				case 20059:
				case 20067:
				case 20068:
					if (chance < 2) {
						st.giveItems(MONSTER_EYE_LENS, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					} else if (chance < 35) {
						st.giveItems(MONSTER_EYE_CARCASS, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case 20070:
				case 20072:
					if (chance < 18) {
						st.giveItems(BASILISK_GIZZARD, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
			}
		}

		return null;
	}
}