/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package smtlib.ast;

public class DeclareType extends Command
{
	public DeclareType(String type, Source... ctors)
	{
		super(new Text("declare-datatype"), new Text(type), new Bracketed(ctors));
	}

	public DeclareType(String type)
	{
		super(new Text("declare-sort"), new Text(type), new Text("0"));
	}

	public String getName()
	{
		return get(1).toString();
	}

	@Override
	public String toFormat(int indent)
	{
		return "\n" + super.toFormat(indent);
	}
}
