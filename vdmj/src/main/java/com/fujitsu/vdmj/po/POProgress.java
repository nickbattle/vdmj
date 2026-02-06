/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package com.fujitsu.vdmj.po;

/**
 * An interface to assist with asynchronous progress reporting for POG. This is implemented
 * by POModuleList and POClassList.
 */
public interface POProgress
{
	/**
	 * Start a new progress run.
	 */
	public void resetProgress();

	/**
	 * The maximum value that the progress can have. The minimum is assumed to be zero.
	 */
	public int getTotal();

	/**
	 * Get the current value of the progress. This is 0 &lt;= x &lt;= getTotal().
	 */
	public int getProgress();

	/**
	 * Advance the progress towards the total by "n".
	 */
	public void makeProgress(int n);

	/**
	 * Indicate that the task being progressed should terminate.
	 */
	public void cancelProgress();

	/**
	 * Test whether a cancel request has been received.
	 */
	public boolean cancelRequested();
}
