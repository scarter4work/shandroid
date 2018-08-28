package voidious.gun;

import ags.utils.dataStructures.Entry;
import robocode.util.Utils;
import voidious.gun.formulas.MainGunFormula;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

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

class MainGun extends DuelGun<TimestampedFiringAngle>
{
	private static final String VIEW_NAME = "Main";
	private static final int K_SIZE = 100;
	private static final int K_DIVISOR = 10;

	MainGun(GunDataManager gunDataManager, BattleField battleField)
	{
		super(gunDataManager, battleField, null);
	}

	@Override
	public String getLabel()
	{
		return "Main Gun";
	}

	@Override
	protected double aimInternal(Wave w)
	{
		GunEnemy gunData = this.getGdManager().getEnemyData(w.getBotName());
		KnnView<TimestampedFiringAngle> view = gunData.getViews().get(VIEW_NAME);

		if (view.size() == 0)
		{
			return w.getAbsBearing();
		}

		List<Entry<TimestampedFiringAngle>> nearestNeighbors = view.nearestNeighbors(w, true);
		int numScans = nearestNeighbors.size();
		Double[] firingAngles = new Double[numScans];

		for (int x = 0; x < numScans; x++)
		{
			Point2D.Double dispVector = nearestNeighbors.get(x).getValue().getDisplacementVector();
			Point2D.Double projectedLocation = w.projectLocationFromDisplacementVector(dispVector);

			if (!this.getBattleField().getRectangle().contains(projectedLocation))
			{
				firingAngles[x] = null;
			}
			else
			{
				firingAngles[x] = Utils.normalRelativeAngle(w.firingAngleFromTargetLocation(projectedLocation)
						- w.getAbsBearing());
			}
		}

		Double bestAngle = null;
		double bestDensity = Double.NEGATIVE_INFINITY;
		double bandwidth = 2 * DiaUtils.botWidthAimAngle(w.getSourceLocation().distance(w.getTargetLocation()));

		for (int x = 0; x < numScans; x++)
		{
			if (firingAngles[x] == null)
			{
				continue;
			}

			double xFiringAngle = firingAngles[x];
			double xDensity = 0;

			for (int y = 0; y < numScans; y++)
			{
				if (x == y || firingAngles[y] == null)
				{
					continue;
				}

				double yFiringAngle = firingAngles[y];
				double ux = (xFiringAngle - yFiringAngle) / bandwidth;
				xDensity += Math.exp(-0.5 * ux * ux);
			}

			if (xDensity > bestDensity)
			{
				bestAngle = xFiringAngle;
				bestDensity = xDensity;
			}
		}

		if (bestAngle == null)
		{
			return w.getAbsBearing();
		}

		return Utils.normalAbsoluteAngle(w.getAbsBearing() + bestAngle);
	}

	@Override
	public List<KnnView<TimestampedFiringAngle>> newDataViews()
	{
		List<KnnView<TimestampedFiringAngle>> views = new ArrayList<>();
		views.add(new KnnView<TimestampedFiringAngle>(new MainGunFormula(this.getGdManager().getEnemiesTotal())).setK(K_SIZE)
				.setKDivisor(K_DIVISOR).visitsOn().virtualWavesOn().meleeOn().setName(VIEW_NAME));
		return views;
	}
}