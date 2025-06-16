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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.scheduler;

import com.fujitsu.vdmj.values.BUSValue;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.ValueList;

public class MessageRequest extends MessagePacket
{
	private static final long serialVersionUID = 1L;
	public final boolean breakAtStart;
	public final ValueList args;
	public final Holder<MessageResponse> replyTo;

	public MessageRequest(BUSValue bus, CPUValue from, CPUValue to,
		ObjectValue target,	OperationValue operation,
		ValueList args, Holder<MessageResponse> replyTo, boolean breakAtStart)
	{
		super(bus, from, to, target, operation);

		this.breakAtStart = breakAtStart;
		this.args = args;
		this.replyTo = replyTo;
	}

	public int getSize()
	{
		return args.toString().length();
	}
}
