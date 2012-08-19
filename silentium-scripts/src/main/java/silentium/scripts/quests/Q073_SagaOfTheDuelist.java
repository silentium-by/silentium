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
	public static final String qn1 = "Q073_SagaOfTheDuelist";
	public static final int qnu = 73;
	public static final String qna = "quests";

	public Q073_SagaOfTheDuelist() {
		super(qnu, qn1, qna);

		NPC = new int[] { 30849, 31624, 31226, 31331, 31639, 31646, 31647, 31653, 31654, 31655, 31656, 31277 };

		Items = new int[] { 7080, 7537, 7081, 7488, 7271, 7302, 7333, 7364, 7395, 7426, 7096, 7546 };

		Mob = new int[] { 27289, 27222, 27281 };

		qn = qn1;
		classid = 88;
		prevclass = 0x02;

		X = new int[] { 164650, 47429, 47391 };

		Y = new int[] { -74121, -56923, -56929 };

		Z = new int[] { -2871, -2383, -2370 };

		registerNPCs();
	}
}