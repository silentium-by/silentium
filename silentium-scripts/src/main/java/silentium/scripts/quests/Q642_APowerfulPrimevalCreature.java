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

public class Q642_APowerfulPrimevalCreature extends Quest implements ScriptFile {
	private static final String qn = "Q642_APowerfulPrimevalCreature";

	// Items
	private static final int DINOSAUR_TISSUE = 8774;
	private static final int DINOSAUR_EGG = 8775;

	private static final int ancientEgg = 18344;

	// Rewards
	private static final int[] REWARDS = { 8690, 8692, 8694, 8696, 8698, 8700, 8702, 8704, 8706, 8708, 8710 };

	public Q642_APowerfulPrimevalCreature(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { DINOSAUR_TISSUE, DINOSAUR_EGG };

		addStartNpc(32105); // Dinn
		addTalkId(32105);

		// Dinosaurs + egg
		addKillId(22196, 22197, 22198, 22199, 22200, 22201, 22202, 22203, 22204, 22205, 22218, 22219, 22220, 22223, 22224, 22225, ancientEgg);
	}

	public static void onLoad() {
		new Q642_APowerfulPrimevalCreature(642, "Q642_APowerfulPrimevalCreature", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("32105-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("32105-08.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(DINOSAUR_TISSUE) >= 150 && st.hasQuestItems(DINOSAUR_EGG))
				htmltext = "32105-06.htm";
		} else if ("32105-07.htm".equalsIgnoreCase(event)) {
			final int tissues = st.getQuestItemsCount(DINOSAUR_TISSUE);
			if (tissues > 0) {
				st.takeItems(DINOSAUR_TISSUE, -1);
				st.rewardItems(57, tissues * 5000);
			} else
				htmltext = "32105-08.htm";
		} else if (event.contains("event_")) {
			if (st.getQuestItemsCount(DINOSAUR_TISSUE) >= 150 && st.hasQuestItems(DINOSAUR_EGG)) {
				htmltext = "32105-07.htm";

				st.takeItems(DINOSAUR_TISSUE, 150);
				st.takeItems(DINOSAUR_EGG, 1);
				st.rewardItems(57, 44000);
				st.giveItems(REWARDS[Integer.parseInt(event.split("_")[1])], 1);
			} else
				htmltext = "32105-08.htm";
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
				if (player.getLevel() >= 75)
					htmltext = "32105-01.htm";
				else {
					htmltext = "32105-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = !st.hasQuestItems(DINOSAUR_TISSUE) ? "32105-08.htm" : "32105-05.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null || !st.isStarted())
			return null;

		if (npc.getNpcId() == ancientEgg) {
			if (Rnd.get(100) == 0) {
				st.giveItems(DINOSAUR_EGG, 1);

				if (st.getQuestItemsCount(DINOSAUR_TISSUE) >= 150 && st.getQuestItemsCount(DINOSAUR_EGG) == 1)
					st.playSound(QuestState.SOUND_MIDDLE);
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
		} else {
			if (Rnd.get(100) < 33) {
				st.giveItems(DINOSAUR_TISSUE, 1);

				if (st.getQuestItemsCount(DINOSAUR_TISSUE) == 150 && st.getQuestItemsCount(DINOSAUR_EGG) >= 1)
					st.playSound(QuestState.SOUND_MIDDLE);
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}