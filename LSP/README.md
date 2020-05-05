# VDMJ LSP Server

This project contains a developmental LSP/DAP language server for the VDM dialects supported by VDMJ.
It is intended to work with the vscode client, but it also works with lsp4e.

The server is started with -vdmsl, -vdmpp or -vdmrt to define the dialect to use, and -lsp and -dap options to pass the ports to listen on. It must include the VDMJ jar as well as LSP, and the annotations jars if you want to support them:
```
java -Dlog.filename=/dev/tty
    -cp annotations-1.0.0.jar:annotations2-1.0.0.jar:vdmj-4.3.0.jar:lsp-0.0.1-SNAPSHOT-200505.jar
    lsp.LSPServerSocket -vdmpp -lsp 8000 -dap 8001
```
The log.filename option sends full logging to a file of your choice. This includes all LSP and DAP messages, as well as some internals:
```
20:43:11.024: LSP VDM_PP Server listening on port 8000
20:43:11.034: DAP VDM_PP Server listening on port 8001
20:43:17.561: >>> LSP {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"processId":29453,"rootPath":"/home/nick/Digital Twins/VSCode/VSCodeWorkspacePP","rootUri":"file:///home/nick/Digital%20Twins/VSCode/VSCodeWorkspacePP","capabilities":{"workspace":{"applyEdit":true,"didChangeConfiguration":{"dynamicRegistration":true},"didChangeWatchedFiles":{"dynamicRegistration":true},"symbol":{"dynamicRegistration":true},"executeCommand":{"dynamicRegistration":true}},"textDocument":{"synchronization":{"dynamicRegistration":true,"willSave":true,"willSaveWaitUntil":true,"didSave":true},"completion":{"dynamicRegistration":true,"completionItem":{"snippetSupport":true,"commitCharactersSupport":true}},"hover":{"dynamicRegistration":true},"signatureHelp":{"dynamicRegistration":true},"definition":{"dynamicRegistration":true},"references":{"dynamicRegistration":true},"documentHighlight":{"dynamicRegistration":true},"documentSymbol":{"dynamicRegistration":true},"codeAction":{"dynamicRegistration":true},"codeLens":{"dynamicRegistration":true},"formatting":{"dynamicRegistration":true},"rangeFormatting":{"dynamicRegistration":true},"onTypeFormatting":{"dynamicRegistration":true},"rename":{"dynamicRegistration":true},"documentLink":{"dynamicRegistration":true}}},"trace":"off"}}
20:43:17.603: <<< LSP { "jsonrpc" : "2.0", "result" : { "serverInfo" : { "name" : "VDMJ LSP Server", "version" : "0.1" }, "capabilities" : { "definitionProvider" : true, "documentSymbolProvider" : true, "textDocumentSync" : { "openClose" : true, "change" : 2 } } }, "id" : 0 }
20:43:17.648: >>> LSP {"jsonrpc":"2.0","method":"initialized","params":{}}
20:43:18.317: <<< LSP { "jsonrpc" : "2.0", "method" : "client/registerCapability", "params" : { "registrations" : [ { "id" : "12345", "method" : "workspace/didChangeWatchedFiles", "registerOptions" : { "watchers" : [ { "globPattern" : "**/*.vpp" }, { "globPattern" : "**/*.vdmpp" } ] } } ] }, "id" : -1 }
20:43:18.320: <<< LSP { "jsonrpc" : "2.0", "method" : "textDocument/publishDiagnostics", "params" : { "uri" : "file:/home/nick/Digital%20Twins/VSCode/VSCodeWorkspacePP/debugging.vdmpp", "diagnostics" : [  ] } }
20:43:18.322: <<< LSP { "jsonrpc" : "2.0", "method" : "textDocument/publishDiagnostics", "params" : { "uri" : "file:/home/nick/Digital%20Twins/VSCode/VSCodeWorkspacePP/factorial.vdmpp", "diagnostics" : [  ] } }
20:43:18.324: >>> LSP {"jsonrpc":"2.0","method":"textDocument/didOpen","params":{"textDocument":{"uri":"file:///home/nick/Digital%20Twins/VSCode/VSCodeWorkspacePP/factorial.vdmpp","languageId":"VDM_PP","version":1,"text":"..."}}}
20:43:18.326: Opening new file: /home/nick/Digital Twins/VSCode/VSCodeWorkspacePP/factorial.vdmpp

```

For more information, contact @nickbattle.

![vscode session](images/vscode_screen.png)
