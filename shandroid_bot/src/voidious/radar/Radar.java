package voidious.radar;

import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import scarter4work.Shandroid;
import voidious.utils.BattleField;
import voidious.utils.DiaUtils;
import voidious.utils.MovementPredictor;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

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

public class Radar
{
	private static final double MAX_RADAR_TRACKING_AMOUNT = Math.PI / 4;
	private static final long BOT_NOT_FOUND = -1;

	private Shandroid robot;
	private Map<String, RadarScan> scans;
	private Point2D.Double myLocation;
	private String targetName = null;
	private boolean lockMode = false;
	private long resetTime;
	private Point2D.Double centerField;
	private int radarDirection;
	private double lastRadarHeading;
	private MovementPredictor predictor;
	private int startDirection;

	public Radar(Shandroid robot)
	{
		this.robot = robot;
		this.scans = new HashMap<>();
		this.centerField = new Point2D.Double(this.robot.getBattleFieldWidth() / 2, this.robot.getBattleFieldHeight() / 2);
		this.predictor = new MovementPredictor(new BattleField(this.robot.getBattleFieldWidth(),
				this.robot.getBattleFieldHeight()));
		this.radarDirection = 1;
	}

	public void initRound(Shandroid robot)
	{
		this.myLocation = new Point2D.Double(this.robot.getX(), this.robot.getY());
		this.robot = robot;
		this.scans.clear();
		this.lockMode = false;
		this.resetTime = 0;
		this.lastRadarHeading = this.robot.getRadarHeadingRadians();
		this.startDirection = this.getStartRadarDirection();
	}

	public void execute()
	{
		this.myLocation = new Point2D.Double(this.robot.getX(), this.robot.getY());
		this.checkScansIntegrity();
		if (this.robot.getOthers() == 1 && !this.lockMode && !this.scans.isEmpty())
		{
			this.setRadarLock((String) this.scans.keySet().toArray()[0]);
		}
		this.directRadar();

		this.lastRadarHeading = this.robot.getRadarHeadingRadians();
		this.myLocation = this.predictor.nextLocation(this.robot);
	}

	public void onScannedRobot(ScannedRobotEvent e)
	{
		Point2D.Double enemyLocation = DiaUtils.project(this.myLocation, e.getBearingRadians() + this.robot.getHeadingRadians(),
				e.getDistance());

		this.scans.put(e.getName(), new RadarScan(this.robot.getTime(), enemyLocation));
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		this.scans.remove(e.getName());
		if (this.targetName != null && this.targetName.equals(e.getName()))
		{
			this.lockMode = false;
		}
	}

	private void directRadar()
	{
		if (this.lockMode && !this.scans.containsKey(this.targetName))
		{
			System.out.println("WARNING: Radar locked onto dead or non-existent bot, " + "releasing lock.");
			this.lockMode = false;
		}

		double radarTurnAmount;
		if (this.lockMode && this.scans.get(this.targetName).getLastScanTime() == this.robot.getTime())
		{
			radarTurnAmount = Utils.normalRelativeAngle(DiaUtils.absoluteBearing(this.myLocation, this.scans.get(this.targetName)
					.getLastLocation())
					- this.robot.getRadarHeadingRadians());
			this.radarDirection = DiaUtils.nonZeroSign(radarTurnAmount);
			radarTurnAmount += this.radarDirection * (MAX_RADAR_TRACKING_AMOUNT / 2);
		}
		else
		{
			this.radarDirection = this.nextRadarDirection();
			radarTurnAmount = this.radarDirection * MAX_RADAR_TRACKING_AMOUNT;
		}
		this.robot.setTurnRadarRightRadians(radarTurnAmount);
	}

	private void setRadarLock(String botName)
	{
		if (this.scans.containsKey(botName))
		{
			this.targetName = botName;
			this.lockMode = true;
		}
	}

	private long minTicksToScan(String botName)
	{
		if (!this.scans.containsKey(botName))
		{
			return BOT_NOT_FOUND;
		}

		double absBearing = DiaUtils.absoluteBearing(this.myLocation, this.scans.get(botName).getLastLocation());
		double shortestAngleToScan = Math.abs(Utils.normalRelativeAngle(absBearing - this.robot.getRadarHeadingRadians()));

		return Math.round(Math.ceil(shortestAngleToScan / MAX_RADAR_TRACKING_AMOUNT));
	}

	private int getStartRadarDirection()
	{
		return this.directionToBearing(DiaUtils.absoluteBearing(this.myLocation, this.centerField));
	}

	private int nextRadarDirection()
	{
		if (this.scans.isEmpty() || this.scans.size() < this.robot.getOthers())
		{
			return this.startDirection;
		}

		String stalestBot = this.findStalestBotName();
		Point2D.Double radarTarget;
		if (this.minTicksToScan(stalestBot) == 4)
		{
			radarTarget = this.centerField;
		}
		else
		{
			radarTarget = this.scans.get(this.findStalestBotName()).getLastLocation();
		}

		double absBearingRadarTarget = DiaUtils.absoluteBearing(this.myLocation, radarTarget);

		if (this.justScannedThatSpot(absBearingRadarTarget))
		{
			return this.radarDirection;
		}

		return this.directionToBearing(absBearingRadarTarget);
	}

	private int directionToBearing(double bearing)
	{
		if (Utils.normalRelativeAngle(bearing - this.robot.getRadarHeadingRadians()) > 0)
		{
			return 1;
		}
		return -1;
	}

	private String findStalestBotName()
	{
		long oldestTime = Long.MAX_VALUE;
		String botName = null;

		for (String name : this.scans.keySet())
		{
			if (this.scans.get(name).getLastScanTime() < oldestTime)
			{
				oldestTime = this.scans.get(name).getLastScanTime();
				botName = name;
			}
		}

		return botName;
	}

	private void checkScansIntegrity()
	{
		if (this.scans.size() != this.robot.getOthers() && this.robot.getTime() - this.resetTime > 25
				&& this.robot.getOthers() > 0)
		{
			this.scans.clear();
			this.lockMode = false;
			this.resetTime = this.robot.getTime();
			System.out.println("WARNING: Radar integrity failure detected (time = " + this.resetTime + "), resetting.");
		}
	}

	private boolean justScannedThatSpot(double absBearing)
	{
		return (DiaUtils.nonZeroSign(Utils.normalRelativeAngle(absBearing - this.lastRadarHeading)) == this.radarDirection)
				&& (DiaUtils.nonZeroSign(Utils.normalRelativeAngle(this.robot.getRadarHeadingRadians() - absBearing)) == this.radarDirection);
	}
}