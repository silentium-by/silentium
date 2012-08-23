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
public class Q098_SagaOfTheShillienSaint extends SagasSuperClass {
	private static final String name = "Q098_SagaOfTheShillienSaint";
	private static final int scriptId = 98;
	private static final String dname = "";
	private static final String path = "quests";

	public Q098_SagaOfTheShillienSaint() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31581, 31626, 31588, 31287, 31621, 31646, 31647, 31651, 31654, 31655, 31658, 31287 };

		Items = new int[] { 7080, 7525, 7081, 7513, 7296, 7327, 7358, 7389, 7420, 7451, 7090, 0 };

		Mob = new int[] { 27270, 27247, 27277 };

		qn = name;
		classid = 112;
		prevclass = 0x2b;

		X = new int[] { 119518, 181215, 181227 };

		Y = new int[] { -28658, 36676, 36703 };

		Z = new int[] { -3811, -4812, -4816 };

		registerNPCs();
	}
}