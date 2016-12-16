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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCQualifiedDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	private final TCDefinition def;
	private final TCType type;

	public TCQualifiedDefinition(TCDefinition qualifies, TCType type)
	{
		super(qualifies.pass, qualifies.location, qualifies.name, qualifies.nameScope);
		this.def = qualifies;
		this.type = type;
	}

	public TCQualifiedDefinition(TCDefinition qualifies, NameScope nameScope)
	{
		super(qualifies.pass, qualifies.location, qualifies.name, nameScope);
		this.def = qualifies;
		this.type = qualifies.getType();
	}

	@Override
	public String toString()
	{
		return def.toString();
	}
	
	@Override
	public String kind()
	{
		return def.kind();
	}

	@Override
	public boolean equals(Object other)
	{
		return def.equals(other);
	}

	@Override
	public int hashCode()
	{
		return def.hashCode();
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		def.typeCheck(base, scope);
	}

	@Override
	public void typeResolve(Environment env)
	{
		def.typeResolve(env);
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return def.getDefinitions();
	}

	@Override
	public TCNameList getVariableNames()
	{
		return def.getVariableNames();
	}

	@Override
	public TCType getType()
	{
		return type; // NB. Not delegated!
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
		def.implicitDefinitions(base);
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		return super.findName(sought, scope);	// NB. Not delegated!
	}

	@Override
	public void markUsed()
	{
		def.markUsed();
	}

	@Override
	public TCDefinition findType(TCNameToken sought, String fromModule)
	{
		return def.findType(sought, fromModule);
	}

	@Override
	public void unusedCheck()
	{
		def.unusedCheck();
	}

	@Override
	public void setAccessSpecifier(TCAccessSpecifier access)
	{
		def.setAccessSpecifier(access);
	}

	@Override
	public boolean isAccess(Token kind)
	{
		return def.isAccess(kind);
	}

	@Override
	public boolean isStatic()
	{
		return def.isStatic();
	}

	@Override
	public boolean isFunction()
	{
		return def.isFunction();
	}

	@Override
	public boolean isOperation()
	{
		return def.isOperation();
	}

	@Override
	public boolean isCallableOperation()
	{
		return def.isCallableOperation();
	}

	@Override
	public boolean isCallableFunction()
	{
		return def.isCallableFunction();
	}

	@Override
	public boolean isInstanceVariable()
	{
		return def.isInstanceVariable();
	}

	@Override
	public boolean isTypeDefinition()
	{
		return def.isTypeDefinition();
	}

	@Override
	public boolean isValueDefinition()
	{
		return def.isValueDefinition();
	}

	@Override
	public boolean isRuntime()
	{
		return def.isRuntime();
	}

	@Override
	public boolean isUpdatable()
	{
		return super.isUpdatable();		// Note, not delegated
	}

	@Override
	public void setClassDefinition(TCClassDefinition def)
	{
		def.setClassDefinition(def);
	}

	@Override
	public void report(int number, String msg)
	{
		def.report(number, msg);
	}

	@Override
	public void warning(int number, String msg)
	{
		def.warning(number, msg);
	}

	@Override
	public void detail(String tag, Object obj)
	{
		def.detail(tag, obj);
	}

	@Override
	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		def.detail2(tag1, obj1, tag2, obj2);
	}

	@Override
	public TCDefinition deref()
	{
		return def.deref();
	}

	@Override
	public TCDefinitionList checkDuplicatePatterns(TCDefinitionList defs)
	{
		return def.checkDuplicatePatterns(defs);
	}
}
