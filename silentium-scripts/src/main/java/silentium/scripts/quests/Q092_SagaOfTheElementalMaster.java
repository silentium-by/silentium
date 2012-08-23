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
public class Q092_SagaOfTheElementalMaster extends SagasSuperClass {
	private static final String name = "Q092_SagaOfTheElementalMaster";
	private static final int scriptId = 92;
	private static final String dname = "Saga Of The Elemental Master";
	private static final String path = "quests";

	public Q092_SagaOfTheElementalMaster() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30174, 31281, 31614, 31614, 31629, 31646, 31648, 31652, 31654, 31655, 31659, 31614 };

		Items = new int[] { 7080, 7605, 7081, 7507, 7290, 7321, 7352, 7383, 7414, 7445, 7111, 0 };

		Mob = new int[] { 27314, 27241, 27311 };

		qn = name;
		classid = 104;
		prevclass = 0x1c;

		X = new int[] { 161719, 124376, 124355 };

		Y = new int[] { -92823, 82127, 82155 };

		Z = new int[] { -1893, -2796, -2803 };

		registerNPCs();
	}
}