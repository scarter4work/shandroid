package voidious.gun.formulas;

import voidious.utils.DistanceFormula;
import voidious.utils.Wave;

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

public class MeleeFormula extends DistanceFormula
{

	public MeleeFormula()
	{
		this.weights = new double[] { 4, 6, 2, 4, 3, 1, 1 };
	}

	@Override
	public double[] dataPointFromWave(Wave w, boolean aiming)
	{
		return new double[] { (Math.min(91, w.getTargetDistance() / w.getBulletSpeed()) / 91),
				(((w.getTargetVelocitySign() * w.getTargetVelocity()) + 0.1) / 8.1),
				((Math.cos(w.getTargetRelativeHeading()) + 1) / 2), (Math.min(1.0, w.getTargetWallDistance())),
				(Math.min(1.0, w.getTargetRevWallDistance())), (w.getTargetDl8t() / 64), (w.getTargetDl20t() / 160) };
	}
}
