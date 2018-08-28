package voidious.utils.genetic;

public class Gene implements Cloneable
{
	private final String name;
	private final GeneType type;
	private final long max;
	private final boolean negatives;
	private final int size;
	private int position;

	private Gene(String name, GeneType type, long max, boolean negatives)
	{
		this(name, type, max, negatives, type.getSize());
	}

	private Gene(String name, GeneType type, long max, boolean negatives, int size)
	{
		if (type == GeneType.NUMBER && size == 0)
		{
			throw new IllegalArgumentException("Must specify a size for GeneType.NUMBER!");
		}
		this.name = name;
		this.type = type;
		this.max = max;
		this.negatives = negatives;
		this.size = size;
		this.position = 0;
	}

	public Gene(String name, int numBits)
	{
		this(name, numBits, Long.MAX_VALUE);
	}

	private Gene(String name, int numBits, long max)
	{
		this(name, GeneType.NUMBER, max, false, numBits);
	}

	@Override
	public Object clone()
	{
		return new Gene(this.name, this.type, this.max, this.negatives);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.max ^ (this.max >>> 32));
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = prime * result + (this.negatives ? 1231 : 1237);
		result = prime * result + this.position;
		result = prime * result + this.size;
		result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Gene other = (Gene) obj;
		if (this.max != other.max)
			return false;
		if (this.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.name.equals(other.name))
			return false;
		return this.negatives == other.negatives && this.position == other.position && this.size == other.size && this.type == other.type;
	}

	/**
	 * @return the position
	 */
	public int getPosition()
	{
		return this.position;
	}

	/**
	 * @param position
	 *            the position to set
	 */
	public void setPosition(int position)
	{
		this.position = position;
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * @return the type
	 */
	public GeneType getType()
	{
		return this.type;
	}

	/**
	 * @return the max
	 */
	public long getMax()
	{
		return this.max;
	}

	/**
	 * @return the negatives
	 */
	public boolean isNegatives()
	{
		return this.negatives;
	}

	/**
	 * @return the size
	 */
	public int getSize()
	{
		return this.size;
	}
}