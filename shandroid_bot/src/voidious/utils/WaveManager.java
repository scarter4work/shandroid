package voidious.utils;

import voidious.enums.WavePosition;
import voidious.gun.AllWaveListener;
import voidious.gun.CurrentWaveListener;
import voidious.gun.WaveBreakListener;

import java.awt.geom.Point2D;
import java.util.*;

import static voidious.utils.DiaUtils.square;

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

public class WaveManager
{
	private static final int WAVE_MATCH_THRESHOLD = 50;
	private static final double COOLING_RATE = 0.1;
	private static final double MAX_GUN_HEAT = 1.6;

	private List<Wave> waves;
	private Map<Wave, RobotStateLog> stateLogs;

	public WaveManager()
	{
		this.waves = new ArrayList<>();
		this.stateLogs = new HashMap<>();
	}

	public void initRound()
	{
		this.waves.clear();
		this.stateLogs.clear();
	}

	public void addWave(Wave wave)
	{
		this.waves.add(wave);
	}

	public void checkCurrentWaves(long currentTime, CurrentWaveListener listener)
	{
		this.waves.stream().filter(w -> w.getFireTime() == currentTime).forEach(listener::onCurrentWave);
	}

	public void forAllWaves(AllWaveListener listener)
	{
		this.waves.forEach(listener::onWave);
	}

	public void checkActiveWaves(long currentTime, RobotState lastScanState, WaveBreakListener listener)
	{
		if (lastScanState.getTime() == currentTime)
		{
			Iterator<Wave> wavesIterator = this.waves.iterator();
			while (wavesIterator.hasNext())
			{
				Wave w = wavesIterator.next();
				this.addRobotState(w, lastScanState);
				if (w.checkWavePosition(lastScanState) == WavePosition.GONE)
				{
					List<RobotState> waveBreakStates = this.getWaveBreakStates(w, currentTime);
					listener.onWaveBreak(w, waveBreakStates);
					wavesIterator.remove();
				}
			}
		}
	}

	private void addRobotState(Wave w, RobotState state)
	{
		RobotStateLog stateLog;
		if (this.stateLogs.containsKey(w))
		{
			stateLog = this.stateLogs.get(w);
		}
		else
		{
			stateLog = new RobotStateLog();
			this.stateLogs.put(w, stateLog);
		}
		stateLog.addState(state);
	}

	private List<RobotState> getWaveBreakStates(Wave w, long currentTime)
	{
		List<RobotState> waveBreakStates = new ArrayList<>();
		if (this.stateLogs.containsKey(w))
		{
			RobotStateLog log = this.stateLogs.get(w);
			for (long time = w.getFireTime(); time < currentTime; time++)
			{
				RobotState state = log.getState(time);
				if (state != null && w.checkWavePosition(state).isBreaking())
				{
					waveBreakStates.add(state);
				}
			}
		}
		return waveBreakStates;
	}

	public Wave findClosestWave(Point2D.Double targetLocation, long currentTime, boolean onlyFiring, String botName,
			double bulletPower)
	{
		double closestDistance = Double.POSITIVE_INFINITY;
		Wave closestWave = null;

		for (Wave w : this.waves)
		{
			if (!w.isAltWave() && (!onlyFiring || w.isFiringWave())
					&& (bulletPower == Wave.ANY_BULLET_POWER || Math.abs(bulletPower - w.getBulletPower()) < 0.001)
					&& (botName == null || botName.equals(w.getBotName()) || botName.equals("")))
			{
				double targetDistanceSq = w.getSourceLocation().distanceSq(targetLocation);
				double waveDistanceTraveled = w.distanceTraveled(currentTime);
				if (targetDistanceSq < square(waveDistanceTraveled + WAVE_MATCH_THRESHOLD)
						&& targetDistanceSq > square(Math.max(0, waveDistanceTraveled - WAVE_MATCH_THRESHOLD)))
				{
					double distanceFromTargetToWave = Math.sqrt(targetDistanceSq) - waveDistanceTraveled;
					if (Math.abs(distanceFromTargetToWave) < closestDistance)
					{
						closestDistance = Math.abs(distanceFromTargetToWave);
						closestWave = w;
					}
				}
			}
		}

		return closestWave;
	}

	public Wave findSurfableWave(int surfIndex, RobotState targetState, WavePosition unsurfablePosition)
	{
		int searchWaveIndex = 0;

		for (Wave w : this.waves)
		{
			if (w.isFiringWave() && !w.processedBulletHit())
			{
				WavePosition wavePosition = w.checkWavePosition(targetState, unsurfablePosition);
				if (wavePosition.getIndex() < unsurfablePosition.getIndex())
				{
					if (searchWaveIndex == surfIndex)
					{
						return w;
					}

					searchWaveIndex++;
				}
			}
		}

		return null;
	}

	public Wave getWaveByFireTime(long fireTime)
	{
		for (Wave w : this.waves)
		{
			if (w.getFireTime() == fireTime)
			{
				return w;
			}
		}
		return null;
	}

	public Wave interpolateWaveByFireTime(long fireTime, double sourceHeading, double sourceVelocity,
										  RobotStateLog stateLog, BattleField battleField, MovementPredictor predictor)
	{
		Wave beforeWave = null;
		Wave afterWave = null;
		for (Wave w : this.waves)
		{
			if (!w.isAltWave())
			{
				if (w.getFireTime() < fireTime)
				{
					if (beforeWave == null || w.getFireTime() > beforeWave.getFireTime())
					{
						beforeWave = w;
					}
				}
				if (w.getFireTime() > fireTime)
				{
					if (afterWave == null || w.getFireTime() < afterWave.getFireTime())
					{
						afterWave = w;
					}
				}
			}
		}

		if (beforeWave == null && afterWave == null)
		{
			return null;
		}
		else if (beforeWave == null)
		{
			return this
					.interpolateWave(afterWave, fireTime - afterWave.getFireTime(), sourceHeading, sourceVelocity, battleField);
		}
		else if (afterWave == null)
		{
			return this.interpolateWave(beforeWave, fireTime - beforeWave.getFireTime(), sourceHeading, sourceVelocity,
					battleField);
		}
		else
		{
			return this.interpolateWave(beforeWave, afterWave, fireTime, stateLog, battleField, predictor);
		}
	}

