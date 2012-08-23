/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.vmasters;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;

public class ElvenHumanBuffers2 extends OccupationEngine {
	public ElvenHumanBuffers2(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);
		for (final int i : new int[] { 30120, 30191, 30857, 30905, 31276, 31321, 31279, 31755, 31968, 32095, 31336 }) {
			addStartNpc(i);
			addTalkId(i);
		}
	}

	public static void onLoad() {
		new ElvenHumanBuffers2(-1, "ElvenHumanBuffers2", "", "vmasters");
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		if (player.isSubClassActive())
			return null;
		if (player.getRace().ordinal() == 0 || player.getRace().ordinal() == 1) {
			if (player.getClassId().getId() == 29) // oracle
				return "30120-01.htm";
			else if (player.getClassId().getId() == 15) // cleric
				return "30120-05.htm";
			else if (player.getClassId().level() == 0) // first occupation change not made yet
				return "30120-24.htm";
			else return player.getClassId().level() >= 2 ? "30120-25.htm" : "30120-26.htm";
		} else
			return "30120-26.htm"; // other races
	}
}
