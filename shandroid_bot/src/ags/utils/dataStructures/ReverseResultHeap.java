package ags.utils.dataStructures;

public class ReverseResultHeap extends ResultHeap
{
	public ReverseResultHeap(int size)
	{
		super(size);
	}

	@Override
	public void addValue(double dist, Object value)
	{
		int values = this.getValues();

		// If there is still room in the heap
		if (values < this.getSize())
		{
			// Insert new value at the end
			this.getData()[values] = value;
			this.getDistance()[values] = dist;
			this.upHeapify(values);
			values++;
			this.setValues(values);
		}
		// If there is no room left in the heap, and the new entry is higher
		// than the min entry
		else if (dist > this.getDistance()[0])
		{
			// Replace the min entry with the new entry
			this.getData()[0] = value;
			this.getDistance()[0] = dist;
			this.downHeapify(0);
		}
	}

	@Override
	protected void upHeapify(int c)
	{
		for (int p = (c - 1) / 2; c != 0 && this.getDistance()[c] < this.getDistance()[p]; c = p, p = (c - 1) / 2)
		{
			Object pData = this.getData()[p];
			double pDist = this.getDistance()[p];
			this.getData()[p] = this.getData()[c];
			this.getDistance()[p] = this.getDistance()[c];
			this.getData()[c] = pData;
			this.getDistance()[c] = pDist;
		}
	}

	@Override
	protected void downHeapify(int p)
	{
		for (int c = p * 2 + 1; c < this.getValues(); p = c, c = p * 2 + 1)
		{
			if (c + 1 < this.getValues() && this.getDistance()[c] > this.getDistance()[c + 1])
			{
				c++;
			}
			if (this.getDistance()[p] > this.getDistance()[c])
			{
				// Swap the points
				Object pData = this.getData()[p];
				double pDist = this.getDistance()[p];
				this.getData()[p] = this.getData()[c];
				this.getDistance()[p] = this.getDistance()[c];
				this.getData()[c] = pData;
				this.getDistance()[c] = pDist;
			}
			else
			{
				break;
			}
		}
	}
}