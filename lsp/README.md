# VDMJ LSP/DAP Server

This project contains a developmental LSP/DAP language server for the VDM dialects supported by VDMJ.
It is intended to work with the VS Code IDE client, but it should also work with `lsp4e` in Eclipse.
The testing has focused on VS Code.

## Installation

Firstly, install VS Code itself. See [https://code.visualstudio.com/](https://code.visualstudio.com/). 

Secondly, locate the VS Code extensions for VDM [https://github.com/jonaskrask/vdm-vscode](https://github.com/jonaskrask/vdm-vscode).

The VS Code project includes three separate extensions for the VDM-SL, VDM++ and VDM-RT dialects respectively. The VSIX file for each extension is located in a folder of the dialect name, eg. "vdmsl/vdmsl-lsp-N.N.N.vsix". To install a VSIX file, go to the Extensions panel in VS Code, and click the "..." (Views and More Actions) menu at the top of the screen, above the search box. This has an option called, "Install from VSIX...".

```
https://github.com/jonaskrask/vdm-vscode/blob/master/vdmsl/vdmsl-lsp-0.0.2.vsix
https://github.com/jonaskrask/vdm-vscode/blob/master/vdmpp/vdmpp-lsp-0.0.2.vsix
https://github.com/jonaskrask/vdm-vscode/blob/master/vdmrt/vdmrt-lsp-0.0.2.vsix
```
(Though check for the latest version, before using these links!)

## Editing VDM Specifications

Note that the LSP server uses VS Code folders rather than multi-root workspaces.

To start a new project folder, open VS Code and `Open Folder...` from the File menu. You can create a new folder, or open an existing one. The server will treat **all** files in the folder and subfolders (recursively) as part of the same specification, as long as the files match the file extension for the chosen dialect (eg. `*.vdmsl`, `*.vdmpp` or `*.vdmrt`). Other files in the folder will be ignored.

File and folder creation and deletion, moving and so on should work correctly.

Syntax errors are displayed for the file being edited as you type; the whole specification is type-checked when you save a file.

The F12 key will navigate from a symbol name to its definition (eg. from a function call to its definition).

Typing a "." after a record variable will offer field names to complete the expression. Typing CTRL-SPACE will offer global names with which to complete the name you are typing.

Standard VDMJ annotations should work as expected.


## Execution

To evaluate expressions against the specification for debugging, open the `Run...` panel. Initially, if there is
no `.vscode/launch.json` file in your folder, you will be prompted to create one. Select the `VDM Debug`
confguration; the default settings can be accepted. You can then launch the interpreter using the "Run" menu,
or just by pressing F5. You cannot start the interpreter if there are type checking errors (warnings are ok).

The debug console opens and allows you to enter multiple expressions to evaluate:

```
*
* VDMJ VDM_SL Interpreter
* DEBUG enabled
*

Default module is DEFAULT
Initialized in 0.001 secs.

help
default <name> - set the default class or module name
print <exp> - evaluate an expression
...
help [<command>] - information about commands

print fac(10)
= 3628800
Executed in 6.105 secs.

p 1+1
= 2
Executed in 0.003 secs.

```
If breakpoints have been set, the evaluation will stop and can be single stepped (over, in, out) or continued,
stack frames and values viewed and watched, using standard VS Code controls. Multi-threaded VDM++ debugging should
work correctly.

CTRL-F5 launches a session with debugging disabled (breakpoints will be ignored).

You can modify a specification while debugging, but the changes you make will not take effect until you start a
new debug session. You will be given an error if you attempt a new expression evaluation without a restart.

There are some screenshots below.

## Problems

Various things do not work perfectly yet:

- The F12 navigation does not work correctly for a few expressions that involve types. For example, you cannot
navigate to the `Clock` definition in the expression `is_(var, Clock | Timer)`. But most common type usage is OK, like record field definitions or function signatures.
- The "." field completion is awkward to use because it requires the spec to be cleanly type-checked. So if you type `var.field`, the field part will not be offered unless you type `var`, then save the spec to type check it, and then type ".".
- If you click the `Stop` (Shift-F5 or the red square) debugging button, the session is usually closed and the debug console says so. But if you click `Stop` at a breakpoint or an exception that has been caught, control returns to the debug console and the session is still active. Clicking `Stop` once again will stop for real.


For more information or if you have problems, contact @nickbattle.

![vscode session 1](images/vscode_screen.png)
![vscode session 2](images/completion_definitions.png)
![vscode session 3](images/annotation_example.png)
