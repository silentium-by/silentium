/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.custom;

import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.L2Skill;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.L2Playable;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.PcInventory;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.model.quest.State;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.network.serverpackets.WareHouseWithdrawalList;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.tables.SkillTable;
import silentium.gameserver.tables.SkillTable.FrequentSkill;
import silentium.gameserver.utils.Util;

/**
 * This script supports :
 * <ul>
 * <li>Varka Orc Village functions</li>
 * <li>Quests failures && alliance downgrade if you kill an allied mob.</li>
 * </ul>
 */
public class VarkaSilenosSupport extends Quest implements ScriptFile {
	private static final String qn = "VarkaSilenosSupport";

	private static final int ASHAS = 31377; // Hierarch
	private static final int NARAN = 31378; // Messenger
	private static final int UDAN = 31379; // Buffer
	private static final int DIYABU = 31380; // Grocer
	private static final int HAGOS = 31381; // Warehouse Keeper
	private static final int SHIKON = 31382; // Trader
	private static final int TERANU = 31383; // Teleporter

	private static final int Varka_Alliance_One = 7221;
	private static final int Varka_Alliance_Two = 7222;
	private static final int Varka_Alliance_Three = 7223;
	private static final int Varka_Alliance_Four = 7224;
	private static final int Varka_Alliance_Five = 7225;

	private static final int SEED = 7187;

	private static final int[] _varkas = { 21350, 21351, 21353, 21354, 21355, 21357, 21358, 21360, 21361, 21362, 21369, 21370, 21364, 21365, 21366, 21368, 21371, 21372, 21373, 21374, 21375 };

	private static final int[][] BUFF = { { 4359, 1, 2 }, // Focus: Requires 2 Nepenthese Seeds
			{ 4360, 1, 2 }, // Death Whisper: Requires 2 Nepenthese Seeds
			{ 4345, 1, 3 }, // Might: Requires 3 Nepenthese Seeds
			{ 4355, 1, 3 }, // Acumen: Requires 3 Nepenthese Seeds
			{ 4352, 1, 3 }, // Berserker: Requires 3 Nepenthese Seeds
			{ 4354, 1, 3 }, // Vampiric Rage: Requires 3 Nepenthese Seeds
			{ 4356, 1, 6 }, // Empower: Requires 6 Nepenthese Seeds
			{ 4357, 1, 6 }
			// Haste: Requires 6 Nepenthese Seeds
	};

	/**
	 * Names of missions which will be automatically dropped if the alliance is broken.
	 */
	private static final String[] varkaMissions = { "Q611_AllianceWithVarkaSilenos", "Q612_WarWithKetraOrcs", "Q613_ProveYourCourage", "Q614_SlayTheEnemyCommander", "Q615_MagicalPowerOfFire_Part1", "Q616_MagicalPowerOfFire_Part2" };

	public static void onLoad() {
		new VarkaSilenosSupport(-1, "VarkaSilenosSupport", "custom");
	}

