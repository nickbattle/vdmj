/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.plugins;

import java.util.List;

import com.fujitsu.vdmj.messages.VDMMessage;

/**
 * An interface implemented by subscribers to the EventHub. 
 */
public interface EventListener
{
	public final static int AST_PRIORITY = Integer.getInteger("vdmj.plugin.priority.ast", 100);
	public final static int TC_PRIORITY  = Integer.getInteger("vdmj.plugin.priority.tc", 200);
	public final static int IN_PRIORITY  = Integer.getInteger("vdmj.plugin.priority.in", 300);
	public final static int PO_PRIORITY  = Integer.getInteger("vdmj.plugin.priority.po", 400);
	public final static int CMD_PRIORITY = Integer.getInteger("vdmj.plugin.priority.cmd", 500);

	public final static int USER_PRIORITY = Integer.getInteger("vdmj.plugin.priority.user", 1000);

	public String getName();
	public int getPriority();
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception;
}
