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
import java.util.ListIterator;

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotYetSpecifiedExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubclassResponsibilityExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an explicit function definition.
 */
public class TCExplicitFunctionDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameList typeParams;
	public TCFunctionType type;
	public final TCPatternListList paramPatternList;
	public final TCExpression precondition;
	public final TCExpression postcondition;
	public final TCExpression body;
	public final boolean isTypeInvariant;
	public final TCNameToken measure;
	public final boolean isCurried;

	public TCExplicitFunctionDefinition predef;
	public TCExplicitFunctionDefinition postdef;
	public TCDefinitionListList paramDefinitionList;

	public boolean recursive = false;
	public boolean isUndefined = false;
	private TCType actualResult;
	private TCType expectedResult;
	public int measureLexical = 0;
	private TCDefinition measuredef;

	public TCExplicitFunctionDefinition(TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCNameList typeParams, TCFunctionType type,
		TCPatternListList parameters, TCExpression body,
		TCExpression precondition,
		TCExpression postcondition, boolean typeInvariant, TCNameToken measure)
	{
		super(Pass.DEFS, name.getLocation(), name, NameScope.GLOBAL);

		this.accessSpecifier = accessSpecifier;
		this.typeParams = typeParams;
		this.type = type;
		this.paramPatternList = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		this.isTypeInvariant = typeInvariant;
		this.measure = measure;
		this.isCurried = parameters.size() > 1;

		type.definitions = new TCDefinitionList(this);
		type.instantiated = typeParams == null ? null : false;
	}

	@Override
	public String toString()
	{
		StringBuilder params = new StringBuilder();

		for (TCPatternList plist: paramPatternList)
		{
			params.append("(" + Utils.listToString(plist) + ")");
		}

		return accessSpecifier.ifSet(" ") + name +
				(typeParams == null ? ": " : "[" + typeParams + "]: ") + type +
				"\n\t" + name + params + " ==\n" + body +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}
	
	@Override
	public String kind()
	{
		return "explicit function";
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
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

	@Override
	public void setClassDefinition(TCClassDefinition def)
	{
		super.setClassDefinition(def);

		if (predef != null)
		{
			predef.setClassDefinition(def);
		}

		if (postdef != null)
		{
			postdef.setClassDefinition(def);
		}
	}

	public TCDefinitionList getTypeParamDefinitions()
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCNameToken pname: typeParams)
		{
			TCDefinition p = new TCLocalDefinition(
					pname.getLocation(), pname, new TCParameterType(pname));

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

		if (base.isVDMPP())
		{
			name.setTypeQualifier(type.parameters);
		}

		if (body instanceof TCSubclassResponsibilityExpression ||
			body instanceof TCNotYetSpecifiedExpression)
		{
			isUndefined = true;
		}

		if (precondition != null)
		{
			predef.typeResolve(base);
		}

		if (postcondition != null)
		{
			postdef.typeResolve(base);
		}

		for (TCPatternList pp: paramPatternList)
		{
			pp.typeResolve(base);
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		TCDefinitionList defs = new TCDefinitionList();

		if (typeParams != null)
		{
			defs.addAll(getTypeParamDefinitions());
		}
		
		TypeComparator.checkComposeTypes(type, base, false);

		expectedResult = checkParams(paramPatternList.listIterator(), type);

		paramDefinitionList = getParamDefinitions();

		for (TCDefinitionList pdef: paramDefinitionList)
		{
			defs.addAll(pdef);	// All definitions of all parameter lists
		}

		FlatEnvironment local = new FlatCheckedEnvironment(defs, base, scope);
		FlatCheckedEnvironment checked = (FlatCheckedEnvironment)local;
		checked.setStatic(accessSpecifier);
		checked.setEnclosingDefinition(this);
		checked.setFunctional(true);

		defs.typeCheck(local, scope);

		if (base.isVDMPP() && !accessSpecifier.isStatic)
		{
			local.add(getSelfDefinition());
		}

		if (predef != null)
		{
			TCBooleanType expected = new TCBooleanType(location);
			TCType b = predef.body.typeCheck(local, null, NameScope.NAMES, expected);

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
			TCPattern rp = new TCIdentifierPattern(name.getResultName(location));
			TCDefinitionList rdefs = rp.getDefinitions(expectedResult, NameScope.NAMES);
			FlatCheckedEnvironment post =
				new FlatCheckedEnvironment(rdefs, local, NameScope.NAMES);

			TCBooleanType expected = new TCBooleanType(location);
			TCType b = postdef.body.typeCheck(post, null, NameScope.NAMES, expected);

			if (!b.isType(TCBooleanType.class, location))
			{
				report(3018, "Postcondition returns unexpected type");
				detail2("Actual", b, "Expected", expected);
			}
		}

		// This check returns the type of the function body in the case where
		// all of the curried parameter sets are provided.

		actualResult = body.typeCheck(local, null, scope, expectedResult);

		if (!TypeComparator.compatible(expectedResult, actualResult))
		{
			report(3018, "Function returns unexpected type");
			detail2("Actual", actualResult, "Expected", expectedResult);
		}

		if (type.narrowerThan(accessSpecifier))
		{
			report(3019, "Function parameter visibility less than function definition");
		}
		
		if (base.isVDMPP()
			&& accessSpecifier.access == Token.PRIVATE
			&& body instanceof TCSubclassResponsibilityExpression)
		{
			report(3329, "Abstract function/operation must be public or protected");
		}

		if (measure == null && recursive)
		{
			warning(5012, "Recursive function has no measure");
		}
		else if (measure != null)
		{
			if (base.isVDMPP()) measure.setTypeQualifier(getMeasureParams());
			measuredef = base.findName(measure, scope);

			if (measuredef == null)
			{
				measure.report(3270, "Measure " + measure + " is not in scope");
			}
			else if (!(measuredef instanceof TCExplicitFunctionDefinition))
			{
				measure.report(3271, "Measure " + measure + " is not an explicit function");
			}
			else if (measuredef == this)
			{
				measure.report(3304, "Recursive function cannot be its own measure");
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
				
				if (typeParams != null)		// Polymorphic, so compare "shape" of param signature
				{
					if (!mtype.parameters.toString().equals(getMeasureParams().toString()))
					{
						measure.report(3303, "Measure parameters different to function");
						detail2(measure.getName(), mtype.parameters, "Expected", getMeasureParams());
					}
				}
				else if (!TypeComparator.compatible(mtype.parameters, getMeasureParams()))
				{
					measure.report(3303, "Measure parameters different to function");
					detail2(measure.getName(), mtype.parameters, "Expected", getMeasureParams());
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
								break;
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
		return type;		// NB entire "->" type, not the result
	}

	public TCFunctionType getType(TCTypeList actualTypes)
	{
		Iterator<TCType> ti = actualTypes.iterator();
		TCFunctionType ftype = type;

		if (typeParams != null)
		{
			for (TCNameToken pname: typeParams)
			{
				TCType ptype = ti.next();
				ftype = (TCFunctionType)ftype.polymorph(pname, ptype);
			}

			ftype.instantiated = true;
		}

		return ftype;
	}
	
	private TCTypeList getMeasureParams()
	{
		TCTypeList params = new TCTypeList();
		params.addAll(type.parameters);
		
		if (isCurried)
		{
			TCType rtype = type.result;

			while (rtype instanceof TCFunctionType)
			{
				TCFunctionType ftype = (TCFunctionType)rtype;
				params.addAll(ftype.parameters);
				rtype = ftype.result;
			}
		}
		
		return params;
	}

	private TCType checkParams(ListIterator<TCPatternList> plists, TCFunctionType ftype)
	{
		TCTypeList ptypes = ftype.parameters;
		TCPatternList patterns = plists.next();

		if (patterns.size() > ptypes.size())
		{
			report(3020, "Too many parameter patterns");
			detail2("Pattern(s)", patterns, "Type(s)", ptypes);
			return ftype.result;
		}
		else if (patterns.size() < ptypes.size())
		{
			report(3021, "Too few parameter patterns");
			detail2("Pattern(s)", patterns, "Type(s)", ptypes);
			return ftype.result;
		}

		if (ftype.result instanceof TCFunctionType)
		{
			if (!plists.hasNext())
			{
				// We're returning the function itself
				return ftype.result;
			}

			// We're returning what the function returns, assuming we
			// pass the right parameters. Note that this recursion
			// means that we finally return the result of calling the
			// function with *all* of the curried argument sets applied.
			// This is because the type check of the body determines
			// the return type when all of the curried parameters are
			// provided.

			return checkParams(plists, (TCFunctionType)ftype.result);
		}

		if (plists.hasNext())
		{
			report(3022, "Too many curried parameters");
		}

		return ftype.result;
	}

	private TCDefinitionListList getParamDefinitions()
	{
		TCDefinitionListList defList = new TCDefinitionListList();
		TCFunctionType ftype = type;	// Start with the overall function
		Iterator<TCPatternList> piter = paramPatternList.iterator();

		while (piter.hasNext())
		{
			TCPatternList plist = piter.next();
			TCDefinitionList defs = new TCDefinitionList();
			TCTypeList ptypes = ftype.parameters;
			Iterator<TCType> titer = ptypes.iterator();

			if (plist.size() != ptypes.size())
			{
				// This is a type/param mismatch, reported elsewhere. But we
				// have to create definitions to avoid a cascade of errors.

				TCType unknown = new TCUnknownType(location);

				for (TCPattern p: plist)
				{
					defs.addAll(p.getDefinitions(unknown, NameScope.LOCAL));
				}
			}
			else
			{
    			for (TCPattern p: plist)
    			{
    				defs.addAll(p.getDefinitions(titer.next(), NameScope.LOCAL));
    			}
			}
			
			defList.add(checkDuplicatePatterns(defs));

			if (ftype.result instanceof TCFunctionType)	// else???
			{
				ftype = (TCFunctionType)ftype.result;
			}
		}

		return defList;
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

	private TCExplicitFunctionDefinition getPreDefinition()
	{
		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(accessSpecifier, name.getPreName(precondition.location),
			typeParams, type.getCurriedPreType(isCurried), paramPatternList, precondition, null, null, false, null);

		def.classDefinition = classDefinition;
		return def;
	}

	private TCExplicitFunctionDefinition getPostDefinition()
	{
		TCPatternList last = new TCPatternList();
		int psize = paramPatternList.size();

		for (TCPattern p: paramPatternList.get(psize - 1))
		{
			last.add(p);
		}

		last.add(new TCIdentifierPattern(name.getResultName(location)));

		TCPatternListList parameters = new TCPatternListList();

		if (psize > 1)
		{
			parameters.addAll(paramPatternList.subList(0, psize - 1));
		}

		parameters.add(last);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(accessSpecifier, name.getPostName(postcondition.location),
			typeParams, type.getCurriedPostType(isCurried), parameters, postcondition, null, null, false, null);

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
		return true;
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof TCSubclassResponsibilityExpression;
	}
}
