package scarter4work;

import robocode.*;
import voidious.gun.Gun;
import voidious.move.Move;
import voidious.radar.Radar;
import voidious.utils.ErrorLogger;

import java.awt.*;

/**
 * Diamond was the base robot of my experimenting from 2013.  This robot has
 * been completely rewritten to offer a more standard java programming 
 * experience.  The bot has had a bullet shield added, and the classes
 * supporting the genetic algorithms have been cleaned up.  While most of the
 * credit for the effort goes to Voidious, much effort has been put into
 * this version of this robot by the extending author.
 */

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
 * Diamond - a robot by Voidious
 * 
 * A Melee and 1v1 bot.
 * 
 * In Melee it uses: - Minimum Risk Movement - Dynamic Clustering - Displacement
 * Vectors - "Shadow" Melee Gun
 * 
 * In 1v1 it uses: - Wave Surfing movement with Dynamic Clustering - Dynamic
 * Clustering / GuessFactors for the two guns: one tuned for surfers, the other
 * tuned for non-adaptive movements.
 * 
 * For more details, see: http://robowiki.net?Diamond
 */
public class Shandroid extends AdvancedRobot
{
	private static final boolean LOG_ERRORS = true;
	private static double RANDOM_COLORS = Math.random();

	static
	{
		ErrorLogger.enabled = LOG_ERRORS;
	}

	private Radar radar;
	private Move move;
	private Gun gun;

	private double maxVelocity;

	@Override
	public void run()
	{
		ErrorLogger.init(this);

		try
		{
			this.initComponents();
			this.initColors();

			this.setAdjustGunForRobotTurn(true);
			this.setAdjustRadarForGunTurn(true);

			while (true)
			{
				this.move.execute();
				this.gun.execute();
				this.radar.execute();
				this.execute();
			}
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	private void initComponents()
	{
		if (this.radar == null)
		{
			this.radar = new Radar(this);
		}
		if (this.move == null)
		{
			this.move = new Move(this);
		}
		if (this.gun == null)
		{
			this.gun = new Gun(this);
			this.gun.addFireListener(this.move);
		}

		this.radar.initRound(this);
		this.move.initRound(this);
		this.gun.initRound(this);
	}

	private void initColors()
	{
		if (RANDOM_COLORS < .05)
		{
			this.setGoldColors();
		}
		else
		{
			this.setShandroidColors();
		}
	}

	private void setShandroidColors()
	{
		Color shandroidSilver = new Color(192, 192, 192);
		this.setColors(shandroidSilver, Color.black, shandroidSilver);
	}

	private void setGoldColors()
	{
		if (this.getRoundNum() == 0)
		{
			System.out.println("Activating Gold colors.");
		}

		Color gold = new Color(240, 235, 170);
		this.setColors(gold, gold, gold);
	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e)
	{
		try
		{
			this.radar.onScannedRobot(e);
			this.move.onScannedRobot(e);
			this.gun.onScannedRobot(e);
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	@Override
	public void onRobotDeath(RobotDeathEvent e)
	{
		try
		{
			this.radar.onRobotDeath(e);
			this.move.onRobotDeath(e);
			this.gun.onRobotDeath(e);
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	@Override
	public void onHitByBullet(HitByBulletEvent e)
	{
		try
		{
			this.move.onHitByBullet(e);
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	@Override
	public void onBulletHit(BulletHitEvent e)
	{
		try
		{
			this.move.onBulletHit(e);
			this.gun.onBulletHit(e);
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	@Override
	public void onBulletHitBullet(BulletHitBulletEvent e)
	{
		try
		{
			this.move.onBulletHitBullet(e);
			this.gun.onBulletHitBullet(e);
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	@Override
	public void onHitWall(HitWallEvent e)
	{
		System.out.println("WARNING: I hit a wall (" + this.getTime() + ").");
	}

	@Override
	public void onWin(WinEvent e)
	{
		try
		{
			this.gun.onWin(e);
			this.move.onWin(e);
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	@Override
	public void onDeath(DeathEvent e)
	{
		try
		{
			this.gun.onDeath(e);
			this.move.onDeath(e);
		}
		catch (RuntimeException re)
		{
			this.logAndRethrowException(re);
		}
	}

	@Override
	public void onSkippedTurn(SkippedTurnEvent e)
	{
		System.out.println("WARNING: Turn skipped at: " + e.getTime());
	}

	private void logAndRethrowException(RuntimeException e)
	{
		String moreInfo = "getOthers(): " + this.getOthers() + "\n" + "getEnemiesAlive(): " + this.gun.getEnemiesAlive() + "\n"
				+ "getRoundNum(): " + this.getRoundNum() + "\n" + "getTime(): " + this.getTime();
		ErrorLogger.getInstance().logException(e, moreInfo);

		throw e;
	}

	@Override
	public void setMaxVelocity(double maxVelocity)
	{
		super.setMaxVelocity(maxVelocity);
		this.maxVelocity = maxVelocity;
	}

	public double getMaxVelocity()
	{
		return this.maxVelocity;
	}

	/**
	 * @return the radar
	 */
	public Radar getRadar()
	{
		return this.radar;
	}

	/**
	 * @param radar
	 *            the radar to set
	 */
	public void setRadar(Radar radar)
	{
		this.radar = radar;
	}

	/**
	 * @return the move
	 */
	public Move getMove()
	{
		return this.move;
	}

	/**
	 * @param move
	 *            the move to set
	 */
	public void setMove(Move move)
	{
		this.move = move;
	}

	/**
	 * @return the gun
	 */
	public Gun getGun()
	{
		return this.gun;
	}

	/**
	 * @param gun
	 *            the gun to set
	 */
	public void setGun(Gun gun)
	{
		this.gun = gun;
	}
}