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
public class Q085_SagaOfTheCardinal extends SagasSuperClass {
	private static final String name = "Q085_SagaOfTheCardinal";
	private static final int scriptId = 85;
	private static final String dname = "Saga Of The Cardinal";
	private static final String path = "quests";

	public Q085_SagaOfTheCardinal() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30191, 31626, 31588, 31280, 31644, 31646, 31647, 31651, 31654, 31655, 31658, 31280 };

		Items = new int[] { 7080, 7522, 7081, 7500, 7283, 7314, 7345, 7376, 7407, 7438, 7087, 0 };

		Mob = new int[] { 27267, 27234, 27274 };

		qn = name;
		classid = 97;
		prevclass = 0x10;

		X = new int[] { 119518, 181215, 181227 };

		Y = new int[] { -28658, 36676, 36703 };

		Z = new int[] { -3811, -4812, -4816 };

		registerNPCs();
	}
}