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

import java.util.HashSet;
import java.util.Set;

public class DefineFunsRec extends DefineFun
{
	public final int id;
	public final Set<DefineFun> members;
	public boolean added;

	public DefineFunsRec(int id)
	{
		super(id);
		this.id = id;
		this.members = new HashSet<DefineFun>();
		this.added = false;
	}

	public void init()
	{
		members.clear();
		added = false;
	}

	@Override
	public String getName()
	{
		return "define-funcs-rec-" + id;
	}

	public void addMember(DefineFun define)
	{
		members.add(define);
	}

	@Override
	public String toSource()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("(define-funs-rec");
		sb.append("(");

		for (DefineFun member: members)
		{
			sb.append(member.toSignature());
		}

		sb.append(")(");

		for (DefineFun member: members)
		{
			sb.append(member.toBody());
		}

		sb.append("))");

		return sb.toString();
	}

	@Override
	public String toFormat(int indent)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\n");
		sb.append("(define-funs-rec\n");
		sb.append("  (\n");

		for (DefineFun member: members)
		{
			sb.append("    ");
			sb.append(member.toSignature().toSource());
			sb.append("\n");
		}

		sb.append("  )\n  (\n");

		for (DefineFun member: members)
		{
			sb.append(member.toBody().toFormat(4));
			sb.append("\n");
		}

		sb.append("  )\n)");

		return sb.toString();
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof DefineFunsRec)
		{
			DefineFunsRec dfr = (DefineFunsRec)other;
			return dfr.id == this.id;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return id;
	}
}
