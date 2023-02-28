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

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCImportedType extends TCImport
{
	private static final long serialVersionUID = 1L;
	public final TCTypeDefinition def;

	public TCImportedType(TCNameToken name, TCTypeDefinition def, TCNameToken renamed)
	{
		super(name, renamed);
		this.def = def;
	}

	@Override
	public String toString()
	{
		return "import type " +
				(def == null ? name.getName() : def.toString()) +
				(renamed == null ? "" : " renamed " + renamed.getName());
	}

	@Override
	public TCDefinitionList getDefinitions(TCModule module)
	{
		TCDefinitionList list = new TCDefinitionList();
		from = module;
		TCDefinition expdef = from.exportdefs.findType(name, null);

		if (expdef == null)
		{
			report(3191, "No export declared for import of type " + name + " from " + from.name);
		}
		else
		{
			if (renamed != null)
			{
				expdef = new TCRenamedDefinition(renamed, expdef);
			}
			else
			{
				expdef = new TCImportedDefinition(name.getLocation(), expdef, null);
			}

			list.add(expdef);
		}

		return list;
	}

	@Override
	public void typeCheck(Environment env)
	{
		TCTypeDefinition expdef = null;

		if (from != null)
		{
			TCDefinition edef = from.exportdefs.findType(name, null);
			checkKind(edef);
			
			if (edef instanceof TCTypeDefinition)
			{
				expdef = (TCTypeDefinition)edef;
			}
		}
		
		if (def != null)
		{
			def.type = (TCInvariantType)def.type.typeResolve(env);
			TypeComparator.checkComposeTypes(def.type, env, false);

			if (expdef != null)
			{
				TCType exptype = expdef.getType().typeResolve(env);

				// TypeComparator.compatible(def.type, exptype))
				if (!def.type.toDetailedString().equals(exptype.toDetailedString()) ||
					!String.valueOf(def.invExpression).equals(String.valueOf(expdef.invExpression)) ||
					!String.valueOf(def.ordExpression).equals(String.valueOf(expdef.ordExpression)) ||
					!String.valueOf(def.eqExpression).equals(String.valueOf(expdef.eqExpression)))
				{
					report(3192, "Type import of " + name + " does not match export from " + from.name);
					detail2("Import", def.toString(), "Export", expdef.toString());
				}
			}
		}
	}

	@Override
	public boolean isExpectedKind(TCDefinition def)
	{
		return def.isTypeDefinition() || def.kind().equals("state");
	}

	@Override
	public String kind()
	{
		return "type";
	}

	@Override
	public <R, S> R apply(TCImportExportVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImportedType(this, arg);
	}
}
