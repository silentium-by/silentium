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
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.model.quest.State;
import silentium.gameserver.scripting.ScriptFile;

public class Clan extends Quest implements ScriptFile {
	public Clan(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		addStartNpc(30026, 30031, 30037, 30066, 30070, 30109, 30115, 30120, 30154, 30174, 30175, 30176, 30187, 30191, 30195, 30288, 30289, 30290, 30297, 30358, 30373, 30462, 30474, 30498, 30499, 30500, 30503, 30504, 30505, 30508, 30511, 30512, 30513, 30520, 30525, 30565, 30594, 30595, 30676, 30677, 30681, 30685, 30687, 30689, 30694, 30699, 30704, 30845, 30847, 30849, 30854, 30857, 30862, 30865, 30894, 30897, 30900, 30905, 30910, 30913, 31269, 31272, 31276, 31279, 31285, 31288, 31314, 31317, 31321, 31324, 31326, 31328, 31331, 31334, 31336, 31755, 31958, 31961, 31965, 31968, 31974, 31977, 31996, 32092, 32093, 32094, 32095, 32096, 32097, 32098);

		addTalkId(30026, 30031, 30037, 30066, 30070, 30109, 30115, 30120, 30154, 30174, 30175, 30176, 30187, 30191, 30195, 30288, 30289, 30290, 30297, 30358, 30373, 30462, 30474, 30498, 30499, 30500, 30503, 30504, 30505, 30508, 30511, 30512, 30513, 30520, 30525, 30565, 30594, 30595, 30676, 30677, 30681, 30685, 30687, 30689, 30694, 30699, 30704, 30845, 30847, 30849, 30854, 30857, 30862, 30865, 30894, 30897, 30900, 30905, 30910, 30913, 31269, 31272, 31276, 31279, 31285, 31288, 31314, 31317, 31321, 31324, 31326, 31328, 31331, 31334, 31336, 31755, 31958, 31961, 31965, 31968, 31974, 31977, 31996, 32092, 32093, 32094, 32095, 32096, 32097, 32098);
	}

	public static void onLoad() {
		new Clan(-1, "Clan", "Clan", "vmasters");
	}

	@Override
	public String onEvent(final String event, final QuestState st) {
		final boolean clanLeader = st.getPlayer().isClanLeader();
		final int clan = st.getPlayer().getClanId();
		String htmltext = event;

		if ("9000-03.htm".equalsIgnoreCase(event))
			if (clan == 0 || !clanLeader)
				htmltext = "9000-03-no.htm";
			else if ("9000-04.htm".equalsIgnoreCase(event))
				if (clan == 0 || !clanLeader)
					htmltext = "9000-04-no.htm";
				else if ("9000-05.htm".equalsIgnoreCase(event))
					if (clan == 0 || !clanLeader)
						htmltext = "9000-05-no.htm";
					else if ("9000-07.htm".equalsIgnoreCase(event))
						if (clan == 0 || !clanLeader)
							htmltext = "9000-07-no.htm";
						else if ("9000-06a.htm".equalsIgnoreCase(event))
							if (clan == 0 || !clanLeader)
								htmltext = "9000-07-no.htm";
							else if ("9000-12a.htm".equalsIgnoreCase(event))
								if (clan == 0 || !clanLeader)
									htmltext = "9000-07-no.htm";
								else if ("9000-13a.htm".equalsIgnoreCase(event))
									if (clan == 0 || !clanLeader)
										htmltext = "9000-07-no.htm";
									else if ("9000-13b.htm".equalsIgnoreCase(event))
										if (clan == 0 || !clanLeader)
											htmltext = "9000-07-no.htm";
										else if ("9000-14a.htm".equalsIgnoreCase(event))
											if (clan == 0 || !clanLeader)
												htmltext = "9000-07-no.htm";
											else if ("9000-15.htm".equalsIgnoreCase(event))
												if (clan == 0 || !clanLeader)
													htmltext = "9000-07-no.htm";

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = "";
		final QuestState st = player.getQuestState(getName());

		st.set("cond", "0");
		st.setState(State.STARTED);
		htmltext = "9000-01.htm";

		return htmltext;
	}
}