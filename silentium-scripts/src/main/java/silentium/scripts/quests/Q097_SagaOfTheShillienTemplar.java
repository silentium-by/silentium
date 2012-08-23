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
public class Q097_SagaOfTheShillienTemplar extends SagasSuperClass {
	private static final String name = "Q097_SagaOfTheShillienTemplar";
	private static final int scriptId = 97;
	private static final String dname = "";
	private static final String path = "quests";

	public Q097_SagaOfTheShillienTemplar() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31580, 31623, 31285, 31285, 31610, 31646, 31648, 31652, 31654, 31655, 31659, 31285 };

		Items = new int[] { 7080, 7526, 7081, 7512, 7295, 7326, 7357, 7388, 7419, 7450, 7091, 0 };

		Mob = new int[] { 27271, 27246, 27273 };

		qn = name;
		classid = 106;
		prevclass = 0x21;

		X = new int[] { 161719, 124355, 124376 };

		Y = new int[] { -92823, 82155, 82127 };

		Z = new int[] { -1893, -2803, -2796 };

		registerNPCs();
	}
}