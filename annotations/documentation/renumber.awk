#
# Renumber Java source files with "(NNNN," to have contiguous NNNN.
#
# 1. Assumes four digit err numbers.
# 2. Send the next number out on stderr at the end.
# 3. See renumber.sh script.
#

BEGIN {
}
/\([0-9]{4},/ {
    print gensub(/\([0-9]{4},/, "("N",", 1)
    N=N+1
}
!/\([0-9]{4},/ {
    print $0
}
END {
    print N >"/dev/stderr"
}
