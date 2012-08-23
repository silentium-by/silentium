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
public class Q073_SagaOfTheDuelist extends SagasSuperClass {
	private static final String name = "Q073_SagaOfTheDuelist";
	private static final int scriptId = 73;
	private static final String dname = "Saga Of The Duelist";
	private static final String path = "quests";

	public Q073_SagaOfTheDuelist() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30849, 31624, 31226, 31331, 31639, 31646, 31647, 31653, 31654, 31655, 31656, 31277 };

		Items = new int[] { 7080, 7537, 7081, 7488, 7271, 7302, 7333, 7364, 7395, 7426, 7096, 7546 };

		Mob = new int[] { 27289, 27222, 27281 };

		qn = name;
		classid = 88;
		prevclass = 0x02;

		X = new int[] { 164650, 47429, 47391 };

		Y = new int[] { -74121, -56923, -56929 };

		Z = new int[] { -2871, -2383, -2370 };

		registerNPCs();
	}
}