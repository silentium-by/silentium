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
public class Q087_SagaOfEvasSaint extends SagasSuperClass {
	private static final String name = "Q087_SagaOfEvasSaint";
	private static final int scriptId = 87;
	private static final String dname = "Saga Of Evas Saint";
	private static final String path = "quests";

	public Q087_SagaOfEvasSaint() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 30191, 31626, 31588, 31280, 31620, 31646, 31649, 31653, 31654, 31655, 31657, 31280 };

		Items = new int[] { 7080, 7524, 7081, 7502, 7285, 7316, 7347, 7378, 7409, 7440, 7088, 0 };

		Mob = new int[] { 27266, 27236, 27276 };

		qn = name;
		classid = 105;
		prevclass = 0x1e;

		X = new int[] { 164650, 46087, 46066 };

		Y = new int[] { -74121, -36372, -36396 };

		Z = new int[] { -2871, -1685, -1685 };

		registerNPCs();
	}
}