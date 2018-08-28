package voidious.utils;

import robocode.util.Utils;
import voidious.enums.WavePosition;
import voidious.utils.geom.Circle;
import voidious.utils.geom.LineSeg;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static voidious.utils.DiaUtils.*;

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

public class Wave implements Cloneable
{
	private static final double PRECISE_MEA_WALL_STICK = 120;
	private static final Point2D.Double ORIGIN = new Point2D.Double(0, 0);
	private static final double MAX_BOT_RADIUS = 18 / Math.cos(Math.PI / 4);
	private static final int CLOCKWISE = 1;
	private static final int COUNTERCLOCKWISE = -1;
	public static final boolean FIRING_WAVE = true;
	private static final boolean POSITIVE_GUESSFACTOR = true;
	private static final boolean NEGATIVE_GUESSFACTOR = false;
	static final double ANY_BULLET_POWER = -1;
	public static final int FIRST_WAVE = 0;

	private String botName;
	private Point2D.Double sourceLocation;
	private Point2D.Double targetLocation;
	private double absBearing;
	private int fireRound;
	private long fireTime;
	private double bulletPower;
	private double bulletSpeed;
	private double maxEscapeAngle;
	private int orbitDirection;
	private double targetHeading;
	private double targetRelativeHeading;
	private double targetVelocity;
	private BattleField battleField;
	private MovementPredictor predictor;
	private boolean hitByBullet;
	private boolean bulletHitBullet;
	private boolean firingWave;
	private boolean altWave;
	private double targetAccel;
	private int targetVelocitySign;
	private double targetDistance;
	private double targetDistanceToNearestBot;
	private long targetDchangeTime;
	private long targetVchangeTime;
	private double targetWallDistance;
	private double targetRevWallDistance;
	private double targetDl8t;
	private double targetDl20t;
	private double targetDl40t;
	private double targetEnergy;
	private double sourceEnergy;
	private double gunHeat;
	private int enemiesAlive;
	private long lastBulletFiredTime;
	private List<BulletShadow> shadows;
	private Double cachedPositiveEscapeAngle = null;
	private Double cachedNegativeEscapeAngle = null;
	private boolean usedNegativeSmoothingMea = false;
	private boolean usedPositiveSmoothingMea = false;

	public Wave(String botName, Point2D.Double sourceLocation, Point2D.Double targetLocation, int fireRound, long fireTime,
			double bulletPower, double targetHeading, double targetVelocity, int targetVelocitySign, BattleField battleField,
			MovementPredictor predictor)
	{
		this.botName = botName;
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
		this.fireRound = fireRound;
		this.fireTime = fireTime;
		this.setBulletPower(bulletPower);
		this.targetHeading = targetHeading;
		this.targetVelocity = targetVelocity;
		this.targetVelocitySign = targetVelocitySign;
		this.battleField = battleField;
		this.predictor = predictor;
		this.absBearing = absoluteBearing(sourceLocation, targetLocation);
		double relativeHeading = Utils.normalRelativeAngle(this.effectiveHeading()
				- absoluteBearing(sourceLocation, targetLocation));
		this.orbitDirection = (relativeHeading < 0) ? COUNTERCLOCKWISE : CLOCKWISE;
		this.targetRelativeHeading = Math.abs(relativeHeading);
		this.hitByBullet = false;
		this.bulletHitBullet = false;
		this.firingWave = false;
		this.altWave = false;
		this.shadows = new ArrayList<>();
	}

	public Wave setAbsBearing(double absBearing)
	{
		this.absBearing = absBearing;
		return this;
	}

	public Wave setBulletPower(double power)
	{
		this.bulletPower = power;
		this.bulletSpeed = (20 - (3 * power));
		this.maxEscapeAngle = Math.asin(8.0 / this.bulletSpeed);
		this.clearCachedPreciseEscapeAngles();
		return this;
	}

	private double effectiveHeading()
	{
		return Utils.normalAbsoluteAngle(this.targetHeading + (this.targetVelocitySign == 1 ? 0 : Math.PI));
	}

	public double distanceTraveled(long currentTime)
	{
		return (currentTime - this.fireTime) * this.bulletSpeed;
	}

