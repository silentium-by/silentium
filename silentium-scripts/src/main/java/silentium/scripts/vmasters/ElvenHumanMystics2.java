/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.vmasters;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;

public class ElvenHumanMystics2 extends OccupationEngine
{
	public ElvenHumanMystics2(int id, String name, String descr)
	{
		super(id, name, descr);
		for (int i : new int[] { 30115, 30174, 30176, 30694, 30854, 31996 })
		{
			addStartNpc(i);
			addTalkId(i);
		}
	}

	public static void main(String[] args)
	{
		new ElvenHumanMystics2(-1, "ElvenHumanMystics2", "vmasters");
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		if (player.isSubClassActive())
			return null;
		if (player.getRace().ordinal() == 0 || player.getRace().ordinal() == 1)
		{
			if (player.getClassId().getId() == 26) // elven wizard
				return "30115-01.htm";
			else if (player.getClassId().getId() == 11) // human wizard
				return "30115-08.htm";
			else if (player.getClassId().level() == 0) // first occupation change not made yet
				return "30115-38.htm";
			else if (player.getClassId().level() == 1) // buffers/oracles
				return "30115-40.htm";
			else if (player.getClassId().level() >= 2) // second/third occupation change already made
				return "30115-25.htm";
			else
				return "30115-40.htm"; // other conditions
		}
		else
			return "30115-40.htm"; // other races
	}
}