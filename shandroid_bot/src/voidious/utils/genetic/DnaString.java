package voidious.utils.genetic;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * Copyright (c) 2011-2012 - Voidious
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

// TODO: allow for a "header" that doesn't change with evolution. info about
// structure of DnaSequence
public class DnaString implements Cloneable
{
	private static final int SEEDED = 3;

	private DnaSequence dnaSequence;
	private BitSet bits;
	private int sourceType;

	private DnaString(DnaSequence dnaSequence, BitSet bits, int sourceType)
	{
		this.dnaSequence = dnaSequence;
		if (bits == null)
		{
			this.bits = new BitSet(dnaSequence.length());
		}
		else
		{
			this.bits = (BitSet) bits.clone();
		}
		this.sourceType = sourceType;
	}

	private DnaString(DnaSequence dnaSequence, String numString, int sourceType)
	{
		this(dnaSequence, parseString(numString, dnaSequence.length()), sourceType);
	}

	public DnaString(DnaSequence dnaSequence, String numString)
	{
		this(dnaSequence, numString, SEEDED);
	}

	private DnaSequence getDnaSequence()
	{
		return this.dnaSequence;
	}

	private BitSet getBitSet()
	{
		return this.bits;
	}

	private boolean getBit(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.BIT);
		return this.bits.get(g.getPosition());
	}

	private byte getByte(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.BYTE);
		long value = this.get(g.isNegatives() ? g.getPosition() : g.getPosition() + 1,
				g.isNegatives() ? g.getSize() : g.getSize() - 1, g.isNegatives());
		if (g.getMax() != Long.MAX_VALUE)
		{
			value = value % (g.getMax() + 1);
		}
		return (byte) value;
	}

	private short getShort(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.SHORT);
		long value = this.get(g.isNegatives() ? g.getPosition() : g.getPosition() + 1,
				g.isNegatives() ? g.getSize() : g.getSize() - 1, g.isNegatives());
		if (g.getMax() != Long.MAX_VALUE)
		{
			value = value % (g.getMax() + 1);
		}
		return (short) value;
	}

	private int getInt(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.INTEGER);
		long value = this.get(g.isNegatives() ? g.getPosition() : g.getPosition() + 1,
				g.isNegatives() ? g.getSize() : g.getSize() - 1, g.isNegatives());
		if (g.getMax() != Long.MAX_VALUE)
			value = value % (g.getMax() + 1);
		return (int) value;
	}

	private long getLong(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.LONG);
		long value = this.get(g.isNegatives() ? g.getPosition() : g.getPosition() + 1,
				g.isNegatives() ? g.getSize() : g.getSize() - 1, g.isNegatives());
		if (g.getMax() != Long.MAX_VALUE)
		{
			value = value % (g.getMax() + 1);
		}
		return value;
	}

	public long getNumber(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.NUMBER);
		long value = this.get(g.getPosition(), g.getSize(), g.isNegatives());
		if (g.getMax() != Long.MAX_VALUE)
		{
			value = value % (g.getMax() + 1);
		}
		return value;
	}

	private float getFloat(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.FLOAT);
		float f = Float.intBitsToFloat((int) (this.get(g.getPosition(), g.getSize(), true)));
		if (!g.isNegatives() && f < 0)
		{
			f *= -1; // TODO: this doesn't seem good.
		}
		if (g.getMax() != Long.MAX_VALUE)
		{
			f = f % g.getMax();
		}
		return f;
	}

	private double getDouble(String geneName)
	{
		Gene g = this.dnaSequence.getGene(geneName);
		this.checkGeneType(g, GeneType.DOUBLE);
		double d = Double.longBitsToDouble(((this.get(g.getPosition(), g.getSize(), true))));
		if (!g.isNegatives() && d < 0)
		{
			d *= -1; // TODO: this doesn't seem good.
		}
		if (g.getMax() != Long.MAX_VALUE)
		{
			d = d % g.getMax();
		}
		return d;
	}

	public long get(int position, int numBits, boolean negatives)
	{
		BitSet bits = this.bits.get(position, position + numBits);
		long value = 0;
		long pow = 1;
		for (int x = 0; x < (negatives ? numBits - 1 : numBits); x++)
		{
			boolean bit = bits.get(numBits - x - 1);
			if (bit)
			{
				value += pow;
			}
			pow *= 2;
		}

		if (negatives && bits.get(0))
		{
			value -= pow;
		}

		return value;
	}

	private void checkGeneType(Gene g, GeneType type)
	{
		if (g.getType() != type)
		{
			throw new GeneTypeException("Gene " + g.getName() + " is not a " + type.name().toLowerCase() + "!");
		}
	}

	public String bitString()
	{
		StringBuilder sb = new StringBuilder();
		for (int x = 0; x < this.dnaSequence.length(); x++)
		{
			if (this.bits.get(x))
			{
				sb.append("1");
			}
			else
			{
				sb.append("0");
			}
		}
		return sb.toString();
	}

	private static BitSet parseString(String numString, int length)
	{
		if (numString.length() > 2 && numString.substring(0, 2).equals("0x"))
		{
			return parseHexString(numString, length);
		}
		return parseBitString(numString);
	}

	private static BitSet parseBitString(String bitString)
	{
		BitSet bits = new BitSet(bitString.length());
		char[] bitChars = bitString.toCharArray();
		for (int x = 0; x < bitChars.length; x++)
		{
			if (bitChars[x] == '1')
			{
				bits.set(x);
			}
		}
		return bits;
	}

	private static BitSet parseHexString(String hexString, int length)
	{
		StringBuilder sb = new StringBuilder();
		char[] hexChars = hexString.toCharArray();
		for (int x = 2; x < hexChars.length; x++)
		{
			int i = Integer.parseInt(Character.toString(hexChars[x]), 16);
			sb.append((i >> 3));
			sb.append(((i >> 2) & 0x1));
			sb.append(((i >> 1) & 0x1));
			sb.append((i & 0x1));
		}

		while (sb.length() > length)
		{
			sb.deleteCharAt(sb.length() - 1);
		}

		return parseBitString(sb.toString());
	}

	@Override
	public Object clone()
	{
		return new DnaString(this.dnaSequence, this.bits, this.sourceType);
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof DnaString))
		{
			return false;
		}

		DnaString that = (DnaString) o;

		if (this.getDnaSequence() != that.getDnaSequence())
		{
			return false;
		}

		if (this.getBitSet().equals(that.getBitSet()))
		{
			return true;
		}

		boolean same = true;
		ArrayList<Gene> geneLayout = this.getDnaSequence().getGeneLayout();
		for (Gene g : geneLayout)
		{
			switch (g.getType())
			{
				case BIT:
					same = this.getBit(g.getName()) == that.getBit(g.getName());
					break;
				case BYTE:
					same = this.getByte(g.getName()) == that.getByte(g.getName());
					break;
				case SHORT:
					same = this.getShort(g.getName()) == that.getShort(g.getName());
					break;
				case INTEGER:
					same = this.getInt(g.getName()) == that.getInt(g.getName());
					break;
				case LONG:
					same = this.getLong(g.getName()) == that.getLong(g.getName());
					break;
				case FLOAT:
					same = Math.abs(this.getFloat(g.getName()) - that.getFloat(g.getName())) < 0.0000001;
					break;
				case DOUBLE:
					same = Math.abs(this.getDouble(g.getName()) - that.getDouble(g.getName())) < 0.00000000001;
				case NUMBER:
					same = same && (this.getNumber(g.getName()) == that.getNumber(g.getName()));
					break;
			}

			if (!same)
			{
				break;
			}
		}

		return same;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		ArrayList<Gene> geneLayout = this.dnaSequence.getGeneLayout();
		boolean newLine = false;
		for (Gene g : geneLayout)
		{
			if (newLine)
			{
				sb.append("\n");
			}
			newLine = true;

			sb.append(g.getName());
			sb.append(": ");
			switch (g.getType())
			{
				case BIT:
					sb.append(this.getBit(g.getName()));
					break;
				case BYTE:
					sb.append(this.getByte(g.getName()));
					break;
				case SHORT:
					sb.append(this.getShort(g.getName()));
					break;
				case INTEGER:
					sb.append(this.getInt(g.getName()));
					break;
				case LONG:
					sb.append(this.getLong(g.getName()));
					break;
				case FLOAT:
					sb.append(this.getFloat(g.getName()));
					break;
				case DOUBLE:
					sb.append(this.getDouble(g.getName()));
					break;
				case NUMBER:
					sb.append(this.getNumber(g.getName()));
					break;
				default:
					break;
			}
		}

		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
	}

	private class GeneTypeException extends RuntimeException
	{
		private static final long serialVersionUID = -881296837849502009L;

		GeneTypeException(String message)
		{
			super(message);
		}
	}
}
