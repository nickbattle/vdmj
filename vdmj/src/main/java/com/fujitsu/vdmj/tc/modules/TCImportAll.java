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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.modules;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCImportAll extends TCImport
{
	private static final long serialVersionUID = 1L;
	private final AtomicBoolean importAllUsed;

	public TCImportAll(TCNameToken name)
	{
		this(name, false);
	}

	public TCImportAll(TCNameToken name, boolean used)
	{
		super(name, null);
		
		// This is set up once and re-used when the getDefinitions are rebuilt at the
		// end of the typecheck, so that the final unusedCheck reflects the typechecking.
		importAllUsed = new AtomicBoolean(used);
	}

	@Override
	public TCDefinitionList getDefinitions(TCModule module)
	{
		from = module;

		if (from.exportdefs.isEmpty())
		{
			if (from.exports == null)
			{
				report(3190, "Import all from module with no exports clause?");
			}
			else
			{
				report(3190, "Import all from module with no exported definitions?");
			}
		}

		TCDefinitionList imported = new TCDefinitionList();

		for (TCDefinition d: from.exportdefs)
		{
			// Note, importAllUsed is saved between calls to getDefinitions
			TCDefinition id = new TCImportedDefinition(location, d, importAllUsed);
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

	@Override
	public boolean isExpectedKind(TCDefinition def)
	{
		return true;
	}

	@Override
	public String kind()
	{
		return "all";
	}

	@Override
	public <R, S> R apply(TCImportExportVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImportAll(this, arg);
	}
}
