package voidious.gun;

import robocode.Bullet;
import voidious.enums.WavePosition;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2009-2012 - Voidious
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 *    1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software.
 *
 *    2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 *
 *    3. This notice may not be removed or altered from any source
 *    distribution.
 */

/**
 * Targeting data about an enemy.
 */
class GunEnemy extends Enemy<TimestampedFiringAngle>
{
	private static final double NON_ZERO_VELOCITY_THRESHOLD = 0.1;

	private double previousVelocity;
	private double lastNonZeroVelocity;
	private long timeSinceDirectionChange;
	private long timeSinceVelocityChange;
	private long timeMovingAtMe;
	private Wave lastWaveFired;
	private List<Point2D.Double> hitLocations;
	private int waveBreaks;

	GunEnemy(String botName, double distance, double energy, Point2D.Double location, int round, long time,
			double heading, double velocity, double absBearing, BattleField battleField, MovementPredictor predictor)
	{
		super(botName, location, distance, energy, heading, velocity, absBearing, round, time, battleField, predictor,
				new WaveManager());

		this.waveBreaks = 0;
		this.lastNonZeroVelocity = velocity;
		this.previousVelocity = 0;
		this.timeSinceDirectionChange = 0;
		this.timeSinceVelocityChange = 0;
		this.timeMovingAtMe = 0;
		this.lastWaveFired = null;
		this.hitLocations = new ArrayList<>();
	}

	@Override
	public void initRound()
	{
		super.initRound();
		this.lastNonZeroVelocity = 0;
		this.previousVelocity = 0;
		this.timeSinceDirectionChange = 0;
		this.timeSinceVelocityChange = 0;
		this.hitLocations.clear();
	}

	void execute(final long currentTime, long lastBulletFiredTime, double currentGunHeat,
				 Point2D.Double myLocation, final boolean is1v1, final int enemiesTotal, final List<GunDataListener> gunDataListeners)
	{
		this.getWaveManager()
				.checkCurrentWaves(
						currentTime,
						this.newUpdateWaveListener(myLocation, lastBulletFiredTime, currentGunHeat
						));
		this.getWaveManager().checkActiveWaves(currentTime, this.getLastScanState(),
				this.newWaveBreakListener(currentTime, enemiesTotal, is1v1, gunDataListeners));
	}

	private void updateWave(Wave w, Point2D.Double myLocation, long lastBulletFiredTime, double gunHeat)
	{
		if (!w.isAltWave())
		{
			w.setGunHeat(gunHeat);
			w.setLastBulletFiredTime(lastBulletFiredTime);

			if (this.getLastScanState().getTime() == w.getFireTime())
			{
				w.setSourceLocation(myLocation);
				w.setTargetLocation(this.getLastScanState().getLocation());
				w.setAbsBearing(DiaUtils.absoluteBearing(myLocation, w.getTargetLocation()));
			}
		}
	}

	private void processWaveBreak(Wave w, List<RobotState> waveBreakStates, long currentTime, int enemiesTotal,
			List<GunDataListener> gunDataListeners)
	{
		if (waveBreakStates == null || waveBreakStates.isEmpty())
		{
			// This should be impossible, but it's happening due to a bug in
			// either Robocode or my JVM. It's bad data, so just ignore it.
			return;
		}
		double guessFactor = 0;
		Intersection preciseIntersection = null;
		if (enemiesTotal == 1)
		{
			preciseIntersection = w.preciseIntersection(waveBreakStates);
			guessFactor = w.guessFactorPrecise(preciseIntersection.getAngle());
		}

		RobotState waveBreakState = this.getMedianWaveBreakState(waveBreakStates);
		Point2D.Double dispVector = w.displacementVector(waveBreakState);
		this.logWave(w, dispVector, guessFactor, currentTime, IS_VISIT);
		this.waveBreaks++;

		if (w.getEnemiesAlive() == 1 && w.isFiringWave() && !w.isAltWave())
		{
			if (preciseIntersection == null)
			{
				preciseIntersection = w.preciseIntersection(waveBreakStates);
			}
			double hitAngle = preciseIntersection.getAngle();
			double tolerance = preciseIntersection.getBandwidth();
			for (GunDataListener listener : gunDataListeners)
			{
				listener.on1v1FiringWaveBreak(w, hitAngle, tolerance);
			}
		}
	}

