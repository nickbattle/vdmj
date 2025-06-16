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

package com.fujitsu.vdmj.ast.modules;

import com.fujitsu.vdmj.ast.definitions.ASTTypeDefinition;
import com.fujitsu.vdmj.ast.lex.LexNameToken;

public class ASTImportedType extends ASTImport
{
	private static final long serialVersionUID = 1L;
	public final ASTTypeDefinition def;

	public ASTImportedType(LexNameToken name, LexNameToken renamed)
	{
		super(name, renamed);
		this.def = null;
	}

	public ASTImportedType(ASTTypeDefinition def, LexNameToken renamed)
	{
		super(def.name, renamed);
		this.def = def;
	}

	@Override
	public String toString()
	{
		return "import type " +
				(def == null ? name.name : def.toString()) +
				(renamed == null ? "" : " renamed " + renamed.name);
	}
}
