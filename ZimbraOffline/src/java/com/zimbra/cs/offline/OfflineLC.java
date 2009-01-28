/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.offline;

import java.io.File;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.util.Constants;

public class OfflineLC {
    public static final KnownKey zdesktop_app_id;
    public static final KnownKey zdesktop_name;
    public static final KnownKey zdesktop_version;
	public static final KnownKey zdesktop_relabel;
	public static final KnownKey zdesktop_buildid;
    public static final KnownKey zdesktop_skins;
    public static final KnownKey zdesktop_redolog_enabled;
    
    public static final KnownKey zdesktop_sync_timer_frequency;
    public static final KnownKey zdesktop_dirsync_min_delay;
    public static final KnownKey zdesktop_dirsync_fail_delay;
    public static final KnownKey zdesktop_account_poll_interval;
    public static final KnownKey zdesktop_reauth_delay;
    public static final KnownKey zdesktop_retry_delay_min;
    public static final KnownKey zdesktop_retry_delay_max;    
    public static final KnownKey zdesktop_client_poll_interval;
    public static final KnownKey zdesktop_retry_limit;
    public static final KnownKey zdesktop_gal_sync_trace_enabled;
    public static final KnownKey zdesktop_gal_sync_interval_secs;
    public static final KnownKey zdesktop_gal_refresh_interval_days;
    
    public static final KnownKey zdesktop_sync_batch_size;
    public static final KnownKey zdesktop_sync_zip_level;
    
    public static final KnownKey zdesktop_sync_messages;
    public static final KnownKey zdesktop_sync_contacts;
    public static final KnownKey zdesktop_sync_appointments;
    public static final KnownKey zdesktop_sync_tasks;
    public static final KnownKey zdesktop_sync_chats;
    public static final KnownKey zdesktop_sync_documents;
    public static final KnownKey zdesktop_sync_gal;
    public static final KnownKey zdesktop_sync_mountpoints;
    
    public static final KnownKey zdesktop_sync_skip_idlist;
    
    public static final KnownKey zdesktop_request_timeout;
    public static final KnownKey zdesktop_galsync_request_timeout;
    public static final KnownKey http_so_timeout;
    public static final KnownKey http_connection_timeout;
    public static final KnownKey dns_cache_ttl;
    
    public static final KnownKey auth_token_lifetime;
    
    public static final KnownKey zdesktop_datasource_properties;
    
    public static final KnownKey zdesktop_membuf_limit;
    
    public static final KnownKey zdesktop_upload_size_limit;
    
    public static final KnownKey zdesktop_yauth_appid;
    public static final KnownKey zdesktop_yab_baseuri;
    public static final KnownKey zdesktop_yab_sync_interval;
    public static final KnownKey zdesktop_ymail_baseuri;
    public static final KnownKey zdesktop_imap_fullsync_interval;
    public static final KnownKey zdesktop_ybizmail_smtp_host;
    public static final KnownKey zdesktop_ybizmail_smtp_port;
    public static final KnownKey zdesktop_ybizmail_smtp_ssl;

    public static final KnownKey zdesktop_support_email;
    
    public static final KnownKey zdesktop_min_zcs_version_sync_tgz;
    public static final KnownKey zdesktop_caldav_enabled;

    public static final KnownKey zdesktop_gab_base_url;
    
    public static final KnownKey zdesktop_log_context_filter;
    public static final KnownKey zdesktop_installation_key;

    static void init() {
        // This method is there to guarantee static initializer of this
        // class is run.
    }