	public VarkaSilenosSupport(int id, String name, String descr) {
		super(id, name, descr);

		addFirstTalkId(ASHAS, NARAN, UDAN, DIYABU, HAGOS, SHIKON, TERANU);
		addTalkId(UDAN, HAGOS, TERANU);
		addStartNpc(HAGOS, TERANU);

		// Verify if the killer didn't kill an allied mob. Test his party aswell.
		addKillId(_varkas);

		// Verify if an allied is healing/buff an enemy. Petrify him if it's the case.
		addSkillSeeId(_varkas);
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(getName());
		if (st == null)
			return htmltext;

		if (Util.isDigit(event)) {
			final int eventId = Integer.parseInt(event);
			if (eventId >= 0 && eventId <= 7) {
				if (st.getQuestItemsCount(SEED) >= BUFF[eventId - 1][2]) {
					st.takeItems(SEED, BUFF[eventId - 1][2]);
					npc.setTarget(player);
					npc.doCast(SkillTable.getInstance().getInfo(BUFF[eventId - 1][0], BUFF[eventId - 1][1]));
					npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
					htmltext = "31379-4.htm";
				}
			}
		} else if (event.equals("Withdraw")) {
			if (player.getWarehouse().getSize() == 0)
				htmltext = "31381-0.htm";
			else {
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.setActiveWarehouse(player.getWarehouse());
				player.sendPacket(new WareHouseWithdrawalList(player, 1));
			}
		} else if (event.equals("Teleport")) {
			switch (player.getAllianceWithVarkaKetra()) {
				case -4:
					htmltext = "31383-4.htm";
					break;
				case -5:
					htmltext = "31383-5.htm";
					break;
			}
		}

		return htmltext;
	}

	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);

		final int allianceLevel = player.getAllianceWithVarkaKetra();

		switch (npc.getNpcId()) {
			case ASHAS:
				if (allianceLevel < 0)
					htmltext = "31377-friend.htm";
				else
					htmltext = "31377-no.htm";
				break;

			case NARAN:
				if (allianceLevel < 0)
					htmltext = "31378-friend.htm";
				else
					htmltext = "31378-no.htm";
				break;

			case UDAN:
				st.setState(State.STARTED);
				if (allianceLevel > -1)
					htmltext = "31379-3.htm";
				else if (allianceLevel > -3 && allianceLevel < 0)
					htmltext = "31379-1.htm";
				else if (allianceLevel < -2) {
					if (st.hasQuestItems(SEED))
						htmltext = "31379-4.htm";
					else
						htmltext = "31379-2.htm";
				}
				break;

			case DIYABU:
				if (player.getKarma() >= 1)
					htmltext = "31380-pk.htm";
				else if (allianceLevel >= 0)
					htmltext = "31380-no.htm";
				else if (allianceLevel == -1 || allianceLevel == -2)
					htmltext = "31380-1.htm";
				else
					htmltext = "31380-2.htm";
				break;

			case HAGOS:
				switch (allianceLevel) {
					case -1:
						htmltext = "31381-1.htm";
						break;
					case -2:
					case -3:
						htmltext = "31381-2.htm";
						break;
					default:
						if (allianceLevel >= 0)
							htmltext = "31381-no.htm";
						else if (player.getWarehouse().getSize() == 0)
							htmltext = "31381-3.htm";
						else
							htmltext = "31381-4.htm";
						break;
				}
				break;

			case SHIKON:
				switch (allianceLevel) {
					case -2:
						htmltext = "31382-1.htm";
						break;
					case -3:
					case -4:
						htmltext = "31382-2.htm";
						break;
					case -5:
						htmltext = "31382-3.htm";
						break;
					default:
						htmltext = "31382-no.htm";
						break;
				}
				break;

			case TERANU:
				if (allianceLevel >= 0)
					htmltext = "31383-no.htm";
				else if (allianceLevel < 0 && allianceLevel > -4)
					htmltext = "31383-1.htm";
				else if (allianceLevel == -4)
					htmltext = "31383-2.htm";
				else
					htmltext = "31383-3.htm";
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
					testVarkaDemote(partyMember);
			}
		} else
			testVarkaDemote(player);

		return null;
	}

	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet) {
		// Caster is an allied.
		if (caster.isAlliedWithVarka()) {
			// Caster's skill is a positive effect ? Go further.
			switch (skill.getSkillType()) {
				case BUFF:
				case HEAL:
				case HEAL_PERCENT:
				case HEAL_STATIC:
				case BALANCE_LIFE:
				case HOT:
					for (L2Character target : (L2Character[]) targets) {
						// Character isn't existing, is dead or is current caster, we drop check.
						if (target == null || target.isDead() || target == caster)
							continue;

						// Target isn't a summon nor a player, we drop check.
						if (!(target instanceof L2Playable))
							continue;

						// Retrieve the player behind that target.
						final L2PcInstance player = target.getActingPlayer();

						// If player is neutral or enemy, go further.
						if (!(player.isAlliedWithVarka())) {
							// If the NPC got that player registered in aggro list, go further.
							if (((L2Attackable) npc).containsTarget(player)) {
								// Save current target for future use.
								final L2Object oldTarget = npc.getTarget();

								// Curse the heretic or his pet.
								npc.setTarget((isPet && player.getPet() != null) ? caster.getPet() : caster);
								npc.doCast(FrequentSkill.VARKA_KETRA_PETRIFICATION.getSkill());

								// Revert to old target && drop the loop.
								npc.setTarget(oldTarget);
								break;
							}
						}
					}
					break;
			}
		}

		// Continue normal behavior.
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}

	/**
	 * That method drops current alliance and retrograde badge.<BR>
	 * If any Varka quest is in progress, it stops the quest (and drop all related qItems) :
	 *
	 * @param player The player to check.
	 */
	private static void testVarkaDemote(L2PcInstance player) {
		if (player.isAlliedWithVarka()) {
			// Drop the alliance (old friends become aggro).
			player.setAllianceWithVarkaKetra(0);

			final PcInventory inventory = player.getInventory();
			L2ItemInstance item;

			// Drop by 1 the level of that alliance (symbolized by a quest item).
			item = inventory.getItemByItemId(Varka_Alliance_One);
			if (item != null)
				player.destroyItemByItemId("Quest", Varka_Alliance_One, item.getCount(), player, true);
			else {
				item = inventory.getItemByItemId(Varka_Alliance_Two);
				if (item != null) {
					player.destroyItemByItemId("Quest", Varka_Alliance_Two, item.getCount(), player, true);
					player.addItem("Quest", Varka_Alliance_One, 1, player.getTarget(), true);
				} else {
					item = inventory.getItemByItemId(Varka_Alliance_Three);
					if (item != null) {
						player.destroyItemByItemId("Quest", Varka_Alliance_Three, item.getCount(), player, true);
						player.addItem("Quest", Varka_Alliance_Two, 1, player.getTarget(), true);
					} else {
						item = inventory.getItemByItemId(Varka_Alliance_Four);
						if (item != null) {
							player.destroyItemByItemId("Quest", Varka_Alliance_Four, item.getCount(), player, true);
							player.addItem("Quest", Varka_Alliance_Three, 1, player.getTarget(), true);
						} else {
							item = inventory.getItemByItemId(Varka_Alliance_Five);
							if (item != null) {
								player.destroyItemByItemId("Quest", Varka_Alliance_Five, item.getCount(), player, true);
								player.addItem("Quest", Varka_Alliance_Four, 1, player.getTarget(), true);
							}
						}
					}
				}
			}

			QuestState pst;
			for (String mission : varkaMissions) {
				pst = player.getQuestState(mission);
				if (pst != null)
					pst.exitQuest(true);
			}
		}
	}
}