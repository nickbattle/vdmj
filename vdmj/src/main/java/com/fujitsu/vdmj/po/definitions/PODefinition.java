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

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.definitions.visitors.POGetVariableNamesVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * The abstract parent of all definitions. A definition can represent a data
 * type, a value (constant), implicit or explicit functions, implicit or
 * explicit operations, module state, as well as various sorts of local variable
 * definition.
 */
public abstract class PODefinition extends PONode implements Serializable, Comparable<PODefinition>
{
	private static final long serialVersionUID = 1L;

	/** The name of the object being defined. */
	public final TCNameToken name;
	
	/** The scope of the name */
	public NameScope nameScope = null;
	
	/** A pointer to the enclosing class definition, if any. */
	public POClassDefinition classDefinition = null;	// Set in subclass constructors.
	
	/** A list of annotations, if any */
	public POAnnotationList annotations = null;

	/**
	 * Create a new definition of a particular name and location.
	 */
	public PODefinition(LexLocation location, TCNameToken tcNameToken)
	{
		super(location);
		this.name = tcNameToken;
	}
	
	public void setNameScope(NameScope scope)
	{
		this.nameScope = scope;
	}

	@Override
	abstract public String toString();
	
	/**
	 * For a state definition S, return a pattern like mk_S(a, b, ...) where the field patterns
	 * are the names of the state fields. Similarly with objects, using "obj_C" patterns.
	 */
	public String toPattern()
	{
		return "?";		// Only defined for state and ClassDefinitions
	}

	/**
	 * The definition with its types' module/class(es) explicit, if we are not the same as the
	 * location of this definition.
	 */
	public String toExplicitString(LexLocation from)
	{
		return toString();		// Overridden in defs with types
	}

	@Override
	public int compareTo(PODefinition o)
	{
		return name == null ? 0 : name.compareTo(o.name); 
	};

	@Override
	public boolean equals(Object other)		// Used for sets of definitions.
	{
		if (other instanceof PODefinition)
		{
			PODefinition odef = (PODefinition)other;
			return name != null && odef.name != null && name.equals(odef.name);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();		// Used for sets of definitions (see equals).
	}

	/**
	 * Return a list of variable names that would be defined by the definition.
	 */
	public final TCNameList getVariableNames()
	{
		TCNameList list = new TCNameList();
		list.addAll(apply(new POGetVariableNamesVisitor(), null));
		return list;
	}
	
	/**
	 * True, if the definition contains executable statements that update state.
	 */
	public boolean updatesState()
	{
		return false;
	}
	
	/**
	 * True, if the definition contains executable statements that read state.
	 */
	public boolean readsState()
	{
		return false;
	}

	/**
	 * Return the static type of the definition. For example, the type of a
	 * function or operation definition would be its parameter/result signature;
	 * the type of a value definition would be that value's type; the type of a
	 * type definition is the underlying type being defined.
	 * <p>
	 * Note that for Definitions which define multiple inner definitions (see
	 * {@link #getDefinitions}), this method returns the primary type - eg.
	 * the type of a function, not the types of its pre/post definitions.
	 *
	 * @return The primary type of this definition.
	 */
	abstract public TCType getType();

	/**
	 * Get a list of proof obligations for the definition.
	 *
	 * @param ctxt The call context.
	 * @return A list of POs.
	 */
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		return new ProofObligationList();
	}

	/**
	 * Implemented by all definitions to allow visitor processing.
	 */
	abstract public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg);
}
