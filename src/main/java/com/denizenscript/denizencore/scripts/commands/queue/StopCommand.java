package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class StopCommand extends AbstractCommand {

    // <--[command]
    // @Name Stop
    // @Syntax stop
    // @Required 0
    // @Short Stops the current queue.
    // @Group queue
    //
    // @Description
    // This will immediately stop the current queue, preventing it from processing any further.
    //
    // @Tags
    // <queue>
    //
    // @Usage
    // Use to stop the current queue.
    // - stop
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : ArgumentHelper.interpretArguments(scriptEntry.aHArgs)) {
            arg.reportUnhandled();
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), new QueueTag(scriptEntry.getResidingQueue()).debug());
        }

        scriptEntry.getResidingQueue().clear();
        scriptEntry.getResidingQueue().stop();
    }
}