	public double lateralVelocity()
	{
		return Math.sin(this.targetRelativeHeading) * (this.targetVelocitySign * this.targetVelocity);
	}

	public boolean processedBulletHit()
	{
		return this.hitByBullet || this.bulletHitBullet;
	}

	public void setWallDistances(WallDistanceStyle style)
	{
		switch (style)
		{
			case ORBITAL:
				this.targetWallDistance = this.orbitalWallDistance(this.orbitDirection);
				this.targetRevWallDistance = this.orbitalWallDistance(-this.orbitDirection);
				break;
			case DIRECT:
				this.targetWallDistance = this.directToWallDistance(true);
				this.targetRevWallDistance = this.directToWallDistance(false);
				break;
			case PRECISE_MEA:
				this.targetWallDistance = this.preciseEscapeAngle(POSITIVE_GUESSFACTOR) / this.maxEscapeAngle;
				this.targetRevWallDistance = this.preciseEscapeAngle(NEGATIVE_GUESSFACTOR) / this.maxEscapeAngle;
				break;
		}
	}

	private double orbitalWallDistance(int orientation)
	{
		return Math.min(1.5,
				this.battleField.orbitalWallDistance(this.sourceLocation, this.targetLocation, this.bulletPower, orientation));
	}

	private double directToWallDistance(boolean forward)
	{
		return Math.min(1.5, this.battleField.directToWallDistance(this.targetLocation,
				this.sourceLocation.distance(this.targetLocation), this.effectiveHeading() + (forward ? 0 : Math.PI),
				this.bulletPower));
	}

	public double virtuality()
	{
		long timeSinceLastBullet = (this.fireTime - this.lastBulletFiredTime);
		long timeToNextBullet = Math.round(Math.ceil(this.gunHeat * 10));

		if (this.firingWave)
		{
			return 0;
		}
		else if (this.lastBulletFiredTime > 0)
		{
			return Math.min(timeSinceLastBullet, timeToNextBullet) / 8.0;
		}
		else
		{
			return Math.min(1, timeToNextBullet / 8.0);
		}
	}

	public double firingAngle(double guessFactor)
	{
		return this.absBearing + (guessFactor * this.orbitDirection * this.maxEscapeAngle);
	}

	public double firingAngleFromTargetLocation(Point2D.Double firingTarget)
	{
		return Utils.normalAbsoluteAngle(absoluteBearing(this.sourceLocation, firingTarget));
	}

	public Point2D.Double displacementVector(RobotState waveBreakState)
	{
		return this.displacementVector(waveBreakState.getLocation(), waveBreakState.getTime());
	}

	public Point2D.Double displacementVector(Point2D.Double botLocation, long time)
	{
		double vectorBearing = Utils.normalRelativeAngle(absoluteBearing(this.targetLocation, botLocation)
				- this.effectiveHeading());
		double vectorDistance = this.targetLocation.distance(botLocation) / (time - this.fireTime);
		return DiaUtils.project(ORIGIN, vectorBearing * this.orbitDirection, vectorDistance);
	}

	public Point2D.Double projectLocationFromDisplacementVector(Point2D.Double dispVector)
	{
		return this.projectLocation(this.sourceLocation, dispVector, 0);
	}

	public Point2D.Double projectLocationBlind(Point2D.Double myNextLocation, Point2D.Double dispVector, long currentTime)
	{
		return this.projectLocation(myNextLocation, dispVector, currentTime - this.fireTime + 1);
	}

	private Point2D.Double projectLocation(Point2D.Double firingLocation, Point2D.Double dispVector, long extraTicks)
	{
		double dispAngle = this.effectiveHeading() + (absoluteBearing(ORIGIN, dispVector) * this.orbitDirection);
		double dispDistance = ORIGIN.distance(dispVector);

		Point2D.Double projectedLocation = this.targetLocation;
		long bulletTicks = -1;
		long prevBulletTicks = -1;
		long prevPrevBulletTicks;
		double daSin = Math.sin(dispAngle);
		double daCos = Math.cos(dispAngle);

		do
		{
			prevPrevBulletTicks = prevBulletTicks;
			prevBulletTicks = bulletTicks;
			bulletTicks = DiaUtils.bulletTicksFromSpeed(firingLocation.distance(projectedLocation), this.bulletSpeed) - 1;
			projectedLocation = DiaUtils.project(this.targetLocation, daSin, daCos, (bulletTicks + extraTicks) * dispDistance);
		}
		while (bulletTicks != prevBulletTicks && bulletTicks != prevPrevBulletTicks);

		return projectedLocation;
	}

