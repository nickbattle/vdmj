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

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotYetSpecifiedExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubclassResponsibilityExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePairList;
import com.fujitsu.vdmj.tc.types.TCPatternTypePair;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an implicit function definition.
 */
public class TCImplicitFunctionDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameList typeParams;
	public final TCPatternListTypePairList parameterPatterns;
	public final TCPatternTypePair result;
	public final TCExpression body;
	public final TCExpression precondition;
	public final TCExpression postcondition;
	public final TCNameToken measure;

	public TCFunctionType type;
	public TCExplicitFunctionDefinition predef;
	public TCExplicitFunctionDefinition postdef;

	public boolean recursive = false;
	public boolean isUndefined = false;
	public int measureLexical = 0;
	public TCDefinition measuredef;
	public TCType actualResult;

	public TCImplicitFunctionDefinition(TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCNameList typeParams, TCPatternListTypePairList parameterPatterns,
		TCPatternTypePair result,
		TCExpression body,
		TCExpression precondition,
		TCExpression postcondition,
		TCNameToken measure)
	{
		super(Pass.DEFS, name.getLocation(), name, NameScope.GLOBAL);

		this.accessSpecifier = accessSpecifier;
		this.typeParams = typeParams;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.measure = measure;

		TCTypeList ptypes = new TCTypeList();

		for (TCPatternListTypePair ptp: parameterPatterns)
		{
			ptypes.addAll(ptp.getTypeList());
		}

		// NB: implicit functions are always +> total, apparently
		type = new TCFunctionType(location, ptypes, false, result.type);
		type.definitions = new TCDefinitionList(this);
		type.instantiated = typeParams == null ? null : false;
	}

	@Override
	public String toString()
	{
		return	accessSpecifier + " " +	name.getName() +
				(typeParams == null ? "" : "[" + typeParams + "]") +
				Utils.listToString("(", parameterPatterns, ", ", ")") + result +
				(body == null ? "" : " ==\n\t" + body) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}
	
	@Override
	public String kind()
	{
		return "implicit function";
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
		if (body instanceof TCSubclassResponsibilityExpression ||
			body instanceof TCNotYetSpecifiedExpression)
		{
			isUndefined = true;
		}

		if (precondition != null)
		{
			predef = getPreDefinition();
			predef.markUsed();
		}
		else
		{
			predef = null;
		}

		if (postcondition != null)
		{
			postdef = getPostDefinition();
			postdef.markUsed();
		}
		else
		{
			postdef = null;
		}
	}

	public TCDefinitionList getTypeParamDefinitions()
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCNameToken pname: typeParams)
		{
			TCDefinition p = new TCLocalDefinition(pname.getLocation(),
				pname, new TCParameterType(pname));

			p.markUsed();
			defs.add(p);
		}

		return defs;
	}

	@Override
	public void typeResolve(Environment base)
	{
		if (typeParams != null)
		{
			FlatCheckedEnvironment params =	new FlatCheckedEnvironment(
				getTypeParamDefinitions(), base, NameScope.NAMES);

			type = type.typeResolve(params, null);
		}
		else
		{
			type = type.typeResolve(base, null);
		}

		if (result != null)
		{
			result.typeResolve(base);
		}

		if (base.isVDMPP())
		{
			name.setTypeQualifier(type.parameters);
		}

		if (precondition != null)
		{
			predef.typeResolve(base);
		}

		if (postcondition != null)
		{
			postdef.typeResolve(base);
		}

		for (TCPatternListTypePair pltp: parameterPatterns)
		{
			pltp.typeResolve(base);
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		TCDefinitionList defs = new TCDefinitionList();
		TypeComparator.checkComposeTypes(type, base, false);

		if (typeParams != null)
		{
			defs.addAll(getTypeParamDefinitions());
		}

		TCDefinitionList argdefs = new TCDefinitionList();

		for (TCPatternListTypePair pltp: parameterPatterns)
		{
			argdefs.addAll(pltp.getDefinitions(NameScope.LOCAL));
		}

		defs.addAll(checkDuplicatePatterns(argdefs));
		FlatEnvironment local = new FlatCheckedEnvironment(defs, base, scope);
		FlatCheckedEnvironment checked = (FlatCheckedEnvironment)local;
		checked.setStatic(accessSpecifier);
		checked.setEnclosingDefinition(this);
		checked.setFunctional(true);

		defs.typeCheck(local, scope);

		if (predef != null)
		{
			FlatEnvironment pre = new FlatEnvironment(argdefs, base);
			TCBooleanType expected = new TCBooleanType(location);
			TCType b = predef.body.typeCheck(pre, null, NameScope.NAMES, expected);

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

			actualResult = body.typeCheck(local, null, scope, result.type);

			if (!TypeComparator.compatible(result.type, actualResult))
			{
				report(3029, "Function returns unexpected type");
				detail2("Actual", actualResult, "Expected", result.type);
			}
		}

		if (type.narrowerThan(accessSpecifier))
		{
			report(3030, "Function parameter visibility less than function definition");
		}
		
		if (base.isVDMPP()
			&& accessSpecifier.access == Token.PRIVATE
			&& body instanceof TCSubclassResponsibilityExpression)
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
	    			new FlatCheckedEnvironment(postdefs, local, NameScope.NAMES);
	    		post.setStatic(accessSpecifier);
	    		post.setEnclosingDefinition(this);
	    		post.setFunctional(true);
				b = postdef.body.typeCheck(post, null, NameScope.NAMES, expected);
				post.unusedCheck();
			}
			else
			{
				b = postdef.body.typeCheck(local, null, NameScope.NAMES, expected);
			}

			if (!b.isType(TCBooleanType.class, location))
			{
				report(3018, "Postcondition returns unexpected type");
				detail2("Actual", b, "Expected", expected);
			}
		}

		if (measure == null && recursive)
		{
			warning(5012, "Recursive function has no measure");
		}
		else if (measure != null)
		{
			if (base.isVDMPP()) measure.setTypeQualifier(type.parameters);
			measuredef = base.findName(measure, scope);

			if (body == null)
			{
				measure.report(3273, "Measure not allowed for an implicit function");
			}
			else if (measuredef == null)
			{
				measure.report(3270, "Measure " + measure + " is not in scope");
			}
			else if (!(measuredef instanceof TCExplicitFunctionDefinition))
			{
				measure.report(3271, "Measure " + measure + " is not an explicit function");
			}
			else
			{
				TCExplicitFunctionDefinition efd = (TCExplicitFunctionDefinition)measuredef;
				
				if (this.typeParams == null && efd.typeParams != null)
				{
					measure.report(3309, "Measure must not be polymorphic");
				}
				else if (this.typeParams != null && efd.typeParams == null)
				{
					measure.report(3310, "Measure must also be polymorphic");
				}
				else if (this.typeParams != null && efd.typeParams != null
						&& !this.typeParams.equals(efd.typeParams))
				{
					measure.report(3318, "Measure's type parameters must match function's");
					detail2("Actual", efd.typeParams, "Expected", typeParams);
				}
				
				TCFunctionType mtype = (TCFunctionType)measuredef.getType();

				if (!TypeComparator.compatible(mtype.parameters, type.parameters))
				{
					measure.report(3303, "Measure parameters different to function");
					detail2(measure.getName(), mtype.parameters, name.getName(), type.parameters);
				}

				if (!(mtype.result instanceof TCNaturalType))
				{
					if (mtype.result.isProduct(location))
					{
						TCProductType pt = mtype.result.getProduct();

						for (TCType t: pt.types)
						{
							if (!(t instanceof TCNaturalType))
							{
								measure.report(3272,
									"Measure range is not a nat, or a nat tuple");
								measure.detail("Actual", mtype.result);
							}
						}
						
						measureLexical = pt.types.size();
					}
					else
					{
						measure.report(3272,
							"Measure range is not a nat, or a nat tuple");
						measure.detail("Actual", mtype.result);
					}
				}
			}
		}

		if (!(body instanceof TCNotYetSpecifiedExpression) &&
			!(body instanceof TCSubclassResponsibilityExpression))
		{
			local.unusedCheck();
		}
	}

	@Override
	public TCType getType()
	{
		return type;		// NB overall "->" type, not result type
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		if (super.findName(sought, scope) != null)
		{
			return this;
		}

		if (predef != null && predef.findName(sought, scope) != null)
		{
			return predef;
		}

		if (postdef != null && postdef.findName(sought, scope) != null)
		{
			return postdef;
		}

		return null;
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		TCDefinitionList defs = new TCDefinitionList(this);

		if (predef != null)
		{
			defs.add(predef);
		}

		if (postdef != null)
		{
			defs.add(postdef);
		}

		return defs;
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList(name);
	}

	public TCPatternListList getParamPatternList()
	{
		TCPatternListList parameters = new TCPatternListList();
		TCPatternList plist = new TCPatternList();

		for (TCPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		parameters.add(plist);
		return parameters;
	}

	private TCExplicitFunctionDefinition getPreDefinition()
	{
		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(accessSpecifier, name.getPreName(precondition.location),
			typeParams, type.getPreType(), getParamPatternList(), precondition, null, null, false, null);

		def.classDefinition = classDefinition;
		return def;
	}

	private TCExplicitFunctionDefinition getPostDefinition()
	{
		TCPatternListList parameters = getParamPatternList();
		parameters.get(0).add(result.pattern);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(accessSpecifier, name.getPostName(postcondition.location),
			typeParams, type.getPostType(),	parameters, postcondition,	null, null, false, null);

		def.classDefinition = classDefinition;
		return def;
	}

	@Override
	public boolean isFunction()
	{
		return true;
	}

	@Override
	public boolean isCallableFunction()
	{
		return (body != null);
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof TCSubclassResponsibilityExpression;
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCPatternListTypePair pltp: parameterPatterns)
		{
			defs.addAll(pltp.getDefinitions(NameScope.LOCAL));
		}

		Environment local = new FlatEnvironment(defs, env);
		TCNameSet names = new TCNameSet();
		
		if (body != null)
		{
			names.addAll(body.getFreeVariables(globals, local));
		}
		
		if (predef != null)
		{
			names.addAll(predef.getFreeVariables(globals, local, returns));
		}
		
		if (postdef != null)
		{
			names.addAll(postdef.getFreeVariables(globals, local, returns));
		}
		
		return names;
	}
}