    static {
    	zdesktop_app_id = new KnownKey("zdesktop_app_id");

        zdesktop_name = new KnownKey("zdesktop_name");
        zdesktop_name.setDefault("Yahoo! Zimbra Desktop");
        zdesktop_name.setDoc("UserAgent name of the Zimbra Desktop software.");
    	
        zdesktop_relabel = new KnownKey("zdesktop_relabel");
        zdesktop_relabel.setDefault("ALPHA");
        zdesktop_relabel.setDoc("Release label such as R or BETA");
        
        zdesktop_version = new KnownKey("zdesktop_version");
        zdesktop_version.setDefault("0.1");
        zdesktop_version.setDoc("Version number of the Zimbra Desktop software.");
        
        zdesktop_buildid = new KnownKey("zdesktop_buildid");
        zdesktop_buildid.setDefault("1");
        zdesktop_buildid.setDoc("Build number of the Zimbra Desktop software.");

	    zdesktop_skins = new KnownKey("zdesktop_skins");
	    zdesktop_skins.setDefault("beach");
	    zdesktop_skins.setDoc("Comma delimited list of installed skins.");
	    
	    zdesktop_redolog_enabled = new KnownKey("zdesktop_redolog_enabled");
	    zdesktop_redolog_enabled.setDefault("true");
	    zdesktop_redolog_enabled.setDoc("Whether to use redolog.  Default false.");
    	
	    zdesktop_sync_timer_frequency = new KnownKey("zdesktop_sync_timer_frequency");
	    zdesktop_sync_timer_frequency.setDefault(Long.toString(5 * Constants.MILLIS_PER_SECOND));
	    zdesktop_sync_timer_frequency.setDoc("Main sync loop timer firing frequency. Default 5000 (5 seconds)");    
	    
	    zdesktop_dirsync_min_delay = new KnownKey("zdesktop_dirsync_min_delay");
	    zdesktop_dirsync_min_delay.setDefault(Long.toString(15 * Constants.MILLIS_PER_SECOND));
	    zdesktop_dirsync_min_delay.setDoc("Minimum delay in milliseconds between two directory sync executions. Default 15000 (15 seconds)");
    	
	    zdesktop_dirsync_fail_delay = new KnownKey("zdesktop_dirsync_fail_delay");
	    zdesktop_dirsync_fail_delay.setDefault(Long.toString(15 * Constants.MILLIS_PER_MINUTE));
	    zdesktop_dirsync_fail_delay.setDoc("Minimum delay in milliseconds after a directory sync failure. Default 900000 (15 minutes)");
	    
	    zdesktop_account_poll_interval = new KnownKey("zdesktop_account_poll_interval");
	    zdesktop_account_poll_interval.setDefault(Long.toString(Constants.MILLIS_PER_HOUR));
	    zdesktop_account_poll_interval.setDoc("Minimum delay in milliseconds between two directory sync executions for the same account. Default 3600000 (1 hour).");
	    
	    zdesktop_client_poll_interval = new KnownKey("zdesktop_client_poll_interval");
	    zdesktop_client_poll_interval.setDefault("60");
	    zdesktop_client_poll_interval.setDoc("How often Ajax client should poll for updates. Default 15 (seconds).");
	    
	    zdesktop_reauth_delay = new KnownKey("zdesktop_reauth_delay");
	    zdesktop_reauth_delay.setDefault(Long.toString(Constants.MILLIS_PER_HOUR));
	    zdesktop_reauth_delay.setDoc("Minimum delay in milliseconds to reauth after auth failure. Default 3600000 (1 hour).");
	    
	    zdesktop_retry_delay_min = new KnownKey("zdesktop_retry_delay_min");
	    zdesktop_retry_delay_min.setDefault(Long.toString(Constants.MILLIS_PER_MINUTE));
	    zdesktop_retry_delay_min.setDoc("Minimum delay in milliseconds to retry after becoming offline or encountering error. Default 60000 (1 minute).");
	    
	    zdesktop_retry_delay_max = new KnownKey("zdesktop_retry_delay_max");
	    zdesktop_retry_delay_max.setDefault(Long.toString(10 * Constants.MILLIS_PER_MINUTE));
	    zdesktop_retry_delay_max.setDoc("Maximum delay in milliseconds to retry after becoming offline or encountering error. Default 600000 (10 minutes).");
	    
	    zdesktop_retry_limit = new KnownKey("zdesktop_retry_limit");
	    zdesktop_retry_limit.setDefault("2");
	    zdesktop_retry_limit.setDoc("Number of times to retry if sync fails. Default 2.");

	    zdesktop_gal_sync_trace_enabled = new KnownKey("zdesktop_gal_sync_trace_enabled");
	    zdesktop_gal_sync_trace_enabled.setDefault("false");
	    zdesktop_gal_sync_trace_enabled.setDoc("Whether to enable GAL sync trace logging. Default false");
	    
        zdesktop_gal_sync_interval_secs = new KnownKey("zdesktop_gal_sync_interval_seconds");
        zdesktop_gal_sync_interval_secs.setDefault("43200");
        zdesktop_gal_sync_interval_secs.setDoc("How often offline GAL is delta-sync'ed. Default every 12 hours.");

        zdesktop_gal_refresh_interval_days = new KnownKey("zdesktop_gal_refresh_interval_days");
        zdesktop_gal_refresh_interval_days.setDefault("30");
        zdesktop_gal_refresh_interval_days.setDoc("How often offline GAL is full-sync'ed. Default every 30 days.");
        
	    zdesktop_sync_batch_size = new KnownKey("zdesktop_sync_batch_size");
	    zdesktop_sync_batch_size.setDefault("25");
	    zdesktop_sync_batch_size.setDoc("Max number of messages to download in each transaction. Default 25.");
	    
	    zdesktop_sync_zip_level = new KnownKey("zdesktop_sync_zip_level");
	    zdesktop_sync_zip_level.setDefault("0");
	    zdesktop_sync_zip_level.setDoc("Zip compression level for batch message sync. Default 0 (NO_COMPRESSION).");
	    
	    zdesktop_sync_messages = new KnownKey("zdesktop_sync_messages");
	    zdesktop_sync_messages.setDefault("true");
	    zdesktop_sync_messages.setDoc("Whether to sync messages. Default true");
	    
	    zdesktop_sync_contacts = new KnownKey("zdesktop_sync_contacts");
	    zdesktop_sync_contacts.setDefault("true");
	    zdesktop_sync_contacts.setDoc("Whether to sync contacts. Default true");
	    
	    zdesktop_sync_appointments = new KnownKey("zdesktop_sync_appointments");
	    zdesktop_sync_appointments.setDefault("true");
	    zdesktop_sync_appointments.setDoc("Whether to sync appointments. Default true");
	    
	    zdesktop_sync_tasks = new KnownKey("zdesktop_sync_tasks");
	    zdesktop_sync_tasks.setDefault("true");
	    zdesktop_sync_tasks.setDoc("Whether to sync tasks. Default true");	    
	    
	    zdesktop_sync_chats = new KnownKey("zdesktop_sync_chats");
	    zdesktop_sync_chats.setDefault("true");
	    zdesktop_sync_chats.setDoc("Whether to sync chats. Default true");
	    
	    zdesktop_sync_documents = new KnownKey("zdesktop_sync_documents");
	    zdesktop_sync_documents.setDefault("true");
	    zdesktop_sync_documents.setDoc("Whether to sync documents. Default true");
	    
        zdesktop_sync_gal = new KnownKey("zdesktop_sync_gal");
        zdesktop_sync_gal.setDefault("true");
        zdesktop_sync_gal.setDoc("Whether to sync GAL. Default true");

        zdesktop_sync_mountpoints = new KnownKey("zdesktop_sync_mountpoints");
        zdesktop_sync_mountpoints.setDefault("true");
        zdesktop_sync_mountpoints.setDoc("Whether to sync mountpoints. Default true");
        
	    zdesktop_sync_skip_idlist = new KnownKey("zdesktop_sync_skip_idlist");
	    zdesktop_sync_skip_idlist.setDefault("");
	    zdesktop_sync_skip_idlist.setDoc("Comma delimited list of item IDs to skip during sync.  Default empty.");
	    
	    auth_token_lifetime = new KnownKey("auth_token_lifetime");
	    auth_token_lifetime.setDefault("31536000"); //365 * 24 * 3600
	    auth_token_lifetime.setDoc("Number of seconds before auth token expires. Default 31536000 (1 year).");
	    
	    dns_cache_ttl = new KnownKey("dns_cache_ttl");
	    dns_cache_ttl.setDefault("10");
	    dns_cache_ttl.setDoc("Number of seconds a resolved address stays valid");

	    zdesktop_request_timeout = new KnownKey("zdesktop_request_timeout");
	    zdesktop_request_timeout.setDefault("30000");
	    zdesktop_request_timeout.setDoc("HTTP request timeout in milliseconds while waiting for response. A value of zero means no timeout. Default 30000 (30 seconds).");

	    zdesktop_galsync_request_timeout = new KnownKey("zdesktop_galsync_request_timeout");
	    zdesktop_galsync_request_timeout.setDefault("60000");
	    zdesktop_galsync_request_timeout.setDoc("HTTP GAL sync request timeout in milliseconds while waiting for response. A value of zero means no timeout. Default 60 seconds.");
	        
	    http_so_timeout = new KnownKey("http_so_timeout");
	    http_so_timeout.setDefault("30000");
	    http_so_timeout.setDoc("Socket timeout (SO_TIMEOUT) in milliseconds while waiting for data. A value of zero means no timeout. Default 30000 (30 seconds).");

	    http_connection_timeout = new KnownKey("http_connection_timeout");
	    http_connection_timeout.setDefault("30000");
	    http_connection_timeout.setDoc("Timeout in milliseconds while waiting for connection to establish. A value of zero means no timeout. Default 30000 (30 seconds).");
	    
	    zdesktop_datasource_properties = new KnownKey("zdesktop_datasource_properties");
	    zdesktop_datasource_properties.setDefault("${zimbra_home}" + File.separator + "conf" + File.separator + "datasource.properties");
	    zdesktop_datasource_properties.setDoc("Path to datasource configuration properties file.");
	    
	    zdesktop_membuf_limit = new KnownKey("zdesktop_membuf_limit");
	    zdesktop_membuf_limit.setDefault("33554432"); //32 * 1024 * 1024
	    zdesktop_membuf_limit.setDoc("Number of bytes to hold in memory before start disk streaming during message sync.");
	    
	    zdesktop_upload_size_limit = new KnownKey("zdesktop_upload_size_limit");
	    zdesktop_upload_size_limit.setDefault("1073741824"); //1024 * 1024 * 1024
	    zdesktop_upload_size_limit.setDoc("Message size limit for uploading to server in number of bytes");
	    
	    zdesktop_yauth_appid = new KnownKey("zdesktop_yauth_appid");
	    zdesktop_yauth_appid.setDefault("0YbgbonAkY2iNypMZQOONB8mNDSJkrfBlr3wgxc-");
	    zdesktop_yauth_appid.setDoc("appid for yauth with rw access to ab and mail");
	    
	    zdesktop_yab_baseuri = new KnownKey("zdesktop_yab_baseuri");
	    zdesktop_yab_baseuri.setDefault("http://address.yahooapis.com/v1");
	    zdesktop_yab_baseuri.setDoc("base uri for yab");

        zdesktop_yab_sync_interval = new KnownKey("zdesktop_yab_sync_interval");
        zdesktop_yab_sync_interval.setDefault("60"); // 1 hour
        zdesktop_yab_sync_interval.setDoc("YAB sync interval in minutes");
	    
	    zdesktop_ymail_baseuri = new KnownKey("zdesktop_ymail_baseuri");
	    zdesktop_ymail_baseuri.setDefault("http://mail.yahooapis.com/ws/mail/v1.1/soap");
	    zdesktop_ymail_baseuri.setDoc("base uri for ymail");

        zdesktop_imap_fullsync_interval = new KnownKey("zdesktop_imap_fullsync_interval");
        zdesktop_imap_fullsync_interval.setDefault("60"); // 1 hour
        zdesktop_imap_fullsync_interval.setDoc("Max number of minutes between full sync of INBOX");
        
        zdesktop_ybizmail_smtp_host = new KnownKey("zdesktop_ybizmail_smtp_host");
        zdesktop_ybizmail_smtp_host.setDefault("smtp.bizmail.yahoo.com");
        zdesktop_ybizmail_smtp_host.setDoc("Yahoo bizmail SMTP host");

        zdesktop_ybizmail_smtp_port = new KnownKey("zdesktop_ybizmail_smtp_port");
        zdesktop_ybizmail_smtp_port.setDefault("465");
        zdesktop_ybizmail_smtp_port.setDoc("Yahoo bizmail SMTP port");

        zdesktop_ybizmail_smtp_ssl = new KnownKey("zdesktop_ybizmail_smtp_ssl");
        zdesktop_ybizmail_smtp_ssl.setDefault("true");
        zdesktop_ybizmail_smtp_ssl.setDoc("Yahoo bizmail SMTP SSL enabled");
	    
	    zdesktop_support_email = new KnownKey("zdesktop_support_email");
	    zdesktop_support_email.setDefault("zdesktop-report@zimbra.com");
	    zdesktop_support_email.setDoc("support email address");
	    
	    zdesktop_min_zcs_version_sync_tgz = new KnownKey("zdesktop_min_zcs_version_sync_tgz");
	    zdesktop_min_zcs_version_sync_tgz.setDefault("5.0.11");
	    zdesktop_min_zcs_version_sync_tgz.setDoc("min zcs server version to support tgz format for sync");
	    
	    zdesktop_caldav_enabled = new KnownKey("zdesktop_caldav_enabled", "false", "whether to enable caldav sync");

        zdesktop_gab_base_url = new KnownKey("zdesktop_gab_base_url");
        zdesktop_gab_base_url.setDefault("http://www.google.com/m8/feeds");
        zdesktop_gab_base_url.setDoc("Google contacts base url");
        
        zdesktop_log_context_filter = new KnownKey("zdesktop_log_context_filter", "name,aname,ip,ua", "comma delimited list of log context items to filter out");
        zdesktop_installation_key = new KnownKey("zdesktop_installation_key", null, "unique key of the installation instance");
    }
    
    public static String getFullVersion() {
    	return zdesktop_version.value() + "_" + zdesktop_buildid.value() + "_" + getOSShortName();
    }
    
    private static String getOSShortName() {
    	String os = System.getProperty("os.name");
    	int sp = os.indexOf(' ');
    	return sp > 0 ? os.substring(0, sp) : os;
    }
}