	public double guessFactor(Point2D.Double location)
	{
		double bearingToTarget = absoluteBearing(this.sourceLocation, location);
		return this.guessFactor(bearingToTarget);
	}

	public double guessFactor(double bearingToTarget)
	{
		return this.guessAngle(bearingToTarget) / this.maxEscapeAngle;
	}

	private double guessAngle(double bearingToTarget)
	{
		return this.orbitDirection * Utils.normalRelativeAngle(bearingToTarget - this.absBearing);
	}

	public double guessFactorPrecise(Point2D.Double location)
	{
		double newBearingToTarget = absoluteBearing(this.sourceLocation, location);
		return this.guessFactorPrecise(newBearingToTarget);
	}

	public double guessFactorPrecise(double newBearingToTarget)
	{
		double guessAngle = this.orbitDirection * Utils.normalRelativeAngle(newBearingToTarget - this.absBearing);
		double maxEscAngle = this.preciseEscapeAngle(guessAngle >= 0);
		return guessAngle / maxEscAngle;
	}

	public double preciseEscapeAngle(boolean guessFactorSign)
	{
		if (guessFactorSign)
		{
			if (this.cachedPositiveEscapeAngle == null)
			{
				this.cachedPositiveEscapeAngle = this.calculatePreciseEscapeAngle(true).getAngle();
			}
			return this.cachedPositiveEscapeAngle;
		}

		if (this.cachedNegativeEscapeAngle == null)
		{
			this.cachedNegativeEscapeAngle = this.calculatePreciseEscapeAngle(false).getAngle();
		}
		return this.cachedNegativeEscapeAngle;
	}

	public double escapeAngleRange()
	{
		return (this.preciseEscapeAngle(POSITIVE_GUESSFACTOR) + this.preciseEscapeAngle(NEGATIVE_GUESSFACTOR));
	}

	private MaxEscapeTarget calculatePreciseEscapeAngle(boolean positiveGuessFactor)
	{
		RobotState startState = new RobotState(((Point2D.Double) this.targetLocation.clone()), this.targetHeading,
				this.targetVelocity, this.fireTime, false);
		return this.predictor.preciseEscapeAngle(this.orbitDirection * (positiveGuessFactor ? 1 : -1), this.sourceLocation,
				this.fireTime, this.bulletSpeed, startState, PRECISE_MEA_WALL_STICK);
	}

	private void clearCachedPreciseEscapeAngles()
	{
		this.cachedPositiveEscapeAngle = null;
		this.cachedNegativeEscapeAngle = null;
	}

	public boolean shadowed(double firingAngle)
	{
		for (BulletShadow shadow : this.shadows)
		{
			firingAngle = normalizeAngle(firingAngle, shadow.getMinAngle());
			if (firingAngle >= shadow.getMinAngle() && firingAngle <= shadow.getMaxAngle())
			{
				return true;
			}
		}
		return false;
	}

	public void castShadow(Point2D.Double p1, Point2D.Double p2)
	{
		this.castShadow(absoluteBearing(this.sourceLocation, p1), absoluteBearing(this.sourceLocation, p2));
	}

