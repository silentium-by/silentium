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

public class Q351_BlackSwan extends Quest implements ScriptFile {
	private static final String qn = "Q351_BlackSwan";

	// NPCs
	private static final int GOSTA = 30916;
	private static final int IASON_HEINE = 30969;
	private static final int ROMAN = 30897;

	// Items
	private static final int ORDER_OF_GOSTA = 4296;
	private static final int LIZARD_FANG = 4297;
	private static final int BARREL_OF_LEAGUE = 4298;
	private static final int BILL_OF_IASON_HEINE = 4310;

	public Q351_BlackSwan(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ORDER_OF_GOSTA, BARREL_OF_LEAGUE, LIZARD_FANG };

		addStartNpc(GOSTA);
		addTalkId(GOSTA, IASON_HEINE, ROMAN);

		addKillId(20784, 20785, 21639, 21640, 21642, 21643);
	}

	public static void onLoad() {
		new Q351_BlackSwan(351, "Q351_BlackSwan", "Black Swan", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30916-03.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.giveItems(ORDER_OF_GOSTA, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30969-02a.htm".equalsIgnoreCase(event)) {
			final int lizardFangs = st.getQuestItemsCount(LIZARD_FANG);
			if (lizardFangs > 0) {
				htmltext = "30969-02.htm";

				st.takeItems(LIZARD_FANG, -1);
				st.rewardItems(57, lizardFangs * 20);
			}
		} else if ("30969-03a.htm".equalsIgnoreCase(event)) {
			final int barrels = st.getQuestItemsCount(BARREL_OF_LEAGUE);
			if (barrels > 0) {
				htmltext = "30969-03.htm";

				st.takeItems(BARREL_OF_LEAGUE, -1);
				st.rewardItems(BILL_OF_IASON_HEINE, barrels);

				// Heine explains than player can speak with Roman in order to exchange bills for rewards.
				if (st.getInt("cond") == 1) {
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		} else if ("30969-06.htm".equalsIgnoreCase(event)) {
			// If no more quest items finish the quest for real, else send a "Return" type HTM.
			if (st.getQuestItemsCount(BARREL_OF_LEAGUE) == 0 && st.getQuestItemsCount(LIZARD_FANG) == 0) {
				htmltext = "30969-07.htm";

				st.exitQuest(true);
				st.playSound(QuestState.SOUND_FINISH);
			}
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
				if (player.getLevel() >= 32 && player.getLevel() <= 36)
					htmltext = "30916-01.htm";
				else {
					htmltext = "30916-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case GOSTA:
						htmltext = "30916-04.htm";
						break;

					case IASON_HEINE:
						htmltext = "30969-01.htm";
						break;

					case ROMAN:
						htmltext = st.getQuestItemsCount(BILL_OF_IASON_HEINE) > 0 ? "30897-01.htm" : "30897-02.htm";
						break;
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

		final int random = Rnd.get(20);
		if (random < 10) {
			if (random < 5)
				st.giveItems(LIZARD_FANG, 1);
			else
				st.giveItems(LIZARD_FANG, 2);

			if (random == 0) {
				st.giveItems(BARREL_OF_LEAGUE, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			} else
				st.playSound(QuestState.SOUND_ITEMGET);
		} else if (random == 10) {
			st.giveItems(BARREL_OF_LEAGUE, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}