/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import gnu.trove.map.hash.TIntIntHashMap;
import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

/**
 * This quest supports both Q605 && Q606 onKill sections.
 */
public class Q605_AllianceWithKetraOrcs extends Quest implements ScriptFile {
	private static final String qn = "Q605_AllianceWithKetraOrcs";

	private static final TIntIntHashMap Chance = new TIntIntHashMap();

	static {
		Chance.put(21350, 500000);
		Chance.put(21351, 500000);
		Chance.put(21353, 509000);
		Chance.put(21354, 521000);
		Chance.put(21355, 519000);
		Chance.put(21357, 500000);
		Chance.put(21358, 500000);
		Chance.put(21360, 509000);
		Chance.put(21361, 518000);
		Chance.put(21362, 500000);
		Chance.put(21364, 527000);
		Chance.put(21365, 500000);
		Chance.put(21366, 628000);
		Chance.put(21368, 508000);
		Chance.put(21369, 518000);
		Chance.put(21370, 604000);
		Chance.put(21371, 627000);
		Chance.put(21372, 604000);
		Chance.put(21373, 649000);
		Chance.put(21374, 626000);
		Chance.put(21375, 626000);
	}

	private static final TIntIntHashMap ChanceMane = new TIntIntHashMap();

	static {
		ChanceMane.put(21350, 500);
		ChanceMane.put(21353, 510);
		ChanceMane.put(21354, 522);
		ChanceMane.put(21355, 519);
		ChanceMane.put(21357, 529);
		ChanceMane.put(21358, 529);
		ChanceMane.put(21360, 539);
		ChanceMane.put(21362, 568);
		ChanceMane.put(21364, 558);
		ChanceMane.put(21365, 568);
		ChanceMane.put(21366, 664);
		ChanceMane.put(21368, 568);
		ChanceMane.put(21369, 548);
		ChanceMane.put(21371, 713);
		ChanceMane.put(21373, 773);
	}

	// Quest Items
	private static final int Varka_Badge_Soldier = 7216;
	private static final int Varka_Badge_Officer = 7217;
	private static final int Varka_Badge_Captain = 7218;

	private static final int Ketra_Alliance_One = 7211;
	private static final int Ketra_Alliance_Two = 7212;
	private static final int Ketra_Alliance_Three = 7213;
	private static final int Ketra_Alliance_Four = 7214;
	private static final int Ketra_Alliance_Five = 7215;

	private static final int Valor_Totem = 7219;
	private static final int Wisdom_Totem = 7220;

	private static final int Mane = 7233;

	public Q605_AllianceWithKetraOrcs(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { Varka_Badge_Soldier, Varka_Badge_Officer, Varka_Badge_Captain };

		addStartNpc(31371); // Wahkan
		addTalkId(31371);

		for (final int mobs : Chance.keys())
			addKillId(mobs);
	}

