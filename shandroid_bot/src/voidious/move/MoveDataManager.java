package voidious.move;

import robocode.*;
import robocode.util.Utils;
import voidious.gun.FiredBullet;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
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

class MoveDataManager extends EnemyDataManager<MoveEnemy>
{
	private static final double NON_ZERO_VELOCITY_THRESHOLD = 0.1;
	private static final double DIRECTION_CHANGE_THRESHOLD = Math.PI / 2;
	private static final long EARLIEST_FIRE_TIME = 30L;

	private double previousHeading;
	private double currentHeading;
	private long timeSinceReverseDirection;
	private long timeSinceVelocityChange;
	// TODO: consolidate these? map by time instead?
	private double lastVelocity;
	private double previousVelocity;
	private double lastNonZeroVelocity;
	private RobotStateLog myStateLog;
	private List<FiredBullet> firedBullets;

	MoveDataManager(int enemiesTotal, BattleField battleField, MovementPredictor predictor)
	{
		super(enemiesTotal, battleField, predictor);
		this.previousVelocity = this.lastVelocity = this.lastNonZeroVelocity = 0;
		this.myStateLog = new RobotStateLog();
		this.firedBullets = new ArrayList<>();
	}

	void execute(int round, long time, Point2D.Double myLocation, double heading, double velocity)
	{
		this.myStateLog.addState(new RobotState(myLocation, heading, velocity, time, false));
		this.previousHeading = this.currentHeading;
		if (Math.abs(velocity) > NON_ZERO_VELOCITY_THRESHOLD)
		{
			this.lastNonZeroVelocity = velocity;
		}
		this.previousVelocity = velocity;
		this.currentHeading = Utils.normalAbsoluteAngle(heading + (this.lastNonZeroVelocity < 0 ? Math.PI : 0));
		this.updateBotDistances(myLocation);
		this.updateDamageFactors();
		this.removeOldFiredBullets(time);
		if (this.duelEnemy() != null)
		{
			this.duelEnemy().execute1v1(round, time, myLocation);
		}
		else
		{
			this.updateTimers(velocity);
		}
	}

	private void removeOldFiredBullets(long currentTime)
	{
		Iterator<FiredBullet> bulletIterator = this.firedBullets.iterator();
		while (bulletIterator.hasNext())
		{
			FiredBullet firedBullet = bulletIterator.next();
			if (!this.getBattleField().getRectangle().contains(firedBullet.position(currentTime)))
			{
				bulletIterator.remove();
			}
		}
	}

	private void updateDamageFactors()
	{
		if (this.isMeleeBattle())
		{
			this.getAllEnemyData().forEach(MoveEnemy::updateDamageFactor);
		}
	}

	@Override
	public void initRound()
	{
		super.initRound();
		this.currentHeading = this.previousHeading = 0;
		this.timeSinceReverseDirection = 0;
		this.timeSinceVelocityChange = 0;
		this.previousVelocity = this.lastVelocity = this.lastNonZeroVelocity = 0;
		this.myStateLog.clear();
		this.firedBullets.clear();
	}

	MoveEnemy newEnemy(ScannedRobotEvent e, Point2D.Double enemyLocation, double absBearing, int currentRound,
			boolean is1v1)
	{
		String botName = e.getName();
		MoveEnemy moveData = new MoveEnemy(botName, e.getDistance(), e.getEnergy(), enemyLocation, e.getHeadingRadians(),
				e.getVelocity(), absBearing, currentRound, e.getTime(), this.getBattleField(), this.getPredictor());
		this.saveEnemy(botName, moveData);
		if (is1v1)
		{
			this.setDuelEnemy(moveData);
		}
		return moveData;
	}

	private void saveEnemy(String botName, MoveEnemy moveData)
	{
		this.getEnemies().put(botName, moveData);
	}

	MoveEnemy updateEnemy(ScannedRobotEvent e, Point2D.Double enemyLocation, double absBearing, int currentRound,
			boolean is1v1)
	{
		MoveEnemy moveData = this.getEnemyData(e.getName());
		moveData.setWallHitDamage(this.getWallHitDamage(e, enemyLocation, moveData));
		moveData.setRobotState(new RobotState(enemyLocation, e.getHeadingRadians(), e.getVelocity(), e.getTime(), false));
		moveData.setEnergy(e.getEnergy());
		moveData.setDistance(e.getDistance());
		moveData.setAbsBearing(absBearing);
		moveData.setLastScanRound(currentRound);
		if (is1v1)
		{
			this.setDuelEnemy(moveData);
		}
		return moveData;
	}

