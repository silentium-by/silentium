/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.custom;

import javolution.util.FastMap;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.utils.Util;

import java.util.Map;

/**
 * @authors DrLecter (python), Plim (java)
 * @notes Formerly based on Elektra's script
 */
public class EchoCrystals extends Quest implements ScriptFile {
	private static final String qn = "EchoCrystals";

	private static final int ADENA = 57;
	private static final int COST = 200;

	private static final Map<Integer, ScoreData> SCORES = new FastMap<>();

	private class ScoreData {
		private final int _crystalId;
		private final String _okMsg;
		private final String _noAdenaMsg;
		private final String _noScoreMsg;

		ScoreData(final int crystalId, final String okMsg, final String noAdenaMsg, final String noScoreMsg) {
			_crystalId = crystalId;
			_okMsg = okMsg;
			_noAdenaMsg = noAdenaMsg;
			_noScoreMsg = noScoreMsg;
		}

		public int getCrystalId() {
			return _crystalId;
		}

		public String getOkMsg() {
			return _okMsg;
		}

		public String getNoAdenaMsg() {
			return _noAdenaMsg;
		}

		public String getNoScoreMsg() {
			return _noScoreMsg;
		}
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = "";
		final QuestState st = player.getQuestState(qn);

		if (st != null && Util.isDigit(event)) {
			final int score = Integer.parseInt(event);
			if (SCORES.containsKey(score)) {
				final int crystal = SCORES.get(score).getCrystalId();
				final String ok = SCORES.get(score).getOkMsg();
				final String noadena = SCORES.get(score).getNoAdenaMsg();
				final String noscore = SCORES.get(score).getNoScoreMsg();

				if (st.getQuestItemsCount(score) == 0)
					htmltext = npc.getNpcId() + "-" + noscore + ".htm";
				else if (st.getQuestItemsCount(ADENA) < COST)
					htmltext = npc.getNpcId() + "-" + noadena + ".htm";
				else {
					st.takeItems(ADENA, COST);
					st.giveItems(crystal, 1);
					htmltext = npc.getNpcId() + "-" + ok + ".htm";
				}
			}
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		return "1.htm";
	}

	public static void onLoad() {
		new EchoCrystals(-1, "EchoCrystals", "", "custom");
	}

	public EchoCrystals(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		// Initialize Map
		SCORES.put(4410, new ScoreData(4411, "01", "02", "03"));
		SCORES.put(4409, new ScoreData(4412, "04", "05", "06"));
		SCORES.put(4408, new ScoreData(4413, "07", "08", "09"));
		SCORES.put(4420, new ScoreData(4414, "10", "11", "12"));
		SCORES.put(4421, new ScoreData(4415, "13", "14", "15"));
		SCORES.put(4419, new ScoreData(4417, "16", "05", "06"));
		SCORES.put(4418, new ScoreData(4416, "17", "05", "06"));

		addStartNpc(31042, 31043);
		addTalkId(31042, 31043);
	}
}