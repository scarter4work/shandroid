package voidious.move;

import ags.utils.dataStructures.Entry;
import ags.utils.dataStructures.WeightedSqrEuclid;
import robocode.Bullet;
import robocode.Rules;
import voidious.enums.WavePosition;
import voidious.gun.FiredBullet;
import voidious.gun.WaveBreakListener;
import voidious.move.formulas.FlattenerFormula;
import voidious.move.formulas.NormalFormula;
import voidious.move.formulas.SimpleFormula;
import voidious.utils.*;
import voidious.utils.geom.Circle;
import voidious.utils.geom.LineSeg;

import java.awt.geom.Point2D;
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
 * General info about the enemy. We just keep one of these around for each enemy
 * bot the movement is aware of.
 */
class MoveEnemy extends Enemy<TimestampedGuessFactor>
{
	private static final double RECENT_SCANS_HIT_THRESHOLD = 2.5;
	private static final double LIGHT_FLATTENER_HIT_THRESHOLD = 3.0;
	private static final double FLATTENER_HIT_THRESHOLD = 5.9;
	private static final double DECAY_RATE = 1.8;
	private static final double BOT_WIDTH = 36;
	private static final double[] BULLET_POWER_WEIGHTS = new double[] { 3, 5, 1 };

	private double damageTaken;
	private double lastBulletPower;
	private long lastBulletFireTime;
	private double totalBulletPower;
	private long totalTimesHit;
	private long timeAliveTogether;
	private double totalDistance;
	private double damageFactor;
	private WallHitDamage wallHitDamage;
	private WeightedSqrEuclid<Double> powerTree;
	private Wave imaginaryWave;
	private int imaginaryWaveIndex;
	private int raw1v1ShotsFired;
	private int raw1v1ShotsHit;
	private double weighted1v1ShotsHit;
	private int raw1v1ShotsFiredThisRound;
	private int raw1v1ShotsHitThisRound;
	private double weighted1v1ShotsHitThisRound;
	private boolean isRobot;
	private long lastTimeHit;

	MoveEnemy(String botName, double distance, double energy, Point2D.Double location, double heading, double velocity,
			double absBearing, int round, long time, BattleField battleField, MovementPredictor predictor)
	{
		super(botName, location, distance, energy, heading, velocity, absBearing, round, time, battleField, predictor,
				new WaveManager());
		this.damageTaken = 0;
		this.lastBulletPower = 0;
		this.totalBulletPower = 0;
		this.totalTimesHit = 0;
		this.totalDistance = 500;
		this.wallHitDamage = new WallHitDamage(0, 0);
		this.initSurfViews();
		this.powerTree = new WeightedSqrEuclid<>(BULLET_POWER_WEIGHTS.length, null);
		this.powerTree.setWeights(BULLET_POWER_WEIGHTS);
		this.imaginaryWave = null;
		this.isRobot = false;
		this.lastTimeHit = 0L;
	}

	@Override
	public void initRound()
	{
		super.initRound();
		this.lastBulletFireTime = 0;
		this.lastBulletPower = 0;
		this.imaginaryWave = null;
		this.clearNeighborCache();
	}

