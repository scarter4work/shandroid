package voidious.utils;

import robocode.RobotDeathEvent;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

public abstract class EnemyDataManager<T extends Enemy<? extends Timestamped>>
{
	private static final String WARNING_ROBOT_DEATH_UNKNOWN = "A bot died that I never knew existed!";

	private T duelEnemy;
	private int enemiesTotal;
	private Map<String, T> enemies;
	private BattleField battleField;
	private MovementPredictor predictor;

	protected EnemyDataManager(int enemiesTotal, BattleField battleField, MovementPredictor predictor)
	{
		this.duelEnemy = null;
		this.enemiesTotal = enemiesTotal;
		this.battleField = battleField;
		this.predictor = predictor;
		this.enemies = new HashMap<>(enemiesTotal);
	}

	public boolean hasEnemy(String botName)
	{
		return this.enemies.containsKey(botName);
	}

	public T getEnemyData(String botName)
	{
		return this.enemies.get(botName);
	}

	public Collection<T> getAllEnemyData()
	{
		return Collections.unmodifiableCollection(this.enemies.values());
	}

	public void initRound()
	{
		for (T enemyData : this.getAllEnemyData())
		{
			enemyData.initRound();
		}
		this.duelEnemy = null;
	}

	public void onRobotDeath(RobotDeathEvent e)
	{
		try
		{
			this.getEnemyData(e.getName()).setAlive(false);
		}
		catch (NullPointerException npe)
		{
			System.out.println(this.warning() + WARNING_ROBOT_DEATH_UNKNOWN);
		}
	}

	protected void updateBotDistances(Point2D.Double myLocation)
	{
		if (this.enemies.size() > 1)
		{
			String[] botNames = this.getBotNames();
			for (int x = 0; x < botNames.length; x++)
			{
				T enemyData1 = this.getEnemyData(botNames[x]);
				for (int y = x + 1; y < botNames.length; y++)
				{
					T enemyData2 = this.getEnemyData(botNames[y]);
					if (enemyData1.isAlive() && enemyData2.isAlive())
					{
						double distanceSq = enemyData1.getLastScanState().getLocation()
								.distanceSq(enemyData2.getLastScanState().getLocation());
						enemyData1.setBotDistanceSq(botNames[y], distanceSq);
						enemyData2.setBotDistanceSq(botNames[x], distanceSq);
					}
					else
					{
						if (!enemyData1.isAlive())
						{
							enemyData2.removeDistanceSq(botNames[x]);
						}
						if (!enemyData2.isAlive())
						{
							enemyData1.removeDistanceSq(botNames[y]);
						}
					}
					enemyData1.setDistance(myLocation.distance(enemyData1.getLastScanState().getLocation()));
				}
			}
		}
	}

	private String[] getBotNames()
	{
		String[] botNames = new String[this.enemies.size()];
		this.enemies.keySet().toArray(botNames);
		return botNames;
	}

	public T getClosestLivingBot(Point2D.Double location)
	{
		T closestEnemy = null;
		double closestDistance = Double.POSITIVE_INFINITY;
		for (T enemyData : this.getAllEnemyData())
		{
			if (enemyData.isAlive())
			{
				double thisDistance = location.distanceSq(enemyData.getLastScanState().getLocation());
				if (thisDistance < closestDistance)
				{
					closestEnemy = enemyData;
					closestDistance = thisDistance;
				}
			}
		}
		return closestEnemy;
	}

	public T duelEnemy()
	{
		return this.duelEnemy;
	}

	protected boolean isMeleeBattle()
	{
		return (this.enemiesTotal > 1);
	}

	protected String warning()
	{
		return "WARNING (" + this.getLabel() + "): ";
	}

	abstract protected String getLabel();

	/**
	 * @return the duelEnemy
	 */
	public T getDuelEnemy()
	{
		return this.duelEnemy;
	}

	/**
	 * @param duelEnemy
	 *            the duelEnemy to set
	 */
	public void setDuelEnemy(T duelEnemy)
	{
		this.duelEnemy = duelEnemy;
	}

	/**
	 * @return the enemiesTotal
	 */
	public int getEnemiesTotal()
	{
		return this.enemiesTotal;
	}

	/**
	 * @param enemiesTotal
	 *            the enemiesTotal to set
	 */
	public void setEnemiesTotal(int enemiesTotal)
	{
		this.enemiesTotal = enemiesTotal;
	}

	/**
	 * @return the enemies
	 */
	public Map<String, T> getEnemies()
	{
		return this.enemies;
	}

	/**
	 * @param enemies
	 *            the enemies to set
	 */
	public void setEnemies(Map<String, T> enemies)
	{
		this.enemies = enemies;
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