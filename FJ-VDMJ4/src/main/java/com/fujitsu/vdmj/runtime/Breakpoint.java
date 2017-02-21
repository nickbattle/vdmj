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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.runtime;

import java.io.Serializable;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBreakpointExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;

/**
 * The root of the breakpoint class hierarchy.
 */
public class Breakpoint implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The location of the breakpoint. */
	public final LexLocation location;
	/** The number of the breakpoint. */
	public final int number;
	/** The condition or trace expression, in executable form. */
	public final INExpression condition;
	/** The condition or trace expression, in raw form. */
	public final String trace;

	/** The number of times this breakpoint has been reached. */
	public long hits = 0;

	public Breakpoint(LexLocation location)
	{
		this.location = location;
		this.number = 0;
		this.trace = null;
		this.condition = null;
	}

	/**
	 * Create a breakpoint at the given location. The trace string is
	 * parsed to an ASTExpression structure for subsequent evaluation.
	 *
	 * @param location The location of the breakpoint.
	 * @param number The number that appears in the "list" command.
	 * @param trace Any condition or trace expression.
	 * @throws Exception 
	 */
	public Breakpoint(LexLocation location, int number, String trace)
		throws Exception
	{
		this.location = location;
		this.number = number;
		this.trace = trace;

		if (trace != null)
		{
			LexTokenReader ltr = new LexTokenReader(trace, Settings.dialect);

			ltr.push();
			LexToken tok = ltr.nextToken();

			switch (tok.type)
			{
				case EQUALS:
					condition = readHitCondition(ltr, BreakpointCondition.EQ);
					break;

				case GT:
					condition = readHitCondition(ltr, BreakpointCondition.GT);
					break;

				case GE:
					condition = readHitCondition(ltr, BreakpointCondition.GE);
					break;

				case MOD:
					condition = readHitCondition(ltr, BreakpointCondition.MOD);
					break;

				default:
					ltr.pop();
					ExpressionReader reader = new ExpressionReader(ltr);
        			reader.setCurrentModule(location.module);
        			ASTExpression ast = reader.readExpression();
        			TCExpression tc = ClassMapper.getInstance(TCNode.MAPPINGS).convert(ast);
        			condition = ClassMapper.getInstance(INNode.MAPPINGS).convert(tc);
        			break;
			}
		}
		else
		{
			condition = null;
		}
	}

	private INExpression readHitCondition(LexTokenReader ltr, BreakpointCondition cond)
		throws ParserException, LexException
	{
		LexToken arg = ltr.nextToken();

		if (arg.isNot(Token.NUMBER))
		{
			throw new ParserException(2279, "Invalid breakpoint hit condition", location, 0);
		}

		LexIntegerToken num = (LexIntegerToken)arg;
		return new INBreakpointExpression(this, cond, num.value.longValue());
	}

	@Override
	public String toString()
	{
		return location.toString();
	}

	public String stoppedAtString()
	{
		return "Stopped [" + Thread.currentThread().getName() + "] " + location;
	}

	public void clearHits()
	{
		hits = 0;
	}

	/**
	 * Check whether to stop. The implementation in Breakpoint is used to check
	 * for the "step" and "next" commands, using the stepline, nextctxt and
	 * outctxt fields. If the current line is different to the last step line,
	 * and the current context is not "above" the next context or the current
	 * context equals the out context or neither the next or out context are
	 * set, a {@link Stoppoint} is created and its check method is called -
	 * which starts a DebuggerReader session.
	 *
	 * @param execl The execution location.
	 * @param ctxt The execution context.
	 */
	public void check(LexLocation execl, Context ctxt)
	{
		location.hit();
		hits++;

		ThreadState state = ctxt.threadState;

		if (Settings.dialect != Dialect.VDM_SL)
		{
			state.reschedule(ctxt, execl);
		}

		if (state.stepline != null)
		{
			if (execl.startLine != state.stepline.startLine)	// NB just line, not pos
			{
				if ((state.nextctxt == null && state.outctxt == null) ||
					(state.nextctxt != null && !isAboveNext(ctxt.getRoot())) ||
					(state.outctxt != null && isOutOrBelow(ctxt)))
				{
        			try
        			{
        				new Stoppoint(location, 0, null).check(location, ctxt);
        			}
        			catch (DebuggerException e)
        			{
        				throw e;
        			}
        			catch (Exception e)
        			{
        				// This happens when the Stoppoint throws an error, which
        				// can't happen. But we need a catch clause for it anyway.

        				throw new DebuggerException(
        					"Breakpoint [" + number + "]: " + e.getMessage());
        			}
				}
			}
		}
	}

	/**
	 * True, if the context passed is above nextctxt. That means that the
	 * current context must have an "outer" chain that reaches nextctxt.
	 *
	 * @param current The context to test.
	 * @return True if the current context is above nextctxt.
	 */
	private boolean isAboveNext(Context current)
	{
		Context c = current.outer;

		while (c != null)
		{
			if (c == current.threadState.nextctxt) return true;
			c = c.outer;
		}

		return false;
	}


	/**
	 * True, if the context passed is equal to or below outctxt. That means that
	 * outctxt must have an "outer" chain that reaches current context.
	 *
	 * @param current The context to test.
	 * @return True if the current context is at or below outctxt.
	 */
	private boolean isOutOrBelow(Context current)
	{
		Context c = current.threadState.outctxt;

		while (c != null)
		{
			if (c == current) return true;
			c = c.outer;
		}

		return false;
	}

	protected void print(String line)
	{
		Console.out.print(line);
		Console.out.flush();
	}

	protected void println(String line)
	{
		Console.out.println(line);
	}
}
