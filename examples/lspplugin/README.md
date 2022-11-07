# lspplugin

This project contains a simple example of an LSP plugin.

The plugin registers for all of the possible events that the LSP server can raise, and logs them.
It also returns a code lens to add a "Config" option to open the launch.json file.
It also creates a "example" command, which simply echoes its arguments.