	public static void onLoad() {
		new Q605_AllianceWithKetraOrcs(605, "Q605_AllianceWithKetraOrcs", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31371-03a.htm".equalsIgnoreCase(event)) {
			if (player.getLevel() >= 74) {
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
			} else {
				htmltext = "31371-02b.htm";
				st.exitQuest(true);
				player.setAllianceWithVarkaKetra(0);
			}
		}
		// Stage 1
		else if ("31371-10-1.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(Varka_Badge_Soldier) >= 100) {
				st.takeItems(Varka_Badge_Soldier, -1);
				st.giveItems(Ketra_Alliance_One, 1);
				player.setAllianceWithVarkaKetra(1);
				st.playSound(QuestState.SOUND_MIDDLE);
			} else
				htmltext = "31371-03b.htm";
		}
		// Stage 2
		else if ("31371-10-2.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(Varka_Badge_Soldier) >= 200 && st.getQuestItemsCount(Varka_Badge_Officer) >= 100) {
				st.takeItems(Varka_Badge_Soldier, -1);
				st.takeItems(Varka_Badge_Officer, -1);
				st.takeItems(Ketra_Alliance_One, -1);
				st.giveItems(Ketra_Alliance_Two, 1);
				player.setAllianceWithVarkaKetra(2);
				st.playSound(QuestState.SOUND_MIDDLE);
			} else
				htmltext = "31371-12.htm";
		}
		// Stage 3
		else if ("31371-10-3.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(Varka_Badge_Soldier) >= 300 && st.getQuestItemsCount(Varka_Badge_Officer) >= 200 && st.getQuestItemsCount(Varka_Badge_Captain) >= 100) {
				st.takeItems(Varka_Badge_Soldier, -1);
				st.takeItems(Varka_Badge_Officer, -1);
				st.takeItems(Varka_Badge_Captain, -1);
				st.takeItems(Ketra_Alliance_Two, -1);
				st.giveItems(Ketra_Alliance_Three, 1);
				player.setAllianceWithVarkaKetra(3);
				st.playSound(QuestState.SOUND_MIDDLE);
			} else
				htmltext = "31371-15.htm";
		}
		// Stage 4
		else if ("31371-10-4.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(Varka_Badge_Soldier) >= 300 && st.getQuestItemsCount(Varka_Badge_Officer) >= 300 && st.getQuestItemsCount(Varka_Badge_Captain) >= 200 && st.getQuestItemsCount(Valor_Totem) >= 1) {
				st.takeItems(Varka_Badge_Soldier, -1);
				st.takeItems(Varka_Badge_Officer, -1);
				st.takeItems(Varka_Badge_Captain, -1);
				st.takeItems(Ketra_Alliance_Three, -1);
				st.takeItems(Valor_Totem, -1);
				st.giveItems(Ketra_Alliance_Four, 1);
				player.setAllianceWithVarkaKetra(4);
				st.playSound(QuestState.SOUND_MIDDLE);
			} else
				htmltext = "31371-21.htm";
		}
		// Leave quest
		else if ("31371-20.htm".equalsIgnoreCase(event)) {
			st.takeItems(Ketra_Alliance_One, -1);
			st.takeItems(Ketra_Alliance_Two, -1);
			st.takeItems(Ketra_Alliance_Three, -1);
			st.takeItems(Ketra_Alliance_Four, -1);
			st.takeItems(Ketra_Alliance_Five, -1);
			st.takeItems(Valor_Totem, -1);
			st.takeItems(Wisdom_Totem, -1);
			player.setAllianceWithVarkaKetra(0);
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
				if (player.isAlliedWithVarka()) {
					htmltext = "31371-02a.htm";
					st.exitQuest(true);
				} else
					htmltext = "31371-01.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (st.getQuestItemsCount(Ketra_Alliance_One) + st.getQuestItemsCount(Ketra_Alliance_Two) + st.getQuestItemsCount(Ketra_Alliance_Three) + st.getQuestItemsCount(Ketra_Alliance_Four) + st.getQuestItemsCount(Ketra_Alliance_Five) == 0) {
					htmltext = st.getQuestItemsCount(Varka_Badge_Soldier) < 100 ? "31371-03b.htm" : "31371-09.htm";
				} else if (st.getQuestItemsCount(Ketra_Alliance_One) == 1) {
					if (cond != 2) {
						htmltext = "31371-04.htm";
						st.set("cond", "2");
						player.setAllianceWithVarkaKetra(1);
						st.playSound(QuestState.SOUND_MIDDLE);
					} else {
						htmltext = st.getQuestItemsCount(Varka_Badge_Soldier) < 200 || st.getQuestItemsCount(Varka_Badge_Officer) < 100 ? "31371-12.htm" : "31371-13.htm";
					}
				} else if (st.getQuestItemsCount(Ketra_Alliance_Two) == 1) {
					if (cond != 3) {
						htmltext = "31371-05.htm";
						st.set("cond", "3");
						player.setAllianceWithVarkaKetra(2);
						st.playSound(QuestState.SOUND_MIDDLE);
					} else {
						htmltext = st.getQuestItemsCount(Varka_Badge_Captain) < 100 || st.getQuestItemsCount(Varka_Badge_Soldier) < 300 || st.getQuestItemsCount(Varka_Badge_Officer) < 200 ? "31371-15.htm" : "31371-16.htm";
					}
				} else if (st.getQuestItemsCount(Ketra_Alliance_Three) == 1) {
					if (cond != 4) {
						htmltext = "31371-06.htm";
						st.set("cond", "4");
						player.setAllianceWithVarkaKetra(3);
						st.playSound(QuestState.SOUND_MIDDLE);
					} else {
						htmltext = st.getQuestItemsCount(Varka_Badge_Captain) < 200 || st.getQuestItemsCount(Varka_Badge_Soldier) < 300 || st.getQuestItemsCount(Varka_Badge_Officer) < 300 || st.getQuestItemsCount(Valor_Totem) == 0 ? "31371-21.htm" : "31371-22.htm";
					}
				} else if (st.getQuestItemsCount(Ketra_Alliance_Four) == 1) {
					if (cond != 5) {
						htmltext = "31371-07.htm";
						st.set("cond", "5");
						player.setAllianceWithVarkaKetra(4);
						st.playSound(QuestState.SOUND_MIDDLE);
					} else {
						if (st.getQuestItemsCount(Varka_Badge_Captain) < 200 || st.getQuestItemsCount(Varka_Badge_Soldier) < 400 || st.getQuestItemsCount(Varka_Badge_Officer) < 400 || st.getQuestItemsCount(Wisdom_Totem) == 0)
							htmltext = "31371-17.htm";
						else {
							htmltext = "31371-10-5.htm";
							st.takeItems(Varka_Badge_Soldier, 400);
							st.takeItems(Varka_Badge_Officer, 400);
							st.takeItems(Varka_Badge_Captain, 200);
							st.takeItems(Ketra_Alliance_Four, -1);
							st.takeItems(Wisdom_Totem, -1);
							st.giveItems(Ketra_Alliance_Five, 1);
							player.setAllianceWithVarkaKetra(5);
							st.playSound(QuestState.SOUND_MIDDLE);
						}
					}
				} else if (st.getQuestItemsCount(Ketra_Alliance_Five) == 1) {
					if (cond != 6) {
						htmltext = "31371-18.htm";
						st.set("cond", "6");
						player.setAllianceWithVarkaKetra(5);
						st.playSound(QuestState.SOUND_MIDDLE);
					} else
						htmltext = "31371-08.htm";
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

		final int npcId = npc.getNpcId();

		// Support for Q606.
		final QuestState st2 = partyMember.getQuestState("Q606_WarWithVarkaSilenos");
		if (st2 != null) {
			final int chance = ChanceMane.get(npcId);
			if (chance != 0 && Rnd.get(1) == 0) {
				if (Rnd.get(1000) < chance) {
					st2.giveItems(Mane, 1);
					st2.playSound("Itemsound.quest_itemget");
				}
				return null;
			}
		}

		final QuestState st = partyMember.getQuestState(qn);

		final int cond = st.getInt("cond");
		if (cond == 6)
			return null;

		switch (npcId) {
			case 21350:
			case 21351:
			case 21353:
			case 21354:
			case 21355:
				if (cond == 1)
					st.dropQuestItems(Varka_Badge_Soldier, 1, 100, Chance.get(npcId));
				else if (cond == 2)
					st.dropQuestItems(Varka_Badge_Soldier, 1, 200, Chance.get(npcId));
				else if (cond == 3 || cond == 4)
					st.dropQuestItems(Varka_Badge_Soldier, 1, 300, Chance.get(npcId));
				else if (cond == 5)
					st.dropQuestItems(Varka_Badge_Soldier, 1, 400, Chance.get(npcId));
				break;

			case 21357:
			case 21358:
			case 21360:
			case 21361:
			case 21362:
			case 21369:
			case 21370:
				if (cond == 2)
					st.dropQuestItems(Varka_Badge_Officer, 1, 100, Chance.get(npcId));
				else if (cond == 3)
					st.dropQuestItems(Varka_Badge_Officer, 1, 200, Chance.get(npcId));
				else if (cond == 4)
					st.dropQuestItems(Varka_Badge_Officer, 1, 300, Chance.get(npcId));
				else if (cond == 5)
					st.dropQuestItems(Varka_Badge_Officer, 1, 400, Chance.get(npcId));
				break;

			case 21364:
			case 21365:
			case 21366:
			case 21368:
			case 21371:
			case 21372:
			case 21373:
			case 21374:
			case 21375:
				if (cond == 3)
					st.dropQuestItems(Varka_Badge_Captain, 1, 100, Chance.get(npcId));
				else if (cond == 4 || cond == 5)
					st.dropQuestItems(Varka_Badge_Captain, 1, 200, Chance.get(npcId));
				break;
		}

		return null;
	}
}