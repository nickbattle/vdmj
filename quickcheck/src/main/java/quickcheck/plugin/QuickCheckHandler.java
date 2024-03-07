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
package quickcheck.plugin;

import java.io.IOException;

import dap.DAPHandler;
import dap.DAPMessageList;
import dap.DAPRequest;
import lsp.LSPHandler;
import rpc.RPCHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import workspace.PluginRegistry;

/**
 * LSP Handler for QC DAP commands
 */
public class QuickCheckHandler extends LSPHandler
{
	@Override
	public RPCMessageList request(RPCRequest request)
	{
		try
		{
			QuickCheckLSPPlugin qc = PluginRegistry.getInstance().getPlugin("QC");
			return qc.quickCheck(request);
		}
		catch (Exception e)
		{
			return new RPCMessageList(request, e);
		}
	}
}
