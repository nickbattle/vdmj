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

import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class to hold an inherited definition in VDM++.
 */
public class TCInheritedDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public TCDefinition superdef;

	public TCInheritedDefinition(TCAccessSpecifier accessSpecifier, TCNameToken localname, TCDefinition def)
	{
		super(def.pass, def.location, localname, def.nameScope);

		this.accessSpecifier = accessSpecifier;
		this.superdef = def;

		setAccessSpecifier(def.accessSpecifier);
		setClassDefinition(def.classDefinition);
	}

	private void checkSuperDefinition()
	{
		// This is used to get over the case where an inherited definition
		// is a TCValueDefinition that has since been replaced with a new
		// TCLocalDefinition. It would be better to somehow list the
		// inherited definitions that refer to a TCLocalDefinition and update
		// them...

		if (superdef instanceof TCUntypedDefinition)
		{
			if (classDefinition != null)
			{
				superdef = classDefinition.findName(superdef.name, nameScope);
			}
		}
	}

	@Override
	public TCType getType()
	{
		checkSuperDefinition();
		return superdef.getType();
	}

	@Override
	public String toString()
	{
		return accessSpecifier.ifSet(" ") + superdef.toString();
	}
	
	@Override
	public String kind()
	{
		return superdef.kind();
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		superdef.typeCheck(base, scope);
	}

	@Override
    public void markUsed()
	{
		used = true;
		superdef.markUsed();
	}

	@Override
    protected boolean isUsed()
	{
		return superdef.isUsed();
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return superdef.getDefinitions();
	}

	@Override
	public TCNameList getVariableNames()
	{
		TCNameList names = new TCNameList();
		checkSuperDefinition();

		for (TCNameToken vn: superdef.getVariableNames())
		{
			names.add(vn.getModifiedName(name.getModule()));
		}

		return names;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		// The problem is, when the TCInheritedDefinition is created, we
		// don't know its fully qualified name.
		
		if (superdef instanceof TCInheritedDefinition)
		{
			superdef.findName(sought, scope);
		}

		name.setTypeQualifier(superdef.name.getTypeQualifier());
		TCNameToken oldname = name.getOldName();

		if (name.equals(sought))
		{
			return this;
		}
		else if (scope.matches(NameScope.OLDSTATE) && oldname.equals(sought))
		{
			return this;
		}

		return null;
	}

	@Override
	public TCDefinition findType(TCNameToken sought, String fromModule)
	{
		if (superdef instanceof TCTypeDefinition && sought.equals(name))
		{
			return this;
		}

		return null;
	}

	@Override
	public boolean isFunction()
	{
		return superdef.isFunction();
	}

	@Override
	public boolean isOperation()
	{
		return superdef.isOperation();
	}

	@Override
	public boolean isCallableOperation()
	{
		return superdef.isCallableOperation();
	}

	@Override
	public boolean isInstanceVariable()
	{
		return superdef.isInstanceVariable();
	}

	@Override
	public boolean isTypeDefinition()
	{
		return superdef.isTypeDefinition();
	}

	@Override
	public boolean isValueDefinition()
	{
		return superdef.isValueDefinition();
	}

	@Override
	public boolean isRuntime()
	{
		return superdef.isRuntime();
	}

	@Override
	public boolean isUpdatable()
	{
		return superdef.isUpdatable();
	}

	@Override
	public TCDefinition deref()
	{
		return superdef.deref();
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return superdef.isSubclassResponsibility();
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseInheritedDefinition(this, arg);
	}
}