	private void initSurfViews()
	{
		KnnView<TimestampedGuessFactor> simple = new KnnView<TimestampedGuessFactor>(new SimpleFormula()).setWeight(3).setK(25)
				.setKDivisor(5).bulletHitsOn();

		// TODO: reconfigure all this, hit threshold = 3.0 makes no sense
		KnnView<TimestampedGuessFactor> normal = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(40).setK(20)
				.setKDivisor(5).setHitThreshold(3.0).bulletHitsOn();

		KnnView<TimestampedGuessFactor> recent = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(100).setK(1)
				.setMaxDataPoints(1).setHitThreshold(RECENT_SCANS_HIT_THRESHOLD).bulletHitsOn();
		KnnView<TimestampedGuessFactor> recent2 = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(100).setK(1)
				.setMaxDataPoints(5).setHitThreshold(RECENT_SCANS_HIT_THRESHOLD).bulletHitsOn();
		KnnView<TimestampedGuessFactor> recent3 = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(100).setK(1)
				.setHitThreshold(RECENT_SCANS_HIT_THRESHOLD).setDecayRate(DECAY_RATE).bulletHitsOn();
		KnnView<TimestampedGuessFactor> recent4 = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(100).setK(7)
				.setKDivisor(4).setHitThreshold(RECENT_SCANS_HIT_THRESHOLD).setDecayRate(DECAY_RATE).bulletHitsOn();
		KnnView<TimestampedGuessFactor> recent5 = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(100)
				.setK(35).setKDivisor(3).setHitThreshold(RECENT_SCANS_HIT_THRESHOLD).setDecayRate(DECAY_RATE).bulletHitsOn();
		KnnView<TimestampedGuessFactor> recent6 = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(100)
				.setK(100).setKDivisor(2).setHitThreshold(RECENT_SCANS_HIT_THRESHOLD).setDecayRate(DECAY_RATE).bulletHitsOn();

		KnnView<TimestampedGuessFactor> lightFlattener = new KnnView<TimestampedGuessFactor>(new NormalFormula()).setWeight(10)
				.setK(50).setMaxDataPoints(1000).setKDivisor(5).setPaddedHitThreshold(LIGHT_FLATTENER_HIT_THRESHOLD).visitsOn();

		KnnView<TimestampedGuessFactor> flattener = new KnnView<TimestampedGuessFactor>(new FlattenerFormula()).setWeight(50)
				.setK(25).setMaxDataPoints(300).setKDivisor(12).setPaddedHitThreshold(FLATTENER_HIT_THRESHOLD).visitsOn();
		KnnView<TimestampedGuessFactor> flattener2 = new KnnView<TimestampedGuessFactor>(new FlattenerFormula()).setWeight(500)
				.setK(50).setMaxDataPoints(2000).setKDivisor(14).setPaddedHitThreshold(FLATTENER_HIT_THRESHOLD)
				.setDecayRate(DECAY_RATE).visitsOn();

		this.addView(simple);
		this.addView(normal);
		this.addView(recent);
		this.addView(recent2);
		this.addView(recent3);
		this.addView(recent4);
		this.addView(recent5);
		this.addView(recent6);
		this.addView(lightFlattener);
		this.addView(flattener);
		this.addView(flattener2);
	}

	void execute1v1(int currentRound, long currentTime, Point2D.Double myLocation)
	{
		this.getWaveManager().checkActiveWaves(currentTime, new RobotState(myLocation, currentTime, 0.0, 1, false),
				this.newWaveBreakListener(currentRound, currentTime));
	}

	Wave processBullet(Bullet bullet, int currentRound, long currentTime)
	{
		Point2D.Double bulletLocation = new Point2D.Double(bullet.getX(), bullet.getY());
		String botName = bullet.getName();

		Wave hitWave = this.getWaveManager().findClosestWave(bulletLocation, currentTime, Wave.FIRING_WAVE, botName,
				bullet.getPower());

		if (hitWave == null || !botName.equals(hitWave.getBotName()))
		{
			return null;
		}

		double hitGuessFactor = hitWave.guessFactor(bulletLocation);
		this.getViews().values().stream().filter(view -> view.logBulletHits)
				.forEach(view -> view.logWave(hitWave, new TimestampedGuessFactor(currentRound, currentTime, hitGuessFactor)));

		return hitWave;
	}

	int botsCloser(double distanceSq)
	{
		int botsCloser = 0;
		for (double botDistanceSq : this.getBotDistancesSq().values())
		{
			if (botDistanceSq < distanceSq)
			{
				botsCloser++;
			}
		}
		return botsCloser;
	}

	void clearNeighborCache()
	{
		this.getViews().values().forEach(KnnView::clearCache);
	}

	double getGunHeat(long time)
	{
		double gunHeat;
		if (time <= 30)
		{
			gunHeat = Math.max(0, 3.0 - (time * .1));
		}
		else
		{
			gunHeat = Math.max(0, Rules.getGunHeat(this.lastBulletPower) - ((time - this.lastBulletFireTime) * .1));
		}
		return DiaUtils.round(gunHeat, 6);
	}

	private double[] bulletPowerDataPoint(double distance, double enemyEnergy, double myEnergy)
	{
		return new double[] { Math.min(distance, 800) / 800, Math.min(enemyEnergy, 125) / 125, Math.min(myEnergy, 125) / 125 };
	}

