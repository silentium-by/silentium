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

public class Q171_ActsOfEvil extends Quest implements ScriptFile {
	private static final String qn = "Q171_ActsOfEvil";

	// Items
	private static final int BLADE_MOLD = 4239;
	private static final int TYRAS_BILL = 4240;
	private static final int RANGERS_REPORT1 = 4241;
	private static final int RANGERS_REPORT2 = 4242;
	private static final int RANGERS_REPORT3 = 4243;
	private static final int RANGERS_REPORT4 = 4244;
	private static final int WEAPON_TRADE_CONTRACT = 4245;
	private static final int ATTACK_DIRECTIVES = 4246;
	private static final int CERTIFICATE = 4247;
	private static final int CARGOBOX = 4248;
	private static final int OL_MAHUM_HEAD = 4249;

	// Reward
	private static final int ADENA = 57;

	// NPCs
	private static final int ALVAH = 30381;
	private static final int ARODIN = 30207;
	private static final int TYRA = 30420;
	private static final int ROLENTO = 30437;
	private static final int NETI = 30425;
	private static final int BURAI = 30617;

	public Q171_ActsOfEvil(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { RANGERS_REPORT1, RANGERS_REPORT2, RANGERS_REPORT3, RANGERS_REPORT4, OL_MAHUM_HEAD, CARGOBOX, TYRAS_BILL, CERTIFICATE, BLADE_MOLD, WEAPON_TRADE_CONTRACT };

		addStartNpc(ALVAH);
		addTalkId(ALVAH, ARODIN, TYRA, ROLENTO, NETI, BURAI);

		addKillId(20496, 20497, 20498, 20499, 20062, 20066, 20438);
	}

