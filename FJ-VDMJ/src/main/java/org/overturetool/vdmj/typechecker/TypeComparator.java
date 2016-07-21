/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.typechecker;

import java.util.Vector;

import org.overturetool.vdmj.definitions.Definition;
import org.overturetool.vdmj.definitions.TypeDefinition;
import org.overturetool.vdmj.lex.LexNameList;
import org.overturetool.vdmj.types.BracketType;
import org.overturetool.vdmj.types.ClassType;
import org.overturetool.vdmj.types.FunctionType;
import org.overturetool.vdmj.types.InMapType;
import org.overturetool.vdmj.types.InvariantType;
import org.overturetool.vdmj.types.MapType;
import org.overturetool.vdmj.types.NamedType;
import org.overturetool.vdmj.types.NumericType;
import org.overturetool.vdmj.types.OperationType;
import org.overturetool.vdmj.types.OptionalType;
import org.overturetool.vdmj.types.ParameterType;
import org.overturetool.vdmj.types.ProductType;
import org.overturetool.vdmj.types.RecordType;
import org.overturetool.vdmj.types.Seq1Type;
import org.overturetool.vdmj.types.SeqType;
import org.overturetool.vdmj.types.Set1Type;
import org.overturetool.vdmj.types.SetType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.types.TypeSet;
import org.overturetool.vdmj.types.UndefinedType;
import org.overturetool.vdmj.types.UnionType;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.types.UnresolvedType;
import org.overturetool.vdmj.types.VoidReturnType;
import org.overturetool.vdmj.types.VoidType;


/**
 * A class for static type checking comparisons.
 */

public class TypeComparator
{
	/**
	 * A vector of type pairs that have already been compared. This is to
	 * allow recursive type definitions to be compared without infinite
	 * regress.
	 */

	private static Vector<TypePair> done = new Vector<TypePair>(256);

	/**
	 * A result value for comparison of types. The "Maybe" value is needed so
	 * that the fact that a type's subtypes are being actively compared in
	 * a recursive call can be recorded. For example, if a MySetType contains
	 * references to MySetType in its element's type, the comparison of those
	 * types will see the "Maybe" result of the original call and not recurse.
	 * That will not fail the lower level comparison, but the overall "yes"
	 * will not be recorded until the recursive calls return (assuming there
	 * are no "no" votes of course).
	 */

	private static enum Result { Yes, No, Maybe }

	private static class TypePair
	{
		public Type a;
		public Type b;
		public Result result;

		public TypePair(Type a, Type b)
		{
			this.a = a;
			this.b = b;
			this.result = Result.Maybe;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other instanceof TypePair)
			{
				TypePair to = (TypePair)other;
				return a == to.a && b == to.b;
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			return a.hashCode() + b.hashCode();
		}
	}
	
	/**
	 * The current module name. This is set as the type checker goes from module
	 * to module, and is used to affect the processing of opaque "non-struct"
	 * type exports.
	 */
	
	private static String currentModule = null;

	public static void setCurrentModule(String module)
	{
		currentModule = module;
	}
	
	/**
	 * Test whether the two types are compatible. This means that, at runtime,
	 * it is possible that the two types are the same, or sufficiently similar
	 * that the "from" value can be assigned to the "to" value.
	 *
	 * @param to
	 * @param from
	 * @return True if types "a" and "b" are compatible.
	 */

	public synchronized static boolean compatible(Type to, Type from)
	{
		done.clear();
		return searchCompatible(to, from, false) == Result.Yes;
	}

	public synchronized static boolean compatible(Type to, Type from, boolean paramOnly)
	{
		done.clear();
		return searchCompatible(to, from, paramOnly) == Result.Yes;
	}

	/**
	 * Compare two type lists for placewise compatibility.
	 *
	 * @param to
	 * @param from
	 * @return True if all types compatible.
	 */

	public synchronized static boolean compatible(TypeList to, TypeList from)
	{
		done.clear();
		return allCompatible(to, from, false) == Result.Yes;
	}

