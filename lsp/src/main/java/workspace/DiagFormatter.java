/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A Formatter specialization to define the format diagnostics.
 */
public class DiagFormatter extends Formatter
{
	/**
     * Format the given log record. The format includes the time, the level, and
     * the message, terminated by a newline.
     * 
     * @param rec
     *            The diag record for format.
     * 
     * @return The formatted diag string.
     */
	@Override
	public String format(LogRecord rec)
	{
		Calendar now = new GregorianCalendar();
		
		String time = String.format("%02d:%02d:%02d.%03d",
				now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
				now.get(Calendar.SECOND), now.get(Calendar.MILLISECOND));

		return time +
			": [" + rec.getLevel().getName() + "]" +
			" [" + rec.getThreadID() + "] " +
			rec.getMessage() + "\n";
	}
}
