package voidious.utils;

import java.awt.geom.Point2D;

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

public class MaxEscapeTarget
{
	private final double angle;
	private final Point2D.Double location;
	private final long time;
	private final boolean hitWall;

	MaxEscapeTarget(double angle, Point2D.Double location, long time, boolean hitWall)
	{
		this.angle = angle;
		this.location = location;
		this.time = time;
		this.hitWall = hitWall;
	}

	/**
	 * @return the angle
	 */
	public double getAngle()
	{
		return this.angle;
	}

	/**
	 * @return the location
	 */
	public Point2D.Double getLocation()
	{
		return this.location;
	}

	/**
	 * @return the time
	 */
	public long getTime()
	{
		return this.time;
	}

	/**
	 * @return the hitWall
	 */
	public boolean isHitWall()
	{
		return this.hitWall;
	}
}