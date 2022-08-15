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
import com.fujitsu.vdmj.ast.expressions.ASTBooleanLiteralExpression;
import com.fujitsu.vdmj.ast.expressions.ASTIntegerLiteralExpression;
import com.fujitsu.vdmj.ast.expressions.ASTRealLiteralExpression;
import com.fujitsu.vdmj.ast.expressions.ASTStringLiteralExpression;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexRealToken;
import com.fujitsu.vdmj.ast.lex.LexStringToken;
import com.fujitsu.vdmj.ast.patterns.ASTIdentifierPattern;
import com.fujitsu.vdmj.in.definitions.INSystemDefinition;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;

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
		
		throw new InvalidParameterException("Cannot find definition: " + classname + "`" + pname);
	}
	
	protected void setParameter(ASTClassList classes, String classname, String pname, Double pvalue)
	{
		ASTValueDefinition vdef = findParameterDefinition(classes, classname, pname);
		vdef.setExpression(new ASTRealLiteralExpression(new LexRealToken(pvalue, vdef.location)));
	}
	
	protected void setParameter(ASTClassList classes, String classname, String pname, Integer pvalue)
	{
		ASTValueDefinition vdef = findParameterDefinition(classes, classname, pname);
		vdef.setExpression(new ASTIntegerLiteralExpression(new LexIntegerToken(pvalue, vdef.location)));
	}
	
	protected void setParameter(ASTClassList classes, String classname, String pname, Boolean pvalue)
	{
		ASTValueDefinition vdef = findParameterDefinition(classes, classname, pname);
		vdef.setExpression(new ASTBooleanLiteralExpression(new LexBooleanToken(pvalue, vdef.location)));
	}
	
	protected void setParameter(ASTClassList classes, String classname, String pname, String pvalue)
	{
		ASTValueDefinition vdef = findParameterDefinition(classes, classname, pname);
		vdef.setExpression(new ASTStringLiteralExpression(new LexStringToken(pvalue, vdef.location)));
	}
	
	/**
	 * Support methods for finding and setting system object fields, during a step.
	 */
	private UpdatableValue getSystemValue(String varname, String fieldname)
	{
		NameValuePairList systemMembers = INSystemDefinition.getSystemMembers();
		
		if (systemMembers == null)
		{
			throw new InvalidParameterException("No system class defined");
		}
		
		for (NameValuePair nvp: systemMembers)
		{
			if (nvp.name.getName().equals(varname) && nvp.value.deref() instanceof ObjectValue)
			{
				ObjectValue obj = (ObjectValue)nvp.value.deref();
				TCNameToken key = new TCNameToken(LexLocation.ANY, obj.classdef.name.getName(), fieldname);
				Value v = obj.members.get(key);
				
				if (v instanceof UpdatableValue)
				{
					return (UpdatableValue) v;
				}
				else if (v == null)
				{
					throw new InvalidParameterException("Field not found: " + varname + "`" + fieldname);
				}
				else
				{
					throw new InvalidParameterException("Field not updatable: " + varname + "`" + fieldname);
				}
			}
		}
		
		throw new InvalidParameterException("Cannot find system variable: " + varname);
	}
	
	protected Double getSystemDoubleValue(String varname, String fieldname) throws ValueException
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		return v.realValue(null);
	}
	
	protected Long getSystemIntegerValue(String varname, String fieldname) throws ValueException
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		return v.intValue(null);
	}
	
	protected Boolean getSystemBooleanValue(String varname, String fieldname) throws ValueException
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		return v.boolValue(null);
	}
	
	protected String getSystemStringValue(String varname, String fieldname) throws ValueException
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		return v.stringValue(null);
	}
	
	protected void setSystemValue(String varname, String fieldname, Double value) throws Exception
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		v.set(LexLocation.ANY, new RealValue(value), null);
	}
	
	protected void setSystemValue(String varname, String fieldname, Long value) throws ValueException
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		v.set(LexLocation.ANY, new IntegerValue(value), null);
	}
	
	protected void setSystemValue(String varname, String fieldname, Boolean value) throws ValueException
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		v.set(LexLocation.ANY, new BooleanValue(value), null);
	}
	
	protected void setSystemValue(String varname, String fieldname, String value) throws ValueException
	{
		UpdatableValue v = getSystemValue(varname, fieldname);
		v.set(LexLocation.ANY, new SeqValue(value), null);
	}
	
	/**
	 * Perform any simulation setup actions, including setting parameters. The classes list
	 * can be passed to setParameter methods.
	 */
	abstract public void setup(ASTClassList classes);

	/**
	 * Intercept the simulation. This method is called at the start of the execution (time=0)
	 * and subsequently at (or as soon after as possible) the time returned by this call. The
	 * method can call getSystem<Type>Value and setSystemValue to read or change the value of
	 * objects defined within the system class.
	 */
	abstract public long step(long time);
}
