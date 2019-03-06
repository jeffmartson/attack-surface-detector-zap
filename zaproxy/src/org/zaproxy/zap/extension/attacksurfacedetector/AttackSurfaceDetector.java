///////////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2015 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s):
//              Denim Group, Ltd.
//              Secure Decisions, a division of Applied Visions, Inc
//
////////////////////////////////////////////////////////////////////////

package org.zaproxy.zap.extension.attacksurfacedetector;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

public class AttackSurfaceDetector extends ExtensionAdaptor {
    private AbstractPanel statusPanel;
    JTabbedPane tabbedPane;
    JCheckBox autoSpiderField;
    private static final Logger logger = Logger.getLogger(AttackSurfaceDetector.class);
    public static final String NAME = "AttackSurfaceDetector";
    static { logger.debug("Loading Class"); }

    public AttackSurfaceDetector()
    {
        super();
        logger.debug("calling constructor");
        initialize();
        logger.debug("No-arg Constructor");
    }

    private void initialize()
    {
        logger.debug("Initialize");
        this.setName(NAME);
    }

    @Override
    public String getUIName() {
        return Constant.messages.getString("attacksurfacedetector.name");
    }

    @Override
    public void hook(ExtensionHook extensionHook)
    {
        logger.debug("Hook");
        super.hook(extensionHook);
        if (getView() != null)
        {
            extensionHook.getHookView().addStatusPanel(new AttackSurfaceDetectorPanel(getView(), getModel()));
        }
    }

    @Override
    public String getAuthor()
    {
        logger.debug("Getting Author");
        return "Secure Decisions";
    }

    @Override
    public String getDescription()
    {
        logger.debug("Getting Description");
        return "The Attack Surface Detector analyzes web application source code to generate endpoints that can be used for penetration testing.";
    }

    @Override
    public URL getURL()
    {
        logger.debug("Getting URL");
        try
        {
            return new URL("https://github.com/secdec/attack-surface-detector-zap/wiki");
        }
        catch (MalformedURLException e)
        {
            return null;
        }
    }

    @Override
    public boolean canUnload(){return true;}
}