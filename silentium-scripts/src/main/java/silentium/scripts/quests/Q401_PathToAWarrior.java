/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.network.serverpackets.SocialAction;
import silentium.gameserver.scripting.ScriptFile;

public class Q401_PathToAWarrior extends Quest implements ScriptFile {
	private static final String qn = "Q401_PathToAWarrior";

	// Items
	private static final int AuronsLetter = 1138;
	private static final int WarriorGuildMark = 1139;
	private static final int RustedBronzeSword1 = 1140;
	private static final int RustedBronzeSword2 = 1141;
	private static final int RustedBronzeSword3 = 1142;
	private static final int SimplonsLetter = 1143;
	private static final int PoisonSpiderLeg = 1144;
	private static final int MedallionOfWarrior = 1145;

	// NPCs
	private static final int Auron = 30010;
	private static final int Simplon = 30253;

	public Q401_PathToAWarrior(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { AuronsLetter, WarriorGuildMark, RustedBronzeSword1, RustedBronzeSword2, RustedBronzeSword3, SimplonsLetter, PoisonSpiderLeg };

		addStartNpc(Auron);
		addTalkId(Auron, Simplon);

		addKillId(20035, 20038, 20042, 20043);
	}

	public static void onLoad() {
		new Q401_PathToAWarrior(401, "Q401_PathToAWarrior", "Path To A Warrior", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30010-05.htm".equalsIgnoreCase(event)) {
			if (player.getClassId().getId() == 0x00) {
				if (player.getLevel() >= 19) {
					if (st.getQuestItemsCount(MedallionOfWarrior) == 1) {
						htmltext = "30010-04.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30010-02.htm";
					st.exitQuest(true);
				}
			} else if (player.getClassId().getId() == 0x01) {
				htmltext = "30010-03.htm";
				st.exitQuest(true);
			} else {
				htmltext = "30010-02b.htm";
				st.exitQuest(true);
			}
		} else if ("30010-06.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.giveItems(AuronsLetter, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30253-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(AuronsLetter, 1);
			st.giveItems(WarriorGuildMark, 1);
		} else if ("30010-11.htm".equalsIgnoreCase(event)) {
			st.set("cond", "5");
			st.takeItems(RustedBronzeSword2, 1);
			st.giveItems(RustedBronzeSword3, 1);
			st.takeItems(SimplonsLetter, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
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
				htmltext = "30010-01.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case Auron:
						if (cond == 1)
							htmltext = "30010-07.htm";
						else if (cond == 2 || cond == 3)
							htmltext = "30010-08.htm";
						else if (cond == 4)
							htmltext = "30010-09.htm";
						else if (cond == 5)
							htmltext = "30010-12.htm";
						else if (cond == 6) {
							htmltext = "30010-13.htm";
							st.takeItems(RustedBronzeSword3, 1);
							st.takeItems(PoisonSpiderLeg, -1);
							st.giveItems(MedallionOfWarrior, 1);
							st.addExpAndSp(3200, 1500);
							player.broadcastPacket(new SocialAction(player, 3));

							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(true);
							st.saveGlobalQuestVar("1ClassQuestFinished", "1");
						}
						break;

					case Simplon:
						if (cond == 1)
							htmltext = "30253-01.htm";
						else if (cond == 2) {
							if (st.getQuestItemsCount(RustedBronzeSword1) == 0)
								htmltext = "30253-03.htm";
							else if (st.getQuestItemsCount(RustedBronzeSword1) <= 9)
								htmltext = "30253-03b.htm";
						} else if (cond == 3) {
							st.set("cond", "4");
							st.playSound(QuestState.SOUND_MIDDLE);
							st.takeItems(WarriorGuildMark, 1);
							st.takeItems(RustedBronzeSword1, 10);
							st.giveItems(RustedBronzeSword2, 1);
							st.giveItems(SimplonsLetter, 1);
							htmltext = "30253-04.htm";
						} else if (cond == 4)
							htmltext = "30253-05.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null || !st.isStarted())
			return null;

		switch (npc.getNpcId()) {
			case 20035:
			case 20042:
				if (st.getInt("cond") == 2)
					if (st.dropQuestItems(RustedBronzeSword1, 1, 10, 400000))
						st.set("cond", "3");
				break;

			case 20038:
			case 20043:
				if (st.getInt("cond") == 5 && st.getItemEquipped(Inventory.PAPERDOLL_RHAND) == RustedBronzeSword3)
					if (st.dropAlwaysQuestItems(PoisonSpiderLeg, 1, 20))
						st.set("cond", "6");
				break;
		}

		return null;
	}
}