/*******************************************************************************
 *
 *	Copyright (C) 2014 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.definitions;

import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.lex.LexNameToken;
import org.overturetool.vdmj.lex.Token;
import org.overturetool.vdmj.messages.LocatedException;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.statements.Statement;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.values.NameValuePairList;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueList;

public class QualifiedDefinition extends Definition
{
	private static final long serialVersionUID = 1L;
	private final Definition def;
	private final Type type;

	public QualifiedDefinition(Definition qualifies, Type type)
	{
		super(qualifies.pass, qualifies.location, qualifies.name, qualifies.nameScope);
		this.def = qualifies;
		this.type = type;
	}

	public QualifiedDefinition(Definition qualifies, NameScope nameScope)
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
	public String kind()
	{
		return def.kind();
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
	public DefinitionList getDefinitions()
	{
		return def.getDefinitions();
	}

	@Override
	public LexNameList getVariableNames()
	{
		return def.getVariableNames();
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return def.getValues(ctxt);
	}

	@Override
	public LexNameList getOldNames()
	{
		return def.getOldNames();
	}

	@Override
	public Type getType()
	{
		return type; // NB. Not delegated!
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return def.getProofObligations(ctxt);
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
		def.implicitDefinitions(base);
	}

	@Override
	public Definition findName(LexNameToken sought, NameScope scope)
	{
		return super.findName(sought, scope);	// NB. Not delegated!
	}

	@Override
	public void markUsed()
	{
		def.markUsed();
	}

	@Override
	public Definition findType(LexNameToken sought, String fromModule)
	{
		return def.findType(sought, fromModule);
	}

	@Override
	public Statement findStatement(int lineno)
	{
		return def.findStatement(lineno);
	}

	@Override
	public Expression findExpression(int lineno)
	{
		return def.findExpression(lineno);
	}

	@Override
	public void unusedCheck()
	{
		def.unusedCheck();
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		return def.getNamedValues(ctxt);
	}

	@Override
	public void setAccessSpecifier(AccessSpecifier access)
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
	public void setClassDefinition(ClassDefinition def)
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
	public void abort(int number, String msg, Context ctxt, LexLocation... loc)
	{
		def.abort(number, msg, ctxt, loc);
	}

	@Override
	public Value abort(ValueException ve)
	{
		return def.abort(ve);
	}

	@Override
	public Value abort(LocatedException e, Context ctxt)
	{
		return def.abort(e, ctxt);
	}

	@Override
	public Definition deref()
	{
		return def.deref();
	}

	@Override
	public DefinitionList checkDuplicatePatterns(DefinitionList defs)
	{
		return def.checkDuplicatePatterns(defs);
	}
}
