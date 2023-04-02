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

package com.fujitsu.vdmj.values;

import java.io.Serializable;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.INStateDefinition;
import com.fujitsu.vdmj.in.expressions.INEqualsExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;

public class State implements ValueListener, Serializable
{
	private static final long serialVersionUID = 1L;
	public final INStateDefinition definition;
	public final UpdatableValue recordValue;
	public final Context context;

	public boolean doInvariantChecks = true;

	public State(INStateDefinition definition)
	{
		this.definition = definition;
		NameValuePairList fieldvalues = new NameValuePairList();

		for (TCField f: definition.fields)
		{
			fieldvalues.add(new NameValuePair(f.tagname,
				UpdatableValue.factory(new ValueListenerList(this))));
		}

		TCRecordType rt = (TCRecordType)definition.getType();
		this.recordValue = UpdatableValue.factory(new RecordValue(rt, fieldvalues),
			new ValueListenerList(this));

		this.context = new Context(definition.location, "module state", null);
		this.context.put(definition.name, recordValue);
		this.context.putList(fieldvalues);
	}

	public void initialize(Context globals)
	{
		try
		{
			// We can't check the invariant while we're initializing fields
			doInvariantChecks = false;

			if (definition.initPattern != null)
			{
				// Note that we don't call the initfunc FunctionValue. This is
				// so that calls to init_sigma can test their arguments without
				// changing state. See ASTStateInitExpression.

				if (!definition.canBeExecuted ||
					!(definition.initExpression instanceof INEqualsExpression))
				{
					throw new ValueException(
						4144, "State init expression cannot be executed", globals);
				}

				INEqualsExpression ee = (INEqualsExpression)definition.initExpression;
				ee.location.hit();
				ee.left.location.hit();
				ee.left.breakpoint.check(ee.left.location, globals);
				Value v = ee.right.eval(globals);

				if (!(v instanceof RecordValue))
				{
					throw new ValueException(
						4144, "State init expression cannot be executed", globals);
				}

				RecordValue iv = (RecordValue)v;

				for (TCField f: definition.fields)
				{
					Value sv = context.get(f.tagname);
					sv.set(ee.location, iv.fieldmap.get(f.tag), globals);
				}
			}

			doInvariantChecks = true;
			changedValue(null, null, globals);
		}
		catch (ValueException e)
		{
			definition.abort(e);
		}
		finally
		{
			doInvariantChecks = true;
		}
	}

	public Context getContext()
	{
		return context;
	}

	@Override
	public void changedValue(LexLocation location, Value changed, Context ctxt) throws ValueException
	{
		if (doInvariantChecks && definition.invfunc != null && Settings.invchecks)
		{
			if (location == null)
			{
				location = definition.invfunc.body.location;
			}

			if (!definition.invfunc.eval(
				definition.invfunc.location, recordValue, ctxt).boolValue(ctxt))
			{
				ExceptionHandler.handle(new ContextException(
					4131, "State invariant violated: " + definition.invfunc.name, location, ctxt));
			}
		}
	}
}
