package voidious.move;

import robocode.AdvancedRobot;
import voidious.utils.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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

class MeleeMover
{
	private static final double CURRENT_DESTINATION_BIAS = 0.8;
	private static final int RECENT_LOCATIONS_TO_STORE = 50;
	private static final long NUM_SLICES_BOT = 100;
	private static final double BOT_HALF_WIDTH = 18;

	private AdvancedRobot robot;
	private BattleField battleField;
	private MovementPredictor predictor;
	private LinkedList<OldLocation> recentLocations;
	private Destination currentDestination;

	MeleeMover(AdvancedRobot robot, BattleField battleField)
	{
		this.robot = robot;
		this.battleField = battleField;
		this.predictor = new MovementPredictor(battleField);
		this.recentLocations = new LinkedList<>();
	}

	public void initRound(AdvancedRobot robot, Point2D.Double myLocation)
	{
		this.robot = robot;
		this.currentDestination = new Destination(myLocation, Double.POSITIVE_INFINITY, 0);
		this.recentLocations.clear();
	}

	public void move(Point2D.Double myLocation, Collection<MoveEnemy> enemies, MoveEnemy closestEnemy)
	{
		if (enemies.isEmpty())
		{
			return;
		}

		this.updateRecentLocations(this.robot.getTime(), myLocation);
		List<Destination> destinations = this.generateDestinations(myLocation, enemies, closestEnemy);
		Destination nextDestination = this.getNextDestination(myLocation, destinations);

		double goAngle = DiaUtils.absoluteBearing(myLocation, nextDestination.getLocation());
		DiaUtils.setBackAsFront(this.robot, goAngle);
		this.currentDestination = nextDestination;
	}

	private void updateRecentLocations(long currentTime, Point2D.Double myLocation)
	{
		if (currentTime % 5 == 0)
		{
			for (int x = 0; x < 5; x++)
			{
				this.recentLocations.addFirst(new OldLocation(DiaUtils.project(myLocation, Math.random() * Math.PI * 2,
						5 + Math.random() * Math.random() * 200), currentTime));
			}
			while (this.recentLocations.size() > RECENT_LOCATIONS_TO_STORE)
			{
				this.recentLocations.removeLast();
			}
		}
	}

	private List<Destination> generateDestinations(Point2D.Double myLocation, Collection<MoveEnemy> enemies,
			MoveEnemy closestEnemy)
	{
		List<Destination> possibleDestinations = new ArrayList<>();
		possibleDestinations.addAll(this.generatePointsAroundBot(myLocation, enemies, closestEnemy));

		if (myLocation.distance(this.currentDestination.getLocation()) <= myLocation.distance(closestEnemy.getLastScanState()
				.getLocation()))
		{
			double currentGoAngle = DiaUtils.absoluteBearing(myLocation, this.currentDestination.getLocation());
			double currentRisk = CURRENT_DESTINATION_BIAS
					* this.evaluateRisk(enemies, this.currentDestination.getLocation(), this.currentDestination.getGoAngle());
			this.currentDestination = new Destination(this.currentDestination.getLocation(), currentRisk, currentGoAngle);
			possibleDestinations.add(this.currentDestination);
		}

		return possibleDestinations;
	}

	private ArrayList<Destination> generatePointsAroundBot(Point2D.Double myLocation, Collection<MoveEnemy> enemies,
			MoveEnemy closestEnemy)
	{
		ArrayList<Destination> destinations = new ArrayList<>();
		double distanceToClosestBot = myLocation.distance(closestEnemy.getLastScanState().getLocation());
		double movementStick = Math.min(100 + Math.random() * 100, distanceToClosestBot);

		double sliceSize = (2 * Math.PI) / NUM_SLICES_BOT;
		for (int x = 0; x < NUM_SLICES_BOT; x++)
		{
			double angle = x * sliceSize;
			Point2D.Double dest = DiaUtils.project(myLocation, angle, movementStick);
			dest.x = DiaUtils.limit(BOT_HALF_WIDTH, dest.x, this.battleField.getWidth() - BOT_HALF_WIDTH);
			dest.y = DiaUtils.limit(BOT_HALF_WIDTH, dest.y, this.battleField.getHeight() - BOT_HALF_WIDTH);
			destinations.add(new Destination(dest, this.evaluateRisk(enemies, dest, angle), angle));
		}

		return destinations;
	}

