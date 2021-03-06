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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class XMPPComponentSpec extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-description Domain selector
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=true)
    private final DomainSelector domain;

    /**
     * @zm-api-field-description Server selector
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=true)
    private final ServerSelector server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XMPPComponentSpec() {
        this((String) null, (DomainSelector) null, (ServerSelector) null);
    }

    public XMPPComponentSpec(String name, DomainSelector domain,
                            ServerSelector server) {
        this.name = name;
        this.domain = domain;
        this.server = server;
    }

    public String getName() { return name; }
    public DomainSelector getDomain() { return domain; }
    public ServerSelector getServer() { return server; }
}
