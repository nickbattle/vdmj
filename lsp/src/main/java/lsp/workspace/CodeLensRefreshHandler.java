/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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
package lsp.workspace;

import lsp.LSPHandler;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;

/**
 * A handler for the "workspace/codeLens/refresh" method response, which is ignored.
 */
public class CodeLensRefreshHandler extends LSPHandler
{
	@Override
	public RPCMessageList request(RPCRequest request)
	{
		return new RPCMessageList(request, RPCErrors.InternalError);
	}
}
