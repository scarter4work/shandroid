package voidious.move;

import ags.utils.dataStructures.Entry;
import robocode.AdvancedRobot;
import robocode.Rules;
import robocode.util.Utils;
import voidious.enums.WavePosition;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * Copyright (c) 2012 - Voidious
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

class SurfMover
{
	private static final double WALL_STICK = 160;
	private static final double MEA_WALL_STICK = 100;
	private static final double DISTANCING_DANGER_BASE = 2.5;
	private static final double BASE_DANGER_FACTOR = 1.0;

	private AdvancedRobot robot;
	private BattleField battleField;
	private SurfOption _lastSurfOption = SurfOption.CLOCKWISE;
	private Point2D.Double lastSurfDestination;
	private Point2D.Double stopDestination;
	private Map<SurfOption, Double> surfOptionDangers;
	private Map<SurfOption, Point2D.Double> surfOptionDestinations;
	private DistanceController distancer;
	private Wave lastWaveSurfed;
	private MovementPredictor predictor;

	SurfMover(AdvancedRobot robot, BattleField battleField)
	{
		this.robot = robot;
		this.battleField = battleField;
		this.predictor = new MovementPredictor(battleField);
		this.surfOptionDangers = new HashMap<>();
		this.surfOptionDestinations = new HashMap<>();
		this.distancer = new DistanceController();
	}

	public void initRound()
	{
		this.lastSurfDestination = null;
		this.stopDestination = null;
	}

	public void move(RobotState myRobotState, MoveEnemy duelEnemy, int wavesToSurf)
	{
		if (duelEnemy == null)
		{
			return;
		}

		Point2D.Double myLocation = myRobotState.getLocation();
		Wave surfWave = duelEnemy.findSurfableWave(Wave.FIRST_WAVE, myRobotState);

		if (surfWave == null)
		{
			this.orbit(myLocation, duelEnemy);
		}
		else
		{
			this.surf(myRobotState, duelEnemy, surfWave, wavesToSurf);
		}
	}

	private void orbit(Point2D.Double myLocation, MoveEnemy duelEnemy)
	{
		this.robot.setMaxVelocity(8);
		RobotState enemyState = duelEnemy.getLastScanState();
		double orbitAbsBearing = DiaUtils.absoluteBearing(enemyState.getLocation(), myLocation);
		double retreatAngle = this.distancer.orbitAttackAngle(myLocation.distance(enemyState.getLocation()));
		double counterGoAngle = orbitAbsBearing + (SurfOption.COUNTER_CLOCKWISE.getDirection() * ((Math.PI / 2) + retreatAngle));
		counterGoAngle = this.wallSmoothing(myLocation, counterGoAngle, SurfOption.COUNTER_CLOCKWISE);

		double clockwiseGoAngle = orbitAbsBearing + (SurfOption.CLOCKWISE.getDirection() * ((Math.PI / 2) + retreatAngle));
		clockwiseGoAngle = this.wallSmoothing(myLocation, clockwiseGoAngle, SurfOption.CLOCKWISE);

		double goAngle;
		if (Math.abs(Utils.normalRelativeAngle(clockwiseGoAngle - orbitAbsBearing)) < Math.abs(Utils
				.normalRelativeAngle(counterGoAngle - orbitAbsBearing)))
		{
			this._lastSurfOption = SurfOption.CLOCKWISE;
			goAngle = clockwiseGoAngle;
		}
		else
		{
			this._lastSurfOption = SurfOption.COUNTER_CLOCKWISE;
			goAngle = counterGoAngle;
		}

		DiaUtils.setBackAsFront(this.robot, goAngle);
	}