	void resetBulletShadows(final List<FiredBullet> firedBullets)
	{
		this.getWaveManager().forAllWaves(w -> {
            if (w.isFiringWave())
            {
                w.getShadows().clear();
                for (FiredBullet bullet : firedBullets)
                {
                    MoveEnemy.this.setShadows(w, bullet);
                }
            }
        });
	}

	void setShadows(final FiredBullet bullet)
	{
		this.getWaveManager().forAllWaves(w -> {
            if (w.isFiringWave())
            {
                MoveEnemy.this.setShadows(w, bullet);
            }
        });
	}

	// TODO: I'm fairly certain the shadow created by a bullet going from
	// inside to outside a wave is irrelevant, so I'm ignoring it.
	// Would be nice to prove it or fix this code.
	private void setShadows(Wave w, FiredBullet bullet)
	{
		long startTime = Math.max(w.getFireTime(), bullet.getFireTime());
		if (!w.processedBulletHit()
				&& w.getSourceLocation().distanceSq(bullet.position(startTime)) > DiaUtils.square(w.distanceTraveled(startTime)))
		{
			long time = startTime;
			do
			{
				time++;
				if (w.getSourceLocation().distanceSq(bullet.position(time)) < DiaUtils.square(w.distanceTraveled(time))
						&& time < bullet.getDeathTime())
				{
					this.addBulletShadows(w, bullet, time);
					break;
				}
			}
			while (this.getBattleField().getRectangle().contains(bullet.position(time)));
		}
	}

	private void addBulletShadows(Wave w, FiredBullet b, long time)
	{
		Circle waveCircle1 = new Circle(w.getSourceLocation(), w.distanceTraveled(time - 1));
		Circle waveCircle2 = new Circle(w.getSourceLocation(), w.distanceTraveled(time));
		Point2D.Double bulletPoint1 = b.position(time - 1);
		Point2D.Double bulletPoint2 = b.position(time);
		LineSeg bulletSeg = new LineSeg(bulletPoint1, bulletPoint2);
		Point2D.Double[] xPoints1 = waveCircle1.intersects(bulletSeg);
		Point2D.Double[] xPoints2 = waveCircle2.intersects(bulletSeg);

		if (xPoints1[0] == null && xPoints2[0] == null)
		{
			// whole bullet line segment is between two wave circles,
			// shadow cast along all firing angles covered by line segment
			w.castShadow(bulletPoint1, bulletPoint2);
			// TODO: It's possible the previous bullet line intersected the
			// inner wave circle (then the outer wave circle) last tick
			// and cast 2 shadows, while remaining outside the wave on
			// either endpoint. Should be super rare and probably could
			// never be a reachable shadow anyway.
		}
		else if (xPoints1[0] != null)
		{
			if (xPoints2[0] != null)
			{
				// line segment intersects both wave circles, shadow cast
				// from one intersection to the other
				w.castShadow(xPoints2[0], xPoints1[0]);
			}
			else if (xPoints1[1] != null)
			{
				// line segment begins and ends between wave circles,
				// intersects inner circle in two spots, casts 2 shadows
				Point2D.Double intersect1, intersect2;
				if (bulletPoint1.distanceSq(xPoints1[0]) < bulletPoint1.distanceSq(xPoints1[1]))
				{
					intersect1 = xPoints1[0];
					intersect2 = xPoints1[1];
				}
				else
				{
					intersect1 = xPoints1[1];
					intersect2 = xPoints1[0];
				}
				w.castShadow(bulletPoint1, intersect1);
				w.castShadow(intersect2, bulletPoint2);
			}
			else
			{
				// line segment crosses inner wave circle in one spot
				w.castShadow(bulletPoint1, xPoints1[0]);
			}
		}
		else {
			// line segment crosses outer wave circle in one spot
			w.castShadow(xPoints2[0], bulletPoint2);
		}
	}