	private WallHitDamage getWallHitDamage(ScannedRobotEvent e, Point2D.Double enemyLocation, MoveEnemy moveData)
	{
		RobotState lastState = moveData.getLastScanState();
		if (!moveData.isRobot() && e.getTime() - lastState.getTime() == 1
				&& Math.abs(lastState.getVelocity() - e.getVelocity()) > 2 && Math.abs(e.getVelocity()) < 0.0001
				&& DiaUtils.distanceToWall(enemyLocation, this.getBattleField()) < 0.0001)
		{
			if (Math.abs(e.getEnergy() - moveData.getEnergy()) < 0.0001)
			{
				moveData.setRobot(true);
			}
			else
			{
				double maxSpeed = Math.min(8.0, Math.abs(lastState.getVelocity()) + Rules.ACCELERATION);
				double minSpeed = Math.abs(lastState.getVelocity()) - Rules.DECELERATION;
				return new WallHitDamage(Rules.getWallHitDamage(minSpeed), Rules.getWallHitDamage(maxSpeed));
			}
		}
		return new WallHitDamage(0, 0);
	}

	void onHitByBullet(HitByBulletEvent e, int currentRound, long currentTime)
	{
		String botName = e.getName();
		if (this.hasEnemy(botName))
		{
			MoveEnemy moveData = this.getEnemyData(botName);
			moveData.setLastTimeHit(currentTime);
			moveData.setDamageTaken(moveData.getDamageTaken() + Rules.getBulletDamage(e.getBullet().getPower()));
			moveData.setTotalBulletPower(moveData.getTotalBulletPower() + e.getBullet().getPower());
			moveData.setTotalTimesHit(moveData.getTotalTimesHit() + 1);
			moveData.setEnergy(moveData.getEnergy() + Rules.getBulletHitBonus(e.getBullet().getPower()));
			Wave hitWave = moveData.processBullet(e.getBullet(), currentRound, currentTime);
			if (hitWave != null)
			{
				hitWave.setHitByBullet(true);
			}
		}
		else
		{
			System.out.println(this.warning() + "A bot shot me that I never knew existed! (" + botName + ")");
		}

		if (this.getDuelEnemy() != null)
		{
			this.getDuelEnemy().clearNeighborCache();
		}
	}

	void onBulletHitBullet(BulletHitBulletEvent e, int currentRound, long currentTime)
	{
		String botName = e.getHitBullet().getName();
		if (this.hasEnemy(botName))
		{
			MoveEnemy moveData = this.getEnemyData(botName);
			Wave hitWave = moveData.processBullet(e.getHitBullet(), currentRound, currentTime);
			if (hitWave != null)
			{
				hitWave.setBulletHitBullet(true);
			}
		}
		else if (!botName.equals(e.getBullet().getName()))
		{
			System.out.println(this.warning() + "One of my bullets hit a bullet from a " + "bot that I never knew existed! ("
					+ botName + ")");
		}

		this.removeFiredBullet(e);
		if (this.getDuelEnemy() != null)
		{
			this.getDuelEnemy().resetBulletShadows(this.firedBullets);
			this.getDuelEnemy().clearNeighborCache();
		}
	}

	void onBulletHit(BulletHitEvent e)
	{
		String botName = e.getName();
		try
		{
			MoveEnemy moveData = this.getEnemyData(botName);
			double bulletDamage = Rules.getBulletDamage(e.getBullet().getPower());
			moveData.setEnergy(moveData.getEnergy() - bulletDamage);
			moveData.setDamageGiven(moveData.getDamageGiven() + bulletDamage);
		}
		catch (NullPointerException npe)
		{
			System.out.println(this.warning() + "One of my bullets hit a bot that I never " + "knew existed! (" + botName + ")");
		}
	}

