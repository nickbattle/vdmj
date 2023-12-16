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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;

public class INMultipleBindList extends INMappedList<TCMultipleBind, INMultipleBind>
{
	private static final long serialVersionUID = 1L;

	public INMultipleBindList()
	{
		super();
	}

	public INMultipleBindList(TCMultipleBindList from) throws Exception
	{
		super(from);
	}

	/**
	 * QuickCheck has set some type binds?
	 */
	public boolean isInstrumented()
	{
		for (INMultipleBind bind: this)
		{
			if (bind instanceof INBindingSetter)	// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;
				
				if (setter.getBindValues() != null)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * This is used by the QuickCheck plugin to limit PO execution times.
	 */
	public long getTimeout()
	{
		for (INMultipleBind bind: this)
		{
			if (bind instanceof INBindingSetter)	// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;
				long timeout = setter.getTimeout();
				
				if (timeout > 0)
				{
					return timeout;
				}
			}
		}
		
		return 0;
	}

	/**
	 * This is used by the QuickCheck plugin to report which values failed.
	 */
	public void setCounterexample(Context ctxt, boolean didTimeout)
	{
		for (INMultipleBind bind: this)
		{
			if (bind instanceof INBindingSetter)	// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;
				
				if (setter.getBindValues() != null)	// One we care about (set QC values for)
				{
					setter.setCounterexample(ctxt, didTimeout);
					break;							// Just one will do - see QC printFailPath
				}
			}
		}
	}

	/**
	 * This is used by the QuickCheck plugin to report which values succeeded.
	 */
	public void setWitness(Context ctxt)
	{
		for (INMultipleBind bind: this)
		{
			if (bind instanceof INBindingSetter)	// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;
				
				if (setter.getBindValues() != null)	// One we care about (set QC values for)
				{
					setter.setWitness(ctxt);
					break;							// Just one will do - see QC printFailPath
				}
			}
		}
	}

	public boolean hasAllValues()
	{
		for (INMultipleBind bind: this)
		{
			if (bind instanceof INBindingSetter)	// Type and multitype binds
			{
				INBindingSetter setter = (INBindingSetter)bind;

				if (!setter.hasAllValues())
				{
					return false;	// One hasn't
				}
			}
		}
		
		return true;	// All have
	}
}
