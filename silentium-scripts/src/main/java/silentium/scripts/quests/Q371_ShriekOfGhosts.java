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

public class Q371_ShriekOfGhosts extends Quest implements ScriptFile {
	private static final String qn = "Q371_ShriekOfGhosts";

	// NPCs
	private static final int REVA = 30867;
	private static final int PATRIN = 30929;

	// Item
	private static final int URN = 5903;
	private static final int PORCELAIN = 6002;

	// Mobs
	private static final int HALLATE_WARRIOR = 20818;
	private static final int HALLATE_KNIGHT = 20820;
	private static final int HALLATE_COMMANDER = 20824;

	public Q371_ShriekOfGhosts(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { URN, PORCELAIN };

		addStartNpc(REVA);
		addTalkId(REVA, PATRIN);

		addKillId(HALLATE_WARRIOR, HALLATE_KNIGHT, HALLATE_COMMANDER);
	}

	public static void onLoad() {
		new Q371_ShriekOfGhosts(371, "Q371_ShriekOfGhosts", "Shriek Of Ghosts", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30867-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30867-07.htm".equalsIgnoreCase(event)) {
			int urns = st.getQuestItemsCount(URN);
			if (urns > 0) {
				st.takeItems(URN, urns);
				if (urns >= 100) {
					urns += 13;
					htmltext = "30867-08.htm";
				} else
					urns += 7;
				st.rewardItems(57, urns * 1000);
			}
		} else if ("30867-10.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_GIVEUP);
			st.exitQuest(true);
		} else if ("APPR".equalsIgnoreCase(event)) {
			if (st.hasQuestItems(PORCELAIN)) {
				final int chance = Rnd.get(100);

				st.takeItems(PORCELAIN, 1);

				if (chance < 2) {
					st.giveItems(6003, 1);
					htmltext = "30929-03.htm";
				} else if (chance < 32) {
					st.giveItems(6004, 1);
					htmltext = "30929-04.htm";
				} else if (chance < 62) {
					st.giveItems(6005, 1);
					htmltext = "30929-05.htm";
				} else if (chance < 77) {
					st.giveItems(6006, 1);
					htmltext = "30929-06.htm";
				} else
					htmltext = "30929-07.htm";
			} else
				htmltext = "30929-02.htm";
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = Quest.getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 59 && player.getLevel() <= 71)
					htmltext = "30867-02.htm";
				else {
					htmltext = "30867-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case REVA:
						htmltext = st.hasQuestItems(URN) ? st.hasQuestItems(PORCELAIN) ? "30867-05.htm" : "30867-04.htm" : "30867-06.htm";
						break;

					case PATRIN:
						htmltext = "30929-01.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		final QuestState st = player.getQuestState(qn);

		final int chance = Rnd.get(100);
		switch (npc.getNpcId()) {
			case HALLATE_WARRIOR:
				if (chance < 43) {
					st.giveItems(chance < 38 ? URN : PORCELAIN, 1);
					st.playSound(QuestState.SOUND_ITEMGET);
				}
				break;

			case HALLATE_KNIGHT:
				if (chance < 56) {
					st.giveItems(chance < 48 ? URN : PORCELAIN, 1);
					st.playSound(QuestState.SOUND_ITEMGET);
				}
				break;

			case HALLATE_COMMANDER:
				if (chance < 58) {
					st.giveItems(chance < 50 ? URN : PORCELAIN, 1);
					st.playSound(QuestState.SOUND_ITEMGET);
				}
				break;
		}

		return null;
	}
}