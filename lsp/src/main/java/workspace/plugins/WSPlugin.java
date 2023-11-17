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

package workspace.plugins;

import com.fujitsu.vdmj.lex.Dialect;

import workspace.EventListener;

/**
 * The workspace plugin. This may grow in use, if it makes sense
 * to move the workspace manager tasks here, driven by LSP events.
 */
public class WSPlugin extends AnalysisPlugin implements EventListener
{
	public static AnalysisPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			default:
				return new WSPlugin();
		}
	}
	
	@Override
	public int getPriority()
	{
		return EventListener.WS_PRIORITY;
	}

	@Override
	public String getName()
	{
		return "WS";
	}

	@Override
	public void init()
	{
		// No need to handle any events yet
	}
}
