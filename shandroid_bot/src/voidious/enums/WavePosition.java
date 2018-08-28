package voidious.enums;

public enum WavePosition
{
	/**
	 * Wave has not reached enemy at all.
	 */
	MIDAIR(0, false),
	/**
	 * The wave is intersecting the enemy bot.
	 */
	BREAKING_FRONT(1, true),
	/**
	 * The wave is intersecting the enemy bot and its front edge (next tick) is
	 * past its center.
	 */
	BREAKING_CENTER(2, true),
	/**
	 * The wave is completely beyond the enemy bot.
	 */
	GONE(3, false);

	private int index;
	private boolean breaking;

	private WavePosition(int index, boolean breaking)
	{
		this.index = index;
		this.breaking = breaking;
	}

	/**
	 * @return the index
	 */
	public int getIndex()
	{
		return this.index;
	}

	/**
	 * @param index
	 *            the index to set
	 */
	public void setIndex(int index)
	{
		this.index = index;
	}

	/**
	 * @return the breaking
	 */
	public boolean isBreaking()
	{
		return this.breaking;
	}

	/**
	 * @param breaking
	 *            the breaking to set
	 */
	public void setBreaking(boolean breaking)
	{
		this.breaking = breaking;
	}
}