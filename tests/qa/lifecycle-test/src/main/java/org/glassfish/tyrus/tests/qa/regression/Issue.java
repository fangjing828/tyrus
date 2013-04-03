/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.tyrus.tests.qa.regression;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.glassfish.tyrus.tests.qa.lifecycle.MyException;
import org.glassfish.tyrus.tests.qa.tools.SerializationToolkit;

/**
 * @author Michal Čonos (michal.conos at oracle.com)
 */
public enum Issue implements java.io.Serializable {

    TYRUS_93("ClientEndpoint session.getRequestURI()==null"),
    TYRUS_94("ServerEndPoint: onError(): throwable.getCause()==null"),
    TYRUS_101("CloseReason not propagated to server side (when close() initiated from client)"),
    TYRUS_104("session should raise IllegalStateException when Session.getRemote() called on a closed session");

    private static final Logger logger = Logger.getLogger(Issue.class.getCanonicalName());
    private String description;
    private boolean enabled;
    private SerializationToolkit stool;

    private void setEnabled(boolean enabled)  {
        this.enabled = enabled;
        stool.save(this);
    }

    /**
     * is the issue enabled?
     *
     * @return true if enabled, false if the issue is disabled
     */
    public boolean isEnabled() {
        Object obj = stool.load();
        if(obj!=null && obj instanceof Issue && ((Issue) obj).enabled) {
            logger.log(Level.INFO, "Issue: {0} enabled", this.toString());
            return true;
        }
        return false;
    }

    /**
     * Disable issue
     */
    public void disable()  {
        setEnabled(false);
    }

    /**
     * Enable issue
     */
    public void enable()  {
        setEnabled(true);
    }

    /**
     * Disable all issue but the on requested. Handy for regression testing
     */
    public void disableAllButThisOne()  {
        disableAll();
        this.enable();
    }

    /**
     * Enable All issues in the database
     */
    public static void enableAll()  {
        for (Issue crno : Issue.values()) {
            crno.enable();
        }
    }

    /**
     * Disable all issue in the database
     */
    public static void disableAll() {
        for (Issue crno : Issue.values()) {
            crno.disable();
        }
    }
    
    public static String getTempDirectoryPath() {
        return System.getProperty("java.io.tmpdir");
    }
    
    public static File getTempDirectory() {
        return new File(getTempDirectoryPath());
    }

    /**
     * Issue is created with a description
     *
     * @param description issue description
     */
    Issue(String description)  {
        this.description = description;
        this.enabled = true;
        this.stool=new SerializationToolkit(new File(getTempDirectory(), this.toString()));
        stool.save(this);
    }

    public static boolean checkTyrus93(Session s) {
        if (Issue.TYRUS_93.isEnabled()) {
            try {
                logger.log(Level.INFO, "Tyrus-93: Client connecting:{0}", s.getRequestURI().toString());
            } catch (NullPointerException npe) {
                logger.log(Level.SEVERE, "Tyrus-93: NPE!");
                return false;
            }
        } else {
            logger.log(Level.INFO, "Client connecting:{0}", s.getRequestURI());
        }
        return true;
    }

    public static boolean checkTyrus94(Throwable thr) {
        if (Issue.TYRUS_94.isEnabled()) {
            if(thr instanceof MyException) {
                return true;
            }

            logger.log(Level.INFO, String.format("Received %s, expected MyException.class", thr.getClass().getName()));
            return false;
        }
        return true;
    }

    public static boolean checkTyrus101(CloseReason reason) {
        if (Issue.TYRUS_101.isEnabled()) {
            logger.log(Level.INFO, "TYRUS-101: reason={0}", reason);
            if (reason != null) {
                logger.log(Level.INFO, "TYRUS-101: reason.getCloseCode={0}", reason.getCloseCode());
            }
            return reason != null && reason.getCloseCode().equals(CloseReason.CloseCodes.GOING_AWAY);
        }
        return true;
    }

    public static boolean checkTyrus104(Session s) {
        if (Issue.TYRUS_104.isEnabled()) {
            if (s.isOpen()) {
                logger.log(Level.SEVERE, "TYRUS-104: isOpen on a closed session must return false");
                return false; // isClosed
            }
            try {
                logger.log(Level.INFO, "TYRUS-104: send string on closed connection");
                s.getBasicRemote().sendText("Raise onError now - socket is closed");
                logger.log(Level.SEVERE, "TYRUS-104: IllegalStateException expected, should never get here");
                s.close();
            } catch (IOException ex) {
                return true;
            } catch (IllegalStateException ex) {
                return true;
            }
        }
        return true;
    }
}