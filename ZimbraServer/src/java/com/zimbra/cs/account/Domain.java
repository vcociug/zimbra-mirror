/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013 Zimbra Software, LLC.
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

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.account.ZAttrProvisioning.DomainStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.GalMode;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.soap.type.GalSearchType;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Domain extends ZAttrDomain {
    private String mUnicodeName;
    private Map<String, Object> mAccountDefaults = new HashMap<String, Object>();
    
    public Domain(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
        if (name == null)
            mUnicodeName = name;
        else
            mUnicodeName = IDNUtil.toUnicodeDomainName(name);
        if (attrs != null)
            resetData();
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.DOMAIN;
    }
    
    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public void deleteDomain(String zimbraId) throws ServiceException {
        getProvisioning().deleteDomain(getId());
    }
    
    public List getAllAccounts() throws ServiceException {
            return getProvisioning().getAllAccounts(this);
    }

    public void getAllAccounts(NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllAccounts(this, visitor);
    }

    public void getAllAccounts(Server s, NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllAccounts(this, s, visitor);
    }

    public List getAllCalendarResources() throws ServiceException {
        return getProvisioning().getAllCalendarResources(this);
    }

    public void getAllCalendarResources(NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllCalendarResources(this, visitor);
    }

    public void getAllCalendarResources(Server s, NamedEntry.Visitor visitor) throws ServiceException {
        getProvisioning().getAllCalendarResources(this, s, visitor);
    }

    public List getAllDistributionLists() throws ServiceException {
        return getProvisioning().getAllDistributionLists(this);
    }
    
    public String getGalSearchBase(String searchBaseSpec) throws ServiceException {
        throw ServiceException.FAILURE("unsupported", null);
    }

    @Override
    protected void resetData() {
        super.resetData();
        try {
            getDefaults(AttributeFlag.accountCosDomainInherited, mAccountDefaults);
        } catch (ServiceException e) {
            // TODO log
        }
    }
    
    public Map<String, Object> getAccountDefaults() {
        return mAccountDefaults;
    }
    
    public String getUnicodeName() {
        return mUnicodeName;
    }

    public boolean isSuspended() {
        DomainStatus status = getDomainStatus();
        boolean suspended = status != null && status.isSuspended();

        if (suspended)
            ZimbraLog.account.warn("domain " + mName + " is " + Provisioning.DOMAIN_STATUS_SUSPENDED);
        return suspended;
    }
    
    public boolean isShutdown() {
        DomainStatus status = getDomainStatus();
        boolean shutdown = status != null && status.isShutdown();
        
        if (shutdown)
            ZimbraLog.account.warn("domain " + mName + " is " + Provisioning.DOMAIN_STATUS_SHUTDOWN);
        return shutdown;
    }
    
    public boolean beingRenamed() {
        String renameInfo = getAttr(Provisioning.A_zimbraDomainRenameInfo);
        return (!StringUtil.isNullOrEmpty(renameInfo));
    }
    
    public boolean isLocal() {
        Provisioning.DomainType domainType = getDomainType();
        return Provisioning.DomainType.local == domainType;
    }

    
}
