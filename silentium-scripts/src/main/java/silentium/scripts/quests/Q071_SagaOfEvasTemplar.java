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
public class Q071_SagaOfEvasTemplar extends SagasSuperClass {
	private static final String name = "Q071_SagaOfEvasTemplar";
	private static final int scriptId = 71;
	private static final String dname = "";
	private static final String path = "quests";

	public Q071_SagaOfEvasTemplar() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30852, 31624, 31278, 30852, 31638, 31646, 31648, 31651, 31654, 31655, 31658, 31281 };

		Items = new int[] { 7080, 7535, 7081, 7486, 7269, 7300, 7331, 7362, 7393, 7424, 7094, 6482 };

		Mob = new int[] { 27287, 27220, 27279 };

		qn = name;
		classid = 99;
		prevclass = 0x14;

		X = new int[] { 119518, 181215, 181227 };

		Y = new int[] { -28658, 36676, 36703 };

		Z = new int[] { -3811, -4812, -4816 };

		registerNPCs();
	}
}