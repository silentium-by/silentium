/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://l2j.ru/>.
 */
package silentium.scripts.vmasters;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.scripting.ScriptFile;

public final class Bronk extends Quest implements ScriptFile {
	// Quest NPCs
	private static final int BRONK = 30525;

	public Bronk(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);
		addStartNpc(BRONK);
		addTalkId(BRONK);
	}

	public static void onLoad() {
		new Bronk(-1, "Bronk", "Bronk", "vmasters");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		return event.contains("-01") || event.contains("-02") || event.contains("-03") || event.contains("-04") ? event : null;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance talker) {
		switch (talker.getClassId()) {
			case dwarvenFighter:
				return "30525-01.htm";
			case artisan:
				return "30525-05.htm";
			case warsmith:
				return "30525-06.htm";
			case scavenger:
			case bountyHunter:
				return "30525-07.htm";
			default:
				return "30525-07.htm";
		}
	}
}