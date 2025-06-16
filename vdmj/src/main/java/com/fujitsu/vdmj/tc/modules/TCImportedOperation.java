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
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;
import com.fujitsu.vdmj.tc.types.TCType;

public class TCImportedOperation extends TCImportedValue
{
	private static final long serialVersionUID = 1L;

	public TCImportedOperation(TCNameToken name, TCType type, TCNameToken renamed)
	{
		super(name, type, renamed);
	}

	@Override
	public String toString()
	{
		return "import operation " + name +
				(renamed == null ? "" : " renamed " + renamed.getName()) +
				(type == null ? "" : ":" + type);
	}

	@Override
	public boolean isExpectedKind(TCDefinition def)
	{
		return def.isOperation();
	}

	@Override
	public String kind()
	{
		return "operation";
	}

	@Override
	public <R, S> R apply(TCImportExportVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImportedOperation(this, arg);
	}
}
