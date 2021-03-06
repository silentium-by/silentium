/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.custom;

import javolution.util.FastMap;
import silentium.gameserver.model.L2Spawn;
import silentium.gameserver.model.Location;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.tables.NpcTable;
import silentium.gameserver.tables.SpawnTable;
import silentium.gameserver.templates.chars.L2NpcTemplate;
import silentium.gameserver.utils.Util;

import java.util.Map;

/**
 * @authors: Kerberos (python), Nyaran (java)
 */
public class RaidbossInfo extends Quest implements ScriptFile {
	private static final String qn = "RaidbossInfo";

	private static final Map<Integer, Location> RADAR = new FastMap<>();

	private static final int[] _NPCs = { 31729, 31730, 31731, 31732, 31733, 31734, 31735, 31736, 31737, 31738, 31739, 31740, 31741, 31742, 31743, 31744, 31745, 31746, 31747, 31748, 31749, 31750, 31751, 31752, 31753, 31754, 31755, 31756, 31757, 31758, 31759, 31760, 31761, 31762, 31763, 31764, 31765, 31766, 31767, 31768, 31769, 31770, 31771, 31772, 31773, 31774, 31775, 31776, 31777, 31778, 31779, 31780, 31781, 31782, 31783, 31784, 31785, 31786, 31787, 31788, 31789, 31790, 31791, 31792, 31793, 31794, 31795, 31796, 31797, 31798, 31799, 31800, 31801, 31802, 31803, 31804, 31805, 31806, 31807, 31808, 31809, 31810, 31811, 31812, 31813, 31814, 31815, 31816, 31817,
			31818, 31819, 31820, 31821, 31822, 31823, 31824, 31825, 31826, 31827, 31828, 31829, 31830, 31831, 31832, 31833, 31834, 31835, 31836, 31837, 31838, 31839, 31840, 31841 };

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;

		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (Util.isDigit(event)) {
			htmltext = null;
			final int rbid = Integer.parseInt(event);

			if (RADAR.containsKey(rbid)) {
				final Location loc = RADAR.get(rbid);
				st.addRadar(loc.getX(), loc.getY(), loc.getZ());
			}
			st.exitQuest(true);
		}
		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		return "info.htm";
	}

	public static void onLoad() {
		new RaidbossInfo(-1, "RaidbossInfo", "Raidboss Info", "custom");
	}

	public RaidbossInfo(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		for (final int npcId : _NPCs) {
			addStartNpc(npcId);
			addTalkId(npcId);
		}

		// Add all Raid Bosses to RAIDS list
		for (final L2NpcTemplate raid : NpcTable.getInstance().getAllNpcOfClassType("L2RaidBoss")) {
			for (final L2Spawn spawn : SpawnTable.getInstance().getSpawnTable()) {
				if (spawn.getNpcId() == raid.getNpcId()) {
					RADAR.put(raid.getNpcId(), new Location(spawn.getLocx(), spawn.getLocy(), spawn.getLocz()));
					break;
				}
			}

		}
	}
}