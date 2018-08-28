package voidious.utils.genetic;

enum GeneType
{
	BIT(1), BYTE(8), SHORT(16), INTEGER(32), LONG(64), FLOAT(32), DOUBLE(64), NUMBER(0);

	private int _size;

	GeneType(int size)
	{
		this._size = size;
	}

	public int getSize()
	{
		return this._size;
	}
}