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
public class Q081_SagaOfTheGhostHunter extends SagasSuperClass {
	private static final String name = "Q081_SagaOfTheGhostHunter";
	private static final int scriptId = 81;
	private static final String dname = "Saga Of The Ghost Hunter";
	private static final String path = "quests";

	public Q081_SagaOfTheGhostHunter() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31603, 31624, 31286, 31615, 31617, 31646, 31649, 31653, 31654, 31655, 31656, 31616 };

		Items = new int[] { 7080, 7518, 7081, 7496, 7279, 7310, 7341, 7372, 7403, 7434, 7104, 0 };

		Mob = new int[] { 27301, 27230, 27304 };

		qn = name;
		classid = 108;
		prevclass = 0x24;

		X = new int[] { 164650, 47391, 47429 };

		Y = new int[] { -74121, -56929, -56923 };

		Z = new int[] { -2871, -2370, -2383 };

		registerNPCs();
	}
}