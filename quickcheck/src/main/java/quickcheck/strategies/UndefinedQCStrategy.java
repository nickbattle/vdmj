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

package quickcheck.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.PluginConsole;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;

/**
 * A strategy to attempt to include a VDM-SL state vector that has not been initialized.
 */
public class UndefinedQCStrategy extends QCStrategy
{
	public UndefinedQCStrategy(List<String> argv)
	{
		// No options defined
	}

	@Override
	public String getName()
	{
		return "undefined";
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			PluginConsole.errorln(getName() + " strategy only applies to VDM-SL");
			return false;
		}

		return true;
	}

	@Override
	public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt)
	{
		StrategyResults results = new StrategyResults();
		Environment env = Interpreter.getInstance().getGlobalEnvironment();
		TCStateDefinition state = env.findStateDefinition();

		if (state != null && state.initdef == null)		// has state, but no init clause
		{
			TCRecordType rectype = (TCRecordType)state.getType();

			for (INBindingOverride bind: binds)
			{
				if (bind.getType().equals(rectype))
				{
					// Add a mk_Sigma value with undefined state fields...
					NameValuePairList fvals = new NameValuePairList();
					
					for (TCField field: rectype.fields)
					{
						fvals.add(new NameValuePair(field.tagname, new UndefinedValue()));
					}
					
					RecordValue rval = new RecordValue(rectype, fvals);		// No inv check!

					Map<String, ValueList> values = new HashMap<String, ValueList>();
					values.put(bind.toString(), new ValueList(rval));
					results = new StrategyResults(values, false);
					break;	// One is enough
				}
			}
		}

		return results;
	}

	@Override
	public boolean useByDefault()
	{
		return false;	// So you choose to try the undefined values
	}
}
