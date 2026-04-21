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

public class Expression extends SExp
{
	public Expression()
	{
		super();
	}

	public Expression(String varname)
	{
		super(new Text(varname));
	}

	public Expression(String operator, Source arg)
	{
		super(new Text(operator), arg);
	}

	public Expression(Source... args)
	{
		super(args);
	}

	public Expression(String operator, String arg)
	{
		super(new Text(operator), new Text(arg));
	}

	public Expression(String operator, Source left, Source right)
	{
		super(new Text(operator), left, right);
	}

	public Expression(String operator, String left, String right)
	{
		super(new Text(operator), new Text(left), new Text(right));
	}

	public Expression(String operator, Source one, Source two, Source three)
	{
		super(new Text(operator), one, two, three);
	}
}
