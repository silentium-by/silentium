/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.vmasters;

import java.util.HashMap;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class OccupationEngine extends Quest
{
	public HashMap<String, Classes> classList = new HashMap<String, Classes>();

	public OccupationEngine(int questId, String name, String descr)
	{
		super(questId, name, descr);
		classList.put("1", new Classes(1, 0, 0, "26", "27", "28", "29", new int[] { 1145 }, true));
		classList.put("11", new Classes(11, 10, 0, "23", "24", "25", "26", new int[] { 1292 }, true));
		classList.put("12", new Classes(12, 11, 0, "26", "27", "28", "29", new int[] { 2674, 2734, 2840 }, false));
		classList.put("13", new Classes(13, 11, 0, "30", "31", "32", "33", new int[] { 2674, 2734, 3307 }, false));
		classList.put("14", new Classes(14, 11, 0, "34", "35", "36", "37", new int[] { 2674, 2734, 3336 }, false));
		classList.put("15", new Classes(15, 10, 0, "27", "28", "29", "30", new int[] { 1201 }, true));
		classList.put("16", new Classes(16, 15, 0, "16", "17", "18", "19", new int[] { 2721, 2734, 2820 }, false));
		classList.put("17", new Classes(17, 15, 0, "20", "21", "22", "23", new int[] { 2721, 2734, 2821 }, false));
		classList.put("19", new Classes(19, 18, 1, "18", "19", "20", "21", new int[] { 1204 }, true));
		classList.put("2", new Classes(2, 1, 0, "68", "69", "70", "71", new int[] { 2627, 2734, 2762 }, false));
		classList.put("20", new Classes(20, 19, 1, "36", "37", "38", "39", new int[] { 2633, 3140, 2820 }, false));
		classList.put("21", new Classes(21, 19, 1, "40", "41", "42", "43", new int[] { 2627, 3140, 2762 }, false));
		classList.put("22", new Classes(22, 18, 1, "22", "23", "24", "25", new int[] { 1217 }, true));
		classList.put("23", new Classes(23, 22, 1, "60", "61", "62", "63", new int[] { 2673, 3140, 2809 }, false));
		classList.put("24", new Classes(24, 22, 1, "64", "65", "66", "67", new int[] { 2673, 3140, 3293 }, false));
		classList.put("26", new Classes(26, 25, 1, "15", "16", "17", "18", new int[] { 1230 }, true));
		classList.put("27", new Classes(27, 26, 1, "18", "19", "20", "21", new int[] { 2674, 3140, 2840 }, false));
		classList.put("28", new Classes(28, 26, 1, "22", "23", "24", "25", new int[] { 2674, 3140, 3336 }, false));
		classList.put("29", new Classes(29, 25, 1, "19", "20", "21", "22", new int[] { 1235 }, true));
		classList.put("3", new Classes(3, 1, 0, "72", "73", "74", "75", new int[] { 2627, 2734, 3276 }, false));
		classList.put("30", new Classes(30, 29, 1, "12", "13", "14", "15", new int[] { 2721, 3140, 2820 }, false));
		classList.put("32", new Classes(32, 31, 2, "15", "16", "17", "18", new int[] { 1244 }, true));
		classList.put("33", new Classes(33, 32, 2, "26", "27", "28", "29", new int[] { 2633, 3172, 3307 }, false));
		classList.put("34", new Classes(34, 32, 2, "30", "31", "32", "33", new int[] { 2627, 3172, 2762 }, false));
		classList.put("35", new Classes(35, 31, 2, "19", "20", "21", "22", new int[] { 1252 }, true));
		classList.put("36", new Classes(36, 35, 2, "38", "39", "40", "41", new int[] { 2673, 3172, 2809 }, false));
		classList.put("37", new Classes(37, 35, 2, "42", "43", "44", "45", new int[] { 2673, 3172, 3293 }, false));
		classList.put("39", new Classes(39, 38, 2, "23", "24", "25", "26", new int[] { 1261 }, true));
		classList.put("4", new Classes(4, 0, 0, "30", "31", "32", "33", new int[] { 1161 }, true));
		classList.put("40", new Classes(40, 39, 2, "46", "47", "48", "49", new int[] { 2674, 3172, 2840 }, false));
		classList.put("41", new Classes(41, 39, 2, "50", "51", "52", "53", new int[] { 2674, 3172, 3336 }, false));
		classList.put("42", new Classes(42, 38, 2, "27", "28", "29", "30", new int[] { 1270 }, true));
		classList.put("43", new Classes(43, 42, 2, "34", "35", "36", "37", new int[] { 2721, 3172, 2821 }, false));
		classList.put("45", new Classes(45, 44, 3, "09", "10", "11", "12", new int[] { 1592 }, true));
		classList.put("46", new Classes(46, 45, 3, "20", "21", "22", "23", new int[] { 2627, 3203, 3276 }, false));
		classList.put("47", new Classes(47, 44, 3, "13", "14", "15", "16", new int[] { 1615 }, true));
		classList.put("48", new Classes(48, 47, 3, "16", "17", "18", "19", new int[] { 2627, 3203, 2762 }, false));
		classList.put("5", new Classes(5, 4, 0, "44", "45", "46", "47", new int[] { 2633, 2734, 2820 }, false));
		classList.put("50", new Classes(50, 49, 3, "17", "18", "19", "20", new int[] { 1631 }, true));
		classList.put("51", new Classes(51, 50, 3, "24", "25", "26", "27", new int[] { 2721, 3203, 3390 }, false));
		classList.put("52", new Classes(52, 50, 3, "28", "29", "30", "31", new int[] { 2721, 3203, 2879 }, false));
		classList.put("6", new Classes(6, 4, 0, "48", "49", "50", "51", new int[] { 2633, 2734, 3307 }, false));
		classList.put("7", new Classes(7, 0, 0, "34", "35", "36", "37", new int[] { 1190 }, true));
		classList.put("8", new Classes(8, 7, 0, "52", "53", "54", "55", new int[] { 2673, 2734, 2809 }, false));
		classList.put("9", new Classes(9, 7, 0, "56", "57", "58", "59", new int[] { 2673, 2734, 3293 }, false));
	}

	public static void main(String[] args)
	{
		new OccupationEngine(-1, "OccupationEngine", "vmasters");
	}

	public class Classes
	{
		public int newClass, reqClass, reqRace;
		public String low_ni, low_i, ok_ni, ok_i;
		public int[] reqItems;
		public boolean first;

		public Classes(int _newClass, int _reqClass, int _reqRace, String _low_ni, String _low_i, String _ok_ni, String _ok_i, int[] _reqItems, boolean _first)
		{
			newClass = _newClass;
			reqClass = _reqClass;
			reqRace = _reqRace;
			low_ni = _low_ni;
			low_i = _low_i;
			ok_ni = _ok_ni;
			ok_i = _ok_i;
			reqItems = _reqItems;
			first = _first;
		}
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(getName());
		if (event.endsWith(".htm"))
			return event;
		String htmltext = getNoQuestMsg();
		String suffix = "";
		if (classList.containsKey(event))
		{
			Classes val = classList.get(event);
			if (player.getRace().ordinal() == val.reqRace && player.getClassId().getId() == val.reqClass)
			{
				boolean item = true;
				for (int i : val.reqItems)
				{
					if (player.getItemsCount(i) == 0)
						item = false;
				}
				if ((player.getLevel() < 40 && !val.first) || (player.getLevel() < 20 && val.first))
				{
					suffix = val.low_i;
					if (!item)
						suffix = val.low_ni;
				}
				else
				{
					if (!item)
						suffix = val.ok_ni;
					else
					{
						suffix = val.ok_i;
						if (val.first)
							st.giveItems(8869, 15);
						change(player, val.newClass, val.reqItems);
					}
				}
			}
			if (this instanceof OrcOccupationChange2)
				htmltext = "30513-" + suffix + ".htm";
			else if (this instanceof OrcOccupationChange1)
				htmltext = npc.getNpcId() + suffix + ".htm";
			else if (this instanceof DarkElvenChange1)
				htmltext = npc.getNpcId() + suffix + ".htm";
			else if (this instanceof DarkElvenChange2)
				htmltext = "30474" + suffix + ".htm";
			else if (this instanceof ElvenHumanBuffers2)
				htmltext = "30120" + suffix + ".htm";
			else if (this instanceof ElvenHumanMystics2)
				htmltext = "30115" + suffix + ".htm";
			else if (this instanceof ElvenHumanFighters1)
				htmltext = npc.getNpcId() + suffix + ".htm";
			else if (this instanceof ElvenHumanMystics1)
				htmltext = npc.getNpcId() + suffix + ".htm";
			else if (this instanceof ElvenHumanFighters2)
				htmltext = "30109" + suffix + ".htm";
		}
		return htmltext;
	}

	public static void change(L2PcInstance player, int newclass, int[] items)
	{
		for (int item : items)
			player.takeItems(item, 1);
		player.setClassId(newclass);
		player.setBaseClass(newclass);
		player.broadcastUserInfo();
	}
}