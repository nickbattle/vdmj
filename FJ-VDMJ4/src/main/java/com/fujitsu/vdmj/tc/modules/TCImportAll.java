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

package com.fujitsu.vdmj.tc.modules;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCImportAll extends TCImport
{
	private static final long serialVersionUID = 1L;

	public TCImportAll(TCNameToken name)
	{
		super(name, null);
	}

	@Override
	public TCDefinitionList getDefinitions(TCModule module)
	{
		from = module;

		if (from.exportdefs.isEmpty())
		{
			report(3190, "Import all from module with no exports?");
		}

		TCDefinitionList imported = new TCDefinitionList();

		for (TCDefinition d: from.exportdefs)
		{
			TCDefinition id = new TCImportedDefinition(location, d);
			id.markUsed();	// So imports all is quiet
			imported.add(id);
		}

		return imported;	// The lot!
	}

	@Override
	public String toString()
	{
		return "import all";
	}

	@Override
	public void typeCheck(Environment env)
	{
		return;		// Implicitly OK.
	}
}
