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

package quickcheck.qcplugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.QuickCheck;

public class ExampleQCPlugin extends QCPlugin
{
	public ExampleQCPlugin(List<String> argv)
	{
		// Remove your "qc" plugin arguments from the list here
		// It's useful to include the plugin name, like "-example:n"
	}
	
	@Override
	public String getName()
	{
		return "example";	// Can be used with -p <name>
	}

	@Override
	public boolean hasErrors()
	{
		return false;	// Called after init and getValues
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		return true;	// Return value => whether to do checks or stop 
	}

	@Override
	public Map<String, ValueSet> getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds)
	{
		return new HashMap<String, ValueSet>();
	}

	@Override
	public String help()
	{
		return getName() + " : [options/flags]";
	}

	@Override
	public boolean useByDefault()
	{
		return false;	// Not used if no -p options given
	}
}
