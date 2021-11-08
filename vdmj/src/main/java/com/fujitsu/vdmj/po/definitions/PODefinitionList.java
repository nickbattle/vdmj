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

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.PONameContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;

/**
 * A class to hold a list of Definitions.
 */
@SuppressWarnings("serial")
public class PODefinitionList extends POMappedList<TCDefinition, PODefinition>
{
	public PODefinitionList(TCDefinitionList from) throws Exception
	{
		super(from);
	}

	public PODefinitionList()
	{
		super();
	}

	public PODefinitionList(POExplicitOperationDefinition invariant)
	{
		add(invariant);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (PODefinition d: this)
		{
			for (TCNameToken name: d.getVariableNames())
			{
				sb.append(name.getExplicit(true) + ":" + d.getType());
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		for (PODefinition d: this)
		{
			ctxt.push(new PONameContext(d.getVariableNames()));
			obligations.addAll(d.getProofObligations(ctxt, env));
			ctxt.pop();
		}

		return obligations;
	}
}
