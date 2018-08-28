package voidious.move;

import voidious.utils.DiaUtils;

class DistanceController
{
	private static final double DESIRED_DISTANCE = 650;
	private static final double MAX_ATTACK_ANGLE = Math.PI * .45;

	DistanceController()
	{
		super();
	}

	double surfAttackAngle(double currentDistance)
	{
		return this.attackAngle(currentDistance, 0.6);
	}

	double orbitAttackAngle(double currentDistance)
	{
		return this.attackAngle(currentDistance, 1.65);
	}

	private double attackAngle(double currentDistance, double offsetMultiplier)
	{
		double distanceFactor = (currentDistance - DESIRED_DISTANCE) / DESIRED_DISTANCE;
		return DiaUtils.limit(-MAX_ATTACK_ANGLE, distanceFactor * offsetMultiplier, MAX_ATTACK_ANGLE);
	}
}