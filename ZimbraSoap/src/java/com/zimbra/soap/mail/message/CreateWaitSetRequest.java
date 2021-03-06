/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.WaitSetAddSpec;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Create a waitset to listen for changes on one or more accounts
 * <br />
 * Called once to initialize a WaitSet and to set its "default interest types"
 * <p>
 * <b>WaitSet</b>: scalable mechanism for listening for changes to one or more accounts
 * </p>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_WAIT_SET_REQUEST)
public class CreateWaitSetRequest {

    /**
     * @zm-api-field-tag default-interests
     * @zm-api-field-description Default interest types: comma-separated list.  Currently:
     * <table>
     * <tr> <td> <b>f</b> </td> <td> folders </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> messages </td> </tr>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "f,m,c,a,t,d") </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_DEFTYPES /* defTypes */, required=true)
    private final String defaultInterests;

    /**
     * @zm-api-field-tag all-accounts
     * @zm-api-field-description If <b>{all-accounts}</b> is set, then all mailboxes on the system will be listened
     * to, including any mailboxes which are created on the system while the WaitSet is in existence.
     * Additionally:
     * <ul>
     * <li> &lt;add>, &lt;remove> and &lt;update> tags are IGNORED
     * <li> The requesting authtoken must be an admin token
     * </ul>
     * <p>
     * AllAccounts WaitSets are *semi-persistent*, that is, even if the server restarts, it is OK to call
     * &lt;WaitSetRequest> passing in your previous sequence number.  The server will attempt to resynchronize the
     * waitset using the sequence number you provide (the server's ability to do this is limited by the RedoLogs that
     * are available)
     */
    @XmlAttribute(name=MailConstants.A_ALL_ACCOUNTS /* allAccounts */, required=false)
    private ZmBoolean allAccounts;

    /**
     * @zm-api-field-description Waitsets to add
     */
    @XmlElementWrapper(name=MailConstants.E_WAITSET_ADD /* add */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<WaitSetAddSpec> accounts = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateWaitSetRequest() {
        this((String) null);
    }

    public CreateWaitSetRequest(String defaultInterests) {
        this.defaultInterests = defaultInterests;
    }

    public void setAllAccounts(Boolean allAccounts) {
        this.allAccounts = ZmBoolean.fromBool(allAccounts);
    }
    public void setAccounts(Iterable <WaitSetAddSpec> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public CreateWaitSetRequest addAccount(WaitSetAddSpec account) {
        this.accounts.add(account);
        return this;
    }

    public String getDefaultInterests() { return defaultInterests; }
    public Boolean getAllAccounts() { return ZmBoolean.toBool(allAccounts); }
    public List<WaitSetAddSpec> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("defaultInterests", defaultInterests)
            .add("allAccounts", allAccounts)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
