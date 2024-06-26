/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;

import quickcheck.QuickCheck;

abstract public class QCStrategy
{
	abstract public String getName();
	abstract public boolean hasErrors();
	abstract public boolean useByDefault();
	abstract public boolean init(QuickCheck qc);
	abstract public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt);
	abstract public String help();
	
	/**
	 * These methods help with access to the JSON parameters passed from VSCode.
	 */
	
	protected Map<String, Object> getParams(List<Map<String, Object>> list, String name)
	{
		if (list != null)
		{
			for (Map<String, Object> entry: list)
			{
				if (name.equals(entry.get("name")))
				{
					return entry;
				}
			}
		}
		
		return new HashMap<String, Object>();
	}
	
	protected int get(Map<String, Object> map, String key, int def)
	{
		Long value = (Long) map.get(key);
		return (value != null) ? value.intValue() : def;
	}
	
	protected long get(Map<String, Object> map, String key, long def)
	{
		Long value = (Long) map.get(key);
		return (value != null) ? value.longValue() : def;
	}
	
	protected boolean get(Map<String, Object> map, String key, boolean def)
	{
		Boolean value = (Boolean) map.get(key);
		return (value != null) ? value.booleanValue() : def;
	}
}
