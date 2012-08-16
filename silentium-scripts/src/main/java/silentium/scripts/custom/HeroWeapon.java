/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.custom;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.utils.Util;

public class HeroWeapon extends Quest implements ScriptFile {
	private final static int[] weaponIds = { 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621 };

	public static void onLoad() {
		new HeroWeapon(-1, "HeroWeapon", "custom");
	}

	public HeroWeapon(int questId, String name, String descr) {
		super(questId, name, descr);

		addStartNpc(31690, 31769, 31770, 31771, 31772, 31773);
		addTalkId(31690, 31769, 31770, 31771, 31772, 31773);
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		QuestState st = player.getQuestState(getName());

		int weaponId = Integer.valueOf(event);
		if (Util.contains(weaponIds, weaponId))
			st.giveItems(weaponId, 1);

		st.exitQuest(true);
		return null;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
			newQuestState(player);

		if (st != null) {
			if (player.isHero()) {
				if (hasHeroWeapon(player)) {
					htmltext = "already_have_weapon.htm";
					st.exitQuest(true);
				} else
					htmltext = "weapon_list.htm";
			} else {
				htmltext = "no_hero.htm";
				st.exitQuest(true);
			}
		}

		return htmltext;
	}

	private boolean hasHeroWeapon(L2PcInstance player) {
		for (int i : weaponIds) {
			if (player.getInventory().getItemByItemId(i) != null)
				return true;
		}

		return false;
	}
}