	private void castShadow(double shadowAngle1, double shadowAngle2)
	{
		shadowAngle1 = normalizeAngle(shadowAngle1, this.absBearing);
		shadowAngle2 = normalizeAngle(shadowAngle2, shadowAngle1);
		double min = Math.min(shadowAngle1, shadowAngle2);
		double max = Math.max(shadowAngle1, shadowAngle2);
		this.shadows.add(new BulletShadow(min, max));

		Set<BulletShadow> deadShadows = new HashSet<>();
		this.shadows.stream().filter(shadow1 -> !deadShadows.contains(shadow1))
				.forEach(shadow1 -> this.shadows.stream().filter(shadow2 -> shadow1 != shadow2 && !deadShadows.contains(shadow2) && shadow1.overlaps(shadow2)).forEach(shadow2 -> {
            shadow1.setMinAngle(Math.min(shadow1.getMinAngle(),
                    normalizeAngle(shadow2.getMinAngle(), shadow1.getMinAngle())));
            shadow1.setMaxAngle(Math.max(shadow1.getMaxAngle(),
                    normalizeAngle(shadow2.getMaxAngle(), shadow1.getMaxAngle())));
            deadShadows.add(shadow2);
        }));
		for (BulletShadow deadShadow : deadShadows)
		{
			this.shadows.remove(deadShadow);
		}
	}

	public double shadowFactor(Intersection intersection)
	{
		double min = intersection.getAngle() - intersection.getBandwidth();
		double max = intersection.getAngle() + intersection.getBandwidth();
		double factor = 1;
		for (BulletShadow shadow : this.shadows)
		{
			double shadowMin = normalizeAngle(shadow.getMinAngle(), min);
			double shadowMax = normalizeAngle(shadow.getMaxAngle(), shadowMin);
			if (shadowMin <= min && shadowMax >= max)
			{
				return 0;
			}
			else if (shadowMin >= min && shadowMin <= max)
			{
				factor -= (Math.min(max, shadowMax) - shadowMin) / (max - min);
			}
			else if (shadowMax >= min && shadowMax <= max)
			{
				factor -= (shadowMax - Math.max(min, shadowMin)) / (max - min);
			}
		}

		return factor;
	}

	public Intersection preciseIntersection(List<RobotState> waveBreakStates)
	{
		if (waveBreakStates == null || waveBreakStates.size() == 0)
		{
			return null;
		}

		ArrayList<Double> aimAngles = new ArrayList<>();
		for (RobotState waveBreakState : waveBreakStates)
		{
			List<Point2D.Double> corners = waveBreakState.botCorners();
			Circle waveStart = new Circle(this.sourceLocation.x, this.sourceLocation.y, this.bulletSpeed
					* (waveBreakState.getTime() - this.fireTime));
			Circle waveEnd = new Circle(this.sourceLocation.x, this.sourceLocation.y, this.bulletSpeed
					* (waveBreakState.getTime() - this.fireTime + 1));

			aimAngles.addAll(corners.stream().filter(corner -> waveEnd.contains(corner) && !waveStart.contains(corner))
					.map(corner -> absoluteBearing(this.sourceLocation, corner)).collect(Collectors.toList()));

			for (Line2D.Double side : waveBreakState.botSides())
			{
				LineSeg seg = new LineSeg(side.x1, side.y1, side.x2, side.y2);
				Point2D.Double[] intersects = waveStart.intersects(seg);
				for (Point2D.Double intersect : intersects) {
					if (intersect != null) {
						aimAngles.add(absoluteBearing(this.sourceLocation, intersect));
					}
				}
				intersects = waveEnd.intersects(seg);
				if (intersects != null)
				{
					for (Point2D.Double intersect : intersects) {
						if (intersect != null) {
							aimAngles.add(absoluteBearing(this.sourceLocation, intersect));
						}
					}
				}
			}
		}

		double normalizeReference = aimAngles.get(0);
		double minAngle = normalizeReference;
		double maxAngle = normalizeReference;

		for (double thisAngle : aimAngles)
		{
			thisAngle = normalizeAngle(thisAngle, normalizeReference);
			maxAngle = Math.max(maxAngle, thisAngle);
			minAngle = Math.min(minAngle, thisAngle);
		}

		double centerAngle = (maxAngle + minAngle) / 2;
		double bandwidth = maxAngle - centerAngle;

		return new Intersection(centerAngle, bandwidth);
	}

	public WavePosition checkWavePosition(RobotState currentState)
	{
		return this.checkWavePosition(currentState, false, null);
	}

