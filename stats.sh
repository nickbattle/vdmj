#
# Source code analysis of the project
#

# +------------------+----------------------+----------+----------+----------+----------+----------+----------+
# | Package Name     | File Name            | Type     | Blank    | JavaDoc  | Comment  | Code     | Total    |
# +------------------+----------------------+----------+----------+----------+----------+----------+----------+
# | examples         | ExampleCommand.java  | src      |        8 |       50 |        1 |       33 |       92 |
# | examples         | ExampleLens.java     | src      |        7 |       28 |        1 |       30 |       66 |
# | examples         | ExamplePlugin.java   | src      |       26 |       81 |        4 |      169 |      280 |
# | examples         | ExamplePluginPR.java | src      |       13 |       36 |        0 |       92 |      141 |
# | examples         | ExamplePluginSL.java | src      |       13 |       41 |        0 |       93 |      147 |
# +------------------+----------------------+----------+----------+----------+----------+----------+----------+
# | 1 package(s)     | 5 file(s)            | java     |       67 |      236 |        6 |      417 |      726 |
# +------------------+----------------------+----------+----------+----------+----------+----------+----------+

COUNTS=/tmp/sum$$

function sum()
{
    echo -n "Total $1 lines = "
    cut -f$2 -d \| $COUNTS | awk '{ sum += $1 } END { print sum }'
}

trap "rm -f $COUNTS" EXIT INT

mvn io.github.orhankupusoglu:sloc-maven-plugin:sloc | grep "package(s)" > $COUNTS

sum "Java source" 8
sum Javadoc 7
sum comment 6
sum blank 5

# eof
