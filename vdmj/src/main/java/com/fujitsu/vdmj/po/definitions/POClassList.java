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
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.PublicClassEnvironment;

/**
 * A class for holding a list of ClassDefinitions.
 */
public class POClassList extends POMappedList<TCClassDefinition, POClassDefinition>
{
	private static final long serialVersionUID = 1L;
	
	private final TCClassList tcclasses;

	public POClassList()
	{
		super();
		tcclasses = null;
	}

	public POClassList(TCClassList from) throws Exception
	{
		super(from);
		tcclasses = from;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (POClassDefinition c: this)
		{
			sb.append(c.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public ProofObligationList getProofObligations()
	{
		ProofObligationList obligations = new ProofObligationList();
		
		for (POClassDefinition c: this)
		{
			obligations.addAll(c.getProofObligations(new POContextStack(), new PublicClassEnvironment(tcclasses)));
		}

		return obligations;
	}
}
