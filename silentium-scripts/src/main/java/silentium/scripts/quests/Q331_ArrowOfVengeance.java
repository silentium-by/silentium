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

public class Q331_ArrowOfVengeance extends Quest implements ScriptFile {
	private static final String qn = "Q331_ArrowOfVengeance";

	// Npc
	private static final int BELTON = 30125;

	// Items
	private static final int HARPY_FEATHER = 1452;
	private static final int MEDUSA_VENOM = 1453;
	private static final int WYRMS_TOOTH = 1454;

	public Q331_ArrowOfVengeance(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { HARPY_FEATHER, MEDUSA_VENOM, WYRMS_TOOTH };

		addStartNpc(BELTON);
		addTalkId(BELTON);

		addKillId(20145, 20158, 20176);
	}

	public static void onLoad() {
		new Q331_ArrowOfVengeance(331, "Q331_ArrowOfVengeance", "Arrow Of Vengeance", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30125-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30125-06.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getLevel() >= 32 && player.getLevel() <= 39)
					htmltext = "30125-02.htm";
				else {
					htmltext = "30125-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int harpyFeather = st.getQuestItemsCount(HARPY_FEATHER);
				final int medusaVenom = st.getQuestItemsCount(MEDUSA_VENOM);
				final int wyrmTooth = st.getQuestItemsCount(WYRMS_TOOTH);

				if (harpyFeather + medusaVenom + wyrmTooth > 0) {
					htmltext = "30125-05.htm";
					st.takeItems(HARPY_FEATHER, -1);
					st.takeItems(MEDUSA_VENOM, -1);
					st.takeItems(WYRMS_TOOTH, -1);

					int reward = harpyFeather * 78 + medusaVenom * 88 + wyrmTooth * 92;
					if (harpyFeather + medusaVenom + wyrmTooth > 10)
						reward += 3100;

					st.rewardItems(57, reward);
				} else
					htmltext = "30125-04.htm";
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted() && Rnd.get(10) < 5) {
			switch (npc.getNpcId()) {
				case 20145:
					st.giveItems(HARPY_FEATHER, 1);
					break;

				case 20158:
					st.giveItems(MEDUSA_VENOM, 1);
					break;

				case 20176:
					st.giveItems(WYRMS_TOOTH, 1);
					break;
			}
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}