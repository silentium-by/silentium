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
public class Q090_SagaOfTheStormScreamer extends SagasSuperClass {
	private static final String name = "Q090_SagaOfTheStormScreamer";
	private static final int scriptId = 90;
	private static final String dname = "";
	private static final String path = "quests";

	public Q090_SagaOfTheStormScreamer() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30175, 31627, 31287, 31287, 31598, 31646, 31649, 31652, 31654, 31655, 31659, 31287 };

		Items = new int[] { 7080, 7531, 7081, 7505, 7288, 7319, 7350, 7381, 7412, 7443, 7084, 0 };

		Mob = new int[] { 27252, 27239, 27256 };

		qn = name;
		classid = 110;
		prevclass = 0x28;

		X = new int[] { 161719, 124376, 124355 };

		Y = new int[] { -92823, 82127, 82155 };

		Z = new int[] { -1893, -2796, -2803 };

		registerNPCs();
	}
}