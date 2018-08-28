package voidious.gun;

import voidious.utils.BattleField;
import voidious.utils.DistanceFormula;
import voidious.utils.KnnView;
import voidious.utils.Wave;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2009-2012 - Voidious
 * 
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not claim
 * that you wrote the original software.
 * 
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 
 * 3. This notice may not be removed or altered from any source distribution.
 */

abstract class DuelGun<T>
{
	private Map<Wave, Double> firingAngles;
	private GunDataManager gdManager;
	private BattleField battleField;
	private DistanceFormula formula;

	DuelGun(GunDataManager gdManager, BattleField battleField, DistanceFormula formula)
	{
		this.firingAngles = new HashMap<>();
		this.battleField = battleField;
		this.gdManager = gdManager;
		this.formula = formula;
	}

	void clearCache()
	{
		this.firingAngles.clear();
	}

	double aim(Wave w)
	{
		if (!this.firingAngles.containsKey(w))
		{
			this.firingAngles.put(w, this.aimInternal(w));
		}
		return this.firingAngles.get(w);
	}

	abstract public String getLabel();

	abstract protected double aimInternal(Wave w);

	abstract public List<KnnView<T>> newDataViews();

	/**
	 * @return the firingAngles
	 */
	public Map<Wave, Double> getFiringAngles()
	{
		return this.firingAngles;
	}

	/**
	 * @param firingAngles
	 *            the firingAngles to set
	 */
	public void setFiringAngles(Map<Wave, Double> firingAngles)
	{
		this.firingAngles = firingAngles;
	}

	/**
	 * @return the gdManager
	 */
	GunDataManager getGdManager()
	{
		return this.gdManager;
	}

	/**
	 * @param gdManager
	 *            the gdManager to set
	 */
	public void setGdManager(GunDataManager gdManager)
	{
		this.gdManager = gdManager;
	}

	/**
	 * @return the battleField
	 */
	public BattleField getBattleField()
	{
		return this.battleField;
	}

	/**
	 * @param battleField
	 *            the battleField to set
	 */
	public void setBattleField(BattleField battleField)
	{
		this.battleField = battleField;
	}

	/**
	 * @return the formula
	 */
	DistanceFormula getFormula()
	{
		return this.formula;
	}

	/**
	 * @param formula
	 *            the formula to set
	 */
	public void setFormula(DistanceFormula formula)
	{
		this.formula = formula;
	}
}