	private void surf(RobotState myRobotState, MoveEnemy duelEnemy, Wave surfWave, int wavesToSurf)
	{
		if (surfWave != this.lastWaveSurfed)
		{
			duelEnemy.clearNeighborCache();
			this.lastWaveSurfed = surfWave;
			this.lastSurfDestination = null;
			this.stopDestination = null;
		}

		boolean goingClockwise = (this._lastSurfOption == SurfOption.CLOCKWISE);
		this.updateSurfDangers(myRobotState, duelEnemy, wavesToSurf, goingClockwise);
		double counterDanger = this.surfOptionDangers.get(SurfOption.COUNTER_CLOCKWISE);
		double stopDanger = this.surfOptionDangers.get(SurfOption.STOP);
		double clockwiseDanger = this.surfOptionDangers.get(SurfOption.CLOCKWISE);

		Point2D.Double surfDestination;
		if (stopDanger <= counterDanger && stopDanger <= clockwiseDanger)
		{
			if (this.stopDestination == null)
			{
				this.stopDestination = this.surfOptionDestinations.get(this._lastSurfOption);
			}
			surfDestination = this.stopDestination;
			this.robot.setMaxVelocity(0);
			this.lastSurfDestination = null;
		}
		else
		{
			this.robot.setMaxVelocity(8);
			this._lastSurfOption = (clockwiseDanger < counterDanger) ? SurfOption.CLOCKWISE : SurfOption.COUNTER_CLOCKWISE;
			surfDestination = this.surfOptionDestinations.get(this._lastSurfOption);
			this.lastSurfDestination = surfDestination;
			this.stopDestination = null;
		}

		double goAngle = DiaUtils.absoluteBearing(myRobotState.getLocation(), surfDestination);
		goAngle = this.wallSmoothing(myRobotState.getLocation(), goAngle, this._lastSurfOption);

		DiaUtils.setBackAsFront(this.robot, goAngle);
	}

	private void updateSurfDangers(RobotState myRobotState, MoveEnemy duelEnemy, int wavesToSurf, boolean goingClockwise)
	{
		List<SurfOption> surfOptions = this.getSortedSurfOptions();
		double bestSurfDanger = Double.POSITIVE_INFINITY;
		for (SurfOption testOption : surfOptions)
		{
			double testDanger = this.checkDanger(myRobotState, duelEnemy, myRobotState, testOption, goingClockwise,
					Wave.FIRST_WAVE, wavesToSurf, bestSurfDanger, new RobotStateLog());
			this.surfOptionDangers.put(testOption, testDanger);
			bestSurfDanger = Math.min(bestSurfDanger, testDanger);
		}
	}

	private List<SurfOption> getSortedSurfOptions()
	{
		List<SurfOption> surfOptions = Arrays.asList(SurfOption.values());
		for (int x = 0; x < surfOptions.size(); x++)
		{
			double lowestDanger = this.getSurfOptionDanger(surfOptions.get(x));
			for (int y = x + 1; y < surfOptions.size(); y++)
			{
				if (this.getSurfOptionDanger(surfOptions.get(y)) < lowestDanger)
				{
					lowestDanger = this.getSurfOptionDanger(surfOptions.get(y));
					SurfOption temp = surfOptions.get(x);
					surfOptions.set(x, surfOptions.get(y));
					surfOptions.set(y, temp);
				}
			}
		}
		return surfOptions;
	}

	private double getSurfOptionDanger(SurfOption surfOption)
	{
		if (this.surfOptionDangers.containsKey(surfOption))
		{
			return this.surfOptionDangers.get(surfOption);
		}
		return 0;
	}

