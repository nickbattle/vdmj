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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.plugins.analyses;

import java.util.Collection;
import java.util.List;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckTypeEvent;

/**
 * TC analysis plugin
 */
abstract public class TCPlugin extends AnalysisPlugin implements EventListener
{
	@Override
	public String getName()
	{
		return "TC";
	}
	
	@Override
	public int getPriority()
	{
		return TC_PRIORITY;
	}

	@Override
	public void init()
	{
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckTypeEvent.class, this);
	}

	public static TCPlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new TCPluginSL();
				
			case VDM_PP:
				return new TCPluginPP();
				
			case VDM_RT:
				return new TCPluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}

	@Override
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			return typeCheckPrepare();
		}
		else if (event instanceof CheckTypeEvent)
		{
			return typeCheck();
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}

	abstract protected List<VDMMessage> typeCheckPrepare();

	abstract protected List<VDMMessage> typeCheck();

	abstract public <T extends Collection<?>> T getTC();
}
