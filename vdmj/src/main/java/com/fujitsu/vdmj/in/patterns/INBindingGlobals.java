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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.runtime.Context;

public class INBindingGlobals
{
	private static INBindingGlobals INSTANCE = null;
	
	public static synchronized INBindingGlobals getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new INBindingGlobals();
		}
		
		return INSTANCE;
	}
	
	private Context bindCounterexample = null;
	private Context bindWitness = null;
	private boolean bindAllValues = false;
	private long bindTimeout = 0;
	private boolean didTimeout = false;
	private boolean maybe = false;

	private INBindingGlobals()
	{
		clear();
	}
	
	public void clear()
	{
		bindCounterexample = null;
		bindWitness = null;
		bindAllValues = false;
		bindTimeout = 0;
		didTimeout = false;
		maybe = false;
	}

	public void setTimeout(long timeout)
	{
		bindTimeout = timeout;
	}

	public long getTimeout()
	{
		return bindTimeout;
	}

	public void setAllValues(boolean hasAllValues)
	{
		bindAllValues = hasAllValues;
	}
	
	public boolean hasAllValues()
	{
		return this.bindAllValues;
	}
	
	public void setDidTimeout(boolean did)
	{
		this.didTimeout = did;
	}
	
	public boolean didTimeout()
	{
		return this.didTimeout;
	}
	
	public void setCounterexample(Context ctxt)
	{
		if (ctxt == null)
		{
			bindCounterexample = null;
		}
		else if (bindCounterexample == null)	// Catch first fail, don't overwrite
		{
			bindCounterexample = ctxt;
		}
	}
	
	public Context getCounterexample()
	{
		return bindCounterexample;
	}
	
	public void setWitness(Context ctxt)
	{
		if (ctxt == null)
		{
			bindWitness = null;
		}
		else if (bindWitness == null)	// Catch first witness, don't overwrite
		{
			bindWitness = ctxt;
		}
	}

	public Context getWitness()
	{
		return bindWitness;
	}
	
	public void setMaybe(boolean maybe)
	{
		this.maybe = this.maybe || maybe;	// Cumulative
	}
	
	public void setMaybe()
	{
		this.maybe = this.maybe || (!bindAllValues);	// Cumulative
	}
	
	public boolean hasMaybe()
	{
		return maybe;
	}
}
