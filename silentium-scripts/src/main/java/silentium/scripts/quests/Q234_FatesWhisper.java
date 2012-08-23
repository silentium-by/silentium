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
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.tables.ItemTable;

import java.util.HashMap;
import java.util.Map;

public class Q234_FatesWhisper extends Quest implements ScriptFile {
	private static final String qn = "Q234_FatesWhisper";

	// Items
	private static final int REIRIAS_SOUL_ORB = 4666;
	private static final int KERMONS_INFERNIUM_SCEPTER = 4667;
	private static final int GOLKONDAS_INFERNIUM_SCEPTER = 4668;
	private static final int HALLATES_INFERNIUM_SCEPTER = 4669;

	private static final int INFERNIUM_VARNISH = 4672;
	private static final int REORINS_HAMMER = 4670;
	private static final int REORINS_MOLD = 4671;

	private static final int PIPETTE_KNIFE = 4665;
	private static final int RED_PIPETTE_KNIFE = 4673;

	private static final int CRYSTAL_B = 1460;

	// Reward
	private static final int STAR_OF_DESTINY = 5011;

	// Chest Spawn
	private static final Map<Integer, Integer> CHEST_SPAWN = new HashMap<>();

	static {
		CHEST_SPAWN.put(25035, 31027);
		CHEST_SPAWN.put(25054, 31028);
		CHEST_SPAWN.put(25126, 31029);
		CHEST_SPAWN.put(25220, 31030);
	}

	// Weapons
	private static final Map<Integer, String> Weapons = new HashMap<>();

	static {
		Weapons.put(79, "Sword of Damascus");
		Weapons.put(97, "Lance");
		Weapons.put(171, "Deadman's Glory");
		Weapons.put(175, "Art of Battle Axe");
		Weapons.put(210, "Staff of Evil Spirits");
		Weapons.put(234, "Demon Dagger");
		Weapons.put(268, "Bellion Cestus");
		Weapons.put(287, "Bow of Peril");
		Weapons.put(2626, "Samurai Dual-sword");
		Weapons.put(7883, "Guardian Sword");
		Weapons.put(7889, "Wizard's Tear");
		Weapons.put(7893, "Kaim Vanul's Bones");
		Weapons.put(7901, "Star Buster");
	}

	public Q234_FatesWhisper(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { PIPETTE_KNIFE, RED_PIPETTE_KNIFE };

		addStartNpc(31002);
		addTalkId(31002, 30182, 30847, 30178, 30833, 31028, 31029, 31030, 31027);

		// The 4 bosses which spawn chests
		addKillId(25035, 25054, 25126, 25220);

		// Baium
		addAttackId(29020);
	}

