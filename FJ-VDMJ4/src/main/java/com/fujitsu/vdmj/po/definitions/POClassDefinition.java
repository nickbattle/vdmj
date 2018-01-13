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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.po.statements.POClassInvariantStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;

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

	/**
	 * Create a class definition with the given name, type and list of local definitions.
	 */
	public POClassDefinition(TCNameToken className, TCClassType classtype,
		PODefinitionList definitions, POExplicitOperationDefinition invariant, boolean hasConstructors)
	{
		super(className.getLocation(), className);
		this.classtype = classtype;
		this.definitions = definitions;
		this.invariant = invariant;
		this.hasConstructors = hasConstructors;
	}

	/**
	 * Get a list of the names of the variables defined locally.
	 * @see org.PODefinition.vdmj.definitions.Definition#getVariableNames()
	 */
	@Override
	public TCNameList getVariableNames()
	{
		return definitions.getVariableNames();
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
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return definitions.getProofObligations(ctxt);
	}

	public PODefinitionList getInvDefs()
	{
		// Body of invariant operation is a list of invdefs
		POClassInvariantStatement body = (POClassInvariantStatement)invariant.body;
		return body.invdefs;
	}
}
