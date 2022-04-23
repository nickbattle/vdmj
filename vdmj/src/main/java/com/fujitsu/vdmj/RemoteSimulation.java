/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj;

import java.security.InvalidParameterException;

import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTRealLiteralExpression;
import com.fujitsu.vdmj.ast.lex.LexRealToken;
import com.fujitsu.vdmj.ast.patterns.ASTIdentifierPattern;

abstract public class RemoteSimulation
{
	private static RemoteSimulation INSTANCE = null;
	
	public static RemoteSimulation getInstance()
	{
		return INSTANCE;
	}
	
	protected RemoteSimulation()
	{
		INSTANCE = this;
	}
	
	/**
	 * Support methods for finding and setting parameters, before execution.
	 */
	private ASTValueDefinition findParameterDefinition(ASTClassList classes, String classname, String pname)
	{
		for (ASTClassDefinition cdef: classes)
		{
			if (cdef.name.getName().equals(classname))
			{
				for (ASTDefinition mdef: cdef.definitions)
				{
					if (mdef instanceof ASTValueDefinition)
					{
						ASTValueDefinition vdef = (ASTValueDefinition)mdef;
						
						if (vdef.pattern instanceof ASTIdentifierPattern)
						{
							ASTIdentifierPattern vp = (ASTIdentifierPattern)vdef.pattern;
							
							if (vp.name.getName().equals(pname))
							{
								return vdef;
							}
						}
						else
						{
							throw new InvalidParameterException("Simulation parameter is not a simple value: " + pname);
						}
					}
				}
			}
		}
		
		return null;	// Not found
	}
	
	protected void setParameter(ASTClassList classes, String classname, String pname, Double pvalue)
	{
		ASTValueDefinition vdef = findParameterDefinition(classes, classname, pname);
		String fullName = classname + "`" + pname;
		
		if (vdef != null)
		{
			vdef.setExpression(new ASTRealLiteralExpression(new LexRealToken(pvalue, vdef.location)));
		}
		else
		{
			throw new InvalidParameterException("Cannot find definition: " + fullName);
		}
	}
	
	abstract public void setup(ASTClassList classes);
}
