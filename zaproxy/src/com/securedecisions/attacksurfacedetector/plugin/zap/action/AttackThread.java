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

package com.securedecisions.attacksurfacedetector.plugin.zap.action;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.commons.httpclient.URI;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.ViewDelegate;
import org.parosproxy.paros.extension.history.ExtensionHistory;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.SiteNode;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;
import org.parosproxy.paros.network.HttpSender;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.ascan.ExtensionActiveScan;
import org.zaproxy.zap.extension.spider.ExtensionSpider;
import org.zaproxy.zap.extension.attacksurfacedetector.ZapPropertiesManager;
import org.zaproxy.zap.extension.spider.SpiderScan;
import org.zaproxy.zap.model.Target;


public class AttackThread extends Thread {
    public enum Progress { NOT_STARTED, SPIDER, ASCAN, FAILED, COMPLETE, STOPPED }

    private EndpointsButton extension;
    private URL url;
    private HttpSender httpSender = null;
    private boolean stopAttack = false;
    private ViewDelegate view = null;
    private Map<String, String> nodes = null;

    private static final Logger logger = Logger.getLogger(AttackThread.class);

    public AttackThread(EndpointsButton ext,  ViewDelegate view) {
        this.extension = ext;
        this.view = view;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public void setNodes(Map<String, String> nodes) {this.nodes = nodes;}

    @Override
    public void run()
    {
        stopAttack = false;
        try
        {
            SiteNode startNode = accessNode(this.url, "get");
            String urlString = url.toString();

            logger.debug("Starting at url : " + urlString);

            if (startNode == null)
            {
                logger.error("Failed to access URL " + urlString);
                if(extension != null)
                    extension.notifyProgress(Progress.FAILED);
                return;
            }
            if (stopAttack)
            {
                logger.debug("Attack stopped manually");
                if(extension != null)
                    extension.notifyProgress(Progress.STOPPED);
                return;
            }
            if (ZapPropertiesManager.INSTANCE.getAutoSpider())
            {
                try
                {
                    spider(startNode);
                }
                catch (IllegalStateException ise)
                {
                    logger.debug(ise.getMessage());
                }

            }
            else
            {
                for (Map.Entry<String, String> node : nodes.entrySet())
                {
                    logger.debug("About to call accessNode.");
                    SiteNode childNode = accessNode(new URL(url + node.getKey()), node.getValue());
                    logger.debug("got out of accessNode.");
                    if (childNode != null)
                        logger.debug("Child node != null, child node is " + childNode);
                    else
                        logger.debug("child node was null.");
                }
            }
            ExtensionActiveScan extAscan = Control.getSingleton().getExtensionLoader().getExtension(ExtensionActiveScan.class);
            if (extAscan == null)
            {
                logger.error("No active scanner");
                extension.notifyProgress(Progress.FAILED);
            }
            else
            {
                extension.notifyProgress(Progress.ASCAN);
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            if(extension != null)
                extension.notifyProgress(Progress.FAILED);
        }
    }

    private void spider(SiteNode startNode)throws MalformedURLException
    {
        logger.debug("About to grab spider.");
        ExtensionSpider extSpider = Control.getSingleton().getExtensionLoader().getExtension(ExtensionSpider.class);
        logger.debug("Starting spider.");
        if (extSpider == null) {
            logger.error("No spider");
            if(extension != null)
                extension.notifyProgress(Progress.FAILED);
            return;
        }
        else if (startNode == null)
        {
            logger.error("start node was null");
            if(extension != null)
                extension.notifyProgress(Progress.FAILED);
            return;
        }
        else
        {
            try
            {
                logger.debug("Starting spider.");
                if (extension != null)
                    extension.notifyProgress(Progress.SPIDER);
                startNode.setAllowsChildren(true);
                for (Map.Entry<String, String> node : nodes.entrySet()) {
                    logger.debug("About to call accessNode.");
                    SiteNode childNode = accessNode(new URL(url + node.getKey()), node.getValue());
                    logger.debug("got out of accessNode.");
                    if (childNode != null)
                        logger.debug("Child node != null, child node is " + childNode);
                    else
                        logger.debug("child node was null.");
                }
                logger.debug("about to start the extension. node = " + startNode);
                logger.debug("child count = " + startNode.getChildCount());
                Target spiderTarget = new Target(startNode);
                int id = extSpider.startScan(spiderTarget, null, null);
                sleep(1500);
                SpiderScan spiderScan = extSpider.getScan(id);
                logger.debug("Started the extension.");
                while (spiderScan.isRunning())
                {
                    sleep(1500);
                    if (this.stopAttack)
                    {
                        extSpider.stopScan(id);
                        break;
                    }
                }
                if (stopAttack)
                {
                    logger.debug("Attack stopped manually");
                    if (extension != null)
                        extension.notifyProgress(Progress.STOPPED);
                    return;
                }
                if (stopAttack)
                {
                    logger.debug("Attack stopped manually");
                    if (extension != null)
                        extension.notifyProgress(Progress.STOPPED);
                }

               spiderScan.spiderComplete(true);

            }
            catch (InterruptedException ie)
            {
                logger.debug(ie.getStackTrace());
            }
        }
    }

    private SiteNode accessNode(URL url, String method)
    {
        logger.debug("Trying to find a node for " + url);
        SiteNode startNode = null;
        // Request the URL
        try {
            HttpMessage msg = new HttpMessage(new URI(url.toString(), true));
            if(method.toString().equalsIgnoreCase("requestmethod.post") || method.toString().equalsIgnoreCase("post"))
            {
                msg.getRequestHeader().setMethod(HttpRequestHeader.POST);
            }

            startNode = sendAndProcess(msg);
        }
        catch (Exception e1)
        {
            logger.error(e1.getMessage(), e1);
            return null;
        }
        logger.debug("returning " + startNode);
        return startNode;
    }

    private SiteNode sendAndProcess(HttpMessage msg) throws IOException, InvocationTargetException, InterruptedException {
        try {
            getHttpSender().sendAndReceive(msg, true);
        } catch (ConnectException ce) {
            String warningMsg = Constant.messages.getString("attacksurfacedetector.connectfailed.warning",
                    msg.getRequestHeader().getURI().toString(), ce.getMessage());
            logger.warn(warningMsg);
            if (View.isInitialised()) {
                View.getSingleton().showWarningDialog(warningMsg);
            }
            return null;
        }
        ExtensionHistory extHistory = Control.getSingleton().getExtensionLoader().getExtension(ExtensionHistory.class);
        extHistory.addHistory(msg, HistoryReference.TYPE_ZAP_USER);
        HistoryReference hRef = msg.getHistoryRef();
        hRef.setNote("Endpoint generated by Attack Surface Detector");
        MutableObject<SiteNode> siteNodeWrapper = new MutableObject<>();
        SwingUtilities.invokeAndWait(() -> {
            // Needs to be done on the EDT
            SiteNode node = Model.getSingleton().getSession().getSiteTree().addPath(msg.getHistoryRef(), msg);
            siteNodeWrapper.setValue(node);
        });
        return siteNodeWrapper.getValue();
    }

    private HttpSender getHttpSender()
    {
        if (httpSender == null)
            httpSender = new HttpSender(Model.getSingleton().getOptionsParam().getConnectionParam(), true, HttpSender.MANUAL_REQUEST_INITIATOR);

        return httpSender;
    }
}