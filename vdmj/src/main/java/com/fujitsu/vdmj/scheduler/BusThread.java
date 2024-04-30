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

package com.fujitsu.vdmj.scheduler;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMThreadDeath;
import com.fujitsu.vdmj.runtime.Context;

public class BusThread extends SchedulableThread
{
	private static final long serialVersionUID = 1L;

	public BusThread(Resource resource, long priority)
	{
		super(resource, null, priority, false, 0);
		setName("BusThread-" + getId());
	}

	@Override
	protected void body()
	{
		BUSResource bus = (BUSResource)resource;
		bus.process(this);
	}

	@Override
	protected void handleSignal(Signal sig, Context ctxt, LexLocation location)
	{
		signal = null;

		switch (sig)
		{
			case TERMINATE:
				throw new VDMThreadDeath();

			case SUSPEND:	// Ignore
				break;
				
			default:
				break;
		}
	}
}