	void updateEnemyWaves(Point2D.Double myLocation, double previousEnemyEnergy, String botName, int currentRound,
			long currentTime, double myEnergy, double myHeading, double myVelocity, int wavesToSurf)
	{
		RobotState myRobotState = new RobotState(myLocation, myHeading, myVelocity, currentTime, false);
		this.myStateLog.addState(myRobotState);
		MoveEnemy moveData = this.getEnemyData(botName);
		boolean detectedEnemyBullet = false;
		double energyDrop = previousEnemyEnergy - moveData.getEnergy() - moveData.getWallHitDamage().getMax();
		if (energyDrop > 0.0999 && energyDrop < 3.0001 && moveData.getGunHeat(currentTime) < 0.0001)
		{
			detectedEnemyBullet = true;
		}

		this.updateTimers(myVelocity);
		int velocitySign = DiaUtils.nonZeroSign(Math.abs(myVelocity) > NON_ZERO_VELOCITY_THRESHOLD ? myVelocity
				: this.lastNonZeroVelocity);
		double accel = DiaUtils.limit(-Rules.DECELERATION, DiaUtils.accel(myVelocity, this.previousVelocity), Rules.ACCELERATION);
		double dl8t = this.myStateLog.getDisplacementDistance(myLocation, currentTime, 8);
		double dl20t = this.myStateLog.getDisplacementDistance(myLocation, currentTime, 20);
		double dl40t = this.myStateLog.getDisplacementDistance(myLocation, currentTime, 40);
		double guessedPower = moveData.guessBulletPower(myEnergy);
		long fireTime = currentTime + 1;
		Point2D.Double enemyNextLocation = this.getBattleField().translateToField(
				this.getPredictor().nextLocation(moveData.getLastScanState()));

		moveData.newMoveWave(enemyNextLocation, myLocation,
				DiaUtils.absoluteBearing(moveData.getLastScanState().getLocation(), myLocation), currentRound, fireTime,
				guessedPower, myEnergy, myHeading, myVelocity, velocitySign, accel, dl8t, dl20t, dl40t,
				this.timeSinceReverseDirection, this.timeSinceVelocityChange);
		moveData.updateImaginaryWave(currentTime, myRobotState, wavesToSurf, this.getPredictor());
		if (detectedEnemyBullet && currentTime > EARLIEST_FIRE_TIME)
		{
			moveData.updateFiringWave(currentTime, energyDrop, this.myStateLog, this.firedBullets);
		}
	}

	// In FFA, do this from execute().
	// In 1v1, do this before firing waves.
	private void updateTimers(double velocity)
	{
		if (Math.abs(Utils.normalRelativeAngle(this.currentHeading - this.previousHeading)) > DIRECTION_CHANGE_THRESHOLD)
		{
			this.timeSinceReverseDirection = 0;
		}
		else
		{
			this.timeSinceReverseDirection++;
		}

		if (Math.abs(velocity - this.lastVelocity) > 0.5)
		{
			this.timeSinceVelocityChange = 0;
		}
		else
		{
			this.timeSinceVelocityChange++;
		}
		this.lastVelocity = velocity;
	}

	RobotStateLog myStateLog()
	{
		return this.myStateLog;
	}

	void addFiredBullet(FiredBullet bullet)
	{
		this.firedBullets.add(bullet);
		if (this.getDuelEnemy() != null)
		{
			this.getDuelEnemy().setShadows(bullet);
		}
	}

	private void removeFiredBullet(BulletHitBulletEvent e)
	{
		double closestDistanceSq = Double.POSITIVE_INFINITY;
		FiredBullet closestFiredBullet = null;
		Point2D.Double bulletPoint = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
		for (FiredBullet firedBullet : this.firedBullets)
		{
			double thisDistanceSq = firedBullet.position(e.getTime()).distanceSq(bulletPoint);
			if (thisDistanceSq < closestDistanceSq)
			{
				closestDistanceSq = thisDistanceSq;
				closestFiredBullet = firedBullet;
			}
		}
		int bulletDistanceThreshold = 40;
		if (closestFiredBullet != null && closestDistanceSq < DiaUtils.square(bulletDistanceThreshold))
		{
			closestFiredBullet.setDeathTime(e.getTime());
		}
	}

	@Override
	protected String getLabel()
	{
		return "move";
	}
}
