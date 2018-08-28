package voidious.gun;

import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

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

class GunDataManager extends EnemyDataManager<GunEnemy>
{
	private static final String WARNING_BULLET_HIT_UNKNOWN = "I shot a bot that I never knew existed!";
	private static final String WARNING_BULLET_HIT_BULLET_UNKNOWN = "I shot a bullet by a bot that I never knew existed!";

	private List<GunDataListener> listeners;
	private long lastBulletFiredTime;

	GunDataManager(int enemiesTotal, BattleField battleField, MovementPredictor predictor)
	{
		super(enemiesTotal, battleField, predictor);
		this.listeners = new ArrayList<>();
	}

	@Override
	public void initRound()
	{
		super.initRound();
		this.lastBulletFiredTime = 0;
	}

	void execute(long currentTime, double currentGunHeat, Point2D.Double myLocation,
				 boolean is1v1)
	{
		this.updateBotDistances(myLocation);
		this.getAllEnemyData().stream().filter(Enemy::isAlive)
				.forEach(gunData -> gunData.execute(currentTime, this.lastBulletFiredTime, currentGunHeat, myLocation, is1v1,
                this.getEnemiesTotal(), this.listeners));
	}

	GunEnemy newEnemy(ScannedRobotEvent e, Point2D.Double enemyLocation, double absBearing, int currentRound, boolean is1v1)
	{
		if (this.hasEnemy(e.getName()))
		{
			throw new IllegalArgumentException("GunEnemy already exists for bot: " + e.getName());
		}

		GunEnemy gunData = new GunEnemy(e.getName(), e.getDistance(), e.getEnergy(), enemyLocation, currentRound, e.getTime(),
				e.getHeadingRadians(), e.getVelocity(), absBearing, this.getBattleField(), this.getPredictor());

		gunData.updateTimers(e.getVelocity());
		this.saveEnemy(e.getName(), gunData);

		if (is1v1)
		{
			this.setDuelEnemy(gunData);
		}
		return gunData;
	}

	private void saveEnemy(String botName, GunEnemy gunData)
	{
		this.getEnemies().put(botName, gunData);
	}

	GunEnemy updateEnemy(ScannedRobotEvent e, Point2D.Double enemyLocation, double absBearing, int currentRound,
			boolean is1v1)
	{
		GunEnemy gunData = this.getEnemyData(e.getName());
		long timeSinceLastScan = e.getTime() - gunData.getLastScanState().getTime();
		gunData.setTimeSinceDirectionChange(gunData.getTimeSinceDirectionChange() + timeSinceLastScan);
		gunData.setTimeSinceVelocityChange(gunData.getTimeSinceVelocityChange() + timeSinceLastScan);

		gunData.updateTimers(e.getVelocity());
		gunData.setDistance(e.getDistance());
		gunData.setAbsBearing(absBearing);
		gunData.setEnergy(e.getEnergy());
		gunData.setLastScanRound(currentRound);
		gunData.setPreviousVelocity(gunData.getLastScanState().getVelocity());
		gunData.setRobotState(new RobotState(enemyLocation, e.getHeadingRadians(), e.getVelocity(), e.getTime(), false));
		gunData.setTimeAliveTogether(gunData.getTimeAliveTogether() + 1);
		if (gunData.advancingVelocity() > 6)
		{
			gunData.setTimeMovingAtMe(gunData.getTimeMovingAtMe() + 1);
		}
		if (is1v1)
		{
			this.setDuelEnemy(gunData);
		}
		return gunData;
	}

	void onBulletHit(BulletHitEvent e, long currentTime)
	{
		String botName = e.getName();
		if (this.hasEnemy(botName))
		{
			GunEnemy gunData = this.getEnemyData(e.getName());
			double bulletDamage = Math.min(Rules.getBulletDamage(e.getBullet().getPower()), gunData.getEnergy());
			gunData.setDamageGiven(gunData.getDamageGiven() + bulletDamage);
			gunData.logBulletHitLocation(e.getBullet());
		}
		else
		{
			System.out.println(this.warning() + WARNING_BULLET_HIT_UNKNOWN + " (" + botName + ")");
		}
		for (GunEnemy gunData : this.getAllEnemyData())
		{
			Wave hitWave = gunData.processBulletHit(e.getBullet(), currentTime, (this.getEnemiesTotal() == 1), true);
			if (hitWave != null)
			{
				hitWave.setHitByBullet(true);
			}
		}
	}