	Wave processBulletHit(Bullet bullet, long currentTime, boolean is1v1Battle, boolean logHitWave)
	{
		Point2D.Double bulletLocation = this.bulletLocation(bullet);
		Wave hitWave = this.getWaveManager().findClosestWave(bulletLocation, currentTime, Wave.FIRING_WAVE, this.getBotName(),
				bullet.getPower());

		if (hitWave != null)
		{
			if (logHitWave)
			{
				Point2D.Double hitVector = hitWave.displacementVector(this.getLastScanState().getLocation(), currentTime);
				double hitFactor = 0;
				if (is1v1Battle)
				{
					hitFactor = hitWave.guessFactorPrecise(this.getLastScanState().getLocation());
				}
				this.logWave(hitWave, hitVector, hitFactor, currentTime, IS_BULLET_HIT);
			}
			return hitWave;
		}
		return null;
	}

	void logBulletHitLocation(Bullet bullet)
	{
		this.hitLocations.add(this.bulletLocation(bullet));
	}

	private Double bulletLocation(Bullet bullet)
	{
		return new Point2D.Double(bullet.getX(), bullet.getY());
	}

	private void logWave(Wave w, Point2D.Double dispVector, double guessFactor, long time, boolean isVisit)
	{
		this.getViews().values().stream().filter(view -> (isVisit && view.logVisits || !isVisit && view.logBulletHits) && (view.logVirtual || w.isFiringWave())
				&& (view.logMelee || w.getEnemiesAlive() <= 1)).forEach(view -> view.logWave(w, new TimestampedFiringAngle(w.getFireRound(), time, guessFactor, dispVector)));
	}

	Wave newGunWave(Point2D.Double sourceLocation, Point2D.Double targetLocation, int fireRound, long fireTime,
			long lastBulletFiredTime, double bulletPower, double myEnergy, double gunHeat, int enemiesAlive, double accel,
			double dl8t, double dl20t, double dl40t, boolean altWave)
	{
		Wave newWave = new Wave(this.getBotName(), sourceLocation, targetLocation, fireRound, fireTime, bulletPower, this
				.getLastScanState().getHeading(), this.getLastScanState().getVelocity(),
				DiaUtils.nonZeroSign(this.lastNonZeroVelocity), this.getBattleField(), this.getPredictor());
		newWave.setTargetAccel(accel);
		newWave.setTargetDistance(sourceLocation.distance(targetLocation));
		newWave.setTargetDchangeTime(this.timeSinceDirectionChange);
		newWave.setTargetVchangeTime(this.timeSinceVelocityChange);
		newWave.setTargetDl8t(dl8t);
		newWave.setTargetDl20t(dl20t);
		newWave.setTargetDl40t(dl40t);
		newWave.setTargetEnergy(this.getEnergy());
		newWave.setSourceEnergy(myEnergy);
		newWave.setGunHeat(gunHeat);
		newWave.setEnemiesAlive(enemiesAlive);
		newWave.setLastBulletFiredTime(lastBulletFiredTime);
		newWave.setAltWave(altWave);
		newWave.setWallDistances(enemiesAlive <= 1 ? Wave.WallDistanceStyle.ORBITAL : Wave.WallDistanceStyle.DIRECT);

		if (myEnergy > 0)
		{
			this.getWaveManager().addWave(newWave);
		}
		return newWave;
	}

	void markFiringWaves(long currentTime, boolean is1v1, List<GunDataListener> gunDataListeners)
	{
		this.getWaveManager().checkCurrentWaves(currentTime, this.newMarkFiringWavesListener(is1v1, gunDataListeners));
	}

	void updateTimers(double velocity)
	{
		if (Math.abs(velocity - this.lastNonZeroVelocity) > 0.5)
		{
			this.timeSinceVelocityChange = 0;
		}
		if (Math.abs(velocity) > NON_ZERO_VELOCITY_THRESHOLD)
		{
			if (Math.signum(velocity) != Math.signum(this.lastNonZeroVelocity))
			{
				this.timeSinceDirectionChange = 0;
			}
			this.lastNonZeroVelocity = velocity;
		}
	}

	long getLastWaveFireTime()
	{
		return this.getWaveManager().getLastFireTime();
	}

	Wave interpolateGunWave(long fireTime, double myHeading, double myVelocity, RobotState enemyState)
	{
		Wave w = this.getWaveManager().interpolateWaveByFireTime(fireTime, myHeading, myVelocity,
				this.getStateLog(), this.getBattleField(), this.getPredictor());
		if (w.checkWavePosition(enemyState) == WavePosition.MIDAIR)
		{
			this.getWaveManager().addWave(w);
			return w;
		}
		return null;
	}

