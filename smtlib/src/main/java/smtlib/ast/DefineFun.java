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

import com.fujitsu.vdmj.tc.expressions.TCExpression;

import smtlib.visitors.Utils;

public class DefineFun extends Command
{
	private String expanded = null;

	public DefineFun(String name, Bracketed params, Sort type, Expression body)
	{
		super(new Text("define-fun"), new Text(name), params, type, body);
	}

	public DefineFun(String name)	// Placeholder, updated by set below
	{
		super(new Text("define-fun"), new Text(name), null, null, null);
	}

	public DefineFun(String name, Sort type, Expression body)	// Constant
	{
		super(new Text("define-fun"), new Text(name), new Bracketed(), type, body);
	}

	public String getName()
	{
		return get(1).toString();
	}

	public void set(Bracketed params, Sort type, Expression body)
	{
		this.set(2, params);
		this.set(3, type);
		this.set(4, body);
	}

	public void setRecursive()
	{
		this.set(0, new Text("define-fun-rec"));
	}

	public void setExpanded(TCExpression expanded)
	{
		this.expanded = Utils.expandOneLine(expanded);
	}

	@Override
	public String toFormat(int indent)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n");

		if (expanded != null)
		{
			sb.append("; Expanded: ");
			sb.append(get(1).toSource());
			sb.append(" == ");
			sb.append(expanded);
			sb.append("\n");
		}
		
		sb.append("(");
		sb.append(get(0).toSource());
		sb.append(" ");
		sb.append(get(1).toSource());
		sb.append(" ");
		sb.append(get(2).toSource());
		sb.append(" ");
		sb.append(get(3).toSource());

		String body = get(4).toSource();

		if (body.length() > 10)
		{
			sb.append("\n");
			sb.append(" ".repeat(indent));
			sb.append(get(4).toFormat(indent + 2));
		}
		else
		{
			sb.append(" ");
			sb.append(body);
		}

		sb.append(")");

		return sb.toString();
	}
}
