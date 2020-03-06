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

import java.util.Iterator;

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to hold a value definition.
 */
public class TCValueDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCPattern pattern;
	public TCType type;
	public final TCExpression exp;

	private TCDefinitionList defs = null;
	protected TCType expType = null;

	public TCValueDefinition(NameScope scope, TCAnnotationList annotations,
			TCAccessSpecifier accessSpecifier, TCPattern p, TCType type, TCExpression exp)
	{
		super(Pass.VALUES, p.location, null, scope);

		this.annotations = annotations;
		this.pattern = p;
		this.type = type;
		this.exp = exp;

		defs = new TCDefinitionList();	// Overwritten in typeCheck

		for (TCNameToken var: pattern.getVariableNames())
		{
			defs.add(new TCUntypedDefinition(location, var));
		}
		
		setAccessSpecifier(accessSpecifier);
	}

	@Override
	public void setClassDefinition(TCClassDefinition def)
	{
		super.setClassDefinition(def);
		defs.setClassDefinition(def);
	}

	@Override
	public void setAccessSpecifier(TCAccessSpecifier access)
	{
		if (access == null)
		{
			access = new TCAccessSpecifier(true, false, Token.PRIVATE, false);
		}
		else if (!access.isStatic)
		{
			access = new TCAccessSpecifier(true, false, access.access, false);
		}

		super.setAccessSpecifier(access);
		defs.setAccessibility(accessSpecifier);
	}

	@Override
	public String toString()
	{
		return accessSpecifier.ifSet(" ") + pattern +
				(type == null ? "" : ":" + type) + " = " + exp;
	}
	
	@Override
	public String kind()
	{
		return "value";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCValueDefinition)
		{
			TCValueDefinition vdo = (TCValueDefinition)other;

			if (defs.size() == vdo.defs.size())
			{
				Iterator<TCDefinition> diter = vdo.defs.iterator();

				for (TCDefinition d: defs)
				{
					if (!diter.next().equals(d))
					{
						return false;
					}
				}

				return true;
			}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return defs.hashCode();
	}

	@Override
	public void typeResolve(Environment env)
	{
		if (type != null)
		{
			type = type.typeResolve(env, null);
			pattern.typeResolve(env);
			updateDefs();
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (annotations != null) annotations.tcBefore(this, base, scope);

		getDefinitions().setExcluded(true);
		expType = exp.typeCheck(base, null, scope, type);
		getDefinitions().setExcluded(false);
		
		if (expType instanceof TCUnknownType)
		{
			pass = Pass.FINAL;	// Come back to this
		}
		
		if (expType instanceof TCVoidType)
		{
			exp.report(3048, "Expression does not return a value");
		}
		else if (type != null && !(type instanceof TCUnknownType))
		{
			TypeComparator.checkComposeTypes(type, base, false);
			
			if (!TypeComparator.compatible(type, expType))
			{
				report(3051, "Expression does not match declared type");
				detail2("Declared", type, "Expression", expType);
			}
		}
		else
		{
			type = expType;
		}

		if (base.isVDMPP() && type instanceof TCNamedType)
		{
			TCNamedType named = (TCNamedType)type;
    		TCDefinition typedef = base.findType(named.typename, location.module);

    		if (typedef.accessSpecifier.narrowerThan(accessSpecifier))
    		{
    			report(3052, "Value type visibility less than value definition");
    		}
		}

		pattern.typeResolve(base);
		updateDefs();
		defs.typeCheck(base, scope);

		if (annotations != null) annotations.tcAfter(this, type, base, scope);
	}
	
	private void updateDefs()
	{
		TCDefinitionList newdefs = pattern.getDefinitions(type, nameScope);

		// The untyped definitions may have had "used" markers, so we copy
		// those into the new typed definitions, lest we get warnings. We
		// also mark the local definitions as "ValueDefintions" (proxies),
		// so that classes can be constructed correctly (values are statics).

		for (TCDefinition d: newdefs)
		{
			for (TCDefinition u: defs)
			{
				if (u.name.equals(d.name))
				{
					if (u.isUsed())
					{
						d.markUsed();
					}

					break;
				}
			}

			TCLocalDefinition ld = (TCLocalDefinition)d;
			ld.setValueDefinition(this);
		}

		defs = newdefs;
		defs.setAccessibility(accessSpecifier);
		defs.setClassDefinition(classDefinition);
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		return defs.findName(sought, scope);
	}

	@Override
	public TCType getType()
	{
		return type != null ? type :
				(expType != null ? expType : new TCUnknownType(location));
	}

	@Override
	public void unusedCheck()
	{
		if (used)	// Indicates all definitions exported (used)
		{
			return;
		}

		if (defs != null)
		{
    		for (TCDefinition def: defs)
    		{
    			def.unusedCheck();
    		}
		}
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return defs;	// May be UntypedDefinitions...
	}

	@Override
	public TCNameList getVariableNames()
	{
		return pattern.getVariableNames();
	}

	@Override
	public boolean isValueDefinition()
	{
		return true;
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseValueDefinition(this, arg);
	}
}
