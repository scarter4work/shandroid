package voidious.gun;

import ags.utils.dataStructures.Entry;
import robocode.util.Utils;
import voidious.gun.formulas.MeleeFormula;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2011-2012 - Voidious
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

class MeleeGun
{
	private static final String VIEW_NAME = "Melee";
	private static final int MAX_SCANS = 100;

	private GunDataManager gunDataManager;
	private BattleField battleField;

	MeleeGun(GunDataManager gunDataManager, BattleField battleField)
	{
		this.gunDataManager = gunDataManager;
		this.battleField = battleField;
	}

	double aimAtEveryone(Point2D.Double myNextLocation, long currentTime, int enemiesAlive, double bulletPower,
			GunEnemy closestBot)
	{
		List<MeleeFiringAngle> firingAngles = new ArrayList<>();

		int kSize = this.getCommonKsize(enemiesAlive);
		for (GunEnemy gunData : this.gunDataManager.getAllEnemyData())
		{
			if (gunData.isAlive() && gunData.getViews().get(VIEW_NAME).size() >= 10 && gunData.getLastWaveFired() != null)
			{
				List<MeleeFiringAngle> enemyAngles = new ArrayList<>();
				Wave aimWave = gunData.getLastWaveFired();
				aimWave.setBulletPower(bulletPower);
				KnnView<TimestampedFiringAngle> view = gunData.getViews().get(VIEW_NAME);
				List<Entry<TimestampedFiringAngle>> nearestNeighbors = view.nearestNeighbors(aimWave, true, kSize);

				int numScans = nearestNeighbors.size();
				double totalScanWeight = 0;
				for (int x = 0; x < numScans; x++)
				{
					Entry<TimestampedFiringAngle> entry = nearestNeighbors.get(x);
					double scanWeight = 1 / Math.sqrt(entry.getDistance());
					totalScanWeight += scanWeight;
					Point2D.Double vector = entry.getValue().getDisplacementVector();
					MeleeFiringAngle firingAngle = this.getFiringAngle(myNextLocation, currentTime, vector, scanWeight, aimWave);
					if (firingAngle != null)
					{
						enemyAngles.add(firingAngle);
					}
				}
				for (MeleeFiringAngle enemyAngle : enemyAngles)
				{
					enemyAngle.setScanWeight(enemyAngle.getScanWeight() / totalScanWeight);
				}
				firingAngles.addAll(enemyAngles);
			}
		}

		Double bestAngle = null;
		double bestDensity = Double.NEGATIVE_INFINITY;

		for (MeleeFiringAngle xFiringAngle : firingAngles)
		{
			double xDensity = 0;
			for (MeleeFiringAngle yFiringAngle : firingAngles)
			{
				double ux = Utils.normalRelativeAngle(xFiringAngle.getAngle() - yFiringAngle.getAngle())
						/ yFiringAngle.getBandwidth();
				xDensity += yFiringAngle.getScanWeight() * Math.exp(-0.5 * ux * ux) / yFiringAngle.getDistance();
			}

			if (xDensity > bestDensity)
			{
				bestAngle = xFiringAngle.getAngle();
				bestDensity = xDensity;
			}
		}

		if (firingAngles.isEmpty() || bestAngle == null)
		{
			return closestBot.getLastWaveFired().getAbsBearing();
		}

		return Utils.normalAbsoluteAngle(bestAngle);
	}

	private int getCommonKsize(int enemiesAlive)
	{
		int kSize = MAX_SCANS / enemiesAlive;
		for (GunEnemy gunData : this.gunDataManager.getAllEnemyData())
		{
			if (gunData.isAlive() && gunData.getViews().get(VIEW_NAME).size() >= 10 && gunData.getLastWaveFired() != null)
			{
				kSize = Math.min(kSize, gunData.getViews().get(VIEW_NAME).size() / 10);
			}
		}
		return kSize;
	}

	private MeleeFiringAngle getFiringAngle(Point2D.Double myNextLocation, long currentTime, Point2D.Double dispVector,
			double scanWeight, Wave aimWave)
	{
		Point2D.Double projectedLocation = aimWave.projectLocationBlind(myNextLocation, dispVector, currentTime);
		if (this.battleField.getRectangle().contains(projectedLocation))
		{
			double distance = myNextLocation.distance(projectedLocation);
			return new MeleeFiringAngle(DiaUtils.absoluteBearing(myNextLocation, projectedLocation), distance,
					DiaUtils.botWidthAimAngle(distance), scanWeight, aimWave);
		}
		return null;
	}

	List<KnnView<TimestampedFiringAngle>> newDataViews()
	{
		List<KnnView<TimestampedFiringAngle>> views = new ArrayList<>();
		MeleeFormula distanceMelee = new MeleeFormula();
		KnnView<TimestampedFiringAngle> meleeView = new KnnView<TimestampedFiringAngle>(distanceMelee).visitsOn()
				.virtualWavesOn().meleeOn().setName(VIEW_NAME);
		views.add(meleeView);
		return views;
	}
}