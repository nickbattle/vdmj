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

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotYetSpecifiedExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubclassResponsibilityExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCFunctionCallFinder;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
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
	public final TCExpression measureExp;

	public TCFunctionType type;
	public TCTypeList unresolved;
	public TCExplicitFunctionDefinition predef;
	public TCExplicitFunctionDefinition postdef;
	public TCDefinitionList paramDefinitions;

	public boolean recursive = false;
	public boolean isUndefined = false;
	public TCType actualResult;
	
	public TCExplicitFunctionDefinition measureDef;
	public TCNameToken measureName;


	public TCImplicitFunctionDefinition(TCAnnotationList annotations,
		TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCNameList typeParams, TCPatternListTypePairList parameterPatterns,
		TCPatternTypePair result,
		TCExpression body,
		TCExpression precondition,
		TCExpression postcondition,
		TCExpression measureExp)
	{
		super(Pass.DEFS, name.getLocation(), name, NameScope.GLOBAL);

		this.annotations = annotations;
		this.accessSpecifier = accessSpecifier;
		this.typeParams = typeParams;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.measureExp = measureExp;

		TCTypeList ptypes = new TCTypeList();

		for (TCPatternListTypePair ptp: parameterPatterns)
		{
			ptypes.addAll(ptp.getTypeList());
		}

		// NB: implicit functions are always +> total, apparently
		type = new TCFunctionType(location, ptypes, false, result.type);
		unresolved = type.unresolvedTypes();
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
				(postcondition == null ? "" : "\n\tpost " + postcondition) +
				(measureExp == null ? "" : "\n\tmeasure " + measureExp);
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
		if (annotations != null) annotations.tcBefore(this, base, scope);

		TCDefinitionList defs = new TCDefinitionList();
		TypeComparator.checkImports(base, unresolved, location.module);
		TypeComparator.checkComposeTypes(type, base, false);

		if (typeParams != null)
		{
			defs.addAll(getTypeParamDefinitions());
		}

		paramDefinitions = new TCDefinitionList();

		for (TCPatternListTypePair pltp: parameterPatterns)
		{
			paramDefinitions.addAll(pltp.getDefinitions(NameScope.LOCAL));
		}

		defs.addAll(checkDuplicatePatterns(paramDefinitions));
		FlatEnvironment local = new FlatCheckedEnvironment(defs, base, scope);
		FlatCheckedEnvironment checked = (FlatCheckedEnvironment)local;
		checked.setStatic(accessSpecifier);
		checked.setEnclosingDefinition(this);
		checked.setFunctional(true, true);

		defs.typeCheck(local, scope);

		if (predef != null)
		{
			FlatEnvironment pre = new FlatEnvironment(paramDefinitions, base);
			pre.setEnclosingDefinition(predef);
			predef.paramDefinitionList = predef.getParamDefinitions();

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
			postdef.paramDefinitionList = postdef.getParamDefinitions();

			if (result != null)
			{
	    		TCDefinitionList postdefs = result.getDefinitions();
	    		FlatCheckedEnvironment post = new FlatCheckedEnvironment(postdefs, local, NameScope.NAMES);
	    		post.setStatic(accessSpecifier);
	    		post.setEnclosingDefinition(postdef);
	    		post.setFunctional(true, true);
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

		if (measureExp != null && body == null)
		{
			name.report(3273, "Measure not allowed for an implicit function");
		}
		else if (measureExp instanceof TCVariableExpression)
		{
			TCVariableExpression exp = (TCVariableExpression)measureExp;
			if (base.isVDMPP()) exp.name.setTypeQualifier(type.parameters);
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
			!(body instanceof TCSubclassResponsibilityExpression))
		{
			local.unusedCheck();
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
		
		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, measureName,
				typeParams, type.getMeasureType(false, actual), getParamPatternList(), measureExp, null, null, false, null);

		def.classDefinition = classDefinition;
		def.typeResolve(base);
		
		def.typeCheck(base, scope);
		
		measureDef = def;
	}

	/**
	 * Check that measure is an existing named explicit function definition.
	 */
	private void setMeasureDef(TCNameToken mname, Environment base, NameScope scope)
	{
		if (base.isVDMPP()) mname.setTypeQualifier(type.parameters);
		measureDef = (TCExplicitFunctionDefinition) base.findName(mname, scope);

		if (measureDef == null)
		{
			mname.report(3270, "Measure " + mname + " is not in scope");
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

			if (!TypeComparator.compatible(mtype.parameters, type.parameters))
			{
				mname.report(3303, "Measure parameters different to function");
				detail2(mname.getName(), mtype.parameters, mname.getName(), type.parameters);
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
		
		if (measureDef != null && measureDef.findName(sought, scope) != null)
		{
			return measureDef;
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
		
		if (measureName != null && measureName.getName().startsWith("measure_"))
		{
			defs.add(measureDef);
		}

		return defs;
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
		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, name.getPreName(precondition.location),
			typeParams, type.getPreType(), getParamPatternList(), precondition, null, null, false, null);

		def.classDefinition = classDefinition;
		return def;
	}

	private TCExplicitFunctionDefinition getPostDefinition()
	{
		TCPatternListList parameters = getParamPatternList();
		parameters.get(0).add(result.pattern);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier, name.getPostName(postcondition.location),
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
	public TCNameSet getCallMap()
	{
		TCFunctionCallFinder finder = new TCFunctionCallFinder();
		TCNameSet found = new TCNameSet();

		if (body != null)
		{
			found.addAll(body.apply(finder, null));
		}
		
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
		return visitor.caseImplicitFunctionDefinition(this, arg);
	}
}
