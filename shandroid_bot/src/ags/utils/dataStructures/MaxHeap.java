package ags.utils.dataStructures;

public interface MaxHeap<T> {
    int size();
    void offer(double key, T value);
    void replaceMax(double key, T value);
    T getMax();
    double getMaxKey();
}