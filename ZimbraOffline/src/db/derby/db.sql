-- 
# ***** BEGIN LICENSE BLOCK *****
# 
# Zimbra Collaboration Suite Server
# Copyright (C) 2007 Zimbra, Inc.
# 
# The contents of this file are subject to the Yahoo! Public License
# Version 1.0 ("License"); you may not use this file except in
# compliance with the License.  You may obtain a copy of the License at
# http://www.zimbra.com/license.
# 
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
# 
# ***** END LICENSE BLOCK *****
-- 

CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.storage.pageSize', '16384');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.storage.pageCacheSize', '1000');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.language.logQueryPlan', 'true');
CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.language.logStatementText', 'true');

CREATE SCHEMA zimbra;
SET SCHEMA zimbra;

-- -----------------------------------------------------------------------
-- volumes
-- -----------------------------------------------------------------------

-- list of known volumes
CREATE TABLE volume (
   id                     SMALLINT NOT NULL GENERATED BY DEFAULT AS IDENTITY,
   type                   SMALLINT NOT NULL,      -- 1 = primary msg, 2 = secondary msg, 10 = index
   name                   VARCHAR(255) NOT NULL,
   path                   VARCHAR(32672) NOT NULL,
   file_bits              SMALLINT NOT NULL,
   file_group_bits        SMALLINT NOT NULL,
   mailbox_bits           SMALLINT NOT NULL,
   mailbox_group_bits     SMALLINT NOT NULL,
   compress_blobs         SMALLINT NOT NULL,
   compression_threshold  BIGINT NOT NULL,

   CONSTRAINT pk_volume PRIMARY KEY (id),
   CONSTRAINT ui_volume_name UNIQUE (name),
   CONSTRAINT ui_volume_path UNIQUE (path)
);


-- This table has only one row.  It points to message and index volumes
-- to use for newly provisioned mailboxes.
CREATE TABLE current_volumes (
   message_volume_id            SMALLINT NOT NULL,
   secondary_message_volume_id  SMALLINT,
   index_volume_id              SMALLINT NOT NULL,
   next_mailbox_id              INTEGER NOT NULL,

   CONSTRAINT pk_current_volumes_message_volume_id PRIMARY KEY (message_volume_id),
   CONSTRAINT fk_current_volumes_message_volume_id FOREIGN KEY (message_volume_id) REFERENCES volume(id),
   CONSTRAINT fk_current_volumes_secondary_message_volume_id FOREIGN KEY (secondary_message_volume_id) REFERENCES volume(id),
   CONSTRAINT fk_current_volumes_index_volume_id FOREIGN KEY (index_volume_id) REFERENCES volume(id)
);

CREATE INDEX i_message_volume_id ON current_volumes(message_volume_id);
CREATE INDEX i_secondary_message_volume_id ON current_volumes(secondary_message_volume_id);
CREATE INDEX i_index_volume_id ON current_volumes(index_volume_id);


-- -----------------------------------------------------------------------
-- mailbox info
-- -----------------------------------------------------------------------

CREATE TABLE mailbox (
   id                 INTEGER NOT NULL,
   group_id           INTEGER NOT NULL,           -- mailbox group
   account_id         CHAR(36) NOT NULL,          -- e.g. "d94e42c4-1636-11d9-b904-4dd689d02402"
   index_volume_id    SMALLINT NOT NULL,
   item_id_checkpoint INTEGER NOT NULL DEFAULT 0,
   contact_count      INTEGER DEFAULT 0,
   size_checkpoint    BIGINT NOT NULL DEFAULT 0,
   change_checkpoint  INTEGER NOT NULL DEFAULT 0,
   tracking_sync      INTEGER NOT NULL DEFAULT 0,
   tracking_imap      SMALLINT NOT NULL DEFAULT 0,
   last_backup_at     INTEGER,                    -- last full backup time, UNIX-style timestamp
   comment            VARCHAR(255),               -- usually the main email address originally associated with the mailbox
   last_soap_access   INTEGER NOT NULL DEFAULT 0,
   new_messages       INTEGER NOT NULL DEFAULT 0,

   CONSTRAINT pk_mailbox PRIMARY KEY (id),
   CONSTRAINT ui_mailbox_account_id UNIQUE (account_id),
   CONSTRAINT fk_mailbox_index_volume_id FOREIGN KEY (index_volume_id) REFERENCES volume(id)
);

CREATE INDEX i_mailbox_index_volume_id ON mailbox(index_volume_id);
CREATE INDEX i_last_backup_at ON mailbox(last_backup_at, id);

-- -----------------------------------------------------------------------
-- deleted accounts
-- -----------------------------------------------------------------------

CREATE TABLE deleted_account (
    email VARCHAR(255) NOT NULL,
    account_id CHAR(36) NOT NULL,
    mailbox_id INTEGER NOT NULL,
    deleted_at INTEGER NOT NULL,      -- UNIX-style timestamp
   
    CONSTRAINT pk_deleted_account PRIMARY KEY (email)
);

-- -----------------------------------------------------------------------
-- mailbox metadata info
-- -----------------------------------------------------------------------

CREATE TABLE mailbox_metadata (
   mailbox_id  INTEGER NOT NULL,
   section     VARCHAR(64) NOT NULL,       -- e.g. "imap"
   metadata    CLOB,

   CONSTRAINT pk_metadata PRIMARY KEY (mailbox_id, section),
   CONSTRAINT fk_metadata_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE
);


-- -----------------------------------------------------------------------
-- out-of-office reply history
-- -----------------------------------------------------------------------

CREATE TABLE out_of_office (
  mailbox_id  INTEGER NOT NULL,
  sent_to     VARCHAR(255) NOT NULL,
  sent_on     TIMESTAMP NOT NULL,

  CONSTRAINT pk_out_of_office PRIMARY KEY (mailbox_id, sent_to),
  CONSTRAINT fk_out_of_office_mailbox_id FOREIGN KEY (mailbox_id) REFERENCES mailbox(id) ON DELETE CASCADE
);

CREATE INDEX i_out_of_office_sent_on ON out_of_office(sent_on);


-- -----------------------------------------------------------------------
-- etc.
-- -----------------------------------------------------------------------

-- table for global config params
CREATE TABLE config (
  name         VARCHAR(255) NOT NULL,
  value        CLOB,
  description  CLOB,
  modified     TIMESTAMP,

  CONSTRAINT pk_config PRIMARY KEY (name)
);

-- Tracks scheduled tasks
CREATE TABLE scheduled_task (
   class_name      VARCHAR(255) NOT NULL,
   name            VARCHAR(255) NOT NULL,
   mailbox_id      INTEGER NOT NULL,
   exec_time       TIMESTAMP,
   interval_millis INTEGER,
   metadata        CLOB,

   CONSTRAINT pk_scheduled_task PRIMARY KEY (name, mailbox_id, class_name),
   CONSTRAINT fk_st_mailbox_id FOREIGN KEY (mailbox_id)
      REFERENCES mailbox(id) ON DELETE CASCADE
);

CREATE INDEX i_scheduled_task_mailbox_id ON scheduled_task(mailbox_id);