	private double evaluateRisk(Collection<MoveEnemy> enemies, Point2D.Double destination, double goAngle)
	{
		double risk = 0;
		for (MoveEnemy moveData : enemies)
		{
			if (moveData.isAlive())
			{
				double botRisk;
				double distanceSq = destination.distanceSq(moveData.getLastScanState().getLocation());
				botRisk = DiaUtils.limit(0.25, moveData.getEnergy() / this.robot.getEnergy(), 4)
						* (1 + Math.abs(Math.cos(moveData.getAbsBearing() - goAngle))) * moveData.getDamageFactor()
						/ (distanceSq * (moveData.botsCloser(distanceSq * .8) + 1));
				risk += botRisk;
			}
		}

		double randomRisk = 0;
		for (OldLocation oldLocation : this.recentLocations)
		{
			randomRisk += 30.0 / oldLocation.getLocation().distanceSq(destination);
		}
		risk *= 1 + randomRisk;

		return risk;
	}

	private Destination getNextDestination(Point2D.Double myLocation, List<Destination> destinations)
	{
		Destination nextDestination;
		RobotState currentState = new RobotState(myLocation, this.robot.getHeadingRadians(), this.robot.getVelocity(),
				this.robot.getTime(), false);
		do
		{
			nextDestination = this.safestDestination(myLocation, destinations);
			destinations.remove(nextDestination);
		}
		while (this.wouldHitWall(currentState, nextDestination));
		return nextDestination;
	}

	private Destination safestDestination(Point2D.Double myLocation, List<Destination> possibleDestinations)
	{
		double lowestRisk = Double.POSITIVE_INFINITY;
		Destination safest = null;

		for (Destination destination : possibleDestinations)
		{
			if (destination.getRisk() < lowestRisk)
			{
				lowestRisk = destination.getRisk();
				safest = destination;
			}
		}

		if (safest == null)
		{
			String error = "No safe destinations found, there must be a bug " + "in the risk evaluation.\n" + "_myLocation: ("
					+ DiaUtils.round(myLocation.x, 1) + ", " + DiaUtils.round(myLocation.y, 1) + ")\n" + "myEnergy: "
					+ this.robot.getEnergy() + "\n" + "getOthers(): " + this.robot.getOthers();
			ErrorLogger.getInstance().logError(error);

			safest = this.currentDestination;
		}

		return safest;
	}

	private boolean wouldHitWall(RobotState currentState, Destination destination)
	{
		long ticksAhead = 5;
		for (int x = 0; x < ticksAhead; x++)
		{
			currentState = this.predictor.nextLocation(currentState, 8.0,
					DiaUtils.absoluteBearing(currentState.getLocation(), destination.getLocation()), true);
			if (!this.getBattleField().getRectangle().contains(currentState.getLocation()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the robot
	 */
	public AdvancedRobot getRobot()
	{
		return this.robot;
	}

	/**
	 * @param robot
	 *            the robot to set
	 */
	public void setRobot(AdvancedRobot robot)
	{
		this.robot = robot;
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
	 * @return the recentLocations
	 */
	public LinkedList<OldLocation> getRecentLocations()
	{
		return this.recentLocations;
	}

	/**
	 * @param recentLocations
	 *            the recentLocations to set
	 */
	public void setRecentLocations(LinkedList<OldLocation> recentLocations)
	{
		this.recentLocations = recentLocations;
	}

	/**
	 * @return the currentDestination
	 */
	public Destination getCurrentDestination()
	{
		return this.currentDestination;
	}

	/**
	 * @param currentDestination
	 *            the currentDestination to set
	 */
	public void setCurrentDestination(Destination currentDestination)
	{
		this.currentDestination = currentDestination;
	}
}