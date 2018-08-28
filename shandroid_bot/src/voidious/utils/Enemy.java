package voidious.utils;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class Enemy<T extends Timestamped>
{
	private static final double DEFAULT_ENERGY = 100;
	protected static final boolean IS_BULLET_HIT = false;
	protected static final boolean IS_VISIT = true;

	private String botName;
	private RobotState lastScanState;
	private double distance;
	private double absBearing;
	private double energy;
	private boolean alive;
	private int lastScanRound;
	private RobotStateLog stateLog;
	private double damageGiven;
	private long timeAliveTogether;
	private Map<String, KnnView<T>> views;
	private Map<String, Double> botDistancesSq;
	private WaveManager waveManager;
	private BattleField battleField;
	private MovementPredictor predictor;

	protected Enemy(String botName, Point2D.Double location, double distance, double energy, double heading, double velocity,
			double absBearing, int round, long time, BattleField battleField, MovementPredictor predictor, WaveManager waveManager)
	{
		this.botName = botName;
		this.absBearing = absBearing;
		this.distance = distance;
		this.energy = energy;
		this.lastScanRound = round;
		this.timeAliveTogether = 1;
		this.battleField = battleField;
		this.predictor = predictor;
		this.waveManager = waveManager;

		this.damageGiven = 0;
		this.alive = true;
		this.views = new HashMap<>();
		this.stateLog = new RobotStateLog();
		this.setRobotState(new RobotState(location, heading, velocity, time, false));
		this.botDistancesSq = new HashMap<>();
	}

	public void initRound()
	{
		this.energy = DEFAULT_ENERGY;
		this.distance = 1000;
		this.alive = true;
		this.stateLog.clear();
		this.waveManager.initRound();
		this.clearDistancesSq();
	}

	public void addViews(List<KnnView<T>> knnViews)
	{
		knnViews.forEach(this::addView);
	}

	protected void addView(KnnView<T> view)
	{
		this.views.put(view.name, view);
	}

	public void setRobotState(RobotState robotState)
	{
		this.lastScanState = robotState;
		this.stateLog.addState(robotState);
	}

	protected RobotState getState(long time)
	{
		return this.stateLog.getState(time);
	}

	void setBotDistanceSq(String name, double distance)
	{
		this.botDistancesSq.put(name, distance);
	}

	void removeDistanceSq(String name)
	{
		this.botDistancesSq.remove(name);
	}

	private void clearDistancesSq()
	{
		this.botDistancesSq.clear();
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
	 * @return the lastScanState
	 */
	public RobotState getLastScanState()
	{
		return this.lastScanState;
	}

	/**
	 * @param lastScanState
	 *            the lastScanState to set
	 */
	public void setLastScanState(RobotState lastScanState)
	{
		this.lastScanState = lastScanState;
	}

	/**
	 * @return the distance
	 */
	public double getDistance()
	{
		return this.distance;
	}

	/**
	 * @param distance
	 *            the distance to set
	 */
	public void setDistance(double distance)
	{
		this.distance = distance;
	}

	/**
	 * @return the absBearing
	 */
	public double getAbsBearing()
	{
		return this.absBearing;
	}

	/**
	 * @param absBearing
	 *            the absBearing to set
	 */
	public void setAbsBearing(double absBearing)
	{
		this.absBearing = absBearing;
	}

	/**
	 * @return the energy
	 */
	public double getEnergy()
	{
		return this.energy;
	}

	/**
	 * @param energy
	 *            the energy to set
	 */
	public void setEnergy(double energy)
	{
		this.energy = energy;
	}

	/**
	 * @return the alive
	 */
	public boolean isAlive()
	{
		return this.alive;
	}

	/**
	 * @param alive
	 *            the alive to set
	 */
	public void setAlive(boolean alive)
	{
		this.alive = alive;
	}

	/**
	 * @return the lastScanRound
	 */
	public int getLastScanRound()
	{
		return this.lastScanRound;
	}

	/**
	 * @param lastScanRound
	 *            the lastScanRound to set
	 */
	public void setLastScanRound(int lastScanRound)
	{
		this.lastScanRound = lastScanRound;
	}

	/**
	 * @return the stateLog
	 */
	public RobotStateLog getStateLog()
	{
		return this.stateLog;
	}

	/**
	 * @param stateLog
	 *            the stateLog to set
	 */
	public void setStateLog(RobotStateLog stateLog)
	{
		this.stateLog = stateLog;
	}

	/**
	 * @return the damageGiven
	 */
	public double getDamageGiven()
	{
		return this.damageGiven;
	}

	/**
	 * @param damageGiven
	 *            the damageGiven to set
	 */
	public void setDamageGiven(double damageGiven)
	{
		this.damageGiven = damageGiven;
	}

	/**
	 * @return the timeAliveTogether
	 */
	public long getTimeAliveTogether()
	{
		return this.timeAliveTogether;
	}

	/**
	 * @param timeAliveTogether
	 *            the timeAliveTogether to set
	 */
	public void setTimeAliveTogether(long timeAliveTogether)
	{
		this.timeAliveTogether = timeAliveTogether;
	}

	/**
	 * @return the views
	 */
	public Map<String, KnnView<T>> getViews()
	{
		return this.views;
	}

	/**
	 * @param views
	 *            the views to set
	 */
	public void setViews(Map<String, KnnView<T>> views)
	{
		this.views = views;
	}

	/**
	 * @return the botDistancesSq
	 */
	public Map<String, Double> getBotDistancesSq()
	{
		return this.botDistancesSq;
	}

	/**
	 * @param botDistancesSq
	 *            the botDistancesSq to set
	 */
	public void setBotDistancesSq(Map<String, Double> botDistancesSq)
	{
		this.botDistancesSq = botDistancesSq;
	}

	/**
	 * @return the waveManager
	 */
	public WaveManager getWaveManager()
	{
		return this.waveManager;
	}

	/**
	 * @param waveManager
	 *            the waveManager to set
	 */
	public void setWaveManager(WaveManager waveManager)
	{
		this.waveManager = waveManager;
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
}