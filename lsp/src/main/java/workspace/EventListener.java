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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace;

import dap.DAPMessageList;
import rpc.RPCMessageList;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;

/**
 * An interface implemented by subscribers to the EventHub. 
 */
public interface EventListener
{
	public final static int AST_PRIORITY = Integer.getInteger("lspx.plugins.priority.ast", 100);
	public final static int TC_PRIORITY  = Integer.getInteger("lspx.plugins.priority.tc", 200);
	public final static int IN_PRIORITY  = Integer.getInteger("lspx.plugins.priority.in", 300);
	public final static int PO_PRIORITY  = Integer.getInteger("lspx.plugins.priority.po", 400);
	public final static int CT_PRIORITY  = Integer.getInteger("lspx.plugins.priority.co", 500);

	public final static int USER_PRIORITY = Integer.getInteger("lspx.plugins.priority.user", 1000);
	
	public String getName();
	public int getPriority();
	public RPCMessageList handleEvent(LSPEvent event) throws Exception;
	public DAPMessageList handleEvent(DAPEvent event) throws Exception;
}
