package voidious.gun;

import java.util.ArrayList;
import java.util.List;

import robocode.util.Utils;
import voidious.gun.formulas.TripHammerFormula;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.KnnView;
import voidious.utils.TimestampedFiringAngle;
import voidious.utils.Wave;
import ags.utils.dataStructures.Entry;

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

public class TripHammerKnnGun extends DuelGun<TimestampedFiringAngle>
{
	private static final double[] INITIAL_WEIGHTS = new double[] { 0.94, 10.0, 1.73, 3.7, 3.31, 2.13, 5.51, 1.26, 1.57, 5.51 };
	private static final double[] FINAL_WEIGHTS = new double[] { 4.25, 5.43, 0.16, 4.25, 8.74, 3.39, 4.41, 8.03, 7.24, 4.41 };
	private static final int[] FINAL_TIMES = new int[] { 28920, 23040, 23100, 1740, 16680, 5580, 0, 11280, 21420, 1920 };

	private static final String VIEW_NAME = "TripHammerKNN";
	private static final int FIRING_ANGLES = 59;
	private static final int MAX_K_SIZE = 225;
	private static final int K_DIVISOR = 9;

	private int numFiringAngles;
	private int maxK;
	private int kDivisor;

	public TripHammerKnnGun(GunDataManager gunDataManager, BattleField battleField)
	{
		super(gunDataManager, battleField, new TripHammerFormula());
		this.numFiringAngles = FIRING_ANGLES;
		this.maxK = MAX_K_SIZE;
		this.kDivisor = K_DIVISOR;
	}

	@Override
	public String getLabel()
	{
		return "Trip Hammer Knn Gun";
	}

	@Override
	protected double aimInternal(Wave w)
	{
		GunEnemy enemyData = this.getGdManager().getEnemyData(w.getBotName());
		KnnView<TimestampedFiringAngle> view = enemyData.getViews().get(VIEW_NAME);
		int viewSize = view.size();
		if (viewSize == 0)
		{
			return w.getAbsBearing();
		}

		view.setWeights(this.getWeights(view.size()));
		List<Entry<TimestampedFiringAngle>> nearestNeighbors = view.nearestNeighbors(w, true);
		int numScans = nearestNeighbors.size();
		Double[] firingAngles = new Double[numScans];
		double[] weights = new double[numScans];
		for (int x = 0; x < numScans; x++)
		{
			double guessFactor = nearestNeighbors.get(x).getValue().getGuessFactor();
			firingAngles[x] = Double.valueOf(Utils.normalRelativeAngle((guessFactor * w.getOrbitDirection() * w
					.preciseEscapeAngle(guessFactor >= 0))));
			weights[x] = 1 / Math.sqrt(nearestNeighbors.get(x).getDistance());
		}

		double bandwidth = 2 * DiaUtils.botWidthAimAngle(w.getSourceLocation().distance(w.getTargetLocation()));
		Double bestAngle = null;
		double bestDensity = Double.NEGATIVE_INFINITY;
		double[] realAngles = DiaUtils.generateFiringAngles(this.numFiringAngles, w.getMaxEscapeAngle());

		for (int x = 0; x < this.numFiringAngles; x++)
		{
			double density = 0;
			for (int y = 0; y < numScans; y++)
			{
				if (firingAngles[y] == null)
				{
					continue;
				}
				double ux = (realAngles[x] - firingAngles[y].doubleValue()) / bandwidth;
				if (Math.abs(ux) < 1)
				{
					density += (1 - DiaUtils.cube(Math.abs(ux))) * weights[y];
				}
			}

			if (density > bestDensity)
			{
				bestAngle = Double.valueOf(realAngles[x]);
				bestDensity = density;
			}
		}

		if (bestAngle == null)
		{
			return w.getAbsBearing();
		}

		return Utils.normalAbsoluteAngle(w.getAbsBearing() + bestAngle.doubleValue());
	}

	private double[] getWeights(int viewSize)
	{
		double[] newWeights = new double[INITIAL_WEIGHTS.length];
		for (int x = 0; x < newWeights.length; x++)
		{
			newWeights[x] = INITIAL_WEIGHTS[x]
					+ (FINAL_TIMES[x] == 0 ? 1 : Math.min(1, ((double) (viewSize - 1)) / FINAL_TIMES[x]))
					* (FINAL_WEIGHTS[x] - INITIAL_WEIGHTS[x]);
		}
		return newWeights;
	}

	@Override
	public List<KnnView<TimestampedFiringAngle>> newDataViews()
	{
		List<KnnView<TimestampedFiringAngle>> views = new ArrayList<>();
		KnnView<TimestampedFiringAngle> view = new KnnView<TimestampedFiringAngle>(this.getFormula()).setK(this.maxK)
				.setKDivisor(this.kDivisor).visitsOn().virtualWavesOn().setName(VIEW_NAME);
		views.add(view);
		return views;
	}
}
