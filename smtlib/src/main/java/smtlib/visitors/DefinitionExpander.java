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

package smtlib.visitors;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCExpressionSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

import smtlib.ast.AssertCommand;
import smtlib.ast.Bracketed;
import smtlib.ast.DeclareDataType;
import smtlib.ast.DefineConst;
import smtlib.ast.DefineFun;
import smtlib.ast.DefineSort;
import smtlib.ast.Expression;
import smtlib.ast.Sort;
import smtlib.ast.Text;

public class DefinitionExpander
{
	private static DefinitionExpander INSTANCE = null;

	private final Environment env;
	private final Stack<TCNameToken> stack;
	private final Globals globals;

	public static void init(Environment env)
	{
		INSTANCE = new DefinitionExpander(env);
	}

	public static DefinitionExpander getInstance()
	{
		return INSTANCE;
	}

	private DefinitionExpander(Environment env)
	{
		this.env = env;
		this.stack = new Stack<TCNameToken>();
		this.globals = Globals.getInstance();
	}

	public void expandFunction(TCExplicitFunctionDefinition exfn)
	{
		expandFunction(exfn.name,
			exfn.paramPatternList.firstElement(),
			exfn.type,
			exfn.body);

		if (exfn.predef != null)
		{
			TCExplicitFunctionDefinition pdef = exfn.predef;

			expandFunction(pdef.name,
				pdef.paramPatternList.firstElement(),
				pdef.type,
				pdef.body);
		}

		if (exfn.postdef != null)
		{
			TCExplicitFunctionDefinition pdef = exfn.postdef;

			expandFunction(pdef.name,
				pdef.paramPatternList.firstElement(),
				pdef.type,
				pdef.body);
		}

		if (exfn.measureDef != null)
		{
			TCExplicitFunctionDefinition mdef = exfn.measureDef;

			expandFunction(mdef.name,
				mdef.paramPatternList.firstElement(),
				mdef.type,
				mdef.body);
		}
	}

	public void expandFunction(TCImplicitFunctionDefinition imfn)
	{
		if (imfn.body != null)
		{
			expandFunction(imfn.name,
				imfn.getParamPatternList().firstElement(),
				imfn.type,
				imfn.body);
		}

		if (imfn.predef != null)
		{
			TCExplicitFunctionDefinition pdef = imfn.predef;

			expandFunction(pdef.name,
				pdef.paramPatternList.firstElement(),
				pdef.type,
				pdef.body);
		}

		if (imfn.postdef != null)
		{
			TCExplicitFunctionDefinition pdef = imfn.postdef;

			expandFunction(pdef.name,
				pdef.paramPatternList.firstElement(),
				pdef.type,
				pdef.body);
		}

		if (imfn.measureDef != null)
		{
			TCExplicitFunctionDefinition mdef = imfn.measureDef;

			expandFunction(mdef.name,
				mdef.paramPatternList.firstElement(),
				mdef.type,
				mdef.body);
		}
	}

	private void expandFunction(TCNameToken fname, TCPatternList plist, TCFunctionType ftype, TCExpression fbody)
	{
		if (!stack.contains(fname))
		{
			try
			{
				stack.push(fname);
				globals.createFunction(fname);	// Claim position. Updated in functionToSMT
				globals.createFunction(definedName(fname));

				// // Expand any functions called by this one first. Create a local env to include
				// // any parameter definitions, for the fbody typecheck.

				// TCDefinitionList pdefs = new TCDefinitionList();

				// for (int p=0; p<plist.size(); p++)
				// {
				// 	TCType ptype = ftype.parameters.get(p);
				// 	pdefs.addAll(plist.get(p).getDefinitions(ptype, NameScope.NAMES));
				// 	ptype.apply(new TypeExpander(), env);
				// }

				// Environment local = new FlatCheckedEnvironment(pdefs, env, NameScope.NAMES);
				// TCExpression expanded = fbody.apply(new ExpressionExpander(local), new TCExpressionSet());
				// expanded.typeCheck(local, null, NameScope.NAMES, ftype.result);

				functionToSMT(fname, plist, ftype, fbody);
			}
			catch (Throwable th)
			{
				throw th;
			}
			finally
			{
				stack.pop();
			}
		}
	}

