package voidious.utils;

import java.awt.geom.Point2D;
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

public class RobotStateLog implements Cloneable
{
	private Map<Long, RobotState> robotStates;

	public RobotStateLog()
	{
		this.robotStates = new HashMap<>();
	}

	public void clear()
	{
		this.robotStates.clear();
	}

	public void addState(RobotState state)
	{
		this.robotStates.put(state.getTime(), state);
	}

	public RobotState getState(long time)
	{
		return this.getState(time, true);
	}

	private RobotState getState(long time, boolean interpolate)
	{
		if (this.robotStates.containsKey(time))
		{
			RobotState robotState = this.robotStates.get(time);
			return (interpolate || !robotState.isInterpolated()) ? robotState : null;
		}
		else if (interpolate)
		{
			RobotState beforeState = null;
			RobotState afterState = null;
			for (RobotState state : this.robotStates.values())
			{
				if (!state.isInterpolated())
				{
					if (state.getTime() < time)
					{
						if (beforeState == null || state.getTime() > beforeState.getTime())
						{
							beforeState = state;
						}
					}
					if (state.getTime() > time)
					{
						if (afterState == null || state.getTime() < afterState.getTime())
						{
							afterState = state;
						}
					}
				}
			}

			if (beforeState == null || afterState == null)
			{
				return null;
			}

			Interpolator interpolator = new Interpolator(time, beforeState.getTime(), afterState.getTime());
			RobotState interpolatedRobotState = new RobotState(interpolator.getLocation(beforeState.getLocation(),
					afterState.getLocation()), interpolator.getHeading(beforeState.getHeading(), afterState.getHeading()),
					interpolator.avg(beforeState.getVelocity(), afterState.getVelocity()), time, true);
			this.robotStates.put(time, interpolatedRobotState);
			return interpolatedRobotState;
		}
		else
		{
			return null;
		}
	}

	public double getDisplacementDistance(Point2D.Double location, long currentTime, long ticksAgo)
	{
		RobotState pastState = this.getState(currentTime - ticksAgo);
		pastState = (pastState == null) ? this.getOldestState() : pastState;
		return location.distance(pastState.getLocation());
	}

	private RobotState getOldestState()
	{
		return this.robotStates.get(Collections.min(this.robotStates.keySet()));
	}

	public void forAllStates(AllStateListener listener)
	{
		this.robotStates.values().forEach(listener::onRobotState);
	}

	public int size()
	{
		return this.robotStates.size();
	}

	@Override
	public Object clone()
	{
		RobotStateLog newLog = new RobotStateLog();
		for (Long time : this.robotStates.keySet())
		{
			newLog.addState(this.robotStates.get(time));
		}
		return newLog;
	}

	public interface AllStateListener
	{
		void onRobotState(RobotState state);
	}
}
