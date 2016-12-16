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

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold a type definition.
 */
public class POTypeDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final TCInvariantType type;
	public final POPattern invPattern;
	public final POExpression invExpression;

	public POTypeDefinition(TCNameToken name, TCInvariantType type, POPattern invPattern,
		POExpression invExpression)
	{
		super(name.getLocation(), name);

		this.type = type;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
	}

	@Override
	public String toString()
	{
		return name.getName() + " = " + type.toDetailedString() +
				(invPattern == null ? "" :
					"\n\tinv " + invPattern + " == " + invExpression);
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public TCNameList getVariableNames()
	{
		// This is only used in VDM++ type inheritance
		return new TCNameList(name);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = new ProofObligationList();

		if (invExpression != null)
		{
			list.addAll(invExpression.getProofObligations(ctxt));
		}

		return list;
	}
}
