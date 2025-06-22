/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package quickcheck.strategies;

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TrivialQCKnown
{
	public TrivialQCKnown next;
	public TCExpression known;
	public TCNameToken defines = null;
	
	public TrivialQCKnown()
	{
		this.known = null;
		this.next = null;
	}
	
	public TrivialQCKnown(TCExpression known, TrivialQCKnown next)
	{
		this.known = known;
		this.next = next;
	}

	public TrivialQCKnown(TCExpression node, TrivialQCKnown next, TCNameToken defines)
	{
		this(node, next);
		this.defines = defines;
	}

	public TCExpression isKnown(TCExpression node)
	{
		return isKnown(node, node.getVariableNames());
	}

	private TCExpression isKnown(TCExpression node, TCNameSet uses)
	{
		if (node.equals(known))
		{
			return known;
		}
		
		if (defines != null && uses.contains(defines))
		{
			// Don't search lower, because we're using a symbol that is defined here
			return null;
		}
		
		if (next == null)
		{
			return null;
		}
		
		return next.isKnown(node, uses);
	}
}
