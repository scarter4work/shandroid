package ags.utils.dataStructures;

public interface MinHeap<T>
{
	int size();

	void offer(double key, T value);

	void removeMin();

	T getMin();
	
	void replaceMin(double key, T value);

	double getMinKey();
}