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

package com.fujitsu.vdmj.in.definitions;

import java.io.Serializable;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;

/**
 * A class to represent a [pure][static] public/private/protected specifier.
 */
public class INAccessSpecifier extends INNode
{
	private static final long serialVersionUID = 1L;

	public final boolean isStatic;
	public final boolean isAsync;
	public final Token access;
	public final boolean isPure;

	public INAccessSpecifier(boolean isStatic, boolean isAsync, Token access, boolean pure)
	{
		this.isStatic = isStatic;
		this.isAsync = isAsync;
		this.access = access;
		this.isPure = pure;
	}

	@Override
	public String toString()
	{
		return (isPure? "pure " : "") + (isAsync? "async " : "") + (isStatic ? "static " : "") + access;
	}

	public String ifSet(String sep)
	{
		return (Settings.dialect == Dialect.VDM_SL) ? "" : (toString() + sep);
	}
}
