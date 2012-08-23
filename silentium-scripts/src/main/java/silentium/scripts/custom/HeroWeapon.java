/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.custom;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.utils.Util;

public class HeroWeapon extends Quest implements ScriptFile {
	private static final int[] weaponIds = { 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621 };

	public static void onLoad() {
		new HeroWeapon(-1, "HeroWeapon", "", "custom");
	}

	public HeroWeapon(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		addStartNpc(31690, 31769, 31770, 31771, 31772, 31773);
		addTalkId(31690, 31769, 31770, 31771, 31772, 31773);
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final QuestState st = player.getQuestState(getName());

		final int weaponId = Integer.valueOf(event);
		if (Util.contains(weaponIds, weaponId))
			st.giveItems(weaponId, 1);

		st.exitQuest(true);
		return null;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = "";
		final QuestState st = player.getQuestState(getName());
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

	private boolean hasHeroWeapon(final L2PcInstance player) {
		for (final int i : weaponIds) {
			if (player.getInventory().getItemByItemId(i) != null)
				return true;
		}

		return false;
	}
}