	// TODO: make previouslyMovingClockwise either SurfOption or int
	private double checkDanger(RobotState myRobotState, MoveEnemy duelEnemy, RobotState startState, SurfOption surfOption,
			boolean previouslyMovingClockwise, int surfWaveIndex, int numWavesToSurf, double cutoffDanger,
			RobotStateLog predictedStateLog)
	{
		Wave surfWave = duelEnemy.findSurfableWave(surfWaveIndex, myRobotState);
		if (surfWave == null)
		{
			return 0;
		}

		List<RobotState> dangerStates = new ArrayList<>();
		WavePosition startWavePosition = surfWave.checkWavePosition(startState);
		if (surfWaveIndex > Wave.FIRST_WAVE && startWavePosition != WavePosition.MIDAIR)
		{
			dangerStates.addAll(this.replaySurfStates(surfWave, predictedStateLog));
		}

		if (startWavePosition == WavePosition.GONE && dangerStates.isEmpty())
		{
			return 0;
		}

		boolean predictClockwise = this.predictClockwise(surfOption, previouslyMovingClockwise);

		RobotState predictedState = startState;
		RobotState passedState = startState;

		boolean wavePassed = false;
		boolean waveHit = false;
		double maxVelocity;
		SurfOption smoothingSurfOption;
		if (surfOption == SurfOption.STOP)
		{
			maxVelocity = 0;
			smoothingSurfOption = predictClockwise ? SurfOption.CLOCKWISE : SurfOption.COUNTER_CLOCKWISE;
		}
		else
		{
			maxVelocity = 8;
			smoothingSurfOption = surfOption;
		}
		Point2D.Double surfDestination;
		if (surfWaveIndex == Wave.FIRST_WAVE && surfOption == SurfOption.STOP && this.stopDestination != null)
		{
			surfDestination = this.stopDestination;
		}
		else
		{
			surfDestination = this.surfDestination(surfWave, surfWaveIndex, startState, smoothingSurfOption);
		}
		if (surfWaveIndex == Wave.FIRST_WAVE)
		{
			this.surfOptionDestinations.put(surfOption, surfDestination);
		}

		do
		{
			if (!waveHit
					&& surfWave.checkWavePosition(predictedState, WavePosition.BREAKING_FRONT) == WavePosition.BREAKING_FRONT)
			{
				RobotState dangerState = predictedState;
				do
				{
					dangerStates.add(dangerState);
					dangerState = this.predictSurfLocation(dangerState, surfDestination, 0, smoothingSurfOption);
				}
				while (surfWave.checkWavePosition(dangerState, true) != WavePosition.GONE);
				waveHit = true;
			}

			WavePosition wavePosition = surfWave.checkWavePosition(predictedState, true);
			if (wavePosition == WavePosition.BREAKING_CENTER || wavePosition == WavePosition.GONE)
			{
				passedState = predictedState;
				wavePassed = true;
			}
			else
			{
				predictedStateLog.addState(predictedState);
				predictedState = this.predictSurfLocation(predictedState, surfDestination, maxVelocity, smoothingSurfOption);
			}
		}
		while (!wavePassed);

		Intersection intersection = surfWave.preciseIntersection(dangerStates);
		double baseDangerScore = this.normalizedEnemyHitRate(duelEnemy) * BASE_DANGER_FACTOR;
		double danger = baseDangerScore + this.getDangerScore(duelEnemy, surfWave, intersection, surfWaveIndex);
		danger *= surfWave.shadowFactor(intersection);
		danger *= Rules.getBulletDamage(surfWave.getBulletPower());
		double currentDistanceToWaveSource = myRobotState.getLocation().distance(surfWave.getSourceLocation());
		double currentDistanceToWave = currentDistanceToWaveSource - surfWave.distanceTraveled(this.robot.getTime());
		double timeToImpact = Math.max(1, currentDistanceToWave / surfWave.getBulletSpeed());
		danger /= timeToImpact;

		danger *= this.distancingDanger(startState.getLocation(), passedState.getLocation(), duelEnemy.getLastScanState()
				.getLocation());

		if (surfWaveIndex + 1 < numWavesToSurf && danger < cutoffDanger)
		{
			double nextCounterClockwiseDanger = this.checkDanger(myRobotState, duelEnemy, passedState,
					SurfOption.COUNTER_CLOCKWISE, predictClockwise, surfWaveIndex + 1, numWavesToSurf, cutoffDanger,
					(RobotStateLog) predictedStateLog.clone());
			double nextStopDanger = this.checkDanger(myRobotState, duelEnemy, passedState, SurfOption.STOP, predictClockwise,
					surfWaveIndex + 1, numWavesToSurf, cutoffDanger, (RobotStateLog) predictedStateLog.clone());
			double nextClockwiseDanger = this.checkDanger(myRobotState, duelEnemy, passedState, SurfOption.CLOCKWISE,
					predictClockwise, surfWaveIndex + 1, numWavesToSurf, cutoffDanger, (RobotStateLog) predictedStateLog.clone());

			danger += Math.min(nextCounterClockwiseDanger, Math.min(nextStopDanger, nextClockwiseDanger));
		}

		return danger;
	}

