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
public class Q070_SagaOfThePhoenixKnight extends SagasSuperClass {
	private static final String name = "Q070_SagaOfThePhoenixKnight";
	private static final int scriptId = 70;
	private static final String dname = "";
	private static final String path = "quests";

	public Q070_SagaOfThePhoenixKnight() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30849, 31624, 31277, 30849, 31631, 31646, 31647, 31650, 31654, 31655, 31657, 31277 };

		Items = new int[] { 7080, 7534, 7081, 7485, 7268, 7299, 7330, 7361, 7392, 7423, 7093, 6482 };

		Mob = new int[] { 27286, 27219, 27278 };

		qn = name;
		classid = 90;
		prevclass = 0x05;

		X = new int[] { 191046, 46087, 46066 };

		Y = new int[] { -40640, -36372, -36396 };

		Z = new int[] { -3042, -1685, -1685 };

		registerNPCs();
	}
}