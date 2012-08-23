/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

/**
 * @author Emperorc
 */
public class Q094_SagaOfTheSoultaker extends SagasSuperClass {
	private static final String name = "Q094_SagaOfTheSoultaker";
	private static final int scriptId = 94;
	private static final String dname = "Saga Of The Soultaker";
	private static final String path = "quests";

	public Q094_SagaOfTheSoultaker() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30832, 31623, 31279, 31279, 31645, 31646, 31648, 31650, 31654, 31655, 31657, 31279 };

		Items = new int[] { 7080, 7533, 7081, 7509, 7292, 7323, 7354, 7385, 7416, 7447, 7085, 0 };

		Mob = new int[] { 27257, 27243, 27265 };

		qn = name;
		classid = 95;
		prevclass = 0x0d;

		X = new int[] { 191046, 46066, 46087 };

		Y = new int[] { -40640, -36396, -36372 };

		Z = new int[] { -3042, -1685, -1685 };

		registerNPCs();
	}
}