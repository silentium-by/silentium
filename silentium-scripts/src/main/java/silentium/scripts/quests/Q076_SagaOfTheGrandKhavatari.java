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
public class Q076_SagaOfTheGrandKhavatari extends SagasSuperClass {
	private static final String name = "Q076_SagaOfTheGrandKhavatari";
	private static final int scriptId = 76;
	private static final String dname = "Saga Of The Grand Khavatari";
	private static final String path = "quests";

	public Q076_SagaOfTheGrandKhavatari() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31339, 31624, 31589, 31290, 31637, 31646, 31647, 31652, 31654, 31655, 31659, 31290 };

		Items = new int[] { 7080, 7539, 7081, 7491, 7274, 7305, 7336, 7367, 7398, 7429, 7099, 0 };

		Mob = new int[] { 27293, 27226, 27284 };

		qn = name;
		classid = 114;
		prevclass = 0x30;

		X = new int[] { 161719, 124355, 124376 };

		Y = new int[] { -92823, 82155, 82127 };

		Z = new int[] { -1893, -2803, -2796 };

		registerNPCs();
	}
}