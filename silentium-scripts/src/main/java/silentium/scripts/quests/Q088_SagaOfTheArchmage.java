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
public class Q088_SagaOfTheArchmage extends SagasSuperClass {
	private static final String name = "Q088_SagaOfTheArchmage";
	private static final int scriptId = 88;
	private static final String dname = "";
	private static final String path = "quests";

	public Q088_SagaOfTheArchmage() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30176, 31627, 31282, 31282, 31590, 31646, 31647, 31650, 31654, 31655, 31657, 31282 };

		Items = new int[] { 7080, 7529, 7081, 7503, 7286, 7317, 7348, 7379, 7410, 7441, 7082, 0 };

		Mob = new int[] { 27250, 27237, 27254 };

		qn = name;
		classid = 94;
		prevclass = 0x0c;

		X = new int[] { 191046, 46066, 46087 };

		Y = new int[] { -40640, -36396, -36372 };

		Z = new int[] { -3042, -1685, -1685 };

		registerNPCs();
	}
}