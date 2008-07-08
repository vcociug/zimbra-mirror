/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

ZmChatWindow = function(parent, chat, initialSize) {
        //console.time("ZmChatWindow");
	if (arguments.length == 0) return;
	DwtResizableWindow.call(this, parent);
	if (!initialSize)
		initialSize = { x: 350, y: 250 };
	this._init(chat, initialSize);
        //console.timeEnd("ZmChatWindow");
};

ZmChatWindow.prototype = new DwtResizableWindow;
ZmChatWindow.prototype.constructor = ZmChatWindow;

ZmChatWindow.prototype.toString =
function() {
    return "ZmChatWindow";
};

ZmChatWindow.prototype._init = function(chat, initialSize) {
	var tabs = this._tabs = new ZmChatTabs(this);
	tabs.addDisposeListener(new AjxListener(this, this._tabsDisposeListener));
	this.setView(tabs);
	tabs.addTab(chat);
	this.setSize(initialSize.x, initialSize.y);
	this.setMinSize(300, 100);
	this.setMinPos(0, 0);
	tabs = null;
	this.addSelectionListener(new AjxListener(this, this._selectionListener));
	this.addFocusListener(new AjxListener(this, function() {
		this._tabs.getCurrentChatWidget().focus();
	}));
};

ZmChatWindow.prototype.select = function() {
	return this.setActive(true);
};

ZmChatWindow.prototype.getCurrentChatWidget = function() {
	return this._tabs.getCurrentChatWidget();
};

ZmChatWindow.prototype.addTab = function(chat) {
	return this._tabs.addTab(chat);
};

ZmChatWindow.prototype.minimize =
function(minimize) {
	if (minimize == this._minimized) {
		return;
	}
	DwtResizableWindow.prototype.minimize.call(this, minimize);
	this._tabs.setTabsVisible(!minimize);
	this.getCurrentChatWidget()._onMinimize(minimize);
};

ZmChatWindow.prototype._getMinimizedSize =
function() {
	return this.getCurrentChatWidget().getMinimizedSize();
};

ZmChatWindow.prototype._selectionListener = function(ev) {
	if (ev.detail) {
		this._tabs.getCurrentChatWidget().focus();
	}
};

ZmChatWindow.prototype._tabsDisposeListener = function() {
	this.dispose();
};
