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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class POObjectFieldDesignator extends POObjectDesignator
{
	private static final long serialVersionUID = 1L;
	public final POObjectDesignator object;
	public final TCNameToken classname;
	public final TCIdentifierToken fieldname;

	public POObjectFieldDesignator(POObjectDesignator object, TCNameToken classname, TCIdentifierToken fieldname)
	{
		super(object.location);
		this.object = object;
		this.classname = classname;
		this.fieldname = fieldname;
	}

	@Override
	public String toString()
	{
		return object + "." + (classname == null ? fieldname : classname);
	}
}