	private boolean predictClockwise(SurfOption surfOption, boolean previouslyMovingClockwise)
	{
		if (surfOption == SurfOption.STOP)
		{
			return previouslyMovingClockwise;
		}
		return (surfOption == SurfOption.CLOCKWISE);
	}

	private List<RobotState> replaySurfStates(final Wave surfWave, RobotStateLog predictedStateLog)
	{
		final List<RobotState> dangerStates = new ArrayList<>();
		predictedStateLog.forAllStates(state -> {
            WavePosition pastWavePosition = surfWave.checkWavePosition(state);
            if (pastWavePosition.isBreaking())
            {
                dangerStates.add(state);
            }
        });
		return dangerStates;
	}

	private Point2D.Double surfDestination(Wave surfWave, int surfWaveIndex, RobotState startState, SurfOption surfOption)
	{
		if (surfWaveIndex == Wave.FIRST_WAVE && this._lastSurfOption == surfOption && this.lastSurfDestination != null)
		{
			return this.lastSurfDestination;
		}

		double attackAngle = this.distancer.surfAttackAngle(surfWave.getSourceLocation().distance(startState.getLocation()));
		MaxEscapeTarget meaTarget = this.predictor.preciseEscapeAngle(surfOption.getDirection(), surfWave.getSourceLocation(),
				surfWave.getFireTime(), surfWave.getBulletSpeed(), startState, attackAngle, MEA_WALL_STICK);
		return meaTarget.getLocation();
	}

	private RobotState predictSurfLocation(RobotState robotState, Point2D.Double surfDestination, double maxVelocity,
			SurfOption smoothingSurfOption)
	{
		double goAngle = this.wallSmoothing(robotState.getLocation(),
				DiaUtils.absoluteBearing(robotState.getLocation(), surfDestination), smoothingSurfOption);
		return this.predictor.nextLocation(robotState, maxVelocity, goAngle, false);
	}

	private double wallSmoothing(Point2D.Double startLocation, double goAngleRadians, SurfOption surfOption)
	{
		return this.battleField.wallSmoothing(startLocation, goAngleRadians, surfOption.getDirection(), WALL_STICK);
	}

	private double distancingDanger(Point2D.Double startLocation, Point2D.Double predictedLocation, Point2D.Double enemyLocation)
	{
		double distanceToEnemy = enemyLocation.distance(startLocation);
		double predictedDistanceToEnemy = enemyLocation.distance(predictedLocation);

		double distanceQuotient = distanceToEnemy / predictedDistanceToEnemy;

		return Math.pow(DISTANCING_DANGER_BASE, distanceQuotient) / DISTANCING_DANGER_BASE;
	}

