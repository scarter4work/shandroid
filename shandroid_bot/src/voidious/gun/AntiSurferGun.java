package voidious.gun;

import ags.utils.dataStructures.Entry;
import robocode.util.Utils;
import voidious.gun.formulas.AntiSurferFormula;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
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

class AntiSurferGun extends DuelGun<TimestampedFiringAngle>
{
	private static final int FIRING_ANGLES = 59;

	private List<String> viewNames;
	private boolean is1v1Battle;

	AntiSurferGun(GunDataManager gunDataManager, BattleField battleField)
	{
		super(gunDataManager, battleField, new AntiSurferFormula());
		this.viewNames = new ArrayList<>();
		this.is1v1Battle = (gunDataManager.getEnemiesTotal() == 1);
	}

	@Override
	public String getLabel()
	{
		return "Anti-Surfer Gun";
	}

	@Override
	protected double aimInternal(Wave w)
	{
		GunEnemy gunData = this.getGdManager().getEnemyData(w.getBotName());
		List<Entry<TimestampedFiringAngle>> nearestNeighbors = null;
		double[] neighborWeights = null;
		for (String viewName : this.viewNames)
		{
			KnnView<TimestampedFiringAngle> view = gunData.getViews().get(viewName);
			if (view.size() < view.kDivisor)
			{
				continue;
			}

			List<Entry<TimestampedFiringAngle>> thisNeighbors = view.nearestNeighbors(w, true);
			double[] thisWeights = new double[thisNeighbors.size()];
			Arrays.fill(thisWeights, view.weight);

			if (nearestNeighbors == null)
			{
				nearestNeighbors = thisNeighbors;
				neighborWeights = thisWeights;
			}
			else
			{
				nearestNeighbors.addAll(thisNeighbors);
				int newIndex = neighborWeights.length;
				neighborWeights = Arrays.copyOf(neighborWeights, nearestNeighbors.size());
				System.arraycopy(thisWeights, 0, neighborWeights, newIndex, neighborWeights.length - newIndex);
			}
		}

		if (nearestNeighbors == null || nearestNeighbors.size() == 0)
		{
			return w.getAbsBearing();
		}

		int numScans = nearestNeighbors.size();
		Double[] firingAngles = new Double[numScans];
		for (int x = 0; x < numScans; x++)
		{
			if (this.is1v1Battle)
			{
				double guessFactor = nearestNeighbors.get(x).getValue().getGuessFactor();
				firingAngles[x] = Utils.normalRelativeAngle((guessFactor * w.getOrbitDirection() * w
						.preciseEscapeAngle(guessFactor >= 0)));
			}
			else
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
		}

		Double bestAngle = null;
		double bestDensity = Double.NEGATIVE_INFINITY;
		double bandwidth = DiaUtils.botWidthAimAngle(w.getSourceLocation().distance(w.getTargetLocation())) * 2;
		double[] realAngles = DiaUtils.generateFiringAngles(FIRING_ANGLES, w.getMaxEscapeAngle());
		for (int x = 0; x < FIRING_ANGLES; x++)
		{
			double xFiringAngle = realAngles[x];

			double xDensity = 0;
			for (int y = 0; y < numScans; y++)
			{
				if (firingAngles[y] == null)
				{
					continue;
				}

				double yFiringAngle = firingAngles[y];
				double ux = (xFiringAngle - yFiringAngle) / bandwidth;
				xDensity += Math.exp(-0.5 * ux * ux) * neighborWeights[y];
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
		KnnView<TimestampedFiringAngle> asView = new KnnView<TimestampedFiringAngle>(this.getFormula()).setK(3)
				.setMaxDataPoints(125).setKDivisor(10).visitsOn().virtualWavesOn().setName("asView1");
		KnnView<TimestampedFiringAngle> asView2 = new KnnView<TimestampedFiringAngle>(this.getFormula()).setK(3)
				.setMaxDataPoints(400).setKDivisor(10).visitsOn().virtualWavesOn().setName("asView2");
		KnnView<TimestampedFiringAngle> asView3 = new KnnView<TimestampedFiringAngle>(this.getFormula()).setK(3)
				.setMaxDataPoints(1500).setKDivisor(10).visitsOn().virtualWavesOn().setName("asView3");
		KnnView<TimestampedFiringAngle> asView4 = new KnnView<TimestampedFiringAngle>(this.getFormula()).setK(3)
				.setMaxDataPoints(4000).setKDivisor(10).visitsOn().virtualWavesOn().setName("asView4");
		List<KnnView<TimestampedFiringAngle>> views = new ArrayList<>();
		views.add(asView);
		views.add(asView2);
		views.add(asView3);
		views.add(asView4);

		views.stream().filter(view -> !this.viewNames.contains(view.name)).forEach(view -> this.viewNames.add(view.name));

		return views;
	}
}
