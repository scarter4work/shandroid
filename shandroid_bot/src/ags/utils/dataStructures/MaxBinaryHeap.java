package ags.utils.dataStructures;

public class MaxBinaryHeap<T> extends BinaryHeap<T> implements MaxHeap<T>
{
	public MaxBinaryHeap()
	{
		super(defaultCapacity, 1);
	}

	@Override
	public void replaceMax(double key, T value)
	{
		this.replaceTip(key, value);
	}

	@Override
	public T getMax()
	{
		return this.getTip();
	}

	@Override
	public double getMaxKey()
	{
		return this.getTipKey();
	}
}