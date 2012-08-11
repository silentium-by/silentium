/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public final class ClansConfig extends ConfigEngine
{
	public static boolean ALT_MANOR_SAVE_ALL_ACTIONS;
	public static boolean ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH;
	public static boolean REMOVE_CASTLE_CIRCLETS;
	public static int ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED;
	public static int ALT_ALLY_JOIN_DAYS_WHEN_LEAVED;
	public static int ALT_CLAN_CREATE_DAYS;
	public static int ALT_CLAN_DISSOLVE_DAYS;
	public static int ALT_CLAN_JOIN_DAYS;
	public static int ALT_CLAN_MEMBERS_FOR_WAR;
	public static int ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED;
	public static int ALT_MANOR_APPROVE_MIN;
	public static int ALT_MANOR_APPROVE_TIME;
	public static int ALT_MANOR_MAINTENANCE_PERIOD;
	public static int ALT_MANOR_REFRESH_MIN;
	public static int ALT_MANOR_REFRESH_TIME;
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
	public static int ALT_MAX_NUM_OF_CLANS_IN_ALLY;
	public static int CH_CURTAIN1_FEE;
	public static int CH_CURTAIN2_FEE;
	public static int CH_EXPREG1_FEE;
	public static int CH_EXPREG2_FEE;
	public static int CH_EXPREG3_FEE;
	public static int CH_EXPREG4_FEE;
	public static int CH_EXPREG5_FEE;
	public static int CH_EXPREG6_FEE;
	public static int CH_EXPREG7_FEE;
	public static int CH_FRONT1_FEE;
	public static int CH_FRONT2_FEE;
	public static int CH_HPREG1_FEE;
	public static int CH_HPREG10_FEE;
	public static int CH_HPREG11_FEE;
	public static int CH_HPREG12_FEE;
	public static int CH_HPREG13_FEE;
	public static int CH_HPREG2_FEE;
	public static int CH_HPREG3_FEE;
	public static int CH_HPREG4_FEE;
	public static int CH_HPREG5_FEE;
	public static int CH_HPREG6_FEE;
	public static int CH_HPREG7_FEE;
	public static int CH_HPREG8_FEE;
	public static int CH_HPREG9_FEE;
	public static int CH_ITEM1_FEE;
	public static int CH_ITEM2_FEE;
	public static int CH_ITEM3_FEE;
	public static int CH_MPREG1_FEE;
	public static int CH_MPREG2_FEE;
	public static int CH_MPREG3_FEE;
	public static int CH_MPREG4_FEE;
	public static int CH_MPREG5_FEE;
	public static int CH_SUPPORT1_FEE;
	public static int CH_SUPPORT2_FEE;
	public static int CH_SUPPORT3_FEE;
	public static int CH_SUPPORT4_FEE;
	public static int CH_SUPPORT5_FEE;
	public static int CH_SUPPORT6_FEE;
	public static int CH_SUPPORT7_FEE;
	public static int CH_SUPPORT8_FEE;
	public static int CH_TELE1_FEE;
	public static int CH_TELE2_FEE;
	public static long CH_CURTAIN_FEE_RATIO;
	public static long CH_EXPREG_FEE_RATIO;
	public static long CH_FRONT_FEE_RATIO;
	public static long CH_HPREG_FEE_RATIO;
	public static long CH_ITEM_FEE_RATIO;
	public static long CH_MPREG_FEE_RATIO;
	public static long CH_SUPPORT_FEE_RATIO;
	public static long CH_TELE_FEE_RATIO;

	public static void load()
	{
		try (InputStream is = new FileInputStream(new File(CLANS_FILE)))
		{
			Properties clans = new Properties();
			clans.load(is);
			is.close();

			ALT_ACCEPT_CLAN_DAYS_WHEN_DISMISSED = Integer.parseInt(clans.getProperty("DaysBeforeAcceptNewClanWhenDismissed", "1"));
			ALT_ALLY_JOIN_DAYS_WHEN_DISMISSED = Integer.parseInt(clans.getProperty("DaysBeforeJoinAllyWhenDismissed", "1"));
			ALT_ALLY_JOIN_DAYS_WHEN_LEAVED = Integer.parseInt(clans.getProperty("DaysBeforeJoinAllyWhenLeaved", "1"));
			ALT_CLAN_CREATE_DAYS = Integer.parseInt(clans.getProperty("DaysBeforeCreateAClan", "10"));
			ALT_CLAN_DISSOLVE_DAYS = Integer.parseInt(clans.getProperty("DaysToPassToDissolveAClan", "7"));
			ALT_CLAN_JOIN_DAYS = Integer.parseInt(clans.getProperty("DaysBeforeJoinAClan", "5"));
			ALT_CLAN_MEMBERS_FOR_WAR = Integer.parseInt(clans.getProperty("AltClanMembersForWar", "15"));
			ALT_CREATE_ALLY_DAYS_WHEN_DISSOLVED = Integer.parseInt(clans.getProperty("DaysBeforeCreateNewAllyWhenDissolved", "10"));
			ALT_MAX_NUM_OF_CLANS_IN_ALLY = Integer.parseInt(clans.getProperty("AltMaxNumOfClansInAlly", "3"));
			ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH = Boolean.parseBoolean(clans.getProperty("AltMembersCanWithdrawFromClanWH", "False"));

			ALT_MANOR_APPROVE_MIN = Integer.parseInt(clans.getProperty("AltManorApproveMin", "00"));
			ALT_MANOR_APPROVE_TIME = Integer.parseInt(clans.getProperty("AltManorApproveTime", "6"));
			ALT_MANOR_MAINTENANCE_PERIOD = Integer.parseInt(clans.getProperty("AltManorMaintenancePeriod", "360000"));
			ALT_MANOR_REFRESH_MIN = Integer.parseInt(clans.getProperty("AltManorRefreshMin", "00"));
			ALT_MANOR_REFRESH_TIME = Integer.parseInt(clans.getProperty("AltManorRefreshTime", "20"));
			ALT_MANOR_SAVE_ALL_ACTIONS = Boolean.parseBoolean(clans.getProperty("AltManorSaveAllActions", "False"));
			ALT_MANOR_SAVE_PERIOD_RATE = Integer.parseInt(clans.getProperty("AltManorSavePeriodRate", "2"));

			CH_CURTAIN_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallCurtainFunctionFeeRation", "86400000"));
			CH_CURTAIN1_FEE = Integer.parseInt(clans.getProperty("ClanHallCurtainFunctionFeeLvl1", "86400000"));
			CH_CURTAIN2_FEE = Integer.parseInt(clans.getProperty("ClanHallCurtainFunctionFeeLvl2", "86400000"));
			CH_EXPREG_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallExpRegenerationFunctionFeeRation", "86400000"));
			CH_EXPREG1_FEE = Integer.parseInt(clans.getProperty("ClanHallExpRegenerationFeeLvl1", "86400000"));
			CH_EXPREG2_FEE = Integer.parseInt(clans.getProperty("ClanHallExpRegenerationFeeLvl2", "86400000"));
			CH_EXPREG3_FEE = Integer.parseInt(clans.getProperty("ClanHallExpRegenerationFeeLvl3", "86400000"));
			CH_EXPREG4_FEE = Integer.parseInt(clans.getProperty("ClanHallExpRegenerationFeeLvl4", "86400000"));
			CH_EXPREG5_FEE = Integer.parseInt(clans.getProperty("ClanHallExpRegenerationFeeLvl5", "86400000"));
			CH_EXPREG6_FEE = Integer.parseInt(clans.getProperty("ClanHallExpRegenerationFeeLvl6", "86400000"));
			CH_EXPREG7_FEE = Integer.parseInt(clans.getProperty("ClanHallExpRegenerationFeeLvl7", "86400000"));
			CH_FRONT_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallFrontPlatformFunctionFeeRation", "86400000"));
			CH_FRONT1_FEE = Integer.parseInt(clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl1", "86400000"));
			CH_FRONT2_FEE = Integer.parseInt(clans.getProperty("ClanHallFrontPlatformFunctionFeeLvl2", "86400000"));
			CH_HPREG_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallHpRegenerationFunctionFeeRation", "86400000"));
			CH_HPREG1_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl1", "86400000"));
			CH_HPREG10_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl10", "86400000"));
			CH_HPREG11_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl11", "86400000"));
			CH_HPREG12_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl12", "86400000"));
			CH_HPREG13_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl13", "86400000"));
			CH_HPREG2_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl2", "86400000"));
			CH_HPREG3_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl3", "86400000"));
			CH_HPREG4_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl4", "86400000"));
			CH_HPREG5_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl5", "86400000"));
			CH_HPREG6_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl6", "86400000"));
			CH_HPREG7_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl7", "86400000"));
			CH_HPREG8_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl8", "86400000"));
			CH_HPREG9_FEE = Integer.parseInt(clans.getProperty("ClanHallHpRegenerationFeeLvl9", "86400000"));
			CH_ITEM_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallItemCreationFunctionFeeRation", "86400000"));
			CH_ITEM1_FEE = Integer.parseInt(clans.getProperty("ClanHallItemCreationFunctionFeeLvl1", "86400000"));
			CH_ITEM2_FEE = Integer.parseInt(clans.getProperty("ClanHallItemCreationFunctionFeeLvl2", "86400000"));
			CH_ITEM3_FEE = Integer.parseInt(clans.getProperty("ClanHallItemCreationFunctionFeeLvl3", "86400000"));
			CH_MPREG_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallMpRegenerationFunctionFeeRation", "86400000"));
			CH_MPREG1_FEE = Integer.parseInt(clans.getProperty("ClanHallMpRegenerationFeeLvl1", "86400000"));
			CH_MPREG2_FEE = Integer.parseInt(clans.getProperty("ClanHallMpRegenerationFeeLvl2", "86400000"));
			CH_MPREG3_FEE = Integer.parseInt(clans.getProperty("ClanHallMpRegenerationFeeLvl3", "86400000"));
			CH_MPREG4_FEE = Integer.parseInt(clans.getProperty("ClanHallMpRegenerationFeeLvl4", "86400000"));
			CH_MPREG5_FEE = Integer.parseInt(clans.getProperty("ClanHallMpRegenerationFeeLvl5", "86400000"));
			CH_SUPPORT_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallSupportFunctionFeeRation", "86400000"));
			CH_SUPPORT1_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl1", "86400000"));
			CH_SUPPORT2_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl2", "86400000"));
			CH_SUPPORT3_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl3", "86400000"));
			CH_SUPPORT4_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl4", "86400000"));
			CH_SUPPORT5_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl5", "86400000"));
			CH_SUPPORT6_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl6", "86400000"));
			CH_SUPPORT7_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl7", "86400000"));
			CH_SUPPORT8_FEE = Integer.parseInt(clans.getProperty("ClanHallSupportFeeLvl8", "86400000"));
			CH_TELE_FEE_RATIO = Long.parseLong(clans.getProperty("ClanHallTeleportFunctionFeeRation", "86400000"));
			CH_TELE1_FEE = Integer.parseInt(clans.getProperty("ClanHallTeleportFunctionFeeLvl1", "86400000"));
			CH_TELE2_FEE = Integer.parseInt(clans.getProperty("ClanHallTeleportFunctionFeeLvl2", "86400000"));

			REMOVE_CASTLE_CIRCLETS = Boolean.parseBoolean(clans.getProperty("RemoveCastleCirclets", "True"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Server failed to load " + CLANS_FILE + " file.");
		}
	}
}