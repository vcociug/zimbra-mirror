/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ZFilterAction {

    public static final String A_DISCARD = "discard";
    public static final String A_FILEINTO = "fileinto";
    public static final String A_FLAG = "flag";
    public static final String A_KEEP = "keep";
    public static final String A_REDIRECT = "redirect";
    public static final String A_STOP = "stop";
    public static final String A_TAG = "tag";

    public enum FlagOp {

        READ, FLAGGED;

        private static String[] mArgs = { "read", "flagged" };

        public String toProtoArg() { return mArgs[ordinal()]; }

        public static FlagOp fromString(String s) throws ServiceException {
            s = s.trim().toLowerCase();
            for (int i=0; i < mArgs.length; i++)
                if (mArgs[i].equals(s)) return values()[i];
            throw ZClientException.CLIENT_ERROR("invalid arg "+s+", valid values: "+ Arrays.asList(mArgs), null);
        }
    }

    private String mName;
    protected List<String> mArgs;

    private ZFilterAction(String name, String... args) {
        mName = name;
        mArgs = Collections.unmodifiableList(args != null ? Arrays.asList(args) : new ArrayList<String>());
    }

    public String getName() {
        return mName;
    }

    public List<String> getArgs() {
        return mArgs;
    }

    Element toElement(Element parent) {
        Element a = parent.addElement(MailConstants.E_ACTION);
        a.addAttribute(MailConstants.A_NAME, mName);
        for (String arg : mArgs) {
            a.addElement(MailConstants.E_FILTER_ARG).setText(arg);
        }
        return a;
    }
    
    public static ZFilterAction getAction(Element actionElement) throws ServiceException {
        String n = actionElement.getAttribute(MailConstants.A_NAME);
        if (n.equals(A_KEEP))
            return new ZKeepAction();
        else if (n.equals(A_DISCARD))
            return new ZDiscardAction();
        else if (n.equals(A_STOP))
            return new ZStopAction();
        else if (n.equals(A_FILEINTO))
            return new ZFileIntoAction(actionElement.getElement(MailConstants.E_FILTER_ARG).getText());
        else if (n.equals(A_TAG))
            return new ZTagAction(actionElement.getElement(MailConstants.E_FILTER_ARG).getText());
        else if (n.equals(A_REDIRECT))
            return new ZRedirectAction(actionElement.getElement(MailConstants.E_FILTER_ARG).getText());
        else if (n.equals(A_FLAG))
            return new ZFlagAction(FlagOp.fromString(actionElement.getElement(MailConstants.E_FILTER_ARG).getText()));
        else
            throw ZClientException.CLIENT_ERROR("unknown filter action: "+n, null);
    }

    public static class ZKeepAction extends ZFilterAction {
        public ZKeepAction() { super(A_KEEP); }
    }

    public static class ZDiscardAction extends ZFilterAction {
        public ZDiscardAction() { super(A_DISCARD); }
    }

    public static class ZStopAction extends ZFilterAction {
        public ZStopAction() { super(A_STOP); }
    }

    public static class ZFileIntoAction extends ZFilterAction {
        public ZFileIntoAction(String folderPath) { super(A_FILEINTO, folderPath); }
        public String getFolderPath() { return mArgs.get(0); }
    }

    public static class ZTagAction extends ZFilterAction {
        public ZTagAction(String tagName) { super(A_TAG, tagName); }
        public String getTagName() { return mArgs.get(0); }
    }

    public static class ZFlagAction extends ZFilterAction {
        public ZFlagAction(FlagOp op) { super(A_FLAG, op.toProtoArg()); }
    }

    public static class ZRedirectAction extends ZFilterAction {
        public ZRedirectAction(String address) { super(A_REDIRECT, address); }
        public String getAddress() { return mArgs.get(0); }
    }
}
