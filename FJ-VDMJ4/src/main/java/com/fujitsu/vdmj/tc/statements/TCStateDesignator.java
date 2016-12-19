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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.statements;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * The root of the state designator hierarchy.
 */
public abstract class TCStateDesignator implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;

	public TCStateDesignator(LexLocation location)
	{
		this.location = location;
	}

	@Override
	abstract public String toString();

	abstract public TCType typeCheck(Environment env);

	public TCDefinition targetDefinition(@SuppressWarnings("unused") Environment env)
	{
		return null;
	}

	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	public void concern(boolean serious, int number, String msg)
	{
		if (serious)
		{
			TypeChecker.report(number, msg, location);
		}
		else
		{
			TypeChecker.warning(number, msg, location);
		}
	}

	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}
}