	public static void onLoad() {
		new Q171_ActsOfEvil(171, "Q171_ActsOfEvil", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		final int cond = st.getInt("cond");
		if ("30381-02.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30207-02.htm".equalsIgnoreCase(event) && cond == 1)
			st.set("cond", "2");
		else if ("30381-04.htm".equalsIgnoreCase(event) && cond == 4)
			st.set("cond", "5");
		else if ("30381-07.htm".equalsIgnoreCase(event) && cond == 6) {
			st.set("cond", "7");
			st.takeItems(WEAPON_TRADE_CONTRACT, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30437-03.htm".equalsIgnoreCase(event) && cond == 8) {
			st.giveItems(CARGOBOX, 1);
			st.giveItems(CERTIFICATE, 1);
			st.set("cond", "9");
		} else if ("30617-04.htm".equalsIgnoreCase(event) && cond == 9) {
			st.takeItems(CERTIFICATE, 1);
			st.takeItems(ATTACK_DIRECTIVES, 1);
			st.takeItems(CARGOBOX, 1);
			st.set("cond", "10");
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
				if (player.getLevel() >= 27 && player.getLevel() <= 32)
					htmltext = "30381-01.htm";
				else {
					htmltext = "30381-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case ALVAH:
						if (cond >= 1 && cond <= 3)
							htmltext = "30381-02a.htm";
						else if (cond == 4)
							htmltext = "30381-03.htm";
						else if (cond == 5) {
							if (st.getQuestItemsCount(RANGERS_REPORT1) == 1 && st.getQuestItemsCount(RANGERS_REPORT2) == 1 && st.getQuestItemsCount(RANGERS_REPORT3) == 1 && st.getQuestItemsCount(RANGERS_REPORT4) == 1) {
								htmltext = "30381-05.htm";
								st.takeItems(RANGERS_REPORT1, 1);
								st.takeItems(RANGERS_REPORT2, 1);
								st.takeItems(RANGERS_REPORT3, 1);
								st.takeItems(RANGERS_REPORT4, 1);
								st.set("cond", "6");
							} else
								htmltext = "30381-04a.htm";
						} else if (cond == 6) {
							htmltext = st.getQuestItemsCount(WEAPON_TRADE_CONTRACT) == 1 && st.getQuestItemsCount(ATTACK_DIRECTIVES) == 1 ? "30381-06.htm" : "30381-05a.htm";
						} else if (cond >= 7 && cond <= 10)
							htmltext = "30381-07a.htm";
						else if (cond == 11) {
							htmltext = "30381-08.htm";
							st.rewardItems(ADENA, 90000);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;

					case ARODIN:
						if (cond == 1)
							htmltext = "30207-01.htm";
						else if (cond == 2)
							htmltext = "30207-01a.htm";
						else if (cond == 3) {
							if (st.getQuestItemsCount(TYRAS_BILL) == 1) {
								st.takeItems(TYRAS_BILL, 1);
								htmltext = "30207-03.htm";
								st.set("cond", "4");
							} else
								htmltext = "30207-01a.htm";
						} else if (cond >= 4)
							htmltext = "30207-03a.htm";
						break;

					case TYRA:
						if (cond == 2) {
							if (st.getQuestItemsCount(BLADE_MOLD) >= 20) {
								st.takeItems(BLADE_MOLD, -1);
								st.giveItems(TYRAS_BILL, 1);
								htmltext = "30420-01.htm";
								st.set("cond", "3");
							} else
								htmltext = "30420-01b.htm";
						} else if (cond == 3)
							htmltext = "30420-01a.htm";
						else if (cond > 3)
							htmltext = "30420-02.htm";
						break;

					case NETI:
						if (cond == 7) {
							htmltext = "30425-01.htm";
							st.set("cond", "8");
						} else if (cond >= 8)
							htmltext = "30425-02.htm";
						break;

					case ROLENTO:
						if (cond == 8)
							htmltext = "30437-01.htm";
						else if (cond >= 9)
							htmltext = "30437-03a.htm";
						break;

					case BURAI:
						if (cond == 9 && st.getQuestItemsCount(CERTIFICATE) == 1 && st.getQuestItemsCount(CARGOBOX) == 1 && st.getQuestItemsCount(ATTACK_DIRECTIVES) == 1)
							htmltext = "30617-01.htm";
						else if (cond == 10) {
							if (st.getQuestItemsCount(OL_MAHUM_HEAD) >= 30) {
								htmltext = "30617-05.htm";
								st.giveItems(ADENA, 8000);
								st.takeItems(OL_MAHUM_HEAD, -1);
								st.set("cond", "11");
								st.playSound(QuestState.SOUND_ITEMGET);
							} else
								htmltext = "30617-04a.htm";
						}
						break;
				}
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

		final int cond = st.getInt("cond");

		switch (npc.getNpcId()) {
			case 20496:
			case 20497:
			case 20498:
			case 20499:
				if (cond == 2) {
					if (!st.dropQuestItems(BLADE_MOLD, 1, 20, 500000))
						if (Rnd.get(10) == 0)
							st.addSpawn(27190);
				}
				break;

			case 20062:
				if (cond == 5) {
					final int chance = Rnd.get(100);
					if (!st.hasQuestItems(RANGERS_REPORT1) && chance < 100) {
						st.giveItems(RANGERS_REPORT1, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					} else if (!st.hasQuestItems(RANGERS_REPORT2) && chance < 20) {
						st.giveItems(RANGERS_REPORT2, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					} else if (!st.hasQuestItems(RANGERS_REPORT3) && chance < 20) {
						st.giveItems(RANGERS_REPORT3, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					} else if (!st.hasQuestItems(RANGERS_REPORT4) && chance < 20) {
						st.giveItems(RANGERS_REPORT4, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				break;

			case 20066:
				if (cond == 6) {
					final int chance = Rnd.get(100);
					if (!st.hasQuestItems(WEAPON_TRADE_CONTRACT) && chance < 10) {
						st.giveItems(WEAPON_TRADE_CONTRACT, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					} else if (!st.hasQuestItems(ATTACK_DIRECTIVES) && chance < 10) {
						st.giveItems(ATTACK_DIRECTIVES, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				break;

			case 20438:
				if (cond == 10)
					st.dropQuestItems(OL_MAHUM_HEAD, 1, 30, 500000);
				break;
		}

		return null;
	}
}