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

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCPostOpExpression;
import com.fujitsu.vdmj.tc.expressions.TCPreOpExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.statements.TCErrorCase;
import com.fujitsu.vdmj.tc.statements.TCErrorCaseList;
import com.fujitsu.vdmj.tc.statements.TCExternalClause;
import com.fujitsu.vdmj.tc.statements.TCExternalClauseList;
import com.fujitsu.vdmj.tc.statements.TCNotYetSpecifiedStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCSubclassResponsibilityStatement;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePairList;
import com.fujitsu.vdmj.tc.types.TCPatternTypePair;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
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
public class TCImplicitOperationDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCPatternListTypePairList parameterPatterns;
	public final TCPatternTypePair result;
	public final TCExternalClauseList externals;
	public final TCStatement body;
	public final TCExpression precondition;
	public final TCExpression postcondition;
	public final TCErrorCaseList errors;

	public TCOperationType type;		// Created from params/result
	public TCOperationType unresolved;
	public TCExplicitFunctionDefinition predef;
	public TCExplicitFunctionDefinition postdef;
	public TCStateDefinition state;
	public TCType actualResult;
	public TCTypeSet possibleExceptions = null;

	public boolean isConstructor = false;

	public TCImplicitOperationDefinition(TCAnnotationList annotations,
		TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCPatternListTypePairList parameterPatterns, TCPatternTypePair result,
		TCStatement body, TCExternalClauseList externals, TCExpression precondition,
		TCExpression postcondition, TCErrorCaseList errors)
	{
		super(Pass.DEFS, name.getLocation(), name, NameScope.GLOBAL);

		this.annotations = annotations;
		this.accessSpecifier = accessSpecifier;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.externals = externals;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;

		TCTypeList ptypes = new TCTypeList();

		for (TCPatternListTypePair ptp: parameterPatterns)
		{
			ptypes.addAll(ptp.getTypeList());
		}

		type = new TCOperationType(location, ptypes,
					(result == null ? new TCVoidType(name.getLocation()) : result.type));
		
		unresolved = (TCOperationType) type.clone();
		setAccessSpecifier(accessSpecifier);
	}

	@Override
	public String toString()
	{
		return	(type.isPure() ? "pure " : "") + name +
				Utils.listToString("(", parameterPatterns, ", ", ")") +
				(result == null ? "" : " " + result) +
				(externals == null ? "" : "\n\text " + externals) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition) +
				(errors == null ? "" : "\n\terrs " + errors);
	}
	
	@Override
	public String kind()
	{
		return "implicit operation";
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
		state = base.findStateDefinition();

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
		type = type.typeResolve(base, null);

		if (result != null)
		{
			result.typeResolve(base);
		}

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

		for (TCPatternListTypePair ptp: parameterPatterns)
		{
			ptp.typeResolve(base);
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (annotations != null) annotations.tcBefore(this, base, scope);

		scope = NameScope.NAMESANDSTATE;
		TCDefinitionList defs = new TCDefinitionList();
		TCDefinitionList argdefs = new TCDefinitionList();
		TypeComparator.checkComposeTypes(type, base, false);

		for (TCPatternListTypePair ptp: parameterPatterns)
		{
			argdefs.addAll(ptp.getDefinitions(ptp.type.isClass(base) ? NameScope.STATE : NameScope.LOCAL));
		}

		defs.addAll(checkDuplicatePatterns(argdefs));

		if (result != null)
		{
			defs.addAll(result.pattern.getDefinitions(type.result, NameScope.STATE));
		}

		// Now we build local definitions for each of the externals, so
		// that they can be added to the local environment, while the
		// global state is made inaccessible - but only if we have
		// an "ext" clause

		boolean limitStateScope = false;

		if (externals != null)
		{
    		for (TCExternalClause clause: externals)
    		{
    			TypeComparator.checkComposeTypes(clause.type, base, false);
    			
    			for (TCNameToken exname: clause.identifiers)
    			{
    				TCDefinition sdef = base.findName(exname, NameScope.STATE);
    				clause.typeResolve(base);

    				if (sdef == null)
    				{
    					exname.report(3031, "Unknown state variable " + exname);
    				}
    				else
    				{
    					if (!(clause.type instanceof TCUnknownType) &&
    						!sdef.getType().equals(clause.type))
        				{
        					report(3032, "State variable " + exname + " is not this type");
        					detail2("Declared", sdef.getType(), "ext type", clause.type);
        				}
        				else
        				{
            				defs.add(new TCExternalDefinition(sdef, clause.mode.is(Token.READ)));
            				argdefs.add(new TCExternalDefinition(sdef, clause.mode.is(Token.READ)));

            				// VDM++ "ext wr" clauses in a constructor effectively
            				// initialize the instance variable concerned.

            				if (clause.mode.is(Token.WRITE) &&
            					sdef instanceof TCInstanceVariableDefinition &&
            					name.getName().equals(classDefinition.name.getName()))
            				{
            					TCInstanceVariableDefinition iv = (TCInstanceVariableDefinition)sdef;
            					iv.initialized = true;
            				}
        				}
    				}
    			}
    		}

    		// All relevant globals are now in defs (local), so we
    		// limit the state searching scope

    		limitStateScope = true;
		}

		defs.typeCheck(base, scope);

		FlatEnvironment local = new FlatCheckedEnvironment(defs, base, scope);
		FlatCheckedEnvironment checked = (FlatCheckedEnvironment)local;
		local.setLimitStateScope(limitStateScope);
		checked.setStatic(accessSpecifier);
		checked.setEnclosingDefinition(this);
		checked.setFunctional(false);

		if (base.isVDMPP())
		{
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
			FlatEnvironment pre = new FlatEnvironment(argdefs, base);
			pre.setLimitStateScope(limitStateScope);
			pre.setEnclosingDefinition(predef);
			pre.setFunctional(true);
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

		if (body != null)
		{
			if (classDefinition != null && !accessSpecifier.isStatic)
			{
				local.add(getSelfDefinition());
			}

			actualResult = body.typeCheck(local, NameScope.NAMESANDSTATE, type.result, !isConstructor);
			boolean compatible = TypeComparator.compatible(type.result, actualResult);

			if ((isConstructor && !actualResult.isType(TCVoidType.class, location) && !compatible) ||
				(!isConstructor && !compatible))
			{
				report(3035, "Operation returns unexpected type");
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
			report(3036, "Operation parameter visibility less than operation definition");
		}

		if (base.isVDMPP()
			&& accessSpecifier.access == Token.PRIVATE
			&& body instanceof TCSubclassResponsibilityStatement)
		{
			report(3329, "Abstract function/operation must be public or protected");
		}

		// The result variables are in scope for the post condition

		if (postdef != null)
		{
			TCType b = null;
			TCBooleanType expected = new TCBooleanType(location);

			if (result != null)
			{
	    		TCDefinitionList postdefs = result.getDefinitions();
	    		FlatCheckedEnvironment post =
	    			new FlatCheckedEnvironment(postdefs, local, NameScope.NAMESANDANYSTATE);
	    		post.setStatic(accessSpecifier);
	    		post.setEnclosingDefinition(postdef);
	    		post.setFunctional(true);
				b = postdef.body.typeCheck(post, null, NameScope.NAMESANDANYSTATE, expected);
				post.unusedCheck();
			}
			else
			{
	    		FlatEnvironment post = new FlatEnvironment(new TCDefinitionList(), local);
	    		post.setEnclosingDefinition(postdef);
	    		post.setFunctional(true);
				b = postdef.body.typeCheck(post, null, NameScope.NAMESANDANYSTATE, expected);
			}

			if (!b.isType(TCBooleanType.class, location))
			{
				report(3018, "Postcondition returns unexpected type");
				detail2("Actual", b, "Expected", expected);
			}
		}

		if (errors != null)
		{
			TCBooleanType expected = new TCBooleanType(location);

			for (TCErrorCase error: errors)
			{
				TCType a = error.left.typeCheck(local, null, NameScope.NAMESANDSTATE, expected);

				if (!a.isType(TCBooleanType.class, location))
				{
					error.left.report(3307, "Errs clause is not bool -> bool");
				}

				TCType b = error.right.typeCheck(local, null, NameScope.NAMESANDANYSTATE, expected);

				if (!b.isType(TCBooleanType.class, location))
				{
					error.right.report(3307, "Errs clause is not bool -> bool");
				}
			}
		}

		if (!(body instanceof TCNotYetSpecifiedStatement) &&
			!(body instanceof TCSubclassResponsibilityStatement))
		{
			local.unusedCheck();
		}

		if (possibleExceptions == null && body != null)
		{
			possibleExceptions = IN_PROGRESS;
			possibleExceptions = body.exitCheck(base);
		}

		if (annotations != null) annotations.tcAfter(this, type, base, scope);
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope incState)
	{
		if (super.findName(sought, incState) != null)
		{
			return this;
		}

		if (Settings.dialect == Dialect.VDM_SL || Settings.release == Release.CLASSIC)
		{
    		if (predef != null && predef.findName(sought, incState) != null)
    		{
    			return predef;
    		}
    
    		if (postdef != null && postdef.findName(sought, incState) != null)
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

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList(name);
	}

	public TCPatternList getParamPatternList()
	{
		TCPatternList plist = new TCPatternList();

		for (TCPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		return plist;
	}

	public TCPatternListList getListParamPatternList()
	{
		TCPatternListList list = new TCPatternListList();
		TCPatternList plist = new TCPatternList();

		for (TCPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		list.add(plist);
		return list;
	}

	private TCExplicitFunctionDefinition getPreDefinition(Environment base)
	{
		TCPatternListList parameters = new TCPatternListList();
		TCPatternList plist = new TCPatternList();

		for (TCPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		if (state != null)
		{
			plist.add(new TCIdentifierPattern(state.name));
		}
		else if (base.isVDMPP() && !accessSpecifier.isStatic)
		{
			plist.add(new TCIdentifierPattern(name.getSelfName()));
		}

		parameters.add(plist);
		TCExpression preop = new TCPreOpExpression(name, precondition, errors, state);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, TCAccessSpecifier.DEFAULT, name.getPreName(precondition.location),
			null, type.getPreType(state, classDefinition, accessSpecifier.isStatic),
			parameters, preop, null, null, false, null);

		// Operation precondition functions are effectively not static as
		// their expression can directly refer to instance variables, even
		// though at runtime these are passed via a "self" parameter.

		def.setAccessSpecifier(accessSpecifier.getStatic(false));
		def.classDefinition = classDefinition;
		return def;
	}

	private TCExplicitFunctionDefinition getPostDefinition(Environment base)
	{
		TCPatternListList parameters = new TCPatternListList();
		TCPatternList plist = new TCPatternList();

		for (TCPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		if (result != null)
		{
			plist.add(result.pattern);
		}

		if (state != null)
		{
			plist.add(new TCIdentifierPattern(state.name.getOldName()));
			plist.add(new TCIdentifierPattern(state.name));
		}
		else if (base.isVDMPP())
		{
			plist.add(new TCIdentifierPattern(name.getSelfName().getOldName()));
			
			if (!accessSpecifier.isStatic)
			{
				plist.add(new TCIdentifierPattern(name.getSelfName()));
			}
		}

		parameters.add(plist);
		TCExpression postop = new TCPostOpExpression(name, precondition, postcondition, errors, state);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, name.getPostName(postcondition.location),
			null, type.getPostType(state, classDefinition, accessSpecifier.isStatic),
			parameters, postop,
			null, null, false, null);

		// Operation postcondition functions are effectively not static as
		// their expression can directly refer to instance variables, even
		// though at runtime these are passed via a "self" parameter.

		def.setAccessSpecifier(accessSpecifier.getStatic(false));
		def.classDefinition = classDefinition;
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
		return (body != null);
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof TCSubclassResponsibilityStatement;
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImplicitOperationDefinition(this, arg);
	}
}
