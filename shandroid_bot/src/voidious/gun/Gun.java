package voidious.gun;

import robocode.*;
import robocode.util.Utils;
import scarter4work.Shandroid;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
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

public class Gun
{
	private static final int KNN_DATA_THRESHOLD = 9;

	private GunDataManager gunDataManager;
	private VirtualGunsManager<TimestampedFiringAngle> virtualGuns;
	private DuelGun<TimestampedFiringAngle> perceptualGun;
	private DuelGun<TimestampedFiringAngle> currentGun;
	private MeleeGun meleeGun;
	private int enemiesTotal;
	private double aimedBulletPower;
	private List<FireListener> fireListeners;
	private BattleField battleField;
	private MovementPredictor predictor;
	private boolean startedDuel;
	private Shandroid robot;

	public Gun(Shandroid robot)
	{
		this.robot = robot;
		this.battleField = new BattleField(robot.getBattleFieldWidth(), robot.getBattleFieldHeight());
		this.predictor = new MovementPredictor(this.battleField);
		this.virtualGuns = new VirtualGunsManager<>();
		this.enemiesTotal = robot.getOthers();
		this.gunDataManager = new GunDataManager(robot.getOthers(), this.battleField, this.predictor);
		this.gunDataManager.addListener(this.virtualGuns);
		this.fireListeners = new ArrayList<>();
		this.initGuns();
	}

	private void initGuns()
	{
		DuelGun<TimestampedFiringAngle> mainGun;
		if (this.enemiesTotal > 1)
		{
			this.perceptualGun = null;
			mainGun = new MainGun(this.gunDataManager, this.battleField);
			this.currentGun = mainGun;
		}
		else
		{
			this.perceptualGun = new PerceptualGun<>(this.gunDataManager, this.battleField);
			this.currentGun = this.perceptualGun;
			mainGun = new TripHammerKnnGun(this.gunDataManager, this.battleField);
		}

		this.virtualGuns.addGun(mainGun);

		DuelGun<TimestampedFiringAngle> antiSurfGun = new AntiSurferGun(this.gunDataManager, this.battleField);
		this.virtualGuns.addGun(antiSurfGun);

		this.meleeGun = new MeleeGun(this.gunDataManager, this.battleField);
	}

	private void initGunViews(GunEnemy gunData)
	{
		for (DuelGun<TimestampedFiringAngle> gun : this.virtualGuns.getGuns())
		{
			gunData.addViews(gun.newDataViews());
		}
		if (this.isMelee())
		{
			gunData.addViews(this.meleeGun.newDataViews());
		}
	}

	public void initRound(Shandroid robot)
	{
		this.robot = robot;
		this.gunDataManager.initRound();
		this.virtualGuns.initRound();
		this.startedDuel = false;
	}

	public void execute()
	{
		this.gunDataManager.execute(this.robot.getTime(),
				this.robot.getGunHeat(), this.myLocation(), this.is1v1());
		if (this.is1v1())
		{
			GunEnemy duelEnemy = this.gunDataManager.duelEnemy();
			if (duelEnemy != null)
			{
				this.aimAndFire(duelEnemy);
				if (!this.startedDuel)
				{
					this.startedDuel = true;
					this.printCurrentGun(duelEnemy);
				}
			}
		}
		else
		{
			this.aimAndFireAtEveryone();
		}
	}

	private void aimAndFire(GunEnemy gunData)
	{
		if (gunData != null)
		{
			this.fireIfGunTurned(this.aimedBulletPower);

			Wave aimWave = gunData.getLastWaveFired();
			this.aimedBulletPower = aimWave.getBulletPower();
			Point2D.Double myNextLocation = this.predictor.nextLocation(this.robot);
			double firingAngle;
			if (gunData.getEnergy() == 0 || this.ticksUntilGunCool() > 3)
			{
				firingAngle = DiaUtils.absoluteBearing(myNextLocation, aimWave.getTargetLocation());
				this.evaluateVirtualGuns(gunData);
			}
			else
			{
				firingAngle = this.currentGun.aim(aimWave);
			}
			this.robot.setTurnGunRightRadians(Utils.normalRelativeAngle(firingAngle - this.robot.getGunHeadingRadians()));
		}
	}

