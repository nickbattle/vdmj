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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.values;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.values.visitors.ExplicitValueVisitor;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

/**
 * The parent of all runtime values.
 */
abstract public class Value implements Comparable<Value>, Serializable, Formattable, Cloneable
{
	private static final long serialVersionUID = 1L;

	@Override
	abstract public String toString();

	// This is overridden in the few classes that need to change formatting
	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision)
	{
		formatTo(this.toString(), formatter, flags, width, precision);
	}

	protected void formatTo(String value, Formatter formatter, int flags, int width, int precision)
	{
		StringBuilder sb = new StringBuilder("%");

		if ((flags & FormattableFlags.LEFT_JUSTIFY) > 0)
		{
			sb.append('-');
		}
		
		if ((flags & FormattableFlags.ALTERNATE) > 0)
		{
			// Alternates should be dealt with in subclasses. Ignored otherwise.
			// sb.append('#');
		}

		if (width > 0)
		{
			sb.append(width);
		}

		if (precision > 0)
		{
			sb.append('.');
			sb.append(precision);
		}

		sb.append('s');

		formatter.format(sb.toString(), value);
	}

	@Override
	abstract public boolean equals(Object other);

	@Override
	public int compareTo(Value other)
	{
		return toString().compareTo(other.toString());	// Arbitrary order
	}

	@Override
	abstract public int hashCode();

	/** A string with the informal kind of the value, like "set". */
	abstract public String kind();

	@Override
	abstract public Object clone();

	public Value deepCopy()
	{
		return (Value)clone();
	}

	public Value shallowCopy()
	{
		return (Value)clone();
	}

	public String toShortString(int max)
	{
		String value = toString();

		if (value.length() > max)
		{
			value = value.substring(0, max/2) +
				"..." + value.substring(value.length() - max/2);
		}

		return value;
	}
	
	public String toExplicitString(LexLocation from)
	{
		return this.apply(new ExplicitValueVisitor(), from);
	}

	/**
	 * Performing a dynamic type conversion. This method is usually specialized
	 * by subclasses that know how to convert themselves to other types. If they
	 * fail, they delegate the conversion up to this superclass method which
	 * deals with the special cases: unions, type parameters, optional types,
	 * bracketed types and named types. If all these also fail, the method throws
	 * a runtime dynamic type check exception - though that may be caught, for
	 * example by the union processing, as it iterates through the types in the
	 * union given, trying to convert the value.
	 *
	 * @param to The target type.
	 * @param ctxt The context in which to make the conversion.
	 * @return This value converted to the target type.
	 *
	 * @throws ValueException Cannot perform the type conversion.
	 */

	public Value convertTo(TCType to, Context ctxt) throws ValueException
	{
		if (Settings.dynamictypechecks)
		{
			// In VDM++ and VDM-RT, we do not want to do thread swaps half way
			// through a DTC check (which can include calculating an invariant),
			// so we set the atomic flag around the conversion. This also stops
			// VDM-RT from performing "time step" calculations.

			try
			{
				ctxt.threadState.setAtomic(true);
				return convertValueTo(to, ctxt);
			}
			finally
			{
				ctxt.threadState.setAtomic(false);
			}
		}
		else
		{
			return this;	// Good luck!!
		}
	}

	public Value convertValueTo(TCType to, Context ctxt) throws ValueException
	{
		return convertValueTo(to, ctxt, new TCTypeSet());
	}

	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to instanceof TCUnionType)
		{
			TCUnionType uto = (TCUnionType)to;
			Value matched = null;

			for (TCType ut: uto.types)
			{
				if (!done.contains(ut))
				{
    				try
    				{
    					if (ut instanceof TCNamedType)	// These can "recurse"
    					{
    						done.add(ut);
    					}
    					
    					matched = convertValueTo(ut, ctxt, done);
    					
    					if (matched.equals(this))
    					{
    						return matched;		// Immediate return for perfect match
    					}
    				}
    				catch (ValueException e)
    				{
    					// Union type not applicable
    				}
    				catch (ContextException e)
    				{
    					// Pre/post/invariant problems
    				}
				}
			}
			
			if (matched != null)
			{
				return matched;		// Last non-perfect match
			}
		}
		else if (to instanceof TCParameterType)
		{
			TCParameterType pt = (TCParameterType)to;

			// Parameter types are ParameterValues of the given name in
			// the context. They exist in the context, if the function has
			// been instantiated with type parameters (see FunctionValue).

			Value v = ctxt.check(pt.name);

			if (v == null)
			{
				abort(4147, "Polymorphic function missing @" + pt.name, ctxt);
			}
			else if (v instanceof ParameterValue)
			{
				ParameterValue pv = (ParameterValue)v;
				return convertValueTo(pv.type, ctxt, done);
			}

			abort(4086, "Value of type parameter is not a type", ctxt);
		}
		else if (to instanceof TCOptionalType)
		{
			TCOptionalType ot = (TCOptionalType)to;
			return convertValueTo(ot.type, ctxt, done);
		}
		else if (to instanceof TCBracketType)
		{
			TCBracketType bt = (TCBracketType)to;
			return convertValueTo(bt.type, ctxt, done);
		}
		else if (to instanceof TCNamedType)
		{
			TCNamedType ntype = (TCNamedType)to;
			Value converted = convertValueTo(ntype.type, ctxt, done);
			return new InvariantValue(ntype, converted, ctxt);
		}
		else if (to instanceof TCUnknownType)
		{
			return this;	// Suppressing DTC for "?" types
		}

		abort(4087, "Cannot convert " + toShortString(100) + " (" + kind() + ") to " + to, ctxt);
		return null;
	}

	/**
	 * Change the object's value. Normally, values are immutable, but subclasses
	 * of {@link UpdatableValue} implement this set method to replace the object
	 * referenced with another. ReferenceValues like UpdatableValue delegate all
	 * the other Value method to the contained object.
	 *
	 * @param location Unused.
	 * @param newval The new value to set
	 * @param ctxt The context used
	 * @throws ValueException
	 */

	public void set(LexLocation location, Value newval, Context ctxt) throws ValueException
	{
		abort(4088, "Set not permitted for " + kind(), ctxt);
	}

	public Value abort(int number, String msg, Context ctxt) throws ValueException
	{
		if (ctxt == null) ctxt = Context.javaContext();	// javaContext(0), abort(1), xxxValue(null)(2), caller(3)
		throw new ValueException(number, msg, ctxt);
	}

	public Value abort(int number, Exception e, Context ctxt) throws ValueException
	{
		throw new ValueException(number, e.getMessage(), ctxt);
	}
	
	public boolean isUndefined()
	{
		return false;
	}

	public boolean isVoid()
	{
		return false;
	}

	public boolean isNumeric()
	{
		return this instanceof NumericValue;
	}

	public boolean isOrdered()
	{
		return isNumeric();
	}

	public boolean isType(Class<? extends Value> valueclass)
	{
		return valueclass.isInstance(this);
	}

	/**
	 * Find the most primitive underlying value contained. This is typically
	 * used to check whether a value is "really" (say) a MapValue once the
	 * ReferenceValue (Updatablevalue) wrappers have been removed.
	 *
	 * Note that this strips InvariantTypes of their invariance!
	 *
	 * @return The primitive value
	 */

	public Value deref()
	{
		return this;
	}

	/**
	 * Return an UpdatableValue, wrapping this one. This is a deep translation
	 * that recurses into all Values that contain other Values (sets etc),
	 * converting their contents to UpdateableValues. The results can then be
	 * modified in assignment statements or used as state data etc.
	 *
	 * @param listeners The listener to inform of updates to the value.
	 * @return An UpdatableValue for this one.
	 */
	public UpdatableValue getUpdatable(ValueListenerList listeners)
	{
		return UpdatableValue.factory(this, listeners);
	}

	/**
	 * Return a simple Value, removing any Updatable/TransactionValue wrappers.
	 * This is the opposite of getUpdatable(), though it works for all values.
	 *
	 * Note that this will preserve InvariantValues' integrity (unlike deref).
	 *
	 * @return A simple value
	 */

	public Value getConstant()
	{
		return this;
	}

	public BigDecimal realValue(Context ctxt) throws ValueException
	{
		abort(4089, "Can't get real value of " + kind(), ctxt);
		return BigDecimal.ZERO;
	}

	public BigDecimal ratValue(Context ctxt) throws ValueException
	{
		abort(4090, "Can't get rat value of " + kind(), ctxt);
		return BigDecimal.ZERO;
	}

	public BigInteger intValue(Context ctxt) throws ValueException
	{
		abort(4091, "Can't get int value of " + kind(), ctxt);
		return BigInteger.ZERO;
	}

	public BigInteger natValue(Context ctxt) throws ValueException
	{
		abort(4092, "Can't get nat value of " + kind(), ctxt);
		return BigInteger.ZERO;
	}

	public BigInteger nat1Value(Context ctxt) throws ValueException
	{
		abort(4093, "Can't get nat1 value of " + kind(), ctxt);
		return BigInteger.ZERO;
	}

	public boolean boolValue(Context ctxt) throws ValueException
	{
		abort(4094, "Can't get bool value of " + kind(), ctxt);
		return false;
	}

	public char charValue(Context ctxt) throws ValueException
	{
		abort(4095, "Can't get char value of " + kind(), ctxt);
		return 0;
	}

	public ValueList tupleValue(Context ctxt) throws ValueException
	{
		abort(4096, "Can't get tuple value of " + kind(), ctxt);
		return null;
	}

	public RecordValue recordValue(Context ctxt) throws ValueException
	{
		abort(4097, "Can't get record value of " + kind(), ctxt);
		return null;
	}

	public String quoteValue(Context ctxt) throws ValueException
	{
		abort(4098, "Can't get quote value of " + kind(), ctxt);
		return null;
	}

	public ValueList seqValue(Context ctxt) throws ValueException
	{
		abort(4099, "Can't get sequence value of " + kind(), ctxt);
		return null;
	}

	public ValueSet setValue(Context ctxt) throws ValueException
	{
		abort(4100, "Can't get set value of " + kind(), ctxt);
		return null;
	}

	public String stringValue(Context ctxt) throws ValueException
	{
		abort(4101, "Can't get string value of " + kind(), ctxt);
		return null;
	}

	public ValueMap mapValue(Context ctxt) throws ValueException
	{
		abort(4102, "Can't get map value of " + kind(), ctxt);
		return null;
	}

	public FunctionValue functionValue(Context ctxt) throws ValueException
	{
		abort(4103, "Can't get function value of " + kind(), ctxt);
		return null;
	}

	public OperationValue operationValue(Context ctxt) throws ValueException
	{
		abort(4104, "Can't get operation value of " + kind(), ctxt);
		return null;
	}

	public ObjectValue objectValue(Context ctxt) throws ValueException
	{
		abort(4105, "Can't get object value of " + kind(), ctxt);
		return null;
	}

	abstract public <R, S> R apply(ValueVisitor<R, S> visitor, S arg);
}
