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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.definitions;

import java.util.Iterator;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCPostOpExpression;
import com.fujitsu.vdmj.tc.expressions.TCPreOpExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.statements.TCNotYetSpecifiedStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCSubclassResponsibilityStatement;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an explicit operation definition.
 */
public class TCExplicitOperationDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public TCOperationType type;
	public final TCTypeList unresolved;
	public final TCPatternList parameterPatterns;
	public final TCExpression precondition;
	public final TCExpression postcondition;
	public final TCStatement body;

	public TCExplicitFunctionDefinition predef;
	public TCExplicitFunctionDefinition postdef;
	public TCDefinitionList paramDefinitions;
	
	public TCStateDefinition stateDefinition;
	private TCType actualResult = null;
	public boolean isConstructor = false;
	public TCTypeSet possibleExceptions = null;
	public TCNameSet localUpdates = null;
	public TCNameSet transitiveUpdates = null;
	public TCDefinitionSet transitiveCalls = null;

	public TCExplicitOperationDefinition(TCAnnotationList annotations,
		TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCOperationType type, TCPatternList parameters,
		TCExpression precondition, TCExpression postcondition,
		TCStatement body)
	{
		super(Pass.DEFS, name.getLocation(), name, NameScope.GLOBAL);

		this.annotations = annotations;
		this.accessSpecifier = accessSpecifier;
		this.type = type;
		this.unresolved = type.unresolvedTypes();
		this.parameterPatterns = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		
		setAccessSpecifier(accessSpecifier);
	}

	@Override
	public String toString()
	{
		return  accessSpecifier.ifSet(" ") + name.getName() + ": " + type +
				"\n\t" + name.getName() + "(" + Utils.listToString(parameterPatterns) + ")" +
				(body == null ? "" : " ==\n" + body) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}
	
	@Override
	public String kind()
	{
		return "explicit operation";
	}
	
	@Override
	public void setAccessSpecifier(TCAccessSpecifier access)
	{
		super.setAccessSpecifier(access);
		type.setPure(access.isPure);
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
		stateDefinition = base.findStateDefinition();

		if (precondition != null)
		{
			predef = getPreDefinition(base);
			predef.markUsed();
		}

		if (postcondition != null)
		{
			postdef = getPostDefinition(base);
			postdef.markUsed();
		}
	}

	@Override
	public void typeResolve(Environment base)
	{
		type = type.typeResolve(base);
		if (annotations != null) annotations.tcResolve(this, base);

		if (base.isVDMPP())
		{
			name.setTypeQualifier(type.parameters);

			if (name.getName().equals(classDefinition.name.getName()))
			{
				isConstructor = true;
				classDefinition.hasConstructors = true;
			}
		}
		
		if (precondition != null)
		{
			predef.typeResolve(base);
		}

		if (postcondition != null)
		{
			postdef.typeResolve(base);
		}

		for (TCPattern p: parameterPatterns)
		{
			p.typeResolve(base);
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (annotations != null) annotations.tcBefore(this, base, scope);

		scope = NameScope.NAMESANDSTATE;
		TCTypeList ptypes = type.parameters;
		TypeComparator.checkImports(base, unresolved, location.module);
		TypeComparator.checkComposeTypes(type, base, false);

		if (parameterPatterns.size() > ptypes.size())
		{
			report(3023, "Too many parameter patterns");
			detail2("Type params", ptypes.size(),
				"Patterns", parameterPatterns.size());
			return;
		}
		else if (parameterPatterns.size() < ptypes.size())
		{
			report(3024, "Too few parameter patterns");
			detail2("Type params", ptypes.size(),
				"Patterns", parameterPatterns.size());
			return;
		}

		paramDefinitions = getParamDefinitions(base);
		paramDefinitions.typeCheck(base, scope);

		FlatEnvironment local = new FlatCheckedEnvironment(paramDefinitions, base, scope);
		FlatCheckedEnvironment checked = (FlatCheckedEnvironment)local;
		checked.setStatic(accessSpecifier);
		checked.setEnclosingDefinition(this);
		checked.setFunctional(false, false);

		if (base.isVDMPP())
		{
			if (!accessSpecifier.isStatic)
			{
				local.add(getSelfDefinition());
			}

			if (isConstructor)
			{
				if (accessSpecifier.isAsync || accessSpecifier.isStatic || accessSpecifier.isPure)
				{
					report(3286, "Constructor cannot be 'async', 'static' or 'pure'");
				}

				if (type.result.isClass(base))
				{
					TCClassType ctype = type.result.getClassType(base);

					if (ctype.classdef != classDefinition)
					{
						report(3025,
							"Constructor operation must have return type " + classDefinition.name.getName());
					}
				}
				else
				{
					report(3026,
						"Constructor operation must have return type " + classDefinition.name.getName());
				}
			}
		}

		if (predef != null)
		{
			FlatEnvironment pre = new FlatEnvironment(new TCDefinitionList(), local);
			pre.setEnclosingDefinition(predef);
			pre.setFunctional(true, true);
			predef.paramDefinitionList = predef.getParamDefinitions();
			TCBooleanType expected = new TCBooleanType(location);
			TCType b = predef.body.typeCheck(pre, null, NameScope.NAMESANDSTATE, expected);

			if (!b.isType(TCBooleanType.class, location))
			{
				report(3018, "Precondition returns unexpected type");
				detail2("Actual", b, "Expected", expected);
			}

			TCDefinitionList qualified = predef.body.getQualifiedDefs(local);
			
			if (!qualified.isEmpty())
			{
				local = new FlatEnvironment(qualified, local);	// NB Not checked!
			}
		}

		if (postdef != null)
		{
			TCDefinitionList rdefs = new TCDefinitionList();
			FlatEnvironment post = new FlatEnvironment(rdefs, local);
			
			if (!(type.result instanceof TCVoidType))
			{
				TCPattern rp = new TCIdentifierPattern(name.getResultName(location));
				rdefs.addAll(rp.getDefinitions(type.result, NameScope.NAMESANDANYSTATE));
			}
			
			post.setEnclosingDefinition(postdef);
			post.setFunctional(true, true);
			postdef.paramDefinitionList = postdef.getParamDefinitions();
			TCBooleanType expected = new TCBooleanType(location);
			TCType b = postdef.body.typeCheck(post, null, NameScope.NAMESANDANYSTATE, expected);

			if (!b.isType(TCBooleanType.class, location))
			{
				report(3018, "Postcondition returns unexpected type");
				detail2("Actual", b, "Expected", expected);
			}
		}

		actualResult = body.typeCheck(local, NameScope.NAMESANDSTATE, type.result, !isConstructor);
		boolean compatible = TypeComparator.compatible(type.result, actualResult);

		if ((isConstructor && !actualResult.isType(TCVoidType.class, location) && !compatible) ||
			(!isConstructor && !compatible))
		{
			report(3027, "Operation returns unexpected type");
			detail2("Actual", actualResult, "Expected", type.result);
		}
		else if (!isConstructor && !actualResult.isUnknown(location))
		{
			if (type.result.isVoid() && !actualResult.isVoid())
    		{
    			report(3312, "Void operation returns non-void value");
    			detail2("Actual", actualResult, "Expected", type.result);
    		}
    		else if (!type.result.isVoid() && actualResult.hasVoid())
    		{
    			report(3313, "Operation returns void value");
    			detail2("Actual", actualResult, "Expected", type.result);
    		}
		}

		if (accessSpecifier.isAsync && !type.result.isType(TCVoidType.class, location))
		{
			report(3293, "Asynchronous operation '" + name + "' cannot return a value");
		}

		if (accessSpecifier.isPure && type.result.isType(TCVoidType.class, location) && !type.result.isUnknown(location))
		{
			report(3344, "Pure operation '" + name + "' must return a value");
		}

		if (accessSpecifier.isPure && accessSpecifier.isAsync)
		{
			report(3345, "Pure operation '" + name + "' cannot also be async");
		}

		if (type.narrowerThan(accessSpecifier))
		{
			report(3028, "Operation parameter visibility less than operation definition");
		}

		if (base.isVDMPP()
			&& accessSpecifier.access == Token.PRIVATE
			&& body instanceof TCSubclassResponsibilityStatement)
		{
			report(3329, "Abstract function/operation must be public or protected");
		}

		if (!(body instanceof TCNotYetSpecifiedStatement) &&
			!(body instanceof TCSubclassResponsibilityStatement))
		{
			checked.unusedCheck();	// Look underneath qualified definitions, if any
		}

		if (possibleExceptions == null)
		{
			possibleExceptions = IN_PROGRESS;
			possibleExceptions = body.exitCheck(base);
		}

		if (localUpdates == null)
		{
			localUpdates = body.localUpdates();
		}

		if (annotations != null) annotations.tcAfter(this, type, base, scope);
	}

	@Override
	public TCType getType()
	{
		return type;		// NB entire "==>" type, not result
	}

	private TCDefinitionList getParamDefinitions(Environment env)
	{
		TCDefinitionList defs = new TCDefinitionList();
		Iterator<TCType> titer = type.parameters.iterator();

		for (TCPattern p: parameterPatterns)
		{
			TCType ptype = titer.next();
   			defs.addAll(p.getDefinitions(ptype, ptype.isClass(env) ? NameScope.STATE : NameScope.LOCAL));
		}

		return checkDuplicatePatterns(defs);
	}

	public TCPatternListList getParamPatternList()
	{
		TCPatternListList parameters = new TCPatternListList();
		TCPatternList plist = new TCPatternList();

		for (TCPattern p: parameterPatterns)
		{
			plist.add(p);
		}

		parameters.add(plist);
		return parameters;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		if (super.findName(sought, scope) != null)
		{
			return this;
		}

		if (Settings.dialect == Dialect.VDM_SL || Settings.release == Release.CLASSIC)
		{
    		if (predef != null && predef.findName(sought, scope) != null)
    		{
    			return predef;
    		}
    
    		if (postdef != null && postdef.findName(sought, scope) != null)
    		{
    			return postdef;
    		}
		}

		return null;
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		TCDefinitionList defs = new TCDefinitionList(this);

		if (Settings.dialect == Dialect.VDM_SL || Settings.release == Release.CLASSIC)
		{
    		if (predef != null)
    		{
    			defs.add(predef);
    		}
    
    		if (postdef != null)
    		{
    			defs.add(postdef);
    		}
		}

		return defs;
	}

	private TCExplicitFunctionDefinition getPreDefinition(Environment base)
	{
		TCPatternListList parameters = new TCPatternListList();
		TCPatternList plist = new TCPatternList();
		plist.addAll(parameterPatterns);

		if (stateDefinition != null)
		{
			plist.add(new TCIdentifierPattern(stateDefinition.name.getOldName()));
		}
		else if (base.isVDMPP() && !accessSpecifier.isStatic)
		{
			plist.add(new TCIdentifierPattern(classDefinition.name.getSelfName()));
		}

		parameters.add(plist);
		TCPreOpExpression preop = new TCPreOpExpression(name, precondition, null, stateDefinition);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, name.getPreName(precondition.location),
			null, type.getPreType(stateDefinition, classDefinition, accessSpecifier.isStatic),
			parameters, preop, null, null, false, null);

		// Operation precondition functions are effectively not static as
		// their expression can directly refer to instance variables, even
		// though at runtime these are passed via a "self" parameter.

		def.setAccessSpecifier(accessSpecifier.getStatic(false));
		def.classDefinition = classDefinition;
		def.stateDefinition = stateDefinition;
		return def;
	}

	private TCExplicitFunctionDefinition getPostDefinition(Environment base)
	{
		TCPatternListList parameters = new TCPatternListList();
		TCPatternList plist = new TCPatternList();
		plist.addAll(parameterPatterns);

		if (!(type.result instanceof TCVoidType))
		{
    		plist.add(new TCIdentifierPattern(name.getResultName(location)));
		}

		if (stateDefinition != null)	// Two args, called Sigma~ and Sigma
		{
			plist.add(new TCIdentifierPattern(stateDefinition.name.getOldName()));
			plist.add(new TCIdentifierPattern(stateDefinition.name));
		}
		else if (base.isVDMPP())
		{
			// Two arguments called "self~" and "self"
			plist.add(new TCIdentifierPattern(classDefinition.name.getSelfName().getOldName()));
			
			if (!accessSpecifier.isStatic)
			{
				plist.add(new TCIdentifierPattern(classDefinition.name.getSelfName()));
			}
		}

		parameters.add(plist);
		TCPostOpExpression postop = new TCPostOpExpression(name, precondition, postcondition, null, stateDefinition);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, name.getPostName(postcondition.location),
			null, type.getPostType(stateDefinition, classDefinition, accessSpecifier.isStatic),
			parameters, postop, null, null, false, null);

		// Operation postcondition functions are effectively not static as
		// their expression can directly refer to instance variables, even
		// though at runtime these are passed via a "self" parameter.

		def.setAccessSpecifier(accessSpecifier.getStatic(false));
		def.classDefinition = classDefinition;
		def.stateDefinition = stateDefinition;
		return def;
	}

	@Override
	public boolean isOperation()
	{
		return true;
	}

	@Override
	public boolean isCallableOperation()
	{
		return true;
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof TCSubclassResponsibilityStatement;
	}

	@Override
	public boolean updatesState(Environment env)
	{
		return !body.updatesState(env).isEmpty();
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExplicitOperationDefinition(this, arg);
	}
}
