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
public class Q096_SagaOfTheSpectralDancer extends SagasSuperClass {
	private static final String name = "Q096_SagaOfTheSpectralDancer";
	private static final int scriptId = 96;
	private static final String dname = "Saga Of The Spectral Dancer";
	private static final String path = "quests";

	public Q096_SagaOfTheSpectralDancer() {
		super(scriptId, name, dname, path);

		NPC = new int[] { 31582, 31623, 31284, 31284, 31611, 31646, 31649, 31653, 31654, 31655, 31656, 31284 };

		Items = new int[] { 7080, 7527, 7081, 7511, 7294, 7325, 7356, 7387, 7418, 7449, 7092, 0 };

		Mob = new int[] { 27272, 27245, 27264 };

		qn = name;
		classid = 107;
		prevclass = 0x22;

		X = new int[] { 164650, 47429, 47391 };

		Y = new int[] { -74121, -56923, -56929 };

		Z = new int[] { -2871, -2383, -2370 };

		registerNPCs();
	}
}