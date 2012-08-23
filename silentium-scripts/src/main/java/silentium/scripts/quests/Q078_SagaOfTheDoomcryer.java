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
public class Q078_SagaOfTheDoomcryer extends SagasSuperClass {
	private static final String name = "Q078_SagaOfTheDoomcryer";
	private static final int scriptId = 78;
	private static final String dname = "";
	private static final String path = "quests";

	public Q078_SagaOfTheDoomcryer() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31336, 31624, 31589, 31290, 31642, 31646, 31649, 31650, 31654, 31655, 31657, 31290 };

		Items = new int[] { 7080, 7539, 7081, 7493, 7276, 7307, 7338, 7369, 7400, 7431, 7101, 0 };

		Mob = new int[] { 27295, 27227, 27285 };

		qn = name;
		classid = 116;
		prevclass = 0x34;

		X = new int[] { 191046, 46087, 46066 };

		Y = new int[] { -40640, -36372, -36396 };

		Z = new int[] { -3042, -1685, -1685 };

		registerNPCs();
	}
}