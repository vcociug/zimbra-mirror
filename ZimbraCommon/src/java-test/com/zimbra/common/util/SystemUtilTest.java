/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013 Zimbra Software, LLC.
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

package com.zimbra.common.util;

import org.junit.Assert;
import org.junit.Test;

public class SystemUtilTest {

    @Test
    public void coalesce() {
        Assert.assertEquals(1, (int) SystemUtil.coalesce(null, 1));
        Assert.assertEquals(null, SystemUtil.coalesce(null, null, null, null));
        Assert.assertEquals(2, (int) SystemUtil.coalesce(2, 3));
        Assert.assertEquals("good", SystemUtil.coalesce("good", "bad", "ugly"));
    }
}
