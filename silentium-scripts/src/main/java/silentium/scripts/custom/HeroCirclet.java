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

public class HeroCirclet extends Quest implements ScriptFile {
	public static void onLoad() {
		new HeroCirclet(-1, "HeroCirclet", "Hero Circlet", "custom");
	}

	public HeroCirclet(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		addStartNpc(31690, 31769, 31770, 31771, 31772);
		addTalkId(31690, 31769, 31770, 31771, 31772);
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
			st = newQuestState(player);

		if (player.isHero()) {
			if (player.getInventory().getItemByItemId(6842) == null)
				st.giveItems(6842, 1);
			else
				htmltext = "already_have_circlet.htm";
		} else
			htmltext = "no_hero.htm";

		st.exitQuest(true);
		return htmltext;
	}
}