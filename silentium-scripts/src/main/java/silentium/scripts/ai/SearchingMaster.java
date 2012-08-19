/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.ai;

import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.ai.DefaultMonsterAI;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.scripting.ScriptFile;

public class SearchingMaster extends DefaultMonsterAI implements ScriptFile {
	private static final int[] mobs = { 20965, 20966, 20967, 20968, 20969, 20970, 20971, 20972, 20973 };

	public static void onLoad() {
		new SearchingMaster(-1, "SearchingMaster", "ai");
	}

	public SearchingMaster(final int questId, final String name, final String descr) {
		super(questId, name, descr);
		for (final int id : mobs)
			addAttackId(id);
	}

	@Override
	public String onAttack(final L2Npc npc, final L2PcInstance player, final int damage, final boolean isPet) {
		if (player == null)
			return null;

		npc.setIsRunning(true);
		((L2Attackable) npc).addDamageHate(player, 0, 999);
		npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);

		return super.onAttack(npc, player, damage, isPet);
	}
}