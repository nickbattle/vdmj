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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.statements.POClassInvariantStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.PONameContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;

/**
 * A class to represent a VDM++ class definition.
 */
public class POClassDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;

	/** This class' class type. */
	public final TCClassType classtype;
	/** The definitions in this class (excludes superclasses). */
	public final PODefinitionList definitions;
	/** The class invariant operation definition, if any. */
	public final POExplicitOperationDefinition invariant;
	/** True is we have constructors */
	public final boolean hasConstructors;
	/** The TCClassDefinition for typecheck */
	public final TCClassDefinition tcdef;

	/**
	 * Create a class definition with the given name, type and list of local definitions.
	 */
	public POClassDefinition(POAnnotationList annotations, TCNameToken className, TCClassType classtype,
		PODefinitionList definitions, POExplicitOperationDefinition invariant, boolean hasConstructors,
		TCClassDefinition tcdef)
	{
		super(className.getLocation(), className);
		this.annotations = annotations;
		this.classtype = classtype;
		this.definitions = definitions;
		this.invariant = invariant;
		this.hasConstructors = hasConstructors;
		this.tcdef = tcdef;
	}

	/**
	 * Get this class' POClassType.
	 * @see org.PODefinition.vdmj.definitions.Definition#getType()
	 */
	@Override
	public TCType getType()
	{
		return classtype;
	}

	@Override
	public String toString()
	{
		return	"class " + name.getName() + "\n";
	}

	@Override
	public String toPattern()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("obj_");
		sb.append(name);
		sb.append("(");
		String sep = "";

		for (PODefinition field: definitions)
		{
			if (field instanceof POInstanceVariableDefinition)
			{
				sb.append(sep);
				sb.append(field.name.getName());
				sb.append(" |-> ");
				sb.append(field.name.getName());
				sep = ", ";
			}
		}
		
		sb.append(")");
		return sb.toString();
	}
	
	public String toNew()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		sb.append(name);
		sb.append("(");
		String sep = "";

		for (PODefinition field: definitions)
		{
			if (field instanceof POInstanceVariableDefinition)
			{
				sb.append(sep);
				sb.append(field.name.getName());
				sep = ", ";
			}
		}
		
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * True if the class has a constructor that has one parameter for each variable.
	 * This is used in POs that need to call "new X(a, b, c)".
	 */
	public boolean hasNew()
	{
		TCTypeList allvars = new TCTypeList();
		
		for (PODefinition field: definitions)
		{
			if (field instanceof POInstanceVariableDefinition)
			{
				POInstanceVariableDefinition iv = (POInstanceVariableDefinition)field;
				allvars.add(iv.type);
			}
		}

		for (PODefinition field: definitions)
		{
			if (field instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition op = (POExplicitOperationDefinition)field;
				
				if (op.isConstructor)
				{
					if (op.type.parameters.equals(allvars))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment publicEnv)
	{
		ProofObligationList list =
				(annotations != null) ? annotations.poBefore(this) : new ProofObligationList();
		
		Environment env = new PrivateClassEnvironment(tcdef, publicEnv);
		Environment local = new FlatEnvironment(tcdef.getSelfDefinition(), env);

		for (PODefinition def: definitions)
		{
			ctxt.push(new PONameContext(def.getVariableNames()));
			list.addAll(def.getProofObligations(ctxt, new POGState(), local));
		}
		
		list.typeCheck(tcdef.name, local);
		
		if (annotations != null) annotations.poAfter(this, list);
		return list;
	}

	public PODefinitionList getInvDefs()
	{
		// Body of invariant operation is a list of invdefs
		POClassInvariantStatement body = (POClassInvariantStatement)invariant.body;
		return body.invdefs;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseClassDefinition(this, arg);
	}
}