	public WavePosition checkWavePosition(RobotState currentState, boolean skipMidair)
	{
		return this.checkWavePosition(currentState, skipMidair, null);
	}

	public WavePosition checkWavePosition(RobotState currentState, WavePosition maxPosition)
	{
		return this.checkWavePosition(currentState, false, maxPosition);
	}

	private WavePosition checkWavePosition(RobotState currentState, boolean skipMidair, WavePosition maxPosition)
	{
		Point2D.Double location = currentState.getLocation();
		double enemyDistSq = this.sourceLocation.distanceSq(location);
		double endBulletDistance = this.distanceTraveled(currentState.getTime() + 1);
		if (!skipMidair
				&& (maxPosition == WavePosition.MIDAIR || enemyDistSq > square(endBulletDistance + MAX_BOT_RADIUS) || DiaUtils
						.distancePointToBot(this.sourceLocation, currentState) > endBulletDistance))
		{
			return WavePosition.MIDAIR;
		}
		else if (maxPosition == WavePosition.BREAKING_FRONT || enemyDistSq > square(endBulletDistance))
		{
			return WavePosition.BREAKING_FRONT;
		}
		else
		{
			if (maxPosition == WavePosition.BREAKING_CENTER)
			{
				return WavePosition.BREAKING_CENTER;
			}
			List<Point2D.Double> corners = currentState.botCorners();
			double startBulletDistance = this.distanceTraveled(currentState.getTime());
			for (Point2D.Double corner : corners)
			{
				if (corner.distanceSq(this.sourceLocation) > square(startBulletDistance))
				{
					return WavePosition.BREAKING_CENTER;
				}
			}
			return WavePosition.GONE;
		}
	}

	@Override
	public Object clone()
	{
		Wave w = new Wave(this.botName, this.sourceLocation, this.targetLocation, this.fireRound, this.fireTime,
				this.bulletPower, this.targetHeading, this.targetVelocity, this.targetVelocitySign, this.battleField,
				this.predictor);
		w.setAbsBearing(this.absBearing);
		w.setTargetAccel(this.targetAccel);
		w.setTargetDistance(this.targetDistance);
		w.setTargetDchangeTime(this.targetDchangeTime);
		w.setTargetVchangeTime(this.targetVchangeTime);
		w.setTargetDl8t(this.targetDl8t);
		w.setTargetDl20t(this.targetDl20t);
		w.setTargetDl40t(this.targetDl40t);
		w.setTargetEnergy(this.targetEnergy);
		w.setSourceEnergy(this.sourceEnergy);
		w.setAltWave(this.altWave);
		w.setFiringWave(this.firingWave);
		w.setHitByBullet(this.hitByBullet);
		w.setBulletHitBullet(this.bulletHitBullet);
		w.setEnemiesAlive(this.enemiesAlive);
		w.setLastBulletFiredTime(this.lastBulletFiredTime);
		return w;
	}

	public enum WallDistanceStyle
	{
		ORBITAL, DIRECT, PRECISE_MEA
	}

	/**
	 * @return the botName
	 */
	public String getBotName()
	{
		return this.botName;
	}

	/**
	 * @param botName
	 *            the botName to set
	 */
	public void setBotName(String botName)
	{
		this.botName = botName;
	}

	/**
	 * @return the sourceLocation
	 */
	public Point2D.Double getSourceLocation()
	{
		return this.sourceLocation;
	}

	/**
	 * @param sourceLocation
	 *            the sourceLocation to set
	 */
	public void setSourceLocation(Point2D.Double sourceLocation)
	{
		this.sourceLocation = sourceLocation;
	}

	/**
	 * @return the targetLocation
	 */
	public Point2D.Double getTargetLocation()
	{
		return this.targetLocation;
	}

	/**
	 * @param targetLocation
	 *            the targetLocation to set
	 */
	public void setTargetLocation(Point2D.Double targetLocation)
	{
		this.targetLocation = targetLocation;
	}

	/**
	 * @return the fireRound
	 */
	public int getFireRound()
	{
		return this.fireRound;
	}

