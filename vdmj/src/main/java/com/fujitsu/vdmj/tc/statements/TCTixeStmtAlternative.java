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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.patterns.TCPatternBind;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTixeStmtAlternative
{
	public final TCPatternBind patternBind;
	public final TCStatement statement;

	public TCTixeStmtAlternative(TCPatternBind patternBind, TCStatement stmt)
	{
		this.patternBind = patternBind;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return patternBind + " |-> " + statement;
	}

	public void typeCheck(Environment base, NameScope scope, TCType ext, TCType constraint, boolean mandatory)
	{
		// Make a union with "?" so that pattern always matches
		TCUnionType union = new TCUnionType(ext.location, ext, new TCUnknownType(ext.location));
		patternBind.typeCheck(base, scope, union);

		TCDefinitionList defs = patternBind.getDefinitions();
		defs.typeCheck(base, scope);
		Environment local = new FlatCheckedEnvironment(defs, base, scope);
		statement.typeCheck(local, scope, constraint, mandatory);
		local.unusedCheck();
	}
}
