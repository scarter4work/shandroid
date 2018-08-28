package voidious.utils.geom;

import voidious.utils.DiaUtils;

import java.awt.geom.Point2D;

/*
 * Copyright (c) 2009-2011 - Voidious
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

// (x-h)^2 + (y-k)^2 = r^2
public class Circle
{
	private double h;
	private double k;
	private double r;

	public Circle(double x, double y, double r)
	{
		this.h = x;
		this.k = y;
		this.r = r;
	}

	public Circle(Point2D.Double p, double r)
	{
		this(p.x, p.y, r);
	}

	public Point2D.Double[] intersects(LineSeg seg)
	{
		return this.intersects(seg, false);
	}

	private Point2D.Double[] intersects(LineSeg seg, boolean inverted)
	{
		double a = (seg.getM() * seg.getM()) + 1;
		double b = 2 * ((seg.getB() * seg.getM()) - (this.k * seg.getM()) - this.h);
		double c = (this.h * this.h) + (this.k * this.k) + (seg.getB() * seg.getB()) - (2 * seg.getB() * this.k)
				- (this.r * this.r);

		Point2D.Double[] solutions = new Point2D.Double[] { null, null };
		int i = 0;

		if (a == Double.POSITIVE_INFINITY)
		{
			LineSeg invSeg = new LineSeg(seg.getY1(), seg.getX1(), seg.getY2(), seg.getX2());
			Circle invCircle = new Circle(this.k, this.h, this.r);
			Point2D.Double[] invSolutions = invCircle.intersects(invSeg, true);

			for (Point2D.Double invSolution : invSolutions) {
				if (invSolution != null) {
					double t = invSolution.x;
					invSolution.x = invSolution.y;
					invSolution.y = t;
				}
			}

			return invSolutions;
		}

		double discrim = (b * b) - (4 * a * c);
		if (discrim < 0)
		{
			return solutions;
		}

		double sqrtDiscrim = Math.sqrt(discrim);
		double x1 = (-b + sqrtDiscrim) / (2 * a);
		double y1 = (seg.getM() * x1) + seg.getB();

		if (x1 > seg.getxMin() && x1 < seg.getxMax())
		{
			solutions[i++] = new Point2D.Double(x1, y1);
		}

		if (sqrtDiscrim > 0)
		{
			double x2 = (-b - sqrtDiscrim) / (2 * a);
			double y2 = (seg.getM() * x2) + seg.getB();
			if (x2 > seg.getxMin() && x2 < seg.getxMax())
			{
				i++;
				solutions[i] = new Point2D.Double(x2, y2);
			}
		}

		return solutions;
	}

	public boolean contains(Point2D.Double p)
	{
		double z = DiaUtils.square(p.x - this.h) + DiaUtils.square(p.y - this.k);
		return (z < this.r * this.r);
	}

	/**
	 * @return the h
	 */
	public double getH()
	{
		return this.h;
	}

	/**
	 * @param h
	 *            the h to set
	 */
	public void setH(double h)
	{
		this.h = h;
	}

	/**
	 * @return the k
	 */
	public double getK()
	{
		return this.k;
	}

	/**
	 * @param k
	 *            the k to set
	 */
	public void setK(double k)
	{
		this.k = k;
	}

	/**
	 * @return the r
	 */
	public double getR()
	{
		return this.r;
	}

	/**
	 * @param r
	 *            the r to set
	 */
	public void setR(double r)
	{
		this.r = r;
	}
}