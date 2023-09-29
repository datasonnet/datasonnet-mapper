# DataSonnet debugger

## Debugger Adapter Protocol

### What's new:

The Mapper CLI supports a new `-d` parameter that runs the Mapper on debug mode, where it waits for a connection from a DAP client, like VS Code. This provides support for the VS Code DataSonnet Debugger extension ( link here ).

Features 

* Implements a DAP server over STD IN/OUT
* 

### TODOs

Roughly in priority order:

* Support payloads from the client. Currently only self-contained scripts run. This change starts from the client, being able to specify the payload doc, and sent as a parameter of the launch config.
* Set up breakpoints from the client. Currently, the program starts in "auto-stop" mode, stopped at the beginning.
* Send output to the client, to be shown on the debugger console. For example, the program output
* Provide better description of the local and global context, and bindings. See StoppedProgramContext
* Currently only a string representation is returned to the client; complex variables can not be expanded on the client. See StoppedProgramContext
* Handle already wrapped scripts; until this is ready source code refs will be off by one for these scripts
* Be able to debug imported files
* Support restart
* Add tests

See FIXME and TODO annotations on code for more details
