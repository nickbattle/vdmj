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

package com.fujitsu.vdmj;

/**
 * An interface, implemented by all Java "main" providers, to help identify the
 * environment currently running. See Settings.mainClass.
 */
public interface VDMJMain
{
	public static final String VDMJ_NAME = "VDMJ";
	public static final String LSP_NAME = "LSP";
	public static final String DBGP_NAME = "DBGP";
	
	/**
	 * This method should be implemented in every VDMJMain, returning a useful
	 * identifier. The constants above are the ones we know about.
	 */
	public static String getMainName()
	{
		return "undefined";
	}
}