	private void aimAndFireAtEveryone()
	{
		GunEnemy closestBot = this.gunDataManager.getClosestLivingBot(this.myLocation());
		if (closestBot != null)
		{
			this.fireIfGunTurned(this.aimedBulletPower);
			Point2D.Double myNextLocation = this.predictor.nextLocation(this.robot);
			long ticksUntilFire = this.ticksUntilGunCool();

			if (ticksUntilFire % 2 == 0 || ticksUntilFire <= 4)
			{
				this.aimedBulletPower = this.calculateBulletPower();
				double firingAngle = this.meleeGun.aimAtEveryone(myNextLocation, this.robot.getTime(), this.robot.getOthers(), this.aimedBulletPower, closestBot);
				this.robot.setTurnGunRightRadians(Utils.normalRelativeAngle(firingAngle - this.robot.getGunHeadingRadians()));
			}
		}
	}

	private double calculateBulletPower()
	{
		GunEnemy gunData = this.gunDataManager.getClosestLivingBot(this.myLocation());
		double bulletPower = 3;
		if (gunData != null)
		{
			double myEnergy = this.robot.getEnergy();
			if (this.is1v1())
			{
				bulletPower = 1.95;

				if (gunData.getDistance() < 150 || gunData.isRammer())
				{
					bulletPower = 2.95;
				}

				if (gunData.getDistance() > 325)
				{
					double powerDownPoint = DiaUtils.limit(35, 63 + ((gunData.getEnergy() - myEnergy) * 4), 63);
					if (myEnergy < powerDownPoint)
					{
						bulletPower = Math.min(bulletPower, DiaUtils.cube(myEnergy / powerDownPoint) * 1.95);
					}
				}

				bulletPower = Math.min(bulletPower, gunData.getEnergy() / 4);
				bulletPower = Math.max(bulletPower, 0.1);
				bulletPower = Math.min(bulletPower, myEnergy);
			}
			else
			{
				double avgEnemyEnergy = this.gunDataManager.getAverageEnergy();

				bulletPower = 2.999;

				int enemiesAlive = this.robot.getOthers();
				if (enemiesAlive <= 3)
				{
					bulletPower = 1.999;
				}

				if (enemiesAlive <= 5 && gunData.getDistance() > 500)
				{
					bulletPower = 1.499;
				}

				if ((myEnergy < avgEnemyEnergy && enemiesAlive <= 5 && gunData.getDistance() > 300)
						|| gunData.getDistance() > 700)
				{

					bulletPower = 0.999;
				}

				if (myEnergy < 20 && myEnergy < avgEnemyEnergy)
				{
					bulletPower = Math.min(bulletPower, 2 - ((20 - myEnergy) / 11));
				}

				bulletPower = Math.max(bulletPower, 0.1);
				bulletPower = Math.min(bulletPower, myEnergy);
			}
		}

		return bulletPower;
	}

	private void evaluateVirtualGuns(GunEnemy gunData)
	{
		int dataPoints = this.gunDataManager.getDuelDataSize();
		if (dataPoints < KNN_DATA_THRESHOLD)
		{
			this.currentGun = this.perceptualGun;
		}
		else
		{
			if (this.perceptualGun != null)
			{
				System.out.println("Disabling " + this.perceptualGun.getLabel() + " @ " + this.robot.getTime());
				this.perceptualGun = null;
			}

			DuelGun<TimestampedFiringAngle> bestGun = this.virtualGuns.bestGun(gunData.getBotName());
			if (this.currentGun != bestGun)
			{
				this.currentGun = bestGun;
				System.out.println("Switching to " + this.currentGun.getLabel() + " ("
						+ this.virtualGuns.getFormattedRating(this.currentGun, gunData.getBotName()) + ")");
			}
		}
	}

