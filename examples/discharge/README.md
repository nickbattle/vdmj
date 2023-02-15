# discharge

This project contains a simple example of VDMJ plugin.

The plugin registers uses a file of "ranges" to specify the values to give to type binds in a proof obligation.
It then evaluates the PO, which would normally not be executable because of the type binds, using the range
values for each bind. For example, the test.vdm spec in the project folder is:

```
types
	T = nat
	inv t == t < 10;
	
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

So with a ranges file like this:

```
t:nat = {0, ..., 100}
a:T = {0, ..., 9}
```

The discharge command will exercise all the POs with the set of nats and Ts given.

```
> discharge
Usage: discharge <ranges file> [<PO numbers>]

> discharge ranges
PO# 1, Result = true
PO# 2, Result = true
PO# 3, Result = true
PO# 4, Result = true
PO# 5, Result = true
PO# 6, Result = true
PO# 7, Result = false       -- NOTE this fails, because T is too small for a factorial of T
>
```