	public static void onLoad() {
		new Q234_FatesWhisper(234, "Q234_FatesWhisper", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31002-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30182-01c.htm".equalsIgnoreCase(event)) {
			st.giveItems(INFERNIUM_VARNISH, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		} else if ("30178-01a.htm".equalsIgnoreCase(event)) {
			st.set("cond", "6");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30833-01b.htm".equalsIgnoreCase(event)) {
			st.set("cond", "7");
			st.giveItems(PIPETTE_KNIFE, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.startsWith("selectBGrade_")) {
			if (st.getInt("bypass") == 1)
				return null;

			final String bGradeId = event.replace("selectBGrade_", "");
			st.set("weaponId", bGradeId);
			htmltext = st.showHtmlFile("31002-13.htm").replace("%weaponname%", Weapons.get(st.getInt("weaponId")));
		} else if (event.startsWith("confirmWeapon")) {
			st.set("bypass", "1");
			htmltext = st.showHtmlFile("31002-14.htm").replace("%weaponname%", Weapons.get(st.getInt("weaponId")));
		} else if (event.startsWith("selectAGrade_")) {
			if (st.getInt("bypass") == 1) {
				final int itemId = st.getInt("weaponId");
				if (st.hasQuestItems(itemId)) {
					final int aGradeItemId = Integer.parseInt(event.replace("selectAGrade_", ""));

					htmltext = st.showHtmlFile("31002-12.htm").replace("%weaponname%", ItemTable.getInstance().getTemplate(aGradeItemId).getName());
					st.takeItems(itemId, 1);
					st.giveItems(aGradeItemId, 1);
					st.giveItems(STAR_OF_DESTINY, 1);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(false);
				} else
					htmltext = st.showHtmlFile("31002-15.htm").replace("%weaponname%", Weapons.get(itemId));
			} else
				htmltext = "31002-16.htm";
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
				if (player.getLevel() >= 75)
					htmltext = "31002-02.htm";
				else {
					htmltext = "31002-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case 31002:
						if (cond == 1) {
							if (!st.hasQuestItems(REIRIAS_SOUL_ORB))
								htmltext = "31002-04b.htm";
							else {
								st.set("cond", "2");
								htmltext = "31002-05.htm";
								st.takeItems(REIRIAS_SOUL_ORB, 1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						} else if (cond == 2) {
							if (!st.hasQuestItems(KERMONS_INFERNIUM_SCEPTER) || !st.hasQuestItems(GOLKONDAS_INFERNIUM_SCEPTER) || !st.hasQuestItems(HALLATES_INFERNIUM_SCEPTER))
								htmltext = "31002-05c.htm";
							else {
								st.set("cond", "3");
								htmltext = "31002-06.htm";
								st.takeItems(KERMONS_INFERNIUM_SCEPTER, 1);
								st.takeItems(GOLKONDAS_INFERNIUM_SCEPTER, 1);
								st.takeItems(HALLATES_INFERNIUM_SCEPTER, 1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						} else if (cond == 3) {
							if (st.getQuestItemsCount(INFERNIUM_VARNISH) < 1)
								htmltext = "31002-06b.htm";
							else {
								st.set("cond", "4");
								htmltext = "31002-07.htm";
								st.takeItems(INFERNIUM_VARNISH, 1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						} else if (cond == 4) {
							if (st.getQuestItemsCount(REORINS_HAMMER) < 1)
								htmltext = "31002-07b.htm";
							else {
								st.set("cond", "5");
								htmltext = "31002-08.htm";
								st.takeItems(REORINS_HAMMER, 1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						} else if (cond > 4 && cond < 8)
							htmltext = "31002-08b.htm";
						else if (cond == 8) {
							st.set("cond", "9");
							htmltext = "31002-09.htm";
							st.takeItems(REORINS_MOLD, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 9) {
							if (st.getQuestItemsCount(CRYSTAL_B) < 984)
								htmltext = "31002-09b.htm";
							else {
								st.set("cond", "10");
								htmltext = "31002-BGradeList.htm";
								st.takeItems(CRYSTAL_B, 984);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						} else if (cond == 10) {
							// If a weapon is selected
							if (st.getInt("bypass") == 1) {
								// If you got it in the inventory
								final int itemId = st.getInt("weaponId");
								htmltext = st.showHtmlFile(st.hasQuestItems(itemId) ? "31002-AGradeList.htm" : "31002-15.htm").replace("%weaponname%", Weapons.get(itemId));
							}
							// B weapon is still not selected
							else
								htmltext = "31002-BGradeList.htm";
						}
						break;

					case 30182:
						if (cond == 3) {
							htmltext = !st.hasQuestItems(INFERNIUM_VARNISH) ? "30182-01.htm" : "30182-02.htm";
						}
						break;

					case 30847:
						if (cond == 4 && !st.hasQuestItems(REORINS_HAMMER)) {
							htmltext = "30847-01.htm";
							st.giveItems(REORINS_HAMMER, 1);
							st.playSound(QuestState.SOUND_ITEMGET);
						} else if (cond >= 4 && st.hasQuestItems(REORINS_HAMMER))
							htmltext = "30847-02.htm";
						break;

					case 30178:
						if (cond == 5)
							htmltext = "30178-01.htm";
						else if (cond >= 6)
							htmltext = "30178-02.htm";
						break;

					case 30833:
						if (cond == 6)
							htmltext = "30833-01.htm";
						else if (cond == 7) {
							if (st.hasQuestItems(PIPETTE_KNIFE) && !st.hasQuestItems(RED_PIPETTE_KNIFE))
								htmltext = "30833-02.htm";
							else {
								htmltext = "30833-03.htm";
								st.set("cond", "8");
								st.takeItems(RED_PIPETTE_KNIFE, 1);
								st.giveItems(REORINS_MOLD, 1);
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						} else if (cond >= 8)
							htmltext = "30833-04.htm";
						break;

					case 31027:
						if (cond == 1) {
							if (!st.hasQuestItems(REIRIAS_SOUL_ORB)) {
								htmltext = "31027-01.htm";
								st.giveItems(REIRIAS_SOUL_ORB, 1);
								st.playSound(QuestState.SOUND_ITEMGET);
							} else
								htmltext = "31027-02.htm";
						} else
							htmltext = "31027-02.htm";
						break;

					case 31028:
						if (cond == 2) {
							if (!st.hasQuestItems(KERMONS_INFERNIUM_SCEPTER)) {
								htmltext = "31028-01.htm";
								st.giveItems(KERMONS_INFERNIUM_SCEPTER, 1);
								st.playSound(QuestState.SOUND_ITEMGET);
							} else
								htmltext = "31028-02.htm";
						} else
							htmltext = "31028-02.htm";
						break;

					case 31029:
						if (cond == 2) {
							if (!st.hasQuestItems(GOLKONDAS_INFERNIUM_SCEPTER)) {
								htmltext = "31029-01.htm";
								st.giveItems(GOLKONDAS_INFERNIUM_SCEPTER, 1);
								st.playSound(QuestState.SOUND_ITEMGET);
							} else
								htmltext = "31029-02.htm";
						} else
							htmltext = "31029-02.htm";
						break;

					case 31030:
						if (cond == 2) {
							if (!st.hasQuestItems(HALLATES_INFERNIUM_SCEPTER)) {
								htmltext = "31030-01.htm";
								st.giveItems(HALLATES_INFERNIUM_SCEPTER, 1);
								st.playSound(QuestState.SOUND_ITEMGET);
							} else
								htmltext = "31030-02.htm";
						} else
							htmltext = "31030-02.htm";
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
	public String onAttack(final L2Npc npc, final L2PcInstance attacker, final int damage, final boolean isPet) {
		final QuestState st = attacker.getQuestState(qn);
		if (st == null || !st.isStarted() || isPet)
			return null;

		if (st.getInt("cond") == 7) {
			if (attacker.getActiveWeaponItem() != null && attacker.getActiveWeaponItem().getItemId() == PIPETTE_KNIFE && st.getQuestItemsCount(RED_PIPETTE_KNIFE) == 0) {
				st.giveItems(RED_PIPETTE_KNIFE, 1);
				st.takeItems(PIPETTE_KNIFE, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		// Chests aren't influenced by conditions, or even if the killer got the quest. It just spawns.
		final int npcId = npc.getNpcId();
		if (CHEST_SPAWN.containsKey(npcId))
			addSpawn(CHEST_SPAWN.get(npcId), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, 60000);

		return null;
	}
}