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

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.ast.traces.ASTTraceDefinition;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinitionTerm;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class representing a sequence of trace definitions.
 */
@SuppressWarnings("serial")
public class TCTraceDefinitionTerm extends TCMappedList<ASTTraceDefinition, TCTraceDefinition>
{
	public TCTraceDefinitionTerm()
	{
		super();
	}

	public TCTraceDefinitionTerm(ASTTraceDefinitionTerm from) throws Exception
	{
		super(from);
	}

	public void typeCheck(Environment base, NameScope scope)
	{
		for (TCTraceDefinition def: this)
		{
			def.typeCheck(base, scope);
		}
	}
}
