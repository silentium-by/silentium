/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q608_SlayTheEnemyCommander extends Quest implements ScriptFile {
	private static final String qn = "Q608_SlayTheEnemyCommander";

	// Quest Items
	private static final int Mos_Head = 7236;
	private static final int Wisdom_Totem = 7220;
	private static final int Ketra_Alliance_Four = 7214;

	public Q608_SlayTheEnemyCommander(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { Mos_Head };

		addStartNpc(31370); // Kadun Zu Ketra
		addTalkId(31370);

		addKillId(25312); // Mos
	}

	public static void onLoad() {
		new Q608_SlayTheEnemyCommander(608, "Q608_SlayTheEnemyCommander", "Slay the enemy commander!");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31370-04.htm")) {
			if (player.getAllianceWithVarkaKetra() >= 4 && st.getQuestItemsCount(Ketra_Alliance_Four) > 0 && st.getQuestItemsCount(Wisdom_Totem) == 0) {
				if (player.getLevel() >= 75) {
					st.set("cond", "1");
					st.setState(QuestState.STARTED);
					st.playSound(QuestState.SOUND_ACCEPT);
				} else {
					htmltext = "31370-03.htm";
					st.exitQuest(true);
				}
			} else {
				htmltext = "31370-02.htm";
				st.exitQuest(true);
			}
		} else if (event.equalsIgnoreCase("31370-07.htm")) {
			if (st.getQuestItemsCount(Mos_Head) == 1) {
				st.takeItems(Mos_Head, -1);
				st.giveItems(Wisdom_Totem, 1);
				st.addExpAndSp(10000, 0);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			} else {
				htmltext = "31370-06.htm";
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
			}
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				htmltext = "31370-01.htm";
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(Mos_Head) > 0)
					htmltext = "31370-05.htm";
				else
					htmltext = "31370-06.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		final L2Party party = player.getParty();
		if (party != null) {
			for (L2PcInstance partyMember : party.getPartyMembers()) {
				if (partyMember != null)
					rewardPlayer(partyMember);
			}
		} else
			rewardPlayer(player);

		return null;
	}

	private static void rewardPlayer(L2PcInstance player) {
		if (player.getAllianceWithVarkaKetra() >= 4) {
			QuestState st = player.getQuestState(qn);
			if (st.getInt("cond") == 1 && st.getQuestItemsCount(Ketra_Alliance_Four) > 0) {
				st.set("cond", "2");
				st.giveItems(Mos_Head, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
	}
}