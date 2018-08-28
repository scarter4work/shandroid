package ags.utils.dataStructures;

public class MinBinaryHeap<T> extends BinaryHeap<T> implements MinHeap<T>
{
	public MinBinaryHeap()
	{
		super(defaultCapacity, -1);
	}

	@Override
	public void removeMin()
	{
		this.removeTip();
	}

	@Override
	public T getMin()
	{
		return this.getTip();
	}

	@Override
	public double getMinKey()
	{
		return this.getTipKey();
	}

	@Override
	public void replaceMin(double key, T value) 
	{
		this.replaceMin(key, value);
	}
}