	private RobotState getMedianWaveBreakState(List<RobotState> waveBreakStates)
	{
		return waveBreakStates.get(waveBreakStates.size() / 2);
	}

	double advancingVelocity()
	{
		return -Math.cos(this.getLastScanState().getHeading() - this.getAbsBearing()) * this.getLastScanState().getVelocity();
	}

	boolean isRammer()
	{
		return (((double) this.timeMovingAtMe) / this.getTimeAliveTogether()) > 0.5;
	}

	int getWaveBreaks()
	{
		return this.waveBreaks;
	}

	private CurrentWaveListener newUpdateWaveListener(final Point2D.Double myLocation,
													  final long lastBulletFiredTime, final double currentGunHeat)
	{
		return w -> GunEnemy.this.updateWave(w, myLocation, lastBulletFiredTime, currentGunHeat);
	}

	private WaveBreakListener newWaveBreakListener(final long currentTime, final int enemiesTotal, final boolean is1v1,
			final List<GunDataListener> gunDataListeners)
	{
		return (w, waveBreakStates) -> GunEnemy.this.processWaveBreak(w, waveBreakStates, currentTime, enemiesTotal, gunDataListeners);
	}

	private CurrentWaveListener newMarkFiringWavesListener(final boolean is1v1, final List<GunDataListener> gunDataListeners)
	{
		return w -> {
            if (!w.isAltWave())
            {
                w.setFiringWave(true);
                if (is1v1)
                {
                    for (GunDataListener listener : gunDataListeners)
                    {
                        listener.onMarkFiringWave(w);
                    }
                }
            }
        };
	}

	/**
	 * @return the previousVelocity
	 */
	public double getPreviousVelocity()
	{
		return this.previousVelocity;
	}

	/**
	 * @param previousVelocity
	 *            the previousVelocity to set
	 */
	public void setPreviousVelocity(double previousVelocity)
	{
		this.previousVelocity = previousVelocity;
	}

	/**
	 * @return the lastNonZeroVelocity
	 */
	public double getLastNonZeroVelocity()
	{
		return this.lastNonZeroVelocity;
	}

	/**
	 * @param lastNonZeroVelocity
	 *            the lastNonZeroVelocity to set
	 */
	public void setLastNonZeroVelocity(double lastNonZeroVelocity)
	{
		this.lastNonZeroVelocity = lastNonZeroVelocity;
	}

	/**
	 * @return the timeSinceDirectionChange
	 */
	public long getTimeSinceDirectionChange()
	{
		return this.timeSinceDirectionChange;
	}

	/**
	 * @param timeSinceDirectionChange
	 *            the timeSinceDirectionChange to set
	 */
	public void setTimeSinceDirectionChange(long timeSinceDirectionChange)
	{
		this.timeSinceDirectionChange = timeSinceDirectionChange;
	}

	/**
	 * @return the timeSinceVelocityChange
	 */
	public long getTimeSinceVelocityChange()
	{
		return this.timeSinceVelocityChange;
	}

	/**
	 * @param timeSinceVelocityChange
	 *            the timeSinceVelocityChange to set
	 */
	public void setTimeSinceVelocityChange(long timeSinceVelocityChange)
	{
		this.timeSinceVelocityChange = timeSinceVelocityChange;
	}

	/**
	 * @return the timeMovingAtMe
	 */
	public long getTimeMovingAtMe()
	{
		return this.timeMovingAtMe;
	}

	/**
	 * @param timeMovingAtMe
	 *            the timeMovingAtMe to set
	 */
	public void setTimeMovingAtMe(long timeMovingAtMe)
	{
		this.timeMovingAtMe = timeMovingAtMe;
	}

	/**
	 * @return the lastWaveFired
	 */
	public Wave getLastWaveFired()
	{
		return this.lastWaveFired;
	}

	/**
	 * @param lastWaveFired
	 *            the lastWaveFired to set
	 */
	public void setLastWaveFired(Wave lastWaveFired)
	{
		this.lastWaveFired = lastWaveFired;
	}

	/**
	 * @return the hitLocations
	 */
	public List<Point2D.Double> getHitLocations()
	{
		return this.hitLocations;
	}

	/**
	 * @param hitLocations
	 *            the hitLocations to set
	 */
	public void setHitLocations(List<Point2D.Double> hitLocations)
	{
		this.hitLocations = hitLocations;
	}

	/**
	 * @param waveBreaks
	 *            the waveBreaks to set
	 */
	public void setWaveBreaks(int waveBreaks)
	{
		this.waveBreaks = waveBreaks;
	}
}