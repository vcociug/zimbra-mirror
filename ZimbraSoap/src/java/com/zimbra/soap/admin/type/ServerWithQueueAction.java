/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
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

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ServerWithQueueAction {

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    @XmlElement(name=AdminConstants.E_QUEUE, required=true)
    private final MailQueueWithAction queue;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ServerWithQueueAction() {
        this((String) null, (MailQueueWithAction) null);
    }

    public ServerWithQueueAction(String name, MailQueueWithAction queue) {
        this.name = name;
        this.queue = queue;
    }

    public String getName() { return name; }
    public MailQueueWithAction getQueue() { return queue; }
}
