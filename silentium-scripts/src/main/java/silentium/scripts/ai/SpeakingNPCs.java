/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.ai;

import silentium.gameserver.ai.DefaultMonsterAI;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.scripting.ScriptFile;

/**
 * Speaking NPCs implementation.<br>
 * <br>
 * This AI leads the behavior of any speaking NPC.<br>
 * It sends back the good string following the action and the npcId.<br>
 * <br>
 * <font color="red"><b><u>TODO:</b></u> Replace the system of switch by an XML, once a decent amount of NPCs is mapped.</font>
 *
 * @author Tryskell
 */
public class SpeakingNPCs extends DefaultMonsterAI implements ScriptFile {
	private static final int[] NPC_IDS = { 18212, //
			18213, //
			18214, //
			18215, //
			18216, // Archon of Halisha
			18217, //
			18218, //
			18219, //

			27016, // Nerkas
			27021, // Kirunak
			27022, // Merkenis

			27219, //
			27220, //
			27221, //
			27222, //
			27223, //
			27224, //
			27225, //
			27226, //
			27227, //
			27228, //
			27229, //
			27230, //
			27231, //
			27232, // Archon of Halisha
			27233, //
			27234, //
			27235, //
			27236, //
			27237, //
			27238, //
			27239, //
			27240, //
			27241, //
			27242, //
			27243, //
			27244, //
			27245, //
			27246, //
			27247, //
			27249 };

	public static void onLoad() {
		new SpeakingNPCs(-1, "SpeakingNPCs", "SpeakingNPCs", "ai");
	}

	public SpeakingNPCs(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);
		registerMobs(NPC_IDS, QuestEventType.ON_ATTACK, QuestEventType.ON_KILL);
	}

	@Override
	public String onAttack(final L2Npc npc, final L2PcInstance attacker, final int damage, final boolean isPet) {
		if (npc.hasSpoken())
			return super.onAttack(npc, attacker, damage, isPet);

		final int npcId = npc.getNpcId();
		String message = "";

		switch (npcId) {
			case 18212:
			case 18213:
			case 18214:
			case 18215:
			case 18216:
			case 18217:
			case 18218:
			case 18219:
			case 27219:
			case 27220:
			case 27221:
			case 27222:
			case 27223:
			case 27224:
			case 27225:
			case 27226:
			case 27227:
			case 27228:
			case 27229:
			case 27230:
			case 27231:
			case 27232:
			case 27233:
			case 27234:
			case 27235:
			case 27236:
			case 27237:
			case 27238:
			case 27239:
			case 27240:
			case 27241:
			case 27242:
			case 27243:
			case 27244:
			case 27245:
			case 27246:
			case 27247:
			case 27249:
				message = "You dare to disturb the order of the shrine! Die!";
				break;

			case 27016:
				message = "...How dare you challenge me!";
				break;

			case 27021:
				message = "I will taste your blood!";
				break;

			case 27022:
				message = "I shall put you in a never-ending nightmare!";
				break;
		}

		npc.broadcastNpcSay(message);
		npc.setHasSpoken(true); // Make the mob speaks only once, else he will spam.

		return super.onAttack(npc, attacker, damage, isPet);
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final int npcId = npc.getNpcId();
		String message = "";

		switch (npcId) {
			case 18212:
			case 18213:
			case 18214:
			case 18215:
			case 18216:
			case 18217:
			case 18218:
			case 18219:
			case 27219:
			case 27220:
			case 27221:
			case 27222:
			case 27223:
			case 27224:
			case 27225:
			case 27226:
			case 27227:
			case 27228:
			case 27229:
			case 27230:
			case 27231:
			case 27232:
			case 27233:
			case 27234:
			case 27235:
			case 27236:
			case 27237:
			case 27238:
			case 27239:
			case 27240:
			case 27241:
			case 27242:
			case 27243:
			case 27244:
			case 27245:
			case 27246:
			case 27247:
			case 27249:
				message = "My spirit is releasing from this shell. I'm getting close to Halisha...";
				break;

			case 27016:
				message = "May Beleth's power be spread on the whole world...!";
				break;

			case 27021:
				message = "I have fulfilled my contract with Trader Creamees.";
				break;

			case 27022:
				message = "My soul belongs to Icarus...";
				break;
		}

		npc.broadcastNpcSay(message);
		npc.setHasSpoken(false); // Reset the flag, to unmute the NPC.

		return super.onKill(npc, player, isPet);
	}
}