	/**
	 * @param fireRound
	 *            the fireRound to set
	 */
	public void setFireRound(int fireRound)
	{
		this.fireRound = fireRound;
	}

	/**
	 * @return the fireTime
	 */
	public long getFireTime()
	{
		return this.fireTime;
	}

	/**
	 * @param fireTime
	 *            the fireTime to set
	 */
	public void setFireTime(long fireTime)
	{
		this.fireTime = fireTime;
	}

	/**
	 * @return the bulletSpeed
	 */
	public double getBulletSpeed()
	{
		return this.bulletSpeed;
	}

	/**
	 * @param bulletSpeed
	 *            the bulletSpeed to set
	 */
	public void setBulletSpeed(double bulletSpeed)
	{
		this.bulletSpeed = bulletSpeed;
	}

	/**
	 * @return the maxEscapeAngle
	 */
	public double getMaxEscapeAngle()
	{
		return this.maxEscapeAngle;
	}

	/**
	 * @param maxEscapeAngle
	 *            the maxEscapeAngle to set
	 */
	public void setMaxEscapeAngle(double maxEscapeAngle)
	{
		this.maxEscapeAngle = maxEscapeAngle;
	}

	/**
	 * @return the orbitDirection
	 */
	public int getOrbitDirection()
	{
		return this.orbitDirection;
	}

	/**
	 * @param orbitDirection
	 *            the orbitDirection to set
	 */
	public void setOrbitDirection(int orbitDirection)
	{
		this.orbitDirection = orbitDirection;
	}

	/**
	 * @return the targetHeading
	 */
	public double getTargetHeading()
	{
		return this.targetHeading;
	}

	/**
	 * @param targetHeading
	 *            the targetHeading to set
	 */
	public void setTargetHeading(double targetHeading)
	{
		this.targetHeading = targetHeading;
	}

	/**
	 * @return the targetRelativeHeading
	 */
	public double getTargetRelativeHeading()
	{
		return this.targetRelativeHeading;
	}

	/**
	 * @param targetRelativeHeading
	 *            the targetRelativeHeading to set
	 */
	public void setTargetRelativeHeading(double targetRelativeHeading)
	{
		this.targetRelativeHeading = targetRelativeHeading;
	}

	/**
	 * @return the targetVelocity
	 */
	public double getTargetVelocity()
	{
		return this.targetVelocity;
	}

