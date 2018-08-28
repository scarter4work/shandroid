package voidious.utils.genetic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class DnaSequence
{
	private ArrayList<Gene> geneLayout;
	private HashMap<String, Gene> geneMap;
	protected int length = 0;

	private DnaSequence(List<Gene> genes)
	{
		this.geneLayout = new ArrayList<>();
		this.geneMap = new HashMap<>();

		if (genes != null)
		{
			for (Gene g : genes)
			{
				this.addGene((Gene) g.clone());
			}
		}
	}

	public DnaSequence()
	{
		this(new ArrayList<>());
	}

	public void addGene(Gene gene)
	{
		// TODO: handle existing Gene with this name somehow
		// replace it? throw exception? overwrite it? delete/clear Genes?
		this.geneMap.put(gene.getName(), gene);
		this.geneLayout.add(gene);
		gene.setPosition(this.length);
		this.length += gene.getSize();
	}

	Gene getGene(String name)
	{
		return this.geneMap.get(name);
	}

	public int length()
	{
		return this.length;
	}

	ArrayList<Gene> getGeneLayout()
	{
		return this.geneLayout;
	}
}