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
public class Q077_SagaOfTheDominator extends SagasSuperClass {
	private static final String name = "Q077_SagaOfTheDominator";
	private static final int scriptId = 77;
	private static final String dname = "Saga Of The Dominator";
	private static final String path = "quests";

	public Q077_SagaOfTheDominator() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31336, 31624, 31371, 31290, 31636, 31646, 31648, 31653, 31654, 31655, 31656, 31290 };

		Items = new int[] { 7080, 7539, 7081, 7492, 7275, 7306, 7337, 7368, 7399, 7430, 7100, 0 };

		Mob = new int[] { 27294, 27226, 27262 };

		qn = name;
		classid = 115;
		prevclass = 0x33;

		X = new int[] { 164650, 47429, 47391 };

		Y = new int[] { -74121, -56923, -56929 };

		Z = new int[] { -2871, -2383, -2370 };

		registerNPCs();
	}
}