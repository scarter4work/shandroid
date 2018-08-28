package voidious.gun;

import robocode.util.Utils;
import voidious.utils.DiaUtils;
import voidious.utils.Wave;

import java.util.ArrayList;
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

class VirtualGunsManager<T> implements GunDataListener
{
	private static final double TYPICAL_ANGULAR_BOT_WIDTH = 0.1;
	private static final double TYPICAL_ESCAPE_RANGE = 0.9;

	private List<DuelGun<T>> guns;
	private Map<DuelGun<T>, Map<String, GunStats>> gunRatings;

	VirtualGunsManager()
	{
		this.guns = new ArrayList<>();
		this.gunRatings = new HashMap<>();
	}

	void addGun(DuelGun<T> gun)
	{
		this.guns.add(gun);
		this.gunRatings.put(gun, new HashMap<String, GunStats>());
	}

	List<DuelGun<T>> getGuns()
	{
		return this.guns;
	}

	public boolean contains(DuelGun<T> gun)
	{
		return this.guns.contains(gun);
	}

	private double getRating(DuelGun<T> gun, String botName)
	{
		if (this.guns.contains(gun) && this.gunRatings.get(gun).containsKey(botName))
		{
			return this.gunRatings.get(gun).get(botName).gunRating();
		}
		return 0;
	}

	double getFormattedRating(DuelGun<T> gun, String botName)
	{
		return DiaUtils.round(this.getRating(gun, botName) * 100, 2);
	}

	public int getShotsFired(DuelGun<T> gun, String botName)
	{
		if (this.guns.contains(gun) && this.gunRatings.get(gun).containsKey(botName))
		{
			return this.gunRatings.get(gun).get(botName).shotsFired;
		}
		return 0;
	}

	private void fireVirtualBullets(Wave w)
	{
		for (DuelGun<T> gun : this.guns)
		{
			GunStats stats;
			if (this.gunRatings.get(gun).containsKey(w.getBotName()))
			{
				stats = this.gunRatings.get(gun).get(w.getBotName());
			}
			else
			{
				stats = new GunStats();
				this.gunRatings.get(gun).put(w.getBotName(), stats);
			}

			double firingAngle = gun.aim(w);
			stats.virtualBullets.put(w, new VirtualBullet(firingAngle));
		}
	}

	private void registerWaveBreak(Wave w, double hitAngle, double tolerance)
	{
		for (DuelGun<T> gun : this.guns)
		{
			GunStats stats = this.gunRatings.get(gun).get(w.getBotName());
			VirtualBullet vb = stats.virtualBullets.get(w);

			double angularBotWidth = tolerance * 2;
			double hitWeight = (TYPICAL_ANGULAR_BOT_WIDTH / angularBotWidth) * (w.escapeAngleRange() / TYPICAL_ESCAPE_RANGE);
			double ux = Math.abs(Utils.normalRelativeAngle(vb.firingAngle - hitAngle)) / tolerance;
			stats.shotsHit += hitWeight * Math.pow(1.6, -ux);

			stats.shotsFired++;
			stats.virtualBullets.remove(w);
		}
	}

	public void initRound()
	{
		for (DuelGun<T> gun : this.guns)
		{
			for (GunStats stats : this.gunRatings.get(gun).values())
			{
				stats.virtualBullets.clear();
			}
			gun.clearCache();
		}
	}

	DuelGun<T> bestGun(String botName)
	{
		DuelGun<T> bestGun = null;
		double bestRating = 0;

		for (DuelGun<T> gun : this.guns)
		{
			double rating = 0;
			if (this.gunRatings.get(gun).containsKey(botName))
			{
				rating = this.gunRatings.get(gun).get(botName).gunRating();
			}

			if (bestGun == null || rating > bestRating)
			{
				bestGun = gun;
				bestRating = rating;
			}
		}

		return bestGun;
	}

	void printGunRatings(String botName)
	{
		System.out.println("Virtual Gun ratings for " + botName + ":");
		for (DuelGun<T> gun : this.guns)
		{
			if (this.gunRatings.get(gun).containsKey(botName))
			{
				double rating = this.gunRatings.get(gun).get(botName).gunRating();
				System.out.println("  " + gun.getLabel() + ": " + DiaUtils.round(rating * 100, 2));
			}
			else
			{
				System.out.println("WARNING (gun): Never logged any Virtual Guns info for " + gun.getLabel());
			}
		}
	}

	@Override
	public void on1v1FiringWaveBreak(Wave w, double hitAngle, double tolerance)
	{
		this.registerWaveBreak(w, hitAngle, tolerance);
	}

	@Override
	public void onMarkFiringWave(Wave w)
	{
		this.fireVirtualBullets(w);
	}

	private static class GunStats
	{
		int shotsFired;
		double shotsHit;
		Map<Wave, VirtualBullet> virtualBullets;

		GunStats()
		{
			this.shotsFired = 0;
			this.shotsHit = 0;
			this.virtualBullets = new HashMap<>();
		}

		double gunRating()
		{
			return (this.shotsFired == 0) ? 0 : (this.shotsHit / this.shotsFired);
		}
	}

	private static class VirtualBullet
	{
		final double firingAngle;

		VirtualBullet(double firingAngle)
		{
			this.firingAngle = firingAngle;
		}
	}
}