	private double getDangerScore(MoveEnemy duelEnemy, Wave w, Intersection intersection, int surfWaveIndex)
	{
		double dangerAngle = intersection.getAngle();
		double bandwidth = intersection.getBandwidth();
		double totalDanger = 0;
		double totalScanWeight = 0;
		int enabledSize = 0;
		double hitPercentage = this.normalizedEnemyHitPercentage(duelEnemy);
		double marginOfError = this.hitPercentageMarginOfError(duelEnemy);
		for (KnnView<TimestampedGuessFactor> view : duelEnemy.getViews().values())
		{
			if (view.enabled(hitPercentage, marginOfError))
			{
				enabledSize += view.size();
				List<Entry<TimestampedGuessFactor>> nearestNeighbors = this.getNearestNeighbors(view, w, surfWaveIndex);
				Map<Timestamped, Double> weightMap = view.getDecayWeights(nearestNeighbors);

				double density = 0;
				double viewScanWeight = 0;
				for (Entry<TimestampedGuessFactor> nearestNeighbor : nearestNeighbors) {
					TimestampedGuessFactor tsgf = nearestNeighbor.getValue();
					double scanWeight = weightMap.get(tsgf) / Math.sqrt(nearestNeighbor.getDistance());
					double xFiringAngle = DiaUtils.normalizeAngle(w.firingAngle(tsgf.getGuessFactor()), dangerAngle);
					if (!w.shadowed(xFiringAngle)) {
						double ux = (xFiringAngle - dangerAngle) / bandwidth;
						density += scanWeight * Math.pow(2, -Math.abs(ux));
					}
					viewScanWeight += scanWeight;
				}
				totalScanWeight += viewScanWeight * view.weight;
				totalDanger += view.weight * density;
			}
		}

		if (enabledSize == 0)
		{
			return defaultDanger(w, intersection);
		}

		return totalDanger / totalScanWeight;
	}

	private List<Entry<TimestampedGuessFactor>> getNearestNeighbors(KnnView<TimestampedGuessFactor> view, Wave w,
			int surfWaveIndex)
	{
		if (!view.cachedNeighbors.containsKey(surfWaveIndex))
		{
			view.cachedNeighbors.put(surfWaveIndex, view.nearestNeighbors(w, false));
		}
		return view.cachedNeighbors.get(surfWaveIndex);
	}

	// TODO: move these to MoveEnemy
	private double normalizedEnemyHitRate(MoveEnemy duelEnemy)
	{
		return (duelEnemy == null || duelEnemy.getRaw1v1ShotsFired() == 0) ? 0
				: ((duelEnemy.getWeighted1v1ShotsHit()) / duelEnemy.getRaw1v1ShotsFired());
	}

	private double normalizedEnemyHitPercentage(MoveEnemy duelEnemy)
	{
		return 100 * this.normalizedEnemyHitRate(duelEnemy);
	}

	private double rawEnemyHitPercentage(MoveEnemy duelEnemy)
	{
		return (duelEnemy == null || duelEnemy.getRaw1v1ShotsFired() == 0) ? 0
				: ((((double) duelEnemy.getRaw1v1ShotsHit()) / duelEnemy.getRaw1v1ShotsFired()) * 100.0);
	}

	private double hitPercentageMarginOfError(MoveEnemy duelEnemy)
	{
		if (duelEnemy == null)
		{
			return 100;
		}
		double hitProbability = this.normalizedEnemyHitRate(duelEnemy);
		return 100 * DiaUtils.marginOfError(hitProbability, duelEnemy.getRaw1v1ShotsFired());
	}

	private static double defaultDanger(Wave w, Intersection intersection)
	{
		double[] guessFactors = new double[] { 0, 0.85 };
		double[] weights = new double[] { 3, 1 };
		double danger = 0;
		for (int x = 0; x < guessFactors.length; x++)
		{
			double firingAngle = w.firingAngle(guessFactors[x]);
			double ux = (firingAngle - DiaUtils.normalizeAngle(intersection.getAngle(), firingAngle))
					/ intersection.getBandwidth();
			danger += weights[x] * Math.pow(2, -Math.abs(ux));
		}
		return danger;
	}

	void roundOver(MoveEnemy duelEnemy)
	{
		System.out.println("Enemy normalized hit %: " + DiaUtils.round(this.normalizedEnemyHitPercentage(duelEnemy), 2) + "\n"
				+ "Enemy raw hit %: " + DiaUtils.round(this.rawEnemyHitPercentage(duelEnemy), 2));
	}

	private enum SurfOption
	{
		COUNTER_CLOCKWISE(-1), STOP(0), CLOCKWISE(1);

		private int _direction;

		SurfOption(int direction)
		{
			this._direction = direction;
		}

		public int getDirection()
		{
			return this._direction;
		}
	}
}