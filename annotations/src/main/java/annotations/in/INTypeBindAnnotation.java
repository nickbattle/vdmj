/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExistsExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.patterns.INBindingGlobals;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class INTypeBindAnnotation extends INAnnotation
{
	private final INMultipleTypeBind typebind;
	private final INExpression expression;

	public INTypeBindAnnotation(TCIdentifierToken name, INMultipleTypeBind typebind, INExpression expression)
	{
		super(name, null);
		this.typebind = typebind;
		this.expression = expression;
	}
	
	public void inBefore(INExpression exp, Context ctxt)
	{
		INBindingGlobals globals = INBindingGlobals.getInstance();
		
		if (exp instanceof INForAllExpression)
		{
			INForAllExpression forall = (INForAllExpression)exp;
			forall.globals = globals;
			setValues(forall.bindList, ctxt);
		}
		else if (exp instanceof INExistsExpression)
		{
			INExistsExpression exists = (INExistsExpression)exp;
			exists.globals = globals;
			setValues(exists.bindList, ctxt);
		}
	}

	private void setValues(INMultipleBindList bindList, Context ctxt)
	{
		INMultipleTypeBind bind = hasTypeBind(bindList);
		INBindingOverride override = new INBindingOverride(typebind.toString(), typebind.type);
		override.setBindValues(null);
		bind.setter = override;

		try
		{
			Value value = expression.eval(ctxt);
			ValueSet set = value.setValue(ctxt);
			ValueList list = new ValueList();
			list.addAll(set);
			override.setBindValues(list);
		}
		catch (ValueException e)
		{
			// Ignore?
		}
	}

	private INMultipleTypeBind hasTypeBind(INMultipleBindList bindList)
	{
		for (INMultipleBind mbind: bindList)
		{
			if (mbind instanceof INMultipleTypeBind)
			{
				INMultipleTypeBind mtbind = (INMultipleTypeBind)mbind;

				if (mtbind.toString().equals(typebind.toString()))
				{
					return mtbind;
				}
			}
		}

		return null;
	}
}
