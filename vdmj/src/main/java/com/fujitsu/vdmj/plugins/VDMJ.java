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

package com.fujitsu.vdmj.plugins;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.VDMJMain;

/**
 * The main class for the plugin based VDMJ.
 */
public class VDMJ implements VDMJMain
{
	public static String getMainName()
	{
		return VDMJ_MAIN;
	}

	public static void main(String[] args)
	{
		Settings.mainClass = VDMJ.class;
		
		Lifecycle lifecycle = new Lifecycle(args);
		
		System.exit(lifecycle.run() == ExitStatus.EXIT_OK ? 0 : 1);
	}
}
