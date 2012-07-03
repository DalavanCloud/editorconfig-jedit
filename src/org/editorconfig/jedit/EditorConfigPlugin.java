// Copyright (c) 2012 EditorConfig Team
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.
//

package org.editorconfig.jedit;

import java.util.List;
import java.util.Properties;
import javax.script.ScriptException;
import org.editorconfig.core.*;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditPlugin;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.util.Log;

public class EditorConfigPlugin extends EditPlugin implements EBComponent
{
    static private EditorConfigPlugin plugin;

    // get the plugin instance
    static public EditorConfigPlugin getPlugin()
    {
        return plugin;
    }

    public EditorConfigPlugin()
    {
        plugin = this;
    }

    @Override
    public void start()
    {
        EditBus.addToBus(this);
    }

    @Override
    public void stop()
    {
        EditBus.removeFromBus(this);
    }

    // an inner class that stores EditorConfig configuration info
    private class EditorConfigConf
    {
        String      indentStyle = null;
        int         indentSize = 0;
        int         tabWidth = 0;
        String      endOfLine = null;

        // indentStyle will be set to this value if indent_size = tab
        static final int    INDENT_SIZE_TAB = -1000;
    }

    public void loadEditorConfig(Buffer buf)
        throws NumberFormatException, EditorConfigException, ScriptException
    {
        // EditorConfig confs
        EditorConfigConf ecConf = new EditorConfigConf();

        EditorConfig ec = null;

        ec = new EditorConfig();

        List<EditorConfig.OutPair> outPairs = null;

        outPairs = ec.getProperties(buf.getPath());

        for (EditorConfig.OutPair pair : outPairs)
        {
            String key = pair.getKey();
            String value = pair.getVal();

            if (key.equals("indent_style")) // soft or hard tabs?
                ecConf.indentStyle = value;
            else if (key.equals("tab_width")) // the width of tab
                ecConf.tabWidth = Integer.parseInt(value);
            else if (key.equals("indent_size")) // the size of indent
            {
                int indent_size = 0;

                if (value.equals("tab"))
                    ecConf.indentSize = EditorConfigConf.INDENT_SIZE_TAB;
                else
                {
                    indent_size = Integer.parseInt(value);

                    if (indent_size > 0)
                        ecConf.indentSize = indent_size;
                }
            }
            else if (key.equals("end_of_line")) // eof
                ecConf.endOfLine = value;
        }

        // set buffer after reading the stdin

        if (ecConf.indentStyle != null) // indent_style
        {
            if (ecConf.indentStyle.equals("tab"))
                buf.setBooleanProperty("noTabs", false);
            else if (ecConf.indentStyle.equals("space"))
                buf.setBooleanProperty("noTabs", true);
        }

        if (ecConf.indentSize > 0) // indent_size > 0 
        {
            buf.setIntegerProperty("indentSize", ecConf.indentSize);

            // set tabSize here, so this could be overwritten if
            // ecConf.tabWidth > 0
            buf.setIntegerProperty("tabSize", ecConf.indentSize);
        }

        if (ecConf.tabWidth > 0) // tab_width
            buf.setIntegerProperty("tabSize", ecConf.tabWidth);

        // indent_size = tab 
        if (ecConf.indentSize == EditorConfigConf.INDENT_SIZE_TAB)
            buf.setIntegerProperty("indentSize", buf.getTabSize());

        if (ecConf.endOfLine != null) // eof
        {
            if (ecConf.endOfLine.equals("lf"))
                buf.setStringProperty(JEditBuffer.LINESEP, "\n");
            else if (ecConf.endOfLine.equals("crlf"))
                buf.setStringProperty(JEditBuffer.LINESEP, "\r\n");
            else if (ecConf.endOfLine.equals("cr"))
                buf.setStringProperty(JEditBuffer.LINESEP, "\r");
        }
    }
	public void handleMessage(EBMessage msg)
	{
		if (msg instanceof BufferUpdate)
		{
		    BufferUpdate bu_msg = (BufferUpdate) msg;
            Buffer buf = bu_msg.getBuffer();

		    if (bu_msg.getWhat() == BufferUpdate.LOADED)
            {
                try
                {
                    loadEditorConfig(buf);
                } catch (NumberFormatException e) {
                    Log.log(Log.ERROR, this,
                            "Failed to load EditorConfig: " + e.toString());
                    e.printStackTrace();
                } catch (EditorConfigException e) {
                    Log.log(Log.ERROR, this,
                            "Failed to load EditorConfig: " + e.toString());
                    e.printStackTrace();
                } catch (ScriptException e) {
                    Log.log(Log.ERROR, this,
                            "Failed to load EditorConfig: " + e.toString());
                    e.printStackTrace();
                }
            }
		}
    }
}
