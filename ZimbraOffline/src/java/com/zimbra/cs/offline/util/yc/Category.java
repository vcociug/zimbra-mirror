/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
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
package com.zimbra.cs.offline.util.yc;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import com.zimbra.cs.offline.util.Xml;

/**
 * Category reference by name or id.
 */
public final class Category extends Entity {
    private String name;
    private int id = -1;

    public static final String TAG = "category";
    
    private static final String CATID = "catid";

    public Category() {}

    public Category(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public Category(String name) {
        this.name = name;
    }

    public Category(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }

    public static Category fromXml(Element e) {
        Category cat = new Category();
        cat.parseXml(e);
        return cat;
    }

    @Override
    public Element toXml(Document doc) {
        return toXml(doc, TAG);
    }
    
    public Element toXml(Document doc, String tag) {
        Element e = doc.createElement(tag);
        if (id != -1) {
            e.setAttribute(CATID, String.valueOf(id));
        }
        if (name != null) {
            Xml.appendText(e, name);
        }
        return e;
    }

    private void parseXml(Element e) {
        id = Xml.getIntAttribute(e, CATID);
        name = Xml.getTextValue(e);
    }

    @Override
    public void extractFromXml(Element e) {
    }
}