	private Wave interpolateWave(Wave baseWave1, Wave baseWave2, long fireTime, RobotStateLog stateLog, BattleField battleField,
			MovementPredictor predictor)
	{
		Interpolator interpolator = new Interpolator(fireTime, baseWave1.getFireTime(), baseWave2.getFireTime());

		Point2D.Double sourceLocation = interpolator.getLocation(baseWave1.getSourceLocation(), baseWave2.getSourceLocation());
		Point2D.Double targetLocation = interpolator.getLocation(baseWave1.getTargetLocation(), baseWave2.getTargetLocation());
		double bulletPower = interpolator.avg(baseWave1.getBulletPower(), baseWave2.getBulletPower());
		double targetHeading = interpolator.getHeading(baseWave1.getTargetHeading(), baseWave2.getTargetHeading());
		double targetVelocity = interpolator.avg(baseWave1.getTargetVelocity(), baseWave2.getTargetVelocity());
		int targetVelocitySign;

		if (DiaUtils.nonZeroSign(targetVelocity) == DiaUtils.nonZeroSign(baseWave1.getTargetVelocity()))
		{
			targetVelocitySign = baseWave1.getTargetVelocitySign();
		}
		else
		{
			targetVelocitySign = baseWave2.getTargetVelocitySign();
		}

		// TODO: not sure this is the best way to interpolate accel
		double targetAccel = interpolator.avg(baseWave1.getTargetAccel(), baseWave2.getTargetAccel());
		long targetDchangeTime = interpolator.getTimer(baseWave1.getTargetDchangeTime(), baseWave2.getTargetDchangeTime());
		long targetVchangeTime = interpolator.getTimer(baseWave1.getTargetVchangeTime(), baseWave2.getTargetVchangeTime());
		double targetDl8t = stateLog.getDisplacementDistance(targetLocation, fireTime, 8);
		double targetDl20t = stateLog.getDisplacementDistance(targetLocation, fireTime, 20);
		double targetDl40t = stateLog.getDisplacementDistance(targetLocation, fireTime, 40);
		double targetEnergy = interpolator.avg(baseWave1.getTargetEnergy(), baseWave2.getTargetEnergy());
		double sourceEnergy = interpolator.avg(baseWave1.getSourceEnergy(), baseWave2.getSourceEnergy());

		long lastBulletFiredTime = baseWave1.getLastBulletFiredTime();
		double gunHeat = baseWave1.getGunHeat() - ((fireTime - baseWave1.getFireTime()) * COOLING_RATE);
		if (gunHeat <= 0)
		{
			lastBulletFiredTime = baseWave1.getFireTime() + (long) Math.ceil(baseWave1.getGunHeat() / COOLING_RATE);
			gunHeat = (baseWave2.getGunHeat() + ((baseWave2.getFireTime() - fireTime) * COOLING_RATE)) % MAX_GUN_HEAT;
		}

		Wave newWave = new Wave(baseWave1.getBotName(), sourceLocation, targetLocation, baseWave1.getFireRound(), fireTime,
				bulletPower, targetHeading, targetVelocity, targetVelocitySign, battleField, predictor);
		newWave.setAbsBearing(DiaUtils.absoluteBearing(sourceLocation, targetLocation));
		newWave.setTargetAccel(targetAccel);
		newWave.setTargetDistance(sourceLocation.distance(targetLocation));
		newWave.setTargetDchangeTime(targetDchangeTime);
		newWave.setTargetVchangeTime(targetVchangeTime);
		newWave.setTargetDl8t(targetDl8t);
		newWave.setTargetDl20t(targetDl20t);
		newWave.setTargetDl40t(targetDl40t);
		newWave.setTargetEnergy(targetEnergy);
		newWave.setSourceEnergy(sourceEnergy);
		newWave.setGunHeat(gunHeat);
		newWave.setEnemiesAlive(baseWave2.getEnemiesAlive());
		newWave.setLastBulletFiredTime(lastBulletFiredTime);
		return newWave;
	}

	private Wave interpolateWave(Wave baseWave, long timeOffset, double sourceHeading, double sourceVelocity,
			BattleField battleField)
	{
		Wave w = (Wave) baseWave.clone();
		w.setSourceLocation(battleField.translateToField(DiaUtils.project(baseWave.getSourceLocation(), sourceHeading,
				sourceVelocity * timeOffset)));
		w.setTargetLocation(battleField.translateToField(DiaUtils.project(baseWave.getTargetLocation(),
				baseWave.getTargetHeading(), baseWave.getTargetVelocity() * timeOffset)));
		w.setAbsBearing(DiaUtils.absoluteBearing(w.getSourceLocation(), w.getTargetLocation()));
		w.setFireTime(w.getFireTime() + timeOffset);
		w.setTargetDistance(w.getSourceLocation().distance(w.getTargetLocation()));
		return w;
	}

	public long getLastFireTime()
	{
		long lastFireTime = -1;
		for (Wave w : this.waves)
		{
			if (!w.isAltWave() && w.getFireTime() > lastFireTime)
			{
				lastFireTime = w.getFireTime();
			}
		}
		return lastFireTime;
	}

	public int size()
	{
		return this.waves.size();
	}
}