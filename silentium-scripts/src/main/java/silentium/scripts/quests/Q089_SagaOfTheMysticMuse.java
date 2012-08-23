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
public class Q089_SagaOfTheMysticMuse extends SagasSuperClass {
	private static final String name = "Q089_SagaOfTheMysticMuse";
	private static final int scriptId = 89;
	private static final String dname = "";
	private static final String path = "quests";

	public Q089_SagaOfTheMysticMuse() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30174, 31627, 31283, 31283, 31643, 31646, 31648, 31651, 31654, 31655, 31658, 31283 };

		Items = new int[] { 7080, 7530, 7081, 7504, 7287, 7318, 7349, 7380, 7411, 7442, 7083, 0 };

		Mob = new int[] { 27251, 27238, 27255 };

		qn = name;
		classid = 103;
		prevclass = 0x1b;

		X = new int[] { 119518, 181227, 181215 };

		Y = new int[] { -28658, 36703, 36676 };

		Z = new int[] { -3811, -4816, -4812 };

		registerNPCs();
	}
}