	Wave newMoveWave(Point2D.Double sourceLocation, Point2D.Double targetLocation, double absBearing, int fireRound,
			long fireTime, double bulletPower, double myEnergy, double myHeading, double myVelocity, int velocitySign,
			double accel, double dl8t, double dl20t, double dl40t, long timeSinceReverseDirection, long timeSinceVelocityChange)
	{
		Wave w = new Wave(this.getBotName(), sourceLocation, targetLocation, fireRound, fireTime, bulletPower, myHeading,
				myVelocity, velocitySign, this.getBattleField(), this.getPredictor());
		w.setAbsBearing(absBearing);
		w.setTargetAccel(accel);
		w.setTargetDistance(sourceLocation.distance(targetLocation));
		w.setTargetDchangeTime(timeSinceReverseDirection);
		w.setTargetDchangeTime(timeSinceVelocityChange);
		w.setTargetDl8t(dl8t);
		w.setTargetDl20t(dl20t);
		w.setTargetDl40t(dl40t);
		w.setTargetEnergy(myEnergy);
		w.setSourceEnergy(this.getEnergy());
		this.getWaveManager().addWave(w);
		return w;
	}

	void updateImaginaryWave(long currentTime, RobotState myRobotState, int wavesToSurf, MovementPredictor predictor)
	{
		this.imaginaryWaveIndex = -1;
		for (int x = 0; x < wavesToSurf; x++)
		{
			if (this.findSurfableWave(x, myRobotState) == null)
			{
				this.imaginaryWaveIndex = x;
				break;
			}
		}

		double enemyGunHeat = this.getGunHeat(currentTime);
		if (this.imaginaryWaveIndex >= 0 && enemyGunHeat < 0.1000001)
		{
			this.clearNeighborCache();
			Point2D.Double aimedFromLocation;

			if (enemyGunHeat < 0.0000001 && this.getWaveManager().size() >= 2)
			{
				this.imaginaryWave = this.getWaveManager().getWaveByFireTime(currentTime);
				aimedFromLocation = this.getState(currentTime - 1).getLocation();
			}
			else
			{
				this.setImaginaryWave(this.getWaveManager().getWaveByFireTime(currentTime + 1));
				aimedFromLocation = this.getLastScanState().getLocation();
				Point2D.Double sourceLocation = this.getBattleField().translateToField(
						predictor.nextLocation(this.getLastScanState()));

				if (sourceLocation.distance(myRobotState.getLocation()) < BOT_WIDTH)
				{
					sourceLocation = this.getLastScanState().getLocation();
				}

				this.getImaginaryWave().setSourceLocation(sourceLocation);
			}

			if (this.getImaginaryWave() != null)
			{ // null after a skipped turn
				// TODO: use Wave.setWallDistance
				this.getImaginaryWave().setTargetWallDistance(
						Math.min(
								1.5,
								this.getBattleField().orbitalWallDistance(aimedFromLocation,
										this.getImaginaryWave().getTargetLocation(), this.getLastBulletPower(),
										this.getImaginaryWave().getOrbitDirection())));
				this.getImaginaryWave().setTargetWallDistance(
						Math.min(
								1.5,
								this.getBattleField().orbitalWallDistance(aimedFromLocation,
										this.getImaginaryWave().getTargetLocation(), this.getLastBulletPower(),
										-this.getImaginaryWave().getOrbitDirection())));
				this.getImaginaryWave().setAbsBearing(
						DiaUtils.absoluteBearing(aimedFromLocation, this.getImaginaryWave().getTargetLocation()));
			}
		}
	}

	Wave findSurfableWave(int surfWaveIndex, RobotState myRobotState)
	{
		Wave surfableWave = this.getWaveManager().findSurfableWave(surfWaveIndex, myRobotState, WavePosition.BREAKING_CENTER);
		if (surfableWave == null && this.imaginaryWave != null && surfWaveIndex == this.imaginaryWaveIndex)
		{
			surfableWave = this.imaginaryWave;
		}
		return surfableWave;
	}