	private void fireIfGunTurned(double bulletPower)
	{
		if (this.robot.getGunHeat() == 0 && this.robot.getGunTurnRemaining() == 0)
		{
			Bullet realBullet = null;
			if (this.robot.getEnergy() > bulletPower)
			{
				realBullet = this.setFireBulletLogged(bulletPower);
			}

			if (realBullet != null)
			{
				this.gunDataManager.markFiringWaves(this.robot.getTime(), this.is1v1());
			}
		}
	}

	public void onScannedRobot(ScannedRobotEvent e)
	{
		String botName = e.getName();
		double absBearing = e.getBearingRadians() + this.robot.getHeadingRadians();
		Point2D.Double enemyLocation = DiaUtils
				.project(this.myLocation(), Utils.normalAbsoluteAngle(absBearing), e.getDistance());

		GunEnemy gunData;
		if (this.gunDataManager.hasEnemy(botName))
		{
			this.gunDataManager.updateEnemy(e, enemyLocation, absBearing, this.robot.getRoundNum(), this.is1v1());
		}
		else
		{
			gunData = this.gunDataManager.newEnemy(e, enemyLocation, absBearing, this.robot.getRoundNum(), this.is1v1());
			this.initGunViews(gunData);
		}

		this.gunDataManager.fireNextTickWave(this.predictor.nextLocation(this.robot), enemyLocation, botName,
				this.robot.getRoundNum(), this.robot.getTime(), this.calculateBulletPower(), this.robot.getEnergy(),
				this.robot.getGunHeat(), this.robot.getHeadingRadians(), this.robot.getVelocity(), this.robot.getOthers());
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		this.gunDataManager.onRobotDeath(e);
	}

	public void onWin(WinEvent e)
	{
		this.roundOver();
	}

	public void onDeath(DeathEvent e)
	{
		this.roundOver();
	}

	private void roundOver()
	{
		GunEnemy duelEnemy = this.gunDataManager.duelEnemy();
		if (this.is1v1() && duelEnemy != null)
		{
			this.virtualGuns.printGunRatings(duelEnemy.getBotName());
		}
	}

	public void onBulletHit(BulletHitEvent e)
	{
		this.gunDataManager.onBulletHit(e, this.robot.getTime());
	}

	public void onBulletHitBullet(BulletHitBulletEvent e)
	{
		this.gunDataManager.onBulletHitBullet(e, this.robot.getTime());
	}

	private long ticksUntilGunCool()
	{
		return Math.round(Math.ceil(this.robot.getGunHeat() / this.robot.getGunCoolingRate()));
	}

	private boolean is1v1()
	{
		return (this.robot.getOthers() <= 1);
	}

	private boolean isMelee()
	{
		return (this.robot.getOthers() > 1);
	}

	public int getEnemiesAlive()
	{
		return this.robot.getOthers();
	}

	public void addFireListener(FireListener listener)
	{
		this.fireListeners.add(listener);
	}

	private Bullet setFireBulletLogged(double bulletPower)
	{
		Bullet bullet = this.robot.setFireBullet(bulletPower);
		if (bullet != null)
		{
			for (FireListener listener : this.fireListeners)
			{
				listener.bulletFired(new FiredBullet(this.robot.getTime(), this.myLocation(), this.robot.getGunHeadingRadians(),
						(20 - (3 * bulletPower))));
			}
		}

		return bullet;
	}

	private Point2D.Double myLocation()
	{
		return new Point2D.Double(this.robot.getX(), this.robot.getY());
	}

	private void printCurrentGun(GunEnemy gunData)
	{
		System.out.println("Current gun: " + this.currentGun.getLabel() + " ("
				+ this.virtualGuns.getFormattedRating(this.currentGun, gunData.getBotName()) + ")");
	}

	public Shandroid getRobot()
	{
		return this.robot;
	}
}