	void onBulletHitBullet(BulletHitBulletEvent e, long currentTime)
	{
		String botName = e.getHitBullet().getName();
		for (GunEnemy gunData : this.getAllEnemyData())
		{
			Wave hitWave = gunData.processBulletHit(e.getBullet(), currentTime, (this.getEnemiesTotal() == 1),
					botName.equals(gunData.getBotName()));
			if (hitWave != null)
			{
				hitWave.setBulletHitBullet(true);
			}
		}
		if (!this.hasEnemy(botName) && !botName.equals(e.getBullet().getName()))
		{
			System.out.println(this.warning() + WARNING_BULLET_HIT_BULLET_UNKNOWN + " (" + botName + ")");
		}
	}

	double getAverageEnergy()
	{
		double totalEnergy = 0;
		int enemiesAlive = 0;
		for (GunEnemy gunData : this.getAllEnemyData())
		{
			if (gunData.isAlive())
			{
				totalEnergy += gunData.getEnergy();
				enemiesAlive++;
			}
		}
		return (enemiesAlive == 0) ? 0 : totalEnergy / enemiesAlive;
	}

	void markFiringWaves(long currentTime, boolean is1v1)
	{
		this.lastBulletFiredTime = currentTime;
		for (GunEnemy gunData : this.getAllEnemyData())
		{
			gunData.markFiringWaves(currentTime, is1v1, this.listeners);
		}
	}

	void fireNextTickWave(Point2D.Double myNextLocation, Point2D.Double targetLocation, String targetName,
			int currentRound, long currentTime, double bulletPower, double myEnergy, double gunHeat, double myHeading,
			double myVelocity, int enemiesAlive)
	{
		GunEnemy gunData = this.getEnemyData(targetName);
		RobotState lastScanState = gunData.getLastScanState();
		Point2D.Double enemyNextLocation = this.getBattleField().translateToField(
				this.getPredictor().nextLocation(targetLocation, lastScanState.getHeading(), lastScanState.getVelocity()));
		double accel = DiaUtils.limit(-Rules.DECELERATION,
				DiaUtils.accel(lastScanState.getVelocity(), gunData.getPreviousVelocity()), Rules.ACCELERATION);
		long fireTime = currentTime + 1;
		long lastWaveFireTime = gunData.getLastWaveFireTime();
		RobotStateLog stateLog = gunData.getStateLog();
		double dl8t = stateLog.getDisplacementDistance(targetLocation, currentTime, 8);
		double dl20t = stateLog.getDisplacementDistance(targetLocation, currentTime, 20);
		double dl40t = stateLog.getDisplacementDistance(targetLocation, currentTime, 40);

		Wave nextWave = gunData.newGunWave(myNextLocation, enemyNextLocation, currentRound, fireTime, this.lastBulletFiredTime,
				bulletPower, myEnergy, gunHeat, enemiesAlive, accel, dl8t, dl20t, dl40t, false);
		gunData.setLastWaveFired(nextWave);

		this.getAllEnemyData().stream().filter(altGunData -> altGunData.isAlive() && !altGunData.getBotName().equals(targetName)).forEach(altGunData -> {
			Point2D.Double altNextLocation = this.getBattleField().translateToField(
					this.getPredictor().nextLocation(altGunData.getLastScanState().getLocation(),
							altGunData.getLastScanState().getVelocity(), altGunData.getLastScanState().getHeading()));
			gunData.newGunWave(altNextLocation, enemyNextLocation, currentRound, fireTime, this.lastBulletFiredTime,
					bulletPower, altGunData.getEnergy(), gunHeat, enemiesAlive, accel, dl8t, dl20t, dl40t, true);
		});

		if (this.getDuelEnemy() != null && lastWaveFireTime > 0)
		{
			for (long time = lastWaveFireTime + 1; time < fireTime; time++)
			{
				gunData.interpolateGunWave(time, myHeading, myVelocity, lastScanState);
			}
		}
	}

	void addListener(GunDataListener listener)
	{
		this.listeners.add(listener);
	}

	int getDuelDataSize()
	{
		return this.getDuelEnemy().getWaveBreaks();
	}

	@Override
	protected String getLabel()
	{
		return "gun";
	}
}