	void updateFiringWave(long currentTime, double bulletPower, RobotStateLog myStateLog, List<FiredBullet> firedBullets)
	{
		long fireTime = currentTime - 1;
		Wave enemyWave = this.getWaveManager().getWaveByFireTime(fireTime);
		if (enemyWave == null)
		{
			enemyWave = this.getWaveManager().interpolateWaveByFireTime(fireTime,
					this.getLastScanState().getHeading(), this.getLastScanState().getVelocity(), myStateLog,
					this.getBattleField(), this.getPredictor());
			System.out.println("WARNING (move): Wave with fire time " + fireTime + " doesn't exist, interpolation "
					+ (enemyWave == null ? "failed" : "succeeded") + ".");
			if (enemyWave != null)
			{
				enemyWave.setFiringWave(true);
				this.getWaveManager().addWave(enemyWave);
			}
		}
		if (enemyWave != null)
		{
			Point2D.Double aimedFromLocation = this.getState(currentTime - 2).getLocation();
			enemyWave.setSourceLocation(this.getState(currentTime - 1).getLocation());
			enemyWave.setTargetLocation(myStateLog.getState(currentTime - 2).getLocation());
			enemyWave.setAbsBearing(DiaUtils.absoluteBearing(aimedFromLocation, enemyWave.getTargetLocation()));
			enemyWave.setBulletPower(bulletPower);
			// TODO: use Wave.setWallDistance
			enemyWave.setTargetWallDistance(Math.min(
					1.5,
					this.getBattleField().orbitalWallDistance(aimedFromLocation, enemyWave.getTargetLocation(),
							this.lastBulletPower, enemyWave.getOrbitDirection())));
			enemyWave.setTargetRevWallDistance(Math.min(
					1.5,
					this.getBattleField().orbitalWallDistance(aimedFromLocation, enemyWave.getTargetLocation(),
							this.lastBulletPower, -enemyWave.getOrbitDirection())));

			if (this.imaginaryWave != null)
			{
				this.clearNeighborCache();
			}
			this.imaginaryWave = null;

			enemyWave.setFiringWave(true);
			this.lastBulletPower = bulletPower;
			this.lastBulletFireTime = enemyWave.getFireTime();
			for (FiredBullet bullet : firedBullets)
			{
				this.setShadows(enemyWave, bullet);
			}

			double[] dataPoint = this.bulletPowerDataPoint(enemyWave.getTargetDistance(), enemyWave.getSourceEnergy(),
					enemyWave.getTargetEnergy());
			this.powerTree.addPoint(dataPoint, bulletPower);
		}
	}

	void updateDamageFactor()
	{
		if (this.isAlive())
		{
			this.timeAliveTogether++;
			this.totalDistance += this.getDistance();
		}
		this.damageFactor = ((this.damageTaken + 10) / (this.getDamageGiven() + 10)) * this.totalDistance
				/ DiaUtils.square(this.getTimeAliveTogether());
	}

	double guessBulletPower(double myEnergy)
	{
		int numBullets = this.powerTree.size();
		if (numBullets == 0)
		{
			return 1.9;
		}
		double[] searchPoint = this.bulletPowerDataPoint(this.getDistance(), this.getEnergy(), myEnergy);
		List<Entry<Double>> bulletPowers = this.powerTree.nearestNeighbor(searchPoint,
				(int) Math.min(20, Math.ceil(numBullets / 3.0)), false);

		double powerTotal = 0;
		for (Entry<Double> entry : bulletPowers)
		{
			powerTotal += entry.getValue();
		}

		return DiaUtils.round(powerTotal / bulletPowers.size(), 6);
	}

	private WaveBreakListener newWaveBreakListener(final int currentRound, final long currentTime)
	{
		return (w, waveBreakStates) -> {
            if (w.isFiringWave())
            {
                Intersection preciseIntersection = w.preciseIntersection(waveBreakStates);

				MoveEnemy.this.getViews().values().stream().filter(view -> view.logVisits).forEach(view -> {
					double guessFactor = w.guessFactor(preciseIntersection.getAngle());
					view.logWave(w, new TimestampedGuessFactor(currentRound, currentTime, guessFactor));
				});

                if (!w.isBulletHitBullet())
                {
                    MoveEnemy.this.setRaw1v1ShotsFiredThisRound(MoveEnemy.this.getRaw1v1ShotsFiredThisRound() + 1);
                    MoveEnemy.this.setRaw1v1ShotsFired(MoveEnemy.this.getRaw1v1ShotsFired() + 1);

                    if (w.isHitByBullet())
                    {
                        MoveEnemy.this.setWeighted1v1ShotsHitThisRound(MoveEnemy.this.getWeighted1v1ShotsHitThisRound() + 1);
                        MoveEnemy.this.setWeighted1v1ShotsHit(MoveEnemy.this.getWeighted1v1ShotsHit() + 1);
                        MoveEnemy.this.setRaw1v1ShotsFiredThisRound(MoveEnemy.this.getRaw1v1ShotsHit() + 1);
                        MoveEnemy.this.setRaw1v1ShotsHitThisRound(MoveEnemy.this.getRaw1v1ShotsHitThisRound() + 1);
                    }
                }
            }
        };
	}

