package com.zimbra.cs.account.accesscontrol;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;

/*
 * TODO: when things are stable:
 *       rename AclAccessManager to DomainAclAccessManager, then
 *       rename RoleAccessManager to AclAccessManager
 */

public class RoleAccessManager extends AccessManager {

    public RoleAccessManager() throws ServiceException {
        // initialize RightManager
        RightManager.getInstance();
    }
    
    @Override
    public boolean isDomainAdminOnly(AuthToken at) {
        /*
         * returning true to essentially trigger all permission checks 
         * for all admins, not just domain admins.  
         * 
         * Should probably retire this call when we will never ever 
         * go back to the domain admin paradigm.
         */
        return true;
    }
    
    @Override
    public boolean canAccessAccount(AuthToken at, Account target,
            boolean asAdmin) throws ServiceException {
        return canDo(at, target, UserRight.R_loginAs, asAdmin, false);
    }

    @Override
    public boolean canAccessAccount(AuthToken at, Account target)
            throws ServiceException {
        return canAccessAccount(at, target, true);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target,
            boolean asAdmin) throws ServiceException {
        return canDo(credentials, target, UserRight.R_loginAs, asAdmin, false);
    }

    @Override
    public boolean canAccessAccount(Account credentials, Account target)
            throws ServiceException {
        return canAccessAccount(credentials, target, true);
    }

    @Override
    public boolean canAccessCos(AuthToken at, String cosId)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAccessDomain(AuthToken at, String domainName)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAccessDomain(AuthToken at, Domain domain)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canAccessEmail(AuthToken at, String email)
            throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return canDo(grantee, target, rightNeeded, asAdmin, defaultGrant, null);
    }
    
    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return canDo(grantee, target, rightNeeded, asAdmin, defaultGrant, null);
    }
    
    @Override
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant) {
        return canDo(granteeEmail, target, rightNeeded, asAdmin, defaultGrant, null);
    }
    
    @Override
    public boolean canDo(Account grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant, ViaGrant via) {
        try {
            if (grantee == null) {
                if (rightNeeded.isUserRight())
                    grantee = ACL.ANONYMOUS_ACCT;
                else
                    return false;
            }

            // 1. always allow self for user right
            if (rightNeeded.isUserRight() && target instanceof Account) {
                if (((Account)target).getId().equals(grantee.getId()))
                    return true;
            }
            
            // 2. check ACL
            Boolean result = RightChecker.canDo(grantee, target, rightNeeded, via);
            if (result != null)
                return result.booleanValue();
            else {
                // no ACL, see if there is a configured default 
                Boolean defaultValue = rightNeeded.getDefault();
                if (defaultValue != null)
                    return defaultValue.booleanValue();
                
                // no configured default, return default requested by the callsite
                return defaultGrant;
            }
                
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " + 
                                   "grantee=" + grantee.getName() + 
                                   ", target=" + target.getLabel() + 
                                   ", right=" + rightNeeded.getName() + 
                                   " => denied", e);
        }
        return false;
    }
    
    @Override
    public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant, ViaGrant via) {
        try {
            Account granteeAcct;
            if (grantee == null) {
                if (rightNeeded.isUserRight())
                    granteeAcct = ACL.ANONYMOUS_ACCT;
                else
                    return false;
            } else if (grantee.isZimbraUser())
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, grantee.getAccountId());
            else {
                if (rightNeeded.isUserRight())
                    granteeAcct = new ACL.GuestAccount(grantee);
                else
                    return false;
            }
            
            return canDo(granteeAcct, target, rightNeeded, asAdmin, defaultGrant, via);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " +
                                   "grantee=" + grantee.getAccountId() +
                                   ", target=" + target.getLabel() +
                                   ", right=" + rightNeeded.getName() +
                                   " => denied", e);
        }
        
        return false;
    }

    @Override
    public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin, boolean defaultGrant, ViaGrant via) {
        try {
            Account granteeAcct = null;
            
            if (granteeEmail != null)
                granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.name, granteeEmail);
            if (granteeAcct == null) {
                if (rightNeeded.isUserRight())
                    granteeAcct = ACL.ANONYMOUS_ACCT;
                else
                    return false;
            }
            
            return canDo(granteeAcct, target, rightNeeded, asAdmin, defaultGrant, via);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("ACL checking failed: " + 
                                   "grantee=" + granteeEmail + 
                                   ", target=" + target.getLabel() + 
                                   ", right=" + rightNeeded.getName() + 
                                   " => denied", e);
        }
        
        return false;
    }
    
    private AllowedAttrs canAccessAttrs(Account grantee, Entry target, AdminRight rightNeeded, Map<String, Object> attrs) throws ServiceException {
        // Do NOT check for self.  If an admin auth as an admin and want to get/set  
        // his own attrs, he has to have the proper right to do so.
            
        // check ACL
        return RightChecker.canAccessAttrs(grantee, target, rightNeeded, attrs);
    }
    
    @Override
    public AllowedAttrs canGetAttrs(Account grantee, Entry target, Map<String, Object> attrs) throws ServiceException {
        return canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_GET_ATTRS, attrs);
    }

        
    @Override
    public AllowedAttrs canGetAttrs(AuthToken grantee, Entry target, Map<String, Object> attrs) throws ServiceException {
        Account granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, grantee.getAccountId());
        // Account not found, throw PERM_DENIED instead of NO_SUCH_ACCOUNT so we are not vulnerable to account harvest attack
        if (granteeAcct == null)
            throw ServiceException.PERM_DENIED("cannot access attr");
        
        return canGetAttrs(granteeAcct, target, attrs);
    }
    
    @Override
    public AllowedAttrs canSetAttrs(Account grantee, Entry target, Map<String, Object> attrs) throws ServiceException{
        return canAccessAttrs(grantee, target, AdminRight.R_PSEUDO_SET_ATTRS, attrs);
    }
    
    @Override
    public AllowedAttrs canSetAttrs(AuthToken grantee, Entry target,  Map<String, Object> attrs) throws ServiceException {
        Account granteeAcct = Provisioning.getInstance().get(Provisioning.AccountBy.id, grantee.getAccountId());
        // Account not found, throw PERM_DENIED instead of NO_SUCH_ACCOUNT so we are not vulnerable to account harvest attack
        if (granteeAcct == null)
            throw ServiceException.PERM_DENIED("cannot access attr");
        
        return canSetAttrs(granteeAcct, target, attrs);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