	/**
	 * Compare two type lists for placewise compatibility. This is used
	 * to check ordered lists of types such as those in a ProductType or
	 * parameters to a function or operation.
	 *
	 * @param to
	 * @param from
	 * @return Yes or No.
	 */

	private static Result allCompatible(TypeList to, TypeList from, boolean paramOnly)
	{
		if (to.size() != from.size())
		{
			return Result.No;
		}
		else
		{
			for (int i=0; i<to.size(); i++)
			{
				if (searchCompatible(to.get(i), from.get(i), paramOnly) == Result.No)
				{
					return Result.No;
				}
			}
		}

		return Result.Yes;
	}

	/**
	 * Search the {@link #done} vector for an existing comparison of two
	 * types before either returning the previous result, or making a new
	 * comparison and adding that result to the vector.
	 *
	 * @param to
	 * @param from
	 * @return Yes or No.
	 */

	private static Result searchCompatible(Type to, Type from, boolean paramOnly)
	{
		TypePair pair = new TypePair(to, from);
		int i = done.indexOf(pair);

		if (i >= 0)
		{
			return done.get(i).result;		// May be "Maybe".
		}
		else
		{
			done.add(pair);
		}

		// The pair.result is "Maybe" until this call returns.
		pair.result = test(to, from, paramOnly);

		return pair.result;
	}

	/**
	 * The main implementation of the compatibility checker. If "a" and "b" are
	 * the same object the result is "yes"; if either is an {@link UnknownType},
	 * we are dealing with earlier parser errors and so "yes" is returned to
	 * avoid too many errors; if either is a {@link ParameterType} the result is
	 * also "yes" on the grounds that the type cannot be tested at compile time.
	 *
	 * If either type is a {@link BracketType} or a {@link NamedType} the types
	 * are reduced to their underlying type before proceeding; if either is an
	 * {@link OptionalType} and the other is optional also, the result is
	 * "yes", otherwise the underlying type of the optional type is taken before
	 * proceeding; the last two steps are repeated until the types will not
	 * reduce further.
	 *
	 * To compare the reduced types, if "a" is a union type, then all the
	 * component types of "a" are compared to "b" (or b's components, if it too
	 * is a union type) until a match is found; otherwise basic type comparisons
	 * are made, involving any subtypes - for example, if they are both sets,
	 * then the result depends on whether their "set of" subtypes are
	 * compatible, by a recursive call. Similarly with maps and sequences,
	 * function/operation parameter types, and record field types. Lastly, a
	 * simple {@link org.overturetool.vdmj.types.Type#equals} operation is
	 * performed on two basic types to decide the result.
	 *
	 * @param to
	 * @param from
	 * @param paramOnly
	 * @return Yes or No.
	 */

	private static Result test(Type to, Type from, boolean paramOnly)
	{
		if (to instanceof UnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + to, to.location);
		}

