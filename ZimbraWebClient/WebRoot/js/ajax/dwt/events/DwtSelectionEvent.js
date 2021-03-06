/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013 Zimbra Software, LLC.
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

/**
 * 
 * 
 * @private
 */
DwtSelectionEvent = function(init) {
	if (arguments.length == 0) return;
	DwtUiEvent.call(this, true);
	this.reset(true);
}

DwtSelectionEvent.prototype = new DwtUiEvent;
DwtSelectionEvent.prototype.constructor = DwtSelectionEvent;

DwtSelectionEvent.prototype.toString = 
function() {
	return "DwtSelectionEvent";
}

DwtSelectionEvent.prototype.reset =
function(dontCallParent) {
	if (!dontCallParent)
		DwtUiEvent.prototype.reset.call(this);
	this.button = 0;
	this.detail = null;
	this.item = null;
}