	/**
	 * @return the damageTaken
	 */
	public double getDamageTaken()
	{
		return this.damageTaken;
	}

	/**
	 * @param damageTaken
	 *            the damageTaken to set
	 */
	public void setDamageTaken(double damageTaken)
	{
		this.damageTaken = damageTaken;
	}

	/**
	 * @return the lastBulletPower
	 */
	public double getLastBulletPower()
	{
		return this.lastBulletPower;
	}

	/**
	 * @param lastBulletPower
	 *            the lastBulletPower to set
	 */
	public void setLastBulletPower(double lastBulletPower)
	{
		this.lastBulletPower = lastBulletPower;
	}

	/**
	 * @return the lastBulletFireTime
	 */
	public long getLastBulletFireTime()
	{
		return this.lastBulletFireTime;
	}

	/**
	 * @param lastBulletFireTime
	 *            the lastBulletFireTime to set
	 */
	public void setLastBulletFireTime(long lastBulletFireTime)
	{
		this.lastBulletFireTime = lastBulletFireTime;
	}

	/**
	 * @return the totalBulletPower
	 */
	public double getTotalBulletPower()
	{
		return this.totalBulletPower;
	}

	/**
	 * @param totalBulletPower
	 *            the totalBulletPower to set
	 */
	public void setTotalBulletPower(double totalBulletPower)
	{
		this.totalBulletPower = totalBulletPower;
	}

	/**
	 * @return the totalTimesHit
	 */
	public long getTotalTimesHit()
	{
		return this.totalTimesHit;
	}

	/**
	 * @param totalTimesHit
	 *            the totalTimesHit to set
	 */
	public void setTotalTimesHit(long totalTimesHit)
	{
		this.totalTimesHit = totalTimesHit;
	}

	/**
	 * @return the totalDistance
	 */
	public double getTotalDistance()
	{
		return this.totalDistance;
	}

	/**
	 * @param totalDistance
	 *            the totalDistance to set
	 */
	public void setTotalDistance(double totalDistance)
	{
		this.totalDistance = totalDistance;
	}

	/**
	 * @return the damageFactor
	 */
	public double getDamageFactor()
	{
		return this.damageFactor;
	}

	/**
	 * @param damageFactor
	 *            the damageFactor to set
	 */
	public void setDamageFactor(double damageFactor)
	{
		this.damageFactor = damageFactor;
	}

	/**
	 * @return the wallHitDamage
	 */
	public WallHitDamage getWallHitDamage()
	{
		return this.wallHitDamage;
	}

	/**
	 * @param wallHitDamage
	 *            the wallHitDamage to set
	 */
	public void setWallHitDamage(WallHitDamage wallHitDamage)
	{
		this.wallHitDamage = wallHitDamage;
	}

	/**
	 * @return the powerTree
	 */
	public WeightedSqrEuclid<Double> getPowerTree()
	{
		return this.powerTree;
	}

	/**
	 * @param powerTree
	 *            the powerTree to set
	 */
	public void setPowerTree(WeightedSqrEuclid<Double> powerTree)
	{
		this.powerTree = powerTree;
	}

	/**
	 * @return the imaginaryWave
	 */
	public Wave getImaginaryWave()
	{
		return this.imaginaryWave;
	}

	/**
	 * @param imaginaryWave
	 *            the imaginaryWave to set
	 */
	public void setImaginaryWave(Wave imaginaryWave)
	{
		this.imaginaryWave = imaginaryWave;
	}

	/**
	 * @return the imaginaryWaveIndex
	 */
	public int getImaginaryWaveIndex()
	{
		return this.imaginaryWaveIndex;
	}

