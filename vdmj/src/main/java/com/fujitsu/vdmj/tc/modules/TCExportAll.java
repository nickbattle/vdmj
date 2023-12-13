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

package com.fujitsu.vdmj.tc.modules;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCExportAll extends TCExport
{
	private static final long serialVersionUID = 1L;

	public TCExportAll(LexLocation location)
	{
		super(location);
	}

	@Override
	public TCDefinitionList getDefinition(TCDefinitionList actualDefs)
	{
		TCDefinitionList list = new TCDefinitionList();
		
		for (TCDefinition d: actualDefs)
		{
			// if (!(d instanceof TCStateDefinition))	// Everything except state defs
			{
				list.add(d);
			}
		}
		
		return list;
	}

	@Override
	public TCDefinitionList getDefinition()
	{
		return new TCDefinitionList();	// Nothing <shrug>
	}

	@Override
	public String toString()
	{
		return "export all";
	}

	@Override
	public void typeCheck(Environment env, TCDefinitionList actualDefs)
	{
		return;		// Implicitly OK.
	}

	@Override
	public <R, S> R apply(TCImportExportVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExportAll(this, arg);
	}
}
