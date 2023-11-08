# DataSonnet debugger

## Debugger Adapter Protocol

### What's new:

The Mapper CLI supports a new `-d` parameter that runs the Mapper on debug mode, where it waits for a connection from a DAP client, like VS Code. This provides support for the [VS Code DataSonnet Debugger extension](https://marketplace.visualstudio.com/items?itemName=PortX.datasonnet-vscode-debugger).

Features 

* Implements a DAP server over STD IN/OUT, so that it can be used in [launch mode](https://microsoft.github.io/debug-adapter-protocol/overview)
* Once the program finishes, the mapper sends the output to the client to be shown on the debugger console.
* Supports receiving payloads from the client.
* Supports processing payloads with Content-Type other than JSON
* Supports specifying the output MIME type on the debug configuration
* Set up breakpoints from the client.

## TODOs

There are some issues; check the [ones with the debugger labels](https://github.com/datasonnet/datasonnet-mapper/issues?q=is:issue+is:open+label:debugger)

