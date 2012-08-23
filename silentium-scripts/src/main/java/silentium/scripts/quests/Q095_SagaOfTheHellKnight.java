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
public class Q095_SagaOfTheHellKnight extends SagasSuperClass {
	private static final String name = "Q095_SagaOfTheHellKnight";
	private static final int scriptId = 95;
	private static final String dname = "Saga Of The Hell Knight";
	private static final String path = "quests";

	public Q095_SagaOfTheHellKnight() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31582, 31623, 31297, 31297, 31599, 31646, 31647, 31653, 31654, 31655, 31656, 31297 };

		Items = new int[] { 7080, 7532, 7081, 7510, 7293, 7324, 7355, 7386, 7417, 7448, 7086, 0 };

		Mob = new int[] { 27258, 27244, 27263 };

		qn = name;
		classid = 91;
		prevclass = 0x06;

		X = new int[] { 164650, 47391, 47429 };

		Y = new int[] { -74121, -56929, -56923 };

		Z = new int[] { -2871, -2370, -2383 };

		registerNPCs();
	}
}