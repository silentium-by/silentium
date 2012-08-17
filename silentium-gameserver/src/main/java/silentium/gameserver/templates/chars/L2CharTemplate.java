/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.templates.chars;

import silentium.gameserver.templates.StatsSet;

public class L2CharTemplate
{
	// BaseStats
	private final int _baseSTR;
	private final int _baseCON;
	private final int _baseDEX;
	private final int _baseINT;
	private final int _baseWIT;
	private final int _baseMEN;

	private final float _baseHpMax;
	private final float _baseCpMax;
	private final float _baseMpMax;

	private final float _baseHpReg;
	private final float _baseMpReg;

	private final int _basePAtk;
	private final int _baseMAtk;
	private final int _basePDef;
	private final int _baseMDef;

	private final int _basePAtkSpd;
	private final int _baseMAtkSpd;

	private final float _baseMReuseRate;
	private final int _baseShldDef;
	private final int _baseAtkRange;
	private final int _baseShldRate;

	private final int _baseCritRate;
	private final int _baseMCritRate;

	private final int _baseWalkSpd;
	private final int _baseRunSpd;

	private final int _baseMpConsumeRate;
	private final int _baseHpConsumeRate;

	private final int _collisionRadius;
	private final int _collisionHeight;

	public L2CharTemplate(StatsSet set)
	{
		// Base stats
		_baseSTR = set.getInteger("baseSTR");
		_baseCON = set.getInteger("baseCON");
		_baseDEX = set.getInteger("baseDEX");
		_baseINT = set.getInteger("baseINT");
		_baseWIT = set.getInteger("baseWIT");
		_baseMEN = set.getInteger("baseMEN");

		_baseHpMax = set.getFloat("baseHpMax");
		_baseCpMax = set.getFloat("baseCpMax");
		_baseMpMax = set.getFloat("baseMpMax");

		_baseHpReg = set.getFloat("baseHpReg", 1.5f);
		_baseMpReg = set.getFloat("baseMpReg", 0.9f);

		_basePAtk = set.getInteger("basePAtk");
		_baseMAtk = set.getInteger("baseMAtk");
		_basePDef = set.getInteger("basePDef");
		_baseMDef = set.getInteger("baseMDef");

		_basePAtkSpd = set.getInteger("basePAtkSpd");
		_baseMAtkSpd = set.getInteger("baseMAtkSpd");

		_baseMReuseRate = set.getFloat("baseMReuseDelay", 1.f);
		_baseShldDef = set.getInteger("baseShldDef", 0);
		_baseAtkRange = set.getInteger("baseAtkRange", 40);
		_baseShldRate = set.getInteger("baseShldRate", 0);

		_baseCritRate = set.getInteger("baseCritRate");
		_baseMCritRate = set.getInteger("baseMCritRate", 80);

		_baseWalkSpd = set.getInteger("baseWalkSpd", 0);
		_baseRunSpd = set.getInteger("baseRunSpd", 1);

		_baseMpConsumeRate = set.getInteger("baseMpConsumeRate", 0);
		_baseHpConsumeRate = set.getInteger("baseHpConsumeRate", 0);

		_collisionRadius = set.getInteger("collision_radius");
		_collisionHeight = set.getInteger("collision_height");
	}

	public int getBaseSTR()
	{
		return _baseSTR;
	}

	public int getBaseCON()
	{
		return _baseCON;
	}

	public int getBaseDEX()
	{
		return _baseDEX;
	}

	public int getBaseINT()
	{
		return _baseINT;
	}

	public int getBaseWIT()
	{
		return _baseWIT;
	}

	public int getBaseMEN()
	{
		return _baseMEN;
	}

	public float getBaseHpMax()
	{
		return _baseHpMax;
	}

	public float getBaseCpMax()
	{
		return _baseCpMax;
	}

	public float getBaseMpMax()
	{
		return _baseMpMax;
	}

	public float getBaseHpReg()
	{
		return _baseHpReg;
	}

	public float getBaseMpReg()
	{
		return _baseMpReg;
	}

	public int getBasePAtk()
	{
		return _basePAtk;
	}

	public int getBaseMAtk()
	{
		return _baseMAtk;
	}

	public int getBasePDef()
	{
		return _basePDef;
	}

	public int getBaseMDef()
	{
		return _baseMDef;
	}

	public int getBasePAtkSpd()
	{
		return _basePAtkSpd;
	}

	public int getBaseMAtkSpd()
	{
		return _baseMAtkSpd;
	}

	public float getBaseMReuseRate()
	{
		return _baseMReuseRate;
	}

	public int getBaseShldDef()
	{
		return _baseShldDef;
	}

	public int getBaseAtkRange()
	{
		return _baseAtkRange;
	}

	public int getBaseShldRate()
	{
		return _baseShldRate;
	}

	public int getBaseCritRate()
	{
		return _baseCritRate;
	}

	public int getBaseMCritRate()
	{
		return _baseMCritRate;
	}

	public int getBaseWalkSpd()
	{
		return _baseWalkSpd;
	}

	public int getBaseRunSpd()
	{
		return _baseRunSpd;
	}

	public int getBaseMpConsumeRate()
	{
		return _baseMpConsumeRate;
	}

	public int getBaseHpConsumeRate()
	{
		return _baseHpConsumeRate;
	}

	public int getCollisionRadius()
	{
		return _collisionRadius;
	}

	public int getCollisionHeight()
	{
		return _collisionHeight;
	}
}