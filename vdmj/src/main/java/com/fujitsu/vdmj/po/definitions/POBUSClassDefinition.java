/*******************************************************************************
 *
 *	Copyright (c) 2008, 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.tc.definitions.TCBUSClassDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;

public class POBUSClassDefinition extends POClassDefinition
{
	private static final long serialVersionUID = 1L;

	public POBUSClassDefinition(TCClassType ctype, PODefinitionList defs, TCBUSClassDefinition tcdef)
	{
		super(null, new TCNameToken(LexLocation.ANY, "CLASS", "BUS"), ctype, defs, null, false, tcdef);
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBUSClassDefinition(this, arg);
	}
}
