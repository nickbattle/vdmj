/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package rpc;

public enum RPCErrors
{
	// Defined by JSON RPC
	ParseError(-32700),
	InvalidRequest(-32600),
	MethodNotFound(-32601),
	InvalidParams(-32602),
	InternalError(-32603),
	serverErrorStart(-32099),
	serverErrorEnd(-32000),
	ServerNotInitialized(-32002),
	UnknownErrorCode(-32001),
	
	// LSP extensions?
	RequestCancelled(-32800),
	ContentModified(-32801);

	private final Long value;

	RPCErrors(long value)
	{
		this.value = value;
	}
	
	public long getValue()
	{
		return value;
	}
}