	/**
	 * @param targetVelocity
	 *            the targetVelocity to set
	 */
	public void setTargetVelocity(double targetVelocity)
	{
		this.targetVelocity = targetVelocity;
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
	 * @return the predictor
	 */
	public MovementPredictor getPredictor()
	{
		return this.predictor;
	}

	/**
	 * @param predictor
	 *            the predictor to set
	 */
	public void setPredictor(MovementPredictor predictor)
	{
		this.predictor = predictor;
	}

	/**
	 * @return the hitByBullet
	 */
	public boolean isHitByBullet()
	{
		return this.hitByBullet;
	}

	/**
	 * @param hitByBullet
	 *            the hitByBullet to set
	 */
	public void setHitByBullet(boolean hitByBullet)
	{
		this.hitByBullet = hitByBullet;
	}

	/**
	 * @return the bulletHitBullet
	 */
	public boolean isBulletHitBullet()
	{
		return this.bulletHitBullet;
	}

	/**
	 * @param bulletHitBullet
	 *            the bulletHitBullet to set
	 */
	public void setBulletHitBullet(boolean bulletHitBullet)
	{
		this.bulletHitBullet = bulletHitBullet;
	}

	/**
	 * @return the firingWave
	 */
	public boolean isFiringWave()
	{
		return this.firingWave;
	}

	/**
	 * @param firingWave
	 *            the firingWave to set
	 */
	public void setFiringWave(boolean firingWave)
	{
		this.firingWave = firingWave;
	}

	/**
	 * @return the altWave
	 */
	public boolean isAltWave()
	{
		return this.altWave;
	}

	/**
	 * @param altWave
	 *            the altWave to set
	 */
	public void setAltWave(boolean altWave)
	{
		this.altWave = altWave;
	}

	/**
	 * @return the targetAccel
	 */
	public double getTargetAccel()
	{
		return this.targetAccel;
	}

	/**
	 * @param targetAccel
	 *            the targetAccel to set
	 */
	public void setTargetAccel(double targetAccel)
	{
		this.targetAccel = targetAccel;
	}

	/**
	 * @return the targetVelocitySign
	 */
	public int getTargetVelocitySign()
	{
		return this.targetVelocitySign;
	}

	/**
	 * @param targetVelocitySign
	 *            the targetVelocitySign to set
	 */
	public void setTargetVelocitySign(int targetVelocitySign)
	{
		this.targetVelocitySign = targetVelocitySign;
	}

	/**
	 * @return the targetDistance
	 */
	public double getTargetDistance()
	{
		return this.targetDistance;
	}

	/**
	 * @param targetDistance
	 *            the targetDistance to set
	 */
	public void setTargetDistance(double targetDistance)
	{
		this.targetDistance = targetDistance;
	}

	/**
	 * @return the targetDistanceToNearestBot
	 */
	public double getTargetDistanceToNearestBot()
	{
		return this.targetDistanceToNearestBot;
	}

	/**
	 * @param targetDistanceToNearestBot
	 *            the targetDistanceToNearestBot to set
	 */
	public void setTargetDistanceToNearestBot(double targetDistanceToNearestBot)
	{
		this.targetDistanceToNearestBot = targetDistanceToNearestBot;
	}

	/**
	 * @return the targetDchangeTime
	 */
	public long getTargetDchangeTime()
	{
		return this.targetDchangeTime;
	}

	/**
	 * @param targetDchangeTime
	 *            the targetDchangeTime to set
	 */
	public void setTargetDchangeTime(long targetDchangeTime)
	{
		this.targetDchangeTime = targetDchangeTime;
	}

	/**
	 * @return the targetVchangeTime
	 */
	public long getTargetVchangeTime()
	{
		return this.targetVchangeTime;
	}

	/**
	 * @param targetVchangeTime
	 *            the targetVchangeTime to set
	 */
	public void setTargetVchangeTime(long targetVchangeTime)
	{
		this.targetVchangeTime = targetVchangeTime;
	}

	/**
	 * @return the targetWallDistance
	 */
	public double getTargetWallDistance()
	{
		return this.targetWallDistance;
	}

	/**
	 * @param targetWallDistance
	 *            the targetWallDistance to set
	 */
	public void setTargetWallDistance(double targetWallDistance)
	{
		this.targetWallDistance = targetWallDistance;
	}

	/**
	 * @return the targetRevWallDistance
	 */
	public double getTargetRevWallDistance()
	{
		return this.targetRevWallDistance;
	}

	/**
	 * @param targetRevWallDistance
	 *            the targetRevWallDistance to set
	 */
	public void setTargetRevWallDistance(double targetRevWallDistance)
	{
		this.targetRevWallDistance = targetRevWallDistance;
	}

	/**
	 * @return the targetDl8t
	 */
	public double getTargetDl8t()
	{
		return this.targetDl8t;
	}

	/**
	 * @param targetDl8t
	 *            the targetDl8t to set
	 */
	public void setTargetDl8t(double targetDl8t)
	{
		this.targetDl8t = targetDl8t;
	}

	/**
	 * @return the targetDl20t
	 */
	public double getTargetDl20t()
	{
		return this.targetDl20t;
	}

	/**
	 * @param targetDl20t
	 *            the targetDl20t to set
	 */
	public void setTargetDl20t(double targetDl20t)
	{
		this.targetDl20t = targetDl20t;
	}

	/**
	 * @return the targetDl40t
	 */
	public double getTargetDl40t()
	{
		return this.targetDl40t;
	}

	/**
	 * @param targetDl40t
	 *            the targetDl40t to set
	 */
	public void setTargetDl40t(double targetDl40t)
	{
		this.targetDl40t = targetDl40t;
	}

	/**
	 * @return the targetEnergy
	 */
	public double getTargetEnergy()
	{
		return this.targetEnergy;
	}

	/**
	 * @param targetEnergy
	 *            the targetEnergy to set
	 */
	public void setTargetEnergy(double targetEnergy)
	{
		this.targetEnergy = targetEnergy;
	}

	/**
	 * @return the sourceEnergy
	 */
	public double getSourceEnergy()
	{
		return this.sourceEnergy;
	}

	/**
	 * @param sourceEnergy
	 *            the sourceEnergy to set
	 */
	public void setSourceEnergy(double sourceEnergy)
	{
		this.sourceEnergy = sourceEnergy;
	}

	/**
	 * @return the gunHeat
	 */
	public double getGunHeat()
	{
		return this.gunHeat;
	}

	/**
	 * @param gunHeat
	 *            the gunHeat to set
	 */
	public void setGunHeat(double gunHeat)
	{
		this.gunHeat = gunHeat;
	}

	/**
	 * @return the enemiesAlive
	 */
	public int getEnemiesAlive()
	{
		return this.enemiesAlive;
	}

	/**
	 * @param enemiesAlive
	 *            the enemiesAlive to set
	 */
	public void setEnemiesAlive(int enemiesAlive)
	{
		this.enemiesAlive = enemiesAlive;
	}

	/**
	 * @return the lastBulletFiredTime
	 */
	public long getLastBulletFiredTime()
	{
		return this.lastBulletFiredTime;
	}

	/**
	 * @param lastBulletFiredTime
	 *            the lastBulletFiredTime to set
	 */
	public void setLastBulletFiredTime(long lastBulletFiredTime)
	{
		this.lastBulletFiredTime = lastBulletFiredTime;
	}

	/**
	 * @return the shadows
	 */
	public List<BulletShadow> getShadows()
	{
		return this.shadows;
	}

	/**
	 * @param shadows
	 *            the shadows to set
	 */
	public void setShadows(List<BulletShadow> shadows)
	{
		this.shadows = shadows;
	}

	/**
	 * @return the cachedPositiveEscapeAngle
	 */
	public Double getCachedPositiveEscapeAngle()
	{
		return this.cachedPositiveEscapeAngle;
	}

	/**
	 * @param cachedPositiveEscapeAngle
	 *            the cachedPositiveEscapeAngle to set
	 */
	public void setCachedPositiveEscapeAngle(Double cachedPositiveEscapeAngle)
	{
		this.cachedPositiveEscapeAngle = cachedPositiveEscapeAngle;
	}

	/**
	 * @return the cachedNegativeEscapeAngle
	 */
	public Double getCachedNegativeEscapeAngle()
	{
		return this.cachedNegativeEscapeAngle;
	}

	/**
	 * @param cachedNegativeEscapeAngle
	 *            the cachedNegativeEscapeAngle to set
	 */
	public void setCachedNegativeEscapeAngle(Double cachedNegativeEscapeAngle)
	{
		this.cachedNegativeEscapeAngle = cachedNegativeEscapeAngle;
	}

	/**
	 * @return the usedNegativeSmoothingMea
	 */
	public boolean isUsedNegativeSmoothingMea()
	{
		return this.usedNegativeSmoothingMea;
	}

	/**
	 * @param usedNegativeSmoothingMea
	 *            the usedNegativeSmoothingMea to set
	 */
	public void setUsedNegativeSmoothingMea(boolean usedNegativeSmoothingMea)
	{
		this.usedNegativeSmoothingMea = usedNegativeSmoothingMea;
	}

	/**
	 * @return the usedPositiveSmoothingMea
	 */
	public boolean isUsedPositiveSmoothingMea()
	{
		return this.usedPositiveSmoothingMea;
	}

	/**
	 * @param usedPositiveSmoothingMea
	 *            the usedPositiveSmoothingMea to set
	 */
	public void setUsedPositiveSmoothingMea(boolean usedPositiveSmoothingMea)
	{
		this.usedPositiveSmoothingMea = usedPositiveSmoothingMea;
	}

	/**
	 * @return the absBearing
	 */
	public double getAbsBearing()
	{
		return this.absBearing;
	}

	/**
	 * @return the bulletPower
	 */
	public double getBulletPower()
	{
		return this.bulletPower;
	}
}