		if (from instanceof UnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + from, from.location);
		}

		if (to == from)
		{
			return Result.Yes;	// Same object!
		}

		if (to instanceof UnknownType || from instanceof UnknownType)
		{
			return Result.Yes;	// Hmmm... too many errors otherwise
		}

		if (to instanceof UndefinedType || from instanceof UndefinedType)
		{
			return Result.Yes;	// Not defined "yet"...?
		}

		if (from instanceof ParameterType)
		{
			return Result.Yes;	// Runtime checked... Note "to" checked below
		}


		// Obtain the fundamental type of BracketTypes, NamedTypes and
		// OptionalTypes.

		boolean resolved = false;

		while (!resolved)
		{
    		if (to instanceof BracketType)
    		{
    			to = ((BracketType)to).type;
    			continue;
    		}

    		if (from instanceof BracketType)
    		{
    			from = ((BracketType)from).type;
    			continue;
    		}
    		
    		if (to instanceof InvariantType)
    		{
    			InvariantType ito =(InvariantType)to;
    			
	    		if (to instanceof NamedType &&
	    			(!ito.opaque || ito.location.module.equals(currentModule)))
	    		{
	    			to = ((NamedType)to).type;
	    			continue;
	    		}
    		}

    		if (from instanceof InvariantType)
    		{
    			InvariantType ifrom =(InvariantType)from;
    			
	    		if (from instanceof NamedType &&
	    			(!ifrom.opaque || ifrom.location.module.equals(currentModule)))
	    		{
	    			from = ((NamedType)from).type;
	    			continue;
	    		}
    		}

    		if (to instanceof OptionalType)
    		{
    			if (from instanceof OptionalType)
    			{
    				return Result.Yes;
    			}

    			to = ((OptionalType)to).type;
    			continue;
    		}

    		if (from instanceof OptionalType)
    		{
    			// Can't assign nil to a non-optional type? This should maybe
    			// generate a warning here?

    			if (to instanceof OptionalType)
    			{
    				return Result.Yes;
    			}

    			from = ((OptionalType)from).type;
    			continue;
    		}

    		resolved = true;
		}

		// OK... so we have fully resolved the basic types...

		if (to instanceof UnionType)
		{
			UnionType ua = (UnionType)to;

			for (Type ta: ua.types)
			{
				if (searchCompatible(ta, from, paramOnly) == Result.Yes)
				{
					return Result.Yes;
				}
			}
		}
		else
		{
			if (from instanceof UnionType)
			{
				UnionType ub = (UnionType)from;

				for (Type tb: ub.types)
				{
					if (searchCompatible(to, tb, paramOnly) == Result.Yes)
					{
						return Result.Yes;
					}
				}
			}
			else if (to instanceof NumericType)
			{
				return (from instanceof NumericType) ? Result.Yes : Result.No;
			}
			else if (to instanceof ProductType)
			{
				if (!(from instanceof ProductType))
				{
					return Result.No;
				}

				TypeList ta = ((ProductType)to).types;
				TypeList tb = ((ProductType)from).types;
				return allCompatible(ta, tb, paramOnly);
			}
			else if (to instanceof MapType)
			{
				if (!(from instanceof MapType))
				{
					return Result.No;
				}

				MapType ma = (MapType)to;
				MapType mb = (MapType)from;

				return (ma.empty || mb.empty ||
					(searchCompatible(ma.from, mb.from, paramOnly) == Result.Yes &&
					 searchCompatible(ma.to, mb.to, paramOnly) == Result.Yes)) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof SetType)	// Includes set1
			{
				if (!(from instanceof SetType))
				{
					return Result.No;
				}

				SetType sa = (SetType)to;
				SetType sb = (SetType)from;

				if (to instanceof Set1Type && sb.empty)
				{
					return Result.No;
				}

				return (sa.empty || sb.empty ||
						searchCompatible(sa.setof, sb.setof, paramOnly) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof SeqType)	// Includes seq1
			{
				if (!(from instanceof SeqType))
				{
					return Result.No;
				}

				SeqType sa = (SeqType)to;
				SeqType sb = (SeqType)from;

				if (to instanceof Seq1Type && sb.empty)
				{
					return Result.No;
				}

				return (sa.empty || sb.empty ||
						searchCompatible(sa.seqof, sb.seqof, paramOnly) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof FunctionType)
			{
				if (!(from instanceof FunctionType))
				{
					return Result.No;
				}

				FunctionType fa = (FunctionType)to;
				FunctionType fb = (FunctionType)from;

				return (allCompatible(fa.parameters, fb.parameters, paramOnly) == Result.Yes &&
						(paramOnly ||
						 searchCompatible(fa.result, fb.result, paramOnly) == Result.Yes)) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof OperationType)
			{
				if (!(from instanceof OperationType))
				{
					return Result.No;
				}

				OperationType fa = (OperationType)to;
				OperationType fb = (OperationType)from;

				return (allCompatible(fa.parameters, fb.parameters, paramOnly) == Result.Yes &&
						(paramOnly ||
						 searchCompatible(fa.result, fb.result, paramOnly) == Result.Yes)) ?
							Result.Yes : Result.No;
			}
			else if (to instanceof RecordType)
			{
				if (!(from instanceof RecordType))
				{
					return Result.No;
				}

				RecordType rf = (RecordType)from;
				RecordType rt = (RecordType)to;

				return rf.equals(rt) ? Result.Yes : Result.No;
			}
			else if (to instanceof ClassType)
			{
				if (!(from instanceof ClassType))
				{
					return Result.No;
				}

				ClassType cfrom = (ClassType)from;
				ClassType cto = (ClassType)to;

				// VDMTools doesn't seem to worry about sub/super type
				// assignments. This was "cfrom.equals(cto)".

				if (cfrom.hasSupertype(cto) || cto.hasSupertype(cfrom))
				{
					return Result.Yes;
				}
			}
			else if (from instanceof VoidReturnType)
			{
				if (to instanceof VoidType || to instanceof VoidReturnType)
				{
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (to instanceof VoidReturnType)
			{
				if (from instanceof VoidType || from instanceof VoidReturnType)
				{
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (to instanceof ParameterType)
			{
				// If the from type includes the "to" parameter anywhere, then the types must be identical,
				// otherwise they match. We can only test for that easily with toString() :-(
				// See overture bug #562.
				
				String fstr = from.toString();
				String tstr = to.toString();
				
				if (fstr.indexOf(tstr) >= 0)
				{
					return to.equals(from) ? Result.Yes : Result.No;
				}
				else
				{
					return Result.Yes;
				}
			}
			else
			{
				return to.equals(from) ? Result.Yes : Result.No;
			}
		}

		return Result.No;
	}

	/**
	 * Test whether one type is a subtype of another.
	 *
	 * @param sub
	 * @param sup
	 * @param invignore True if type invariants should be ignored 
	 * @return True if sub is a subtype of sup.
	 */

	public synchronized static boolean isSubType(Type sub, Type sup)
	{
		return isSubType(sub, sup, false);	// By default, invariants fail a match 
	}

	public synchronized static boolean isSubType(Type sub, Type sup, boolean invignore)
	{
		done.clear();
		return searchSubType(sub, sup, invignore) == Result.Yes;
	}

	/**
	 * Compare two type lists for placewise subtype compatibility. This is used
	 * to check ordered lists of types such as those in a ProductType or
	 * parameters to a function or operation.
	 *
	 * @param sub
	 * @param sup
	 * @return Yes or No.
	 */

	private static Result allSubTypes(TypeList sub, TypeList sup, boolean invignore)
	{
		if (sub.size() != sup.size())
		{
			return Result.No;
		}
		else
		{
			for (int i=0; i<sub.size(); i++)
			{
				if (searchSubType(sub.get(i), sup.get(i), invignore) == Result.No)
				{
					return Result.No;
				}
			}
		}

		return Result.Yes;
	}

	/**
	 * Search the {@link #done} vector for an existing subtype comparison of two
	 * types before either returning the previous result, or making a new
	 * comparison and adding that result to the vector.
	 *
	 * @param sub
	 * @param sup
	 * @param invignore 
	 * @return Yes or No, if sub is a subtype of sup.
	 */

	private static Result searchSubType(Type sub, Type sup, boolean invignore)
	{
		TypePair pair = new TypePair(sub, sup);
		int i = done.indexOf(pair);

		if (i >= 0)
		{
			return done.get(i).result;		// May be "Maybe".
		}
		else
		{
			done.add(pair);
		}

		// The pair.result is "Maybe" until this call returns.
		pair.result = subtest(sub, sup, invignore);

		return pair.result;
	}

	/**
	 * The main implementation of the subtype checker. If "a" and "b" are
	 * the same object the result is "yes"; if either is an {@link UnknownType},
	 * we are dealing with earlier parser errors and so "yes" is returned to
	 * avoid too many errors; if either is a {@link ParameterType} the result is
	 * also "yes" on the grounds that the type cannot be tested at compile time.
	 *
	 * If either type is a {@link BracketType} or a {@link NamedType} the types
	 * are reduced to their underlying type before proceeding; if either is an
	 * {@link OptionalType} and the other is optional also, the result is
	 * "yes", otherwise the underlying type of the optional type is taken before
	 * proceeding; the last two steps are repeated until the types will not
	 * reduce further.
	 *
	 * To compare the reduced types, if "a" is a union type, then all the
	 * component types of "a" are compared to "b" (or b's components, if it too
	 * is a union type); otherwise basic type comparisons
	 * are made, involving any subtypes - for example, if they are both sets,
	 * then the result depends on whether their "set of" subtypes are
	 * subtypes, by a recursive call. Similarly with maps and sequences,
	 * function/operation parameter types, and record field types. Lastly, a
	 * simple {@link org.overturetool.vdmj.types.Type#equals} operation is
	 * performed on two basic types to decide the result.
	 *
	 * @param sub
	 * @param sup
	 * @param invariants 
	 * @return Yes or No.
	 */

	private static Result subtest(Type sub, Type sup, boolean invignore)
	{
		if (sub instanceof UnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + sub, sub.location);
		}

		if (sup instanceof UnresolvedType)
		{
			throw new TypeCheckException("Unknown type: " + sup, sup.location);
		}

		if (sub instanceof UnknownType || sup instanceof UnknownType)
		{
			return Result.Yes;	// Hmmm... too many errors otherwise
		}

		if (sub instanceof ParameterType || sup instanceof ParameterType)
		{
			return Result.Yes;	// Runtime checked...
		}

		if (sub instanceof UndefinedType || sup instanceof UndefinedType)
		{
			return Result.Yes;	// Usually uninitialized variables etc.
		}

		// Obtain the fundamental type of BracketTypes, NamedTypes and
		// OptionalTypes.

		boolean resolved = false;

		while (!resolved)
		{
    		if (sub instanceof BracketType)
    		{
    			sub = ((BracketType)sub).type;
    			continue;
    		}

    		if (sup instanceof BracketType)
    		{
    			sup = ((BracketType)sup).type;
    			continue;
    		}

    		// NOTE: not testing opaque InvariantTypes here.
    		
    		if (sub instanceof NamedType)
    		{
    			NamedType nt = (NamedType)sub;

       			if (nt.invdef == null || invignore)
       			{
        			sub = nt.type;
        			continue;
        		}
    		}

    		if (sup instanceof NamedType)
    		{
    			NamedType nt = (NamedType)sup;

       			if (nt.invdef == null || invignore)
       			{
        			sup = nt.type;
        			continue;
        		}
    		}

    		if (sub instanceof OptionalType && sup instanceof OptionalType)
    		{
       			sub = ((OptionalType)sub).type;
       			sup = ((OptionalType)sup).type;
    			continue;
    		}

    		resolved = true;
		}

		if (sub instanceof UnknownType || sup instanceof UnknownType)
		{
			return Result.Yes;		// Hmmm... too many errors otherwise
		}

		if (sub == sup)
		{
			return Result.Yes;		// Same object!
		}

		// OK... so we have fully resolved the basic types...

		if (sub instanceof UnionType)
		{
			UnionType subu = (UnionType)sub;

			for (Type suba: subu.types)
			{
				if (searchSubType(suba, sup, invignore) == Result.No)
				{
					return Result.No;
				}
			}

			return Result.Yes;	// Must be all of them
		}
		else
		{
			if (sup instanceof UnionType)
			{
				UnionType supu = (UnionType)sup;

				for (Type supt: supu.types)
				{
					if (searchSubType(sub, supt, invignore) == Result.Yes)
					{
						return Result.Yes;	// Can be any of them
					}
				}

				return Result.No;
			}
			else if (sub instanceof NamedType)
    		{
				NamedType subn = (NamedType)sub;
				return searchSubType(subn.type, sup, invignore);
			}
			else if (sup instanceof OptionalType)
    		{
				// Supertype includes a nil value, and the subtype is not
				// optional (stripped above), so we test the optional's type.

				OptionalType op = (OptionalType)sup;
				return searchSubType(sub, op.type, invignore);
			}
			else if (sub instanceof NumericType)
			{
				if (sup instanceof NumericType)
				{
					NumericType subn = (NumericType)sub;
					NumericType supn = (NumericType)sup;

					return (subn.getWeight() <= supn.getWeight()) ?
						Result.Yes : Result.No;
				}
			}
			else if (sub instanceof ProductType)
			{
				if (!(sup instanceof ProductType))
				{
					return Result.No;
				}

				TypeList subl = ((ProductType)sub).types;
				TypeList supl = ((ProductType)sup).types;

				return allSubTypes(subl, supl, invignore);
			}
			else if (sub instanceof MapType)
			{
				if (!(sup instanceof MapType))
				{
					return Result.No;
				}

				MapType subm = (MapType)sub;
				MapType supm = (MapType)sup;

				if (subm.empty || supm.empty)
				{
					return Result.Yes;
				}

				if (searchSubType(subm.from, supm.from, invignore) == Result.Yes &&
					searchSubType(subm.to, supm.to, invignore) == Result.Yes)
				{
					if (!(sub instanceof InMapType) &&
						 (sup instanceof InMapType))
					{
						return Result.No;
					}
					
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (sub instanceof SetType)	// Includes set1
			{
				if (!(sup instanceof SetType))
				{
					return Result.No;
				}

				SetType subs = (SetType)sub;
				SetType sups = (SetType)sup;

				if ((subs.empty && !(sup instanceof Set1Type)) || sups.empty)
				{
					return Result.Yes;
				}

				if (searchSubType(subs.setof, sups.setof, invignore) == Result.Yes)
				{
					if (!(sub instanceof Set1Type) &&
						 (sup instanceof Set1Type))
					{
						return Result.No;
					}
					
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (sub instanceof SeqType)	// Includes seq1
			{
				if (!(sup instanceof SeqType))
				{
					return Result.No;
				}

				SeqType subs = (SeqType)sub;
				SeqType sups = (SeqType)sup;

				if ((subs.empty && !(sup instanceof Seq1Type)) || sups.empty)
				{
					return Result.Yes;
				}

				if (searchSubType(subs.seqof, sups.seqof, invignore) == Result.Yes)
				{
					if (!(sub instanceof Seq1Type) &&
						 (sup instanceof Seq1Type))
					{
						return Result.No;
					}
					
					return Result.Yes;
				}
				else
				{
					return Result.No;
				}
			}
			else if (sub instanceof FunctionType)
			{
				if (!(sup instanceof FunctionType))
				{
					return Result.No;
				}

				FunctionType subf = (FunctionType)sub;
				FunctionType supf = (FunctionType)sup;

				return (allSubTypes(subf.parameters, supf.parameters, invignore) == Result.Yes &&
						searchSubType(subf.result, supf.result, invignore) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (sub instanceof OperationType)
			{
				if (!(sup instanceof OperationType))
				{
					return Result.No;
				}

				OperationType subo = (OperationType)sub;
				OperationType supo = (OperationType)sup;

				return (allSubTypes(subo.parameters, supo.parameters, invignore) == Result.Yes &&
						searchSubType(subo.result, supo.result, invignore) == Result.Yes) ?
							Result.Yes : Result.No;
			}
			else if (sub instanceof RecordType)
			{
				if (!(sup instanceof RecordType))
				{
					return Result.No;
				}

				RecordType subr = (RecordType)sub;
				RecordType supr = (RecordType)sup;

				return subr.equals(supr) ? Result.Yes : Result.No;
			}
			else if (sub instanceof ClassType)
			{
				if (!(sup instanceof ClassType))
				{
					return Result.No;
				}

				ClassType supc = (ClassType)sup;
				ClassType subc = (ClassType)sub;

				if (subc.hasSupertype(supc))
				{
					return Result.Yes;
				}
			}
			else
			{
				return sub.equals(sup) ? Result.Yes : Result.No;
			}
		}

		return Result.No;
	}
	
	/**
	 * Check that the compose types that are referred to in a type have a matching
	 * definition in the environment. The method returns a list of types that do not
	 * exist if the newTypes parameter is passed. 
	 */
	public static TypeList checkComposeTypes(Type type, Environment env, boolean newTypes)
	{
		TypeList undefined = new TypeList();
		
		for (Type compose: type.getComposeTypes())
		{
			RecordType composeType = (RecordType)compose;
			Definition existing = env.findType(composeType.name, null);
			
			if (existing != null)
			{
				// If the type is already defined, check that it has the same shape and
				// does not have an invariant (which cannot match a compose definition).
				boolean matches = false;
				
				if (existing instanceof TypeDefinition)
				{
					TypeDefinition edef = (TypeDefinition)existing;
					Type etype = existing.getType();
					
					if (edef.invExpression == null && etype instanceof RecordType)
					{
						RecordType retype = (RecordType)etype;
						
						if (retype.fields.equals(composeType.fields))
						{
							matches = true;
						}
					}
				}
					
				if (!matches)
				{
					TypeChecker.report(3325, "Mismatched compose definitions for " + composeType.name, composeType.location);
					TypeChecker.detail2(composeType.name.name, composeType.location, existing.name.name, existing.location);
				}
			}
			else
			{
				if (newTypes)
				{
					undefined.add(composeType);
				}
				else
				{
					TypeChecker.report(3113, "Unknown type name '" + composeType.name + "'", composeType.location);
				}
			}
		}
		
		// Lastly, check that the compose types extracted are compatible
		LexNameList done = new LexNameList();
		
		for (Type c1: undefined)
		{
			for (Type c2: undefined)
			{
				if (c1 != c2)
				{
					RecordType r1 = (RecordType)c1;
					RecordType r2 = (RecordType)c2;
					
					if (r1.name.equals(r2.name) && !done.contains(r1.name) && !r1.fields.equals(r2.fields))
					{
						TypeChecker.report(3325, "Mismatched compose definitions for " + r1.name, r1.location);
						TypeChecker.detail2(r1.name.name, r1.location, r2.name.name, r2.location);
						done.add(r1.name);
					}
				}
			}
		}
		
		return undefined;
	}
	
	/**
	 * Calculate the intersection of two types.
	 */
	public static Type intersect(Type a, Type b)
	{
		TypeSet tsa = new TypeSet();
		TypeSet tsb = new TypeSet();

		// Obtain the fundamental type of BracketTypes, NamedTypes and OptionalTypes.
		boolean resolved = false;

		while (!resolved)
		{
    		if (a instanceof BracketType)
    		{
    			a = ((BracketType)a).type;
    			continue;
    		}

    		if (b instanceof BracketType)
    		{
    			b = ((BracketType)b).type;
    			continue;
    		}

    		if (a instanceof NamedType)
    		{
    			NamedType nt = (NamedType)a;

       			if (nt.invdef == null)
       			{
        			a = nt.type;
        			continue;
        		}
    		}

    		if (b instanceof NamedType)
    		{
    			NamedType nt = (NamedType)b;

       			if (nt.invdef == null)
       			{
        			b = nt.type;
        			continue;
        		}
    		}

    		if (a instanceof OptionalType && b instanceof OptionalType)
    		{
       			a = ((OptionalType)a).type;
       			b = ((OptionalType)b).type;
    			continue;
    		}

    		resolved = true;
		}

		if (a instanceof UnionType)
		{
			UnionType uta = (UnionType)a;
			tsa.addAll(uta.types);
		}
		else
		{
			tsa.add(a);
		}
		
		if (b instanceof UnionType)
		{
			UnionType utb = (UnionType)b;
			tsb.addAll(utb.types);
		}
		else
		{
			tsb.add(b);
		}
		
		// Keep largest types which are compatible (eg. nat and int choses int)
		TypeSet result = new TypeSet();
		
		for (Type atype: tsa)
		{
			for (Type btype: tsb)
			{
				if (isSubType(atype, btype))
				{
					result.add(btype);
				}
				else if (isSubType(btype, atype))
				{
					result.add(atype);
				}
			}
		}
		
		if (result.isEmpty())
		{
			return null;
		}
		else
		{
			return result.getType(a.location);
		}
	}
}
