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

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCExportedType extends TCExport
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	public final boolean struct;

	public TCExportedType(TCNameToken name, boolean struct)
	{
		super(name.getLocation());
		this.name = name;
		this.struct = struct;
	}

	@Override
	public String toString()
	{
		return "export type " + (struct ? "struct " : "") + name.getName();
	}

	@Override
	public TCDefinitionList getDefinition(TCDefinitionList actualDefs)
	{
		TCDefinitionList list = new TCDefinitionList();
		TCDefinition def = actualDefs.findType(name, name.getModule());

		if (def != null)
		{
			list.add(def);

			if (!struct)	// Mark the type as opaque, since it is not struct-exported
			{
				TCType type = def.getType();
				
				if (type instanceof TCInvariantType)
				{
					TCInvariantType itype = (TCInvariantType)type;
					itype.setOpaque(true);
				}
				else
				{
					report(67, "Exported type " + name + " not structured");
				}
			}
		}

		return list;
	}

	@Override
	public TCDefinitionList getDefinition()
	{
		return new TCDefinitionList();
	}

	@Override
	public void typeCheck(Environment env, TCDefinitionList actualDefs)
	{
		TCDefinition def = actualDefs.findType(name, name.getModule());

		if (def == null)
		{
			report(3187, "Exported type " + name + " not defined in module");
		}
	}

	@Override
	public <R, S> R apply(TCImportExportVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExportedType(this, arg);
	}
}
