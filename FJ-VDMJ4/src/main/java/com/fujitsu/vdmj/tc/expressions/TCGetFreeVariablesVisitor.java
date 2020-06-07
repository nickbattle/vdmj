/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.expressions;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCGetFreeVariablesVisitor extends TCLeafExpressionVisitor<TCNameToken, TCNameSet, EnvTriple>
{
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseExpression(TCExpression node, EnvTriple arg)
	{
		return newCollection();
	}

	@Override
	public TCNameSet caseApplyExpression(TCApplyExpression node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		
		if (node.root instanceof TCVariableExpression && node.type != null && node.type.isFunction(node.location))
		{
			// If this is a global call, then we depend on the function
			TCVariableExpression v = (TCVariableExpression)node.root;
			
			if (arg.globals.findName(v.name, NameScope.NAMESANDANYSTATE) != null)
			{
				names.add(v.name);
			}
		}
		
		for (TCExpression a: node.args)
		{
			names.addAll(a.apply(this, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseBooleanBinaryExpression(TCBooleanBinaryExpression node, EnvTriple arg)
	{
		return node.left.apply(this, arg);		// May not do the RHS!
	}
	
	@Override
	public TCNameSet caseCasesExpression(TCCasesExpression node, EnvTriple arg)
	{
		return node.exp.apply(this, arg);		// The rest is conditional
	}
	
	@Override
	public TCNameSet caseExists1Expression(TCExists1Expression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = node.predicate.apply(this, new EnvTriple(arg.globals, local, null));
		names.addAll(node.bind.getFreeVariables(arg.globals, local));
		return names;
	}
	
	@Override
	public TCNameSet caseExistsExpression(TCExistsExpression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = node.predicate.apply(this, new EnvTriple(arg.globals, local, null));
		
		for (TCMultipleBind mb: node.bindList)
		{
			names.addAll(mb.getFreeVariables(arg.globals, local));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseForAllExpression(TCForAllExpression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = node.predicate.apply(this, new EnvTriple(arg.globals, local, null));
		
		for (TCMultipleBind mb: node.bindList)
		{
			names.addAll(mb.getFreeVariables(arg.globals, local));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseIfExpression(TCIfExpression node, EnvTriple arg)
	{
		return node.ifExp.apply(this, arg);		// The rest is conditional
	}
	
	@Override
	public TCNameSet caseIotaExpression(TCIotaExpression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = node.predicate.apply(this, new EnvTriple(arg.globals, local, null));
		names.addAll(node.bind.getFreeVariables(arg.globals, local));
		return names;
	}
	
	@Override
	public TCNameSet caseLambdaExpression(TCLambdaExpression node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();	// Body expression is conditional
		
		for (TCTypeBind bind: node.bindList)
		{
			names.addAll(bind.getFreeVariables(arg.globals, arg.env));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseLetBeStExpression(TCLetBeStExpression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = node.bind.getFreeVariables(arg.globals, local);
		
		if (node.suchThat != null)
		{
			names.addAll(node.suchThat.apply(this, new EnvTriple(arg.globals, local, null)));
		}
		
		names.addAll(node.value.apply(this, new EnvTriple(arg.globals, local, null)));
		return names;
	}
	
	@Override
	public TCNameSet caseLetDefExpression(TCLetDefExpression node, EnvTriple arg)
	{
		Environment local = arg.env;
		TCNameSet names = new TCNameSet();

		for (TCDefinition d: node.localDefs)
		{
			if (d instanceof TCExplicitFunctionDefinition)
			{
				// ignore
			}
			else
			{
				local = new FlatEnvironment(d, local);
				names.addAll(d.getFreeVariables(arg.globals, local, new AtomicBoolean()));
			}
		}

		names.addAll(node.expression.apply(this, new EnvTriple(arg.globals, local, null)));
		return names;
	}
	
	@Override
	public TCNameSet caseMapCompExpression(TCMapCompExpression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = new TCNameSet();	// Note "first" is conditional
		
		if (node.predicate != null)
		{
			node.predicate.apply(this, new EnvTriple(arg.globals, local, null));
		}
		
		for (TCMultipleBind mb: node.bindings)
		{
			names.addAll(mb.getFreeVariables(arg.globals, local));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseMkBasicExpression(TCMkBasicExpression node, EnvTriple arg)
	{
		TCNameSet names = node.type.getFreeVariables(arg.env);
		names.addAll(node.arg.apply(this, arg));
		return names;
	}
	
	@Override
	public TCNameSet caseMkTypeExpression(TCMkTypeExpression node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet(node.typename);
		
		for (TCExpression a: node.args)
		{
			names.addAll(a.apply(this, arg));
		}

		return names;
	}
	
	@Override
	public TCNameSet caseMuExpression(TCMuExpression node, EnvTriple arg)
	{
		TCNameSet names = node.record.apply(this, arg);
		
		for (TCRecordModifier rm: node.modifiers)
		{
			names.addAll(rm.value.apply(this, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseNarrowExpression(TCNarrowExpression node, EnvTriple arg)
	{
		TCNameSet names = node.test.apply(this, arg);
		
		if (node.typename != null)
		{
			names.add(node.typename);
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseSeqCompExpression(TCSeqCompExpression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = new TCNameSet();	// Note "first" is conditional
		
		if (node.predicate != null)
		{
			node.predicate.apply(this, new EnvTriple(arg.globals, local, null));
		}
		
		names.addAll(node.bind.getFreeVariables(arg.globals, local));
		return names;
	}
	
	@Override
	public TCNameSet caseSetCompExpression(TCSetCompExpression node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = new TCNameSet();	// Note "first" is conditional
		
		if (node.predicate != null)
		{
			node.predicate.apply(this, new EnvTriple(arg.globals, local, null));
		}
		
		for (TCMultipleBind mb: node.bindings)
		{
			names.addAll(mb.getFreeVariables(arg.globals, local));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseVariableExpression(TCVariableExpression node, EnvTriple arg)
	{
		TCDefinition d = arg.globals.findName(node.name, NameScope.NAMESANDANYSTATE);
		
		if (d != null && d.isFunction())
		{
			return new TCNameSet();
		}
		
		if (d instanceof TCRenamedDefinition)
		{
			TCRenamedDefinition rd = (TCRenamedDefinition)d;
			
			if (rd.def.name != null)
			{
				return new TCNameSet(rd.def.name.getExplicit(true));
			}
		}
		
		if (arg.env.findName(node.name, NameScope.NAMESANDANYSTATE) == null)
		{
			return new TCNameSet(node.name.getExplicit(true));
		}
		else
		{
			return new TCNameSet();
		}
	}
	
	@Override
	public TCNameSet caseDefExpression(TCDefExpression node, EnvTriple arg)
	{
		return caseLetDefExpression(node, arg);
	}
}
