package voidious.move;

import robocode.*;
import robocode.util.Utils;
import scarter4work.Shandroid;
import voidious.gun.FireListener;
import voidious.gun.FiredBullet;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.MovementPredictor;

import java.awt.geom.Point2D;

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

public class Move implements FireListener
{
	private static final int WAVES_TO_SURF = 2;

	private Shandroid robot;
	private MoveDataManager moveDataManager;
	private MeleeMover meleeMover;
	private SurfMover surfMover;

	public Move(Shandroid robot)
	{
		BattleField bf = new BattleField(robot.getBattleFieldWidth(), robot.getBattleFieldHeight());
		this.moveDataManager = new MoveDataManager(robot.getOthers(), bf, new MovementPredictor(bf));
		this.meleeMover = new MeleeMover(robot, bf);
		this.surfMover = new SurfMover(robot, bf);
	}

	public void initRound(Shandroid robot)
	{
		this.robot = robot;
		this.moveDataManager.initRound();
		this.meleeMover.initRound(robot, this.myLocation());
		this.surfMover.initRound();
	}

	public void execute()
	{
		this.moveDataManager.execute(this.robot.getRoundNum(), this.robot.getTime(), this.myLocation(),
				this.robot.getHeadingRadians(), this.robot.getVelocity());
		this.move();
	}

	private void move()
	{
		if (this.is1v1())
		{
			this.surfMover.move(this.moveDataManager.myStateLog().getState(this.robot.getTime()),
					this.moveDataManager.duelEnemy(), WAVES_TO_SURF);
		}
		else
		{
			this.meleeMover.move(this.myLocation(), this.moveDataManager.getAllEnemyData(),
					this.moveDataManager.getClosestLivingBot(this.myLocation()));
		}
	}

	public void onScannedRobot(ScannedRobotEvent e)
	{
		Point2D.Double myLocation = this.myLocation();
		String botName = e.getName();
		double absBearing = Utils.normalAbsoluteAngle(e.getBearingRadians() + this.robot.getHeadingRadians());
		Point2D.Double enemyLocation = DiaUtils.project(myLocation, absBearing, e.getDistance());

		double previousEnemyEnergy;
		if (this.moveDataManager.hasEnemy(botName))
		{
			previousEnemyEnergy = this.moveDataManager.getEnemyData(botName).getEnergy();
			this.moveDataManager.updateEnemy(e, enemyLocation, absBearing, this.robot.getRoundNum(), this.is1v1());
		}
		else
		{
			previousEnemyEnergy = e.getEnergy();
			this.moveDataManager.newEnemy(e, enemyLocation, absBearing, this.robot.getRoundNum(), this.is1v1());
		}

		if (this.is1v1() && this.moveDataManager.duelEnemy() != null)
		{
			this.moveDataManager.updateEnemyWaves(this.myLocation(), previousEnemyEnergy, botName, this.robot.getRoundNum(),
					this.robot.getTime(), this.robot.getEnergy(), this.robot.getHeadingRadians(), this.robot.getVelocity(),
					WAVES_TO_SURF);
		}
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		this.moveDataManager.onRobotDeath(e);
	}

	public void onHitByBullet(HitByBulletEvent e)
	{
		this.moveDataManager.onHitByBullet(e, this.robot.getRoundNum(), this.robot.getTime());
	}

	public void onBulletHitBullet(BulletHitBulletEvent e)
	{
		this.moveDataManager.onBulletHitBullet(e, this.robot.getRoundNum(), this.robot.getTime());
	}

	public void onBulletHit(BulletHitEvent e)
	{
		this.moveDataManager.onBulletHit(e);
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
		MoveEnemy duelEnemy = this.moveDataManager.duelEnemy();
		if (this.is1v1() && duelEnemy != null)
		{
			this.surfMover.roundOver(duelEnemy);
		}
	}

	private Point2D.Double myLocation()
	{
		return new Point2D.Double(this.robot.getX(), this.robot.getY());
	}

	private boolean is1v1()
	{
		return (this.robot.getOthers() <= 1);
	}

	@Override
	public void bulletFired(FiredBullet bullet)
	{
		this.moveDataManager.addFiredBullet(bullet);
	}

	protected Shandroid getRobot()
	{
		return this.robot;
	}
}