	private void functionToSMT(TCNameToken fname, TCPatternList plist, TCFunctionType ftype, TCExpression fbody)
	{
		Bracketed params = new Bracketed();		// eg. (x Int)
		Bracketed psorts = new Bracketed();		// eg. (Int)
		List<Expression> qualifiers = new Vector<Expression>();
		TCDefinitionList locals = new TCDefinitionList();
		TCExpressionList letquals = new TCExpressionList();

		for (int i=0; i < plist.size(); i++)
		{
			TCPattern pattern = plist.get(i);
			TCType ptype = ftype.parameters.get(i);
			ptype.apply(new TypeExpander(), env);		// Add any type defs needed

			TCIdentifierPattern p2 = Utils.checkPattern(pattern, ptype, letquals, env);
			QualifiedSort qsort = ptype.apply(new TypeToSMT(p2.name.getName()), env);
			params.add(new Bracketed(new Text(p2.name.getName()), qsort.sort));
			psorts.add(qsort.sort);

			if (qsort.qualifier != null)
			{
				qualifiers.add(qsort.qualifier);
			}

			locals.addAll(p2.getDefinitions(ptype, NameScope.LOCAL));
		}

		if (!letquals.isEmpty())
		{
			fbody = Utils.combineQualifiers(letquals, fbody);
		}
		
		Environment paramenv = new FlatEnvironment(locals, env);
		// fbody.typeCheck(paramenv, null, NameScope.NAMES, ftype.result);

		SMTPair body = fbody.apply(new ExpressionToSMTPair(), paramenv);
		QualifiedSort qsort = ftype.result.apply(new TypeToSMT("RESULT"), paramenv);
		body.deltas.addAll(qualifiers);

		// (define-fun g (Int a) Int <exp>)
		DefineFun define = globals.findFunction(fname);
		define.set(params, qsort.sort, body.value);

		// (define-fun g_defined (Int a) Bool <exp>)
		DefineFun define2 = globals.findFunction(definedName(fname));
		define2.set(params, new Sort("Bool"), body.deltas.andOf());
	}

	public void expandValue(TCLocalDefinition ldef)
	{
		String vname = ldef.name.getName();
		TCExpression rhs = ldef.valueDefinition.exp;
		rhs.apply(new ExpressionExpander(env), new TCExpressionSet());	// Analyse RHS variables needed
		Expression body = rhs.apply(new ExpressionToSMT(), env);
		ldef.getType().apply(new TypeExpander(), env);
		QualifiedSort qtype = ldef.getType().apply(new TypeToSMT(vname), env);
		globals.addConstant(new DefineConst(ldef.name, qtype.sort, body));

		if (qtype.qualifier != null)
		{
			globals.addAssert(new AssertCommand(qtype.qualifier));		// eg. nat is >=0
		}
	}

	public void expandType(TCInvariantType node)
	{
		if (node.orddef != null)
		{
			expandFunction(node.orddef);
		}

		if (node.eqdef != null)
		{
			expandFunction(node.eqdef);
		}

		if (node.invdef != null)
		{
			expandFunction(node.invdef);
		}

		if (node instanceof TCRecordType)
		{
			TCRecordType rec = (TCRecordType)node;
			String typename = rec.name.getName();
			Expression ctor = new Expression(new Text("mk_" + typename));
			
			for (TCField field: rec.fields)
			{
				QualifiedSort qsort = field.type.apply(new TypeToSMT("?"), env);
				ctor.add(new Expression(new Text(field.tag), qsort.sort));
			}

			String uname = "undefined" + typename;
				
			globals.addDataType(new DeclareDataType(rec.name, ctor, new Expression("mk_" + uname)));
			globals.addConstant(new DefineConst(uname, new Sort(typename), new Expression("mk_" + uname)));
		}
		else if (node instanceof TCNamedType)
		{
			TCNamedType ntype = (TCNamedType)node;
			QualifiedSort qsort = ntype.type.apply(new TypeToSMT("?"), env);
			globals.addSort(new DefineSort(ntype.typename, qsort.sort));
			globals.addConstant(new DefineConst("undefined" + ntype.typename, qsort.sort, qsort.sort.undefinedConst()));
		}
	}

	private TCNameToken definedName(TCNameToken name)
	{
		return new TCNameToken(name.getLocation(), name.getModule(), name.getName() + "_defined");
	}
}