	/**
	 * @param imaginaryWaveIndex
	 *            the imaginaryWaveIndex to set
	 */
	public void setImaginaryWaveIndex(int imaginaryWaveIndex)
	{
		this.imaginaryWaveIndex = imaginaryWaveIndex;
	}

	/**
	 * @return the raw1v1ShotsFired
	 */
	public int getRaw1v1ShotsFired()
	{
		return this.raw1v1ShotsFired;
	}

	/**
	 * @param raw1v1ShotsFired
	 *            the raw1v1ShotsFired to set
	 */
	public void setRaw1v1ShotsFired(int raw1v1ShotsFired)
	{
		this.raw1v1ShotsFired = raw1v1ShotsFired;
	}

	/**
	 * @return the raw1v1ShotsHit
	 */
	public int getRaw1v1ShotsHit()
	{
		return this.raw1v1ShotsHit;
	}

	/**
	 * @param raw1v1ShotsHit
	 *            the raw1v1ShotsHit to set
	 */
	public void setRaw1v1ShotsHit(int raw1v1ShotsHit)
	{
		this.raw1v1ShotsHit = raw1v1ShotsHit;
	}

	/**
	 * @return the weighted1v1ShotsHit
	 */
	public double getWeighted1v1ShotsHit()
	{
		return this.weighted1v1ShotsHit;
	}

	/**
	 * @param weighted1v1ShotsHit
	 *            the weighted1v1ShotsHit to set
	 */
	public void setWeighted1v1ShotsHit(double weighted1v1ShotsHit)
	{
		this.weighted1v1ShotsHit = weighted1v1ShotsHit;
	}

	/**
	 * @return the raw1v1ShotsFiredThisRound
	 */
	public int getRaw1v1ShotsFiredThisRound()
	{
		return this.raw1v1ShotsFiredThisRound;
	}

	/**
	 * @param raw1v1ShotsFiredThisRound
	 *            the raw1v1ShotsFiredThisRound to set
	 */
	public void setRaw1v1ShotsFiredThisRound(int raw1v1ShotsFiredThisRound)
	{
		this.raw1v1ShotsFiredThisRound = raw1v1ShotsFiredThisRound;
	}

	/**
	 * @return the raw1v1ShotsHitThisRound
	 */
	public int getRaw1v1ShotsHitThisRound()
	{
		return this.raw1v1ShotsHitThisRound;
	}

	/**
	 * @param raw1v1ShotsHitThisRound
	 *            the raw1v1ShotsHitThisRound to set
	 */
	public void setRaw1v1ShotsHitThisRound(int raw1v1ShotsHitThisRound)
	{
		this.raw1v1ShotsHitThisRound = raw1v1ShotsHitThisRound;
	}

	/**
	 * @return the weighted1v1ShotsHitThisRound
	 */
	public double getWeighted1v1ShotsHitThisRound()
	{
		return this.weighted1v1ShotsHitThisRound;
	}

	/**
	 * @param weighted1v1ShotsHitThisRound
	 *            the weighted1v1ShotsHitThisRound to set
	 */
	public void setWeighted1v1ShotsHitThisRound(double weighted1v1ShotsHitThisRound)
	{
		this.weighted1v1ShotsHitThisRound = weighted1v1ShotsHitThisRound;
	}

	/**
	 * @return the timeAliveTogether
	 */
	@Override
	public long getTimeAliveTogether()
	{
		return this.timeAliveTogether;
	}

	/**
	 * @param timeAliveTogether
	 *            the timeAliveTogether to set
	 */
	@Override
	public void setTimeAliveTogether(long timeAliveTogether)
	{
		this.timeAliveTogether = timeAliveTogether;
	}

	/**
	 * @return the isRobot
	 */
	public boolean isRobot()
	{
		return this.isRobot;
	}

	/**
	 * @param isRobot
	 *            the isRobot to set
	 */
	public void setRobot(boolean isRobot)
	{
		this.isRobot = isRobot;
	}

	/**
	 * @return the lastTimeHit
	 */
	public long getLastTimeHit()
	{
		return this.lastTimeHit;
	}

	/**
	 * @param lastTimeHit
	 *            the lastTimeHit to set
	 */
	public void setLastTimeHit(long lastTimeHit)
	{
		this.lastTimeHit = lastTimeHit;
	}
}