# QuickCheck

This project contains a simple example of VDMJ plugin to quickly check proof obligations.

The plugin registers uses a file of "ranges" to specify the values to give to type binds in a proof obligation.
It then evaluates the PO, which would normally not be executable because of the type binds, using the range
values for each bind. For example, the "test.vdm" spec in the project folder is:

```
	values
		MAX_T = 10;
		
	types
		T = nat
		inv t == t < MAX_T;
		
	functions
		f: T -> T
		f(a) ==
			if a = 0
			then 1
			else a * f(a-1)
		measure a; 
```

That creates seven POs:

```
	Proof Obligation 1: (Unproved)
	T: total function obligation in 'DEFAULT' (test.vdm) at line 3:16
	(forall t:nat &
	  is_(inv_T(t), bool))
	
	...
	
	Proof Obligation 7: (Unproved)
	f: subtype obligation in 'DEFAULT' (test.vdm) at line 6:5
	(forall a:T &
	  inv_T((if (a = 0) then 1 else (a * f((a - 1))))) and (is_nat((if (a = 0) then 1 else (a * f((a - 1)))))))
```

A "ranges" file consists of lines of the form `<bind:T> = <VDM set-of-T expression>`. These set values are used
to substitute the type binds in the PO, using the name of the bind to match (so one PO could involve multiple
ranges lines). The set expressions are evaluated in the global environment of the spec, so they can use types
and constants, functions etc, which may help. They can be set enumerations, comprehensions, functions returning
sets and so on. Trailing comments are allowed too.

```
	t:nat = {0, ..., 100}    -- A modest set of nats
	a:T = {0, ..., MAX_T-1}  -- The whole type of T
```

The "quickcheck" command will then exercise POs with the set of nats and Ts given as the type binds in "ranges".

```
	> quickcheck
	Usage: quickcheck <ranges file> [<PO numbers>]
	
	> quickcheck ranges
	PO# 1, Result = true
	PO# 2, Result = true
	PO# 3, Result = true
	PO# 4, Result = true
	PO# 5, Result = true
	PO# 6, Result = true
	PO# 7, Result = false       -- NOTE this fails, because T is too small for a factorial of T
	>
```
