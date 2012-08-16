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
import silentium.gameserver.scripting.ScriptFile;

public class Q355_FamilyHonor extends Quest implements ScriptFile {
	private static final String qn = "Q355_FamilyHonor";

	// NPCs
	private static final int GALIBREDO = 30181;
	private static final int PATRIN = 30929;

	// Items
	private static final int GALIBREDO_BUST = 4252;
	private static final int WORK_OF_BERONA = 4350;
	private static final int STATUE_PROTOTYPE = 4351;
	private static final int STATUE_ORIGINAL = 4352;
	private static final int STATUE_REPLICA = 4353;
	private static final int STATUE_FORGERY = 4354;

	public Q355_FamilyHonor(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { GALIBREDO_BUST };

		addStartNpc(GALIBREDO);
		addTalkId(GALIBREDO, PATRIN);

		addKillId(20767, 20768, 20769, 20770);
	}

	public static void onLoad() {
		new Q355_FamilyHonor(355, "Q355_FamilyHonor", "Family Honor");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30181-2.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("30181-4b.htm")) {
			int count = st.getQuestItemsCount(GALIBREDO_BUST);
			if (count > 0) {
				htmltext = "30181-4.htm";

				int reward = 2800 + count * 120;
				if (count >= 100) {
					htmltext = "30181-4a.htm";
					reward += 5000;
				}

				st.takeItems(GALIBREDO_BUST, count);
				st.rewardItems(57, reward);
			}
		} else if (event.equalsIgnoreCase("30929-7.htm")) {
			if (st.getQuestItemsCount(WORK_OF_BERONA) > 0) {
				int appraising = Rnd.get(100);
				if (appraising <= 20) {
					htmltext = "30929-2.htm";
					st.takeItems(WORK_OF_BERONA, 1);
				} else if (appraising <= 40 && appraising >= 20) {
					htmltext = "30929-3.htm";
					st.takeItems(WORK_OF_BERONA, 1);
					st.giveItems(STATUE_REPLICA, 1);
				} else if (appraising <= 60 && appraising >= 40) {
					htmltext = "30929-4.htm";
					st.takeItems(WORK_OF_BERONA, 1);
					st.giveItems(STATUE_ORIGINAL, 1);
				} else if (appraising <= 80 && appraising >= 60) {
					htmltext = "30929-5.htm";
					st.takeItems(WORK_OF_BERONA, 1);
					st.giveItems(STATUE_FORGERY, 1);
				} else if (appraising <= 100 && appraising >= 80) {
					htmltext = "30929-6.htm";
					st.takeItems(WORK_OF_BERONA, 1);
					st.giveItems(STATUE_PROTOTYPE, 1);
				}
			}
		} else if (event.equalsIgnoreCase("30181-6.htm")) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 36 && player.getLevel() <= 49)
					htmltext = "30181-0.htm";
				else {
					htmltext = "30181-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case GALIBREDO:
						if (st.getQuestItemsCount(GALIBREDO_BUST) > 0)
							htmltext = "30181-3a.htm";
						else
							htmltext = "30181-3.htm";
						break;

					case PATRIN:
						htmltext = "30929-0.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		int chance = Rnd.get(100);
		if (chance < 40) {
			st.giveItems(GALIBREDO_BUST, 1);
			if (chance < 20)
				st.giveItems(WORK_OF_BERONA, 1);

			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}