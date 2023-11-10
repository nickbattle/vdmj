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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.definitions;

import java.util.Iterator;
import java.util.ListIterator;

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotYetSpecifiedExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubclassResponsibilityExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCFunctionCallFinder;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
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
	public final TCTypeList typeParams;
	public TCFunctionType type;
	public final TCTypeList unresolved;
	public final TCPatternListList paramPatternList;
	public final TCExpression precondition;
	public final TCExpression postcondition;
	public final TCExpression body;
	public final boolean isTypeInvariant;
	public final TCExpression measureExp;
	public final boolean isCurried;

	public TCExplicitFunctionDefinition predef;
	public TCExplicitFunctionDefinition postdef;
	public TCDefinitionListList paramDefinitionList;

	public boolean recursive = false;
	public boolean isUndefined = false;
	private TCType actualResult;
	private TCType expectedResult;
	
	public TCExplicitFunctionDefinition measureDef;
	public TCNameToken measureName;

	public TCExplicitFunctionDefinition(TCAnnotationList annotations,
		TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCTypeList typeParams, TCFunctionType type,
		TCPatternListList parameters, TCExpression body,
		TCExpression precondition,
		TCExpression postcondition, boolean typeInvariant, TCExpression measure)
	{
		super(Pass.DEFS, name.getLocation(), name, NameScope.GLOBAL);

		this.annotations = annotations;
		this.accessSpecifier = accessSpecifier;
		this.typeParams = typeParams;
		this.type = type;
		this.unresolved = type.unresolvedTypes();
		this.paramPatternList = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		this.isTypeInvariant = typeInvariant;
		this.measureExp = measure;
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
				(typeParams == null ? ": " : "[" + typeParams + "]: ") + Utils.deBracketed(type) +
				"\n\t" + name + params + " ==\n" + body +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition) +
				(measureExp == null ? "" : "\n\tmeasure " + measureExp);
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

		for (TCType ptype: typeParams)
		{
			TCParameterType param = (TCParameterType)ptype;
			TCDefinition p = new TCLocalDefinition(param.location, param.name, param);
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

			type = type.typeResolve(params);
		}
		else
		{
			type = type.typeResolve(base);
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
		if (annotations != null) annotations.tcBefore(this, base, scope);

		TCDefinitionList defs = new TCDefinitionList();

		if (typeParams != null)
		{
			defs.addAll(getTypeParamDefinitions());
		}
		
		TypeComparator.checkImports(base, unresolved, location.module);
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
		checked.setFunctional(true, true);

		defs.typeCheck(local, scope);

		if (base.isVDMPP() && !accessSpecifier.isStatic)
		{
			local.add(getSelfDefinition());
		}

		/**
		 * The pre/post definitions could be typechecked directly, but we want
		 * special processing to give specific errors and to add qualified
		 * definitions.
		 */
		if (predef != null)
		{
			TCBooleanType expected = new TCBooleanType(location);
			Environment pre = new FlatEnvironment(new TCDefinitionList(), local);
			pre.setEnclosingDefinition(predef);
			predef.paramDefinitionList = predef.getParamDefinitions();
			
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

		if (postdef != null)
		{
			TCPattern rp = new TCIdentifierPattern(name.getResultName(location));
			TCDefinitionList rdefs = rp.getDefinitions(expectedResult, NameScope.NAMES);
			FlatCheckedEnvironment post = new FlatCheckedEnvironment(rdefs, local, NameScope.NAMES);
			post.setStatic(accessSpecifier);
			post.setEnclosingDefinition(postdef);
			post.setFunctional(true, true);
			postdef.paramDefinitionList = postdef.getParamDefinitions();
			TCBooleanType expected = new TCBooleanType(location);
			TCType b = postdef.body.typeCheck(post, null, NameScope.NAMES, expected);
			// post.unusedCheck(); This would detect unused RESULTs 

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
		
		if (measureExp instanceof TCVariableExpression)
		{
			TCVariableExpression exp = (TCVariableExpression)measureExp;
			if (base.isVDMPP()) exp.name.setTypeQualifier(getMeasureParams());
			TCDefinition def = base.findName(exp.name, scope);
			
			if (def instanceof TCExplicitFunctionDefinition)
			{
				setMeasureDef(exp.name, base, scope);
			}
			else
			{
				setMeasureExp(base, local, scope);
			}
		}
		else if (measureExp instanceof TCNotYetSpecifiedExpression)
		{
			// Undefined measure, so ignore (without warning).
			measureDef = null;
			measureName = null;
		}
		else if (measureExp != null)
		{
			setMeasureExp(base, local, scope);
		}

		if (!(body instanceof TCNotYetSpecifiedExpression) &&
			!(body instanceof TCSubclassResponsibilityExpression) &&
			!(name.isMeasureName()))
		{
			checked.unusedCheck();	// Look underneath qualified definitions, if any
		}

		if (annotations != null) annotations.tcAfter(this, type, base, scope);
	}

	/**
	 * Set measureDef to a newly created function, based on the measure expression. 
	 */
	private void setMeasureExp(Environment base, Environment local, NameScope scope)
	{
		TCType actual = measureExp.typeCheck(local, null, NameScope.NAMES, null);
		measureName = name.getMeasureName(measureExp.location);
		checkMeasure(measureName, actual);
		
		// Concatenate the parameter patterns into one list for curried measures
		TCPatternList all = new TCPatternList();
		
		for (TCPatternList p: paramPatternList)
		{
			all.addAll(p);
		}
		
		TCPatternListList cpll = new TCPatternListList();
		cpll.add(all);
		
		// Note that the measure_f has the precondition of the function it measures.
		
		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, measureName,
				typeParams, type.getMeasureType(isCurried, actual), cpll, measureExp, precondition, null, false, null);

		def.classDefinition = classDefinition;
		def.implicitDefinitions(base);
		def.typeResolve(base);
		def.typeCheck(base, scope);

		measureDef = def;
	}

	/**
	 * Check that measure is an existing named explicit function definition.
	 */
	private void setMeasureDef(TCNameToken mname, Environment base, NameScope scope)
	{
		if (base.isVDMPP()) mname.setTypeQualifier(getMeasureParams());
		measureDef = (TCExplicitFunctionDefinition) base.findName(mname, scope);

		if (measureDef == this)
		{
			mname.report(3304, "Recursive function cannot be its own measure");
		}
		else
		{
			TCExplicitFunctionDefinition efd = (TCExplicitFunctionDefinition)measureDef;
			measureName = efd.name;
			
			if (this.typeParams == null && efd.typeParams != null)
			{
				mname.report(3309, "Measure must not be polymorphic");
			}
			else if (this.typeParams != null && efd.typeParams == null)
			{
				mname.report(3310, "Measure must also be polymorphic");
			}
			else if (this.typeParams != null && efd.typeParams != null
					&& !this.typeParams.equals(efd.typeParams))
			{
				mname.report(3318, "Measure's type parameters must match function's");
				detail2("Actual", efd.typeParams, "Expected", typeParams);
			}

			TCFunctionType mtype = (TCFunctionType)measureDef.getType();
			
			if (typeParams != null)		// Polymorphic, so compare "shape" of param signature
			{
				if (!mtype.parameters.toString().equals(getMeasureParams().toString()))
				{
					mname.report(3303, "Measure parameters different to function");
					detail2(mname.getName(), mtype.parameters, "Expected", getMeasureParams());
				}
			}
			else if (!TypeComparator.compatible(mtype.parameters, getMeasureParams()))
			{
				mname.report(3303, "Measure parameters different to function");
				detail2(mname.getName(), mtype.parameters, "Expected", getMeasureParams());
			}

			checkMeasure(mname, mtype.result);
		}
	}

	/**
	 * A measure must return a nat or nat-tuple.
	 */
	private void checkMeasure(TCNameToken mname, TCType result)
	{
		if (!result.isNumeric(location))
		{
			if (result.isProduct(location))
			{
				TCProductType pt = result.getProduct();

				for (TCType t: pt.types)
				{
					if (!t.isNumeric(location))
					{
						mname.report(3272, "Measure range is not a nat, or a nat tuple");
						mname.detail("Actual", result);
						break;
					}
				}
			}
			else
			{
				mname.report(3272, "Measure range is not a nat, or a nat tuple");
				mname.detail("Actual", result);
			}
		}
	}

	@Override
	public TCType getType()
	{
		return type;		// NB entire "->" type, not the result
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

	public TCDefinitionListList getParamDefinitions()
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
		
		if (measureDef != null)
		{
			return measureDef.findName(sought, scope);	// eg. pre_measure_f
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
		
		if (measureName != null && measureName.isMeasureName())
		{
			defs.add(measureDef);
		}

		return defs;
	}

	private TCExplicitFunctionDefinition getPreDefinition()
	{
		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, name.getPreName(precondition.location),
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

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, name.getPostName(postcondition.location),
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
	
	@Override
	public TCNameSet getCallMap()
	{
		TCFunctionCallFinder finder = new TCFunctionCallFinder();
		TCNameSet found = new TCNameSet();
		found.addAll(body.apply(finder, null));
		
		if (predef != null)
		{
			found.addAll(predef.getCallMap());
		}
		
		if (postdef != null)
		{
			found.addAll(postdef.getCallMap());
		}

		return found;
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExplicitFunctionDefinition(this, arg);
	}
}
