/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.modules;

import java.io.Serializable;

import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

/**
 * A class holding all the details for one module.
 */
public class POModule extends PONode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The module name. */
	public final TCIdentifierToken name;
	/** A list of definitions created in the module. */
	public final PODefinitionList defs;
	/** A list of annotations, if any */
	public final POAnnotationList annotations;

	/**
	 * Create a module from the given name and definitions.
	 */
	public POModule(POAnnotationList annotations, TCIdentifierToken name, PODefinitionList defs)
	{
		this.annotations = annotations;
		this.name = name;
		this.defs = defs;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("module " + name.getName() + "\n");

		if (defs != null)
		{
			sb.append("\ndefinitions\n\n");

			for (PODefinition def: defs)
			{
				sb.append(def.toString() + "\n");
			}
		}

		sb.append("\nend " + name.getName() + "\n");

		return sb.toString();
	}

	public ProofObligationList getProofObligations()
	{
		ProofObligationList list =
				(annotations != null) ? annotations.before(this) : new ProofObligationList();
				
		list.addAll(defs.getProofObligations(new POContextStack()));
		
		if (annotations != null) annotations.after(this, list);
		return list;
	}
}
