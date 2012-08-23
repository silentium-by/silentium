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
public class Q091_SagaOfTheArcanaLord extends SagasSuperClass {
	private static final String name = "Q091_SagaOfTheArcanaLord";
	private static final int scriptId = 91;
	private static final String dname = "Saga Of The Arcana Lord";
	private static final String path = "quests";

	public Q091_SagaOfTheArcanaLord() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31605, 31622, 31585, 31608, 31586, 31646, 31647, 31651, 31654, 31655, 31658, 31608 };

		Items = new int[] { 7080, 7604, 7081, 7506, 7289, 7320, 7351, 7382, 7413, 7444, 7110, 0 };

		Mob = new int[] { 27313, 27240, 27310 };

		qn = name;
		classid = 96;
		prevclass = 0x0e;

		X = new int[] { 119518, 181215, 181227 };

		Y = new int[] { -28658, 36676, 36703 };

		Z = new int[] { -3811, -4812, -4816 };

		registerNPCs();
	}
}