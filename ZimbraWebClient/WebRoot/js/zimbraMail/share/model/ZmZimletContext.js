/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

function ZmZimletContext(id, zimlet, appCtxt) {
	this._appCtxt = appCtxt;
	// sanitize JSON here
	this.json = ZmZimletContext.sanitize(zimlet, "zimlet", ZmZimletContext.RE_ARRAY_ELEMENTS);

	this.id = id;
	this.icon = "ZimbraIcon";
	this.ctxt = zimlet.zimletContext;
	this.config = zimlet.zimletConfig;
	zimlet = zimlet.zimlet[0];
	this.name = zimlet.name;
	DBG.println(AjxDebug.DBG2, "Zimlets - context: " + this.name);
	this._url = this.ctxt[0].baseUrl;
	this.priority = this.ctxt[0].priority;
	this.description = zimlet.description;
	this.version = zimlet.version;
	this.includes = this.json.zimlet.include;
	this.includes = this.includes ? this.includes : [];
	this.includes.push("/zimbra/js/msgs/" + this.name + ".js");
	this.includeCSS = this.json.zimlet.includeCSS;
	if(zimlet.serverExtension && zimlet.serverExtension[0].hasKeyword){
		this.keyword = zimlet.serverExtension[0].hasKeyword;
	}

	this._contentActionMenu = null;
	if(zimlet.contentObject){
		this.contentObject = zimlet.contentObject[0];
	}

	this._panelActionMenu = null;
	if(zimlet.zimletPanelItem){
		this.zimletPanelItem = zimlet.zimletPanelItem[0];
		if (this.zimletPanelItem.toolTipText) {
			this.zimletPanelItem.toolTipText = this.zimletPanelItem.toolTipText[0]._content;
		}
		if (this.zimletPanelItem.icon) {
			this.icon = this.zimletPanelItem.icon;
		}
		if (this.zimletPanelItem.contextMenu) {
			this.zimletPanelItem.contextMenu = this.zimletPanelItem.contextMenu[0];
			this._panelActionMenu = new AjxCallback(
				this, this._makeMenu,
				[ this.zimletPanelItem.contextMenu.menuItem ]);
		}
		if (this.zimletPanelItem.onClick) {
			this.zimletPanelItem.onClick = this.zimletPanelItem.onClick[0];
		}
		if (this.zimletPanelItem.onDoubleClick) {
			this.zimletPanelItem.onDoubleClick = this.zimletPanelItem.onDoubleClick[0];
		}
	}
	if(zimlet.handlerObject) {
		this.handlerObject = zimlet.handlerObject[0]._content;
	}
	if(zimlet.userProperties) {
		this.userProperties = zimlet.userProperties[0];
		this._translateUserProp();
	}
	if(this.config) {
		this.config = this.config[0];
		this._translateConfig();
	}

	this._loadIncludes();
	this._loadStyles();

	this._handleMenuItemSelected = new AjxListener(this, this._handleMenuItemSelected);
}

ZmZimletContext.RE_ARRAY_ELEMENTS = /^(dragSource|include|includeCSS|menuItem|param|property|resource)$/;

/** This function creates a 'sane' JSON object, given one returned by the
 * Zimbra server.
 *
 * It will basically remove unnecessary arrays and create String objects for
 * those tags that have text data, so that we don't need to dereference lots of
 * arrays and use _content.  It does the job that the server should do.  *grin*
 *
 * BIG FAT WARNING: usage of an attribute named "length" may give weird
 * results, since we convert tags that have text content to Strings.
 *
 * @param obj -- array or object, whatever was given by server
 * @param tag -- the tag of this object, if it's an array
 * @param wantarray_re -- RegExp that matches tags that must remain an array
 *
 * @return -- sanitized object
 */
ZmZimletContext.sanitize = 
function(obj, tag, wantarray_re) {
	function doit(obj, tag) {
		var cool_json, val, i;
		if (obj instanceof Array) {
			if (obj.length == 1 && !(wantarray_re && wantarray_re.test(tag))) {
				cool_json = doit(obj[0], tag);
			} else {
				cool_json = [];
				for (i = 0; i < obj.length; ++i) {
					cool_json[i] = doit(obj[i], tag);
				}
			}
		} else if (typeof obj == "object") {
			if (obj._content) {
				cool_json = new String(obj._content); 
			} else {
				cool_json = {};
			}
			for (i in obj) {
				cool_json[i] = doit(obj[i], i);
			}
		} else {
			cool_json = obj;
		}
		return cool_json;
	}
	return doit(obj, tag);
};

ZmZimletContext.prototype.constructor = ZmZimletContext;

ZmZimletContext.prototype.toString =
function() {
	return "ZmZimletContext - " + this.name;
};

ZmZimletContext.prototype._loadIncludes =
function() {
	if (!this.includes) {
		this._finished_loadIncludes();
		return;
	}
	AjxInclude(this.includes, this._url, new AjxCallback(this, this._finished_loadIncludes) ,ZmZimletBase.PROXY);
};

ZmZimletContext.prototype._finished_loadIncludes = function() {
	// We don't allow _loadIncludes a second time
	this.includes = null;
	var CTOR  = this.handlerObject ? window[this.handlerObject] : ZmZimletBase;
	this.handlerObject = new CTOR();
	if(!this.handlerObject._init) {
		DBG.println(AjxDebug.DBG1, "ERROR - Zimlet handler (" + this.name + ") not defined. " +
		    "Make sure the Zimlet name and handlerObject defined in " + this.name + ".xml are different.");
	}
	this.handlerObject._init(this, DwtShell.getShell(window));
	if (this.contentObject) {
		this._appCtxt._settings._zmm.registerContentZimlet(this.handlerObject, this.type, this.priority);
	}
	this.handlerObject.init();
	this.handlerObject._zimletContext = this;
	// If it has an _id then we need to make sure the treeItem
	// is up-to-date now that the i18n files have loaded.
	if(this._id) {
		var tree = this._appCtxt.getTree(ZmOrganizer.ZIMLET);
		var zimletItem = tree.getById(this._id);
		zimletItem.resetNames();
	}
	DBG.println(AjxDebug.DBG2, "Zimlets - init() complete: " + this.name);
};

ZmZimletContext.prototype._loadStyles = function() {
	if (!this.includeCSS) {return;}
	var head = document.getElementsByTagName("head")[0];
	for (var i = 0; i < this.includeCSS.length; ++i) {
		var fullurl = this.includeCSS[i];
		if (!(/^((https?|ftps?):\x2f\x2f|\x2f)/).test(fullurl)) {
			fullurl = this._url + fullurl;
		}
		var style = document.createElement("link");
		style.type = "text/css";
		style.rel = "stylesheet";
		style.href = fullurl;
		style.title = this.name + " " + this.includeCSS[i];
		head.appendChild(style);
		style.disabled = true;
		style.disabled = false;
	}
	this.includeCSS = null;
};

ZmZimletContext.prototype.getOrganizer = function() {
	// this._organizer is a ZmZimlet and is set in ZmZimlet.createFromJs
	return this._organizer;
};

ZmZimletContext.prototype.getUrl = function() { return this._url; };

ZmZimletContext.prototype.getVal = function(key) {
	var zimlet = this.json.zimlet;
	return eval("zimlet." + key);
};

ZmZimletContext.prototype.callHandler = function(funcname, args) {
	if (this.handlerObject) {
		var f = this.handlerObject[funcname];
		if (typeof f == "function") {
			if (typeof args == "undefined") {
				args = [];
			}
			if (!(args instanceof Array)) {
				args = [ args ];
			}
			return f.apply(this.handlerObject, args);
		}
	}
};

ZmZimletContext.prototype._translateUserProp = function() {
	var a = this.userProperties = this.userProperties.property;
	this._propsById = {};
	for (var i = 0; i < a.length; ++i) {
		this._propsById[a[i].name] = a[i];
	}
};

ZmZimletContext.prototype.setPropValue = function(name, val) {
	if(this._propsById[name]) {
		this._propsById[name].value = val;
	}
};

ZmZimletContext.prototype.getPropValue = function(name) {
	if(this._propsById[name]) {
		return this._propsById[name].value;
	}
};

ZmZimletContext.prototype.getProp = function(name) {
	return this._propsById[name];
};

ZmZimletContext.prototype._translateConfig = function() {
	if (this.config.global) {
		var prop = this.config.global[0].property;
		this.config.global = {};
		for (var i = 0; i < prop.length; i++) {
			this.config.global[prop[i].name] = prop[i]._content;
		}
	}
	if (this.config.local) {
		var propLocal = this.config.local[0].property;
		this.config.local = {};
		for (var j = 0; j < propLocal.length; j++) {
			this.config.local[propLocal[j].name] = propLocal[j]._content;
		}
	}
};

ZmZimletContext.prototype.getConfig = function(name) {
	if (this.config.local && this.config.local[name]) {
		return this.config.local[name];
	}
	if (this.config.global && this.config.global[name]) {
		return this.config.global[name];
	}
	return undef;
};

ZmZimletContext.prototype.getPanelActionMenu = function() {
	if (this._panelActionMenu instanceof AjxCallback) {
		this._panelActionMenu = this._panelActionMenu.run();
	}
	return this._panelActionMenu;
};

ZmZimletContext.prototype._makeMenu = function(obj) {
	var menu = new ZmPopupMenu(DwtShell.getShell(window));
	for (var i = 0; i < obj.length; ++i) {
		var data = obj[i];
		if (!data.id) {
			menu.createSeparator();
		} else {
			var item = menu.createMenuItem(data.id, data.icon, this.processMessage(data.label),
						       data.disabledIcon, true);
			item.setData("xmlMenuItem", data);
			item.addSelectionListener(this._handleMenuItemSelected);
		}
	}
	return menu;
};

ZmZimletContext.prototype._handleMenuItemSelected = function(ev) {
	var data = ev.item.getData("xmlMenuItem");
	if (data.actionUrl) {
		this.handleActionUrl(data.actionUrl[0], data.canvas);
	} else {
		this.callHandler("menuItemSelected", [ data.id, data ]);
	}
};

ZmZimletContext.RE_SCAN_OBJ = /(^|[^\\])\$\{(?:obj|src)\.([\$a-zA-Z0-9_]+)\}/g;
ZmZimletContext.RE_SCAN_PROP = /(^|[^\\])\$\{prop\.([\$a-zA-Z0-9_]+)\}/g;
ZmZimletContext.RE_SCAN_MSG = /(^|[^\\])\$\{msg\.([\$a-zA-Z0-9_]+)\}/g;

ZmZimletContext.prototype.processString = function(str, obj) {
	return this.replaceObj(ZmZimletContext.RE_SCAN_OBJ, str, obj);
};

ZmZimletContext.prototype.processMessage = function(str) {
	// i18n files load async so if not defined skip translation
	if(!window[this.name]) {
		DBG.println(AjxDebug.DBG2, "processMessage no messages: " + str);
		return str;
	}
	var props = window[this.name];
	return this.replaceObj(ZmZimletContext.RE_SCAN_MSG, str, props);
};

ZmZimletContext.prototype.replaceObj = function(re, str, obj) {
	return str.replace(re,
		function(str, p1, prop) {
			var txt = p1;
			if (obj instanceof Array && obj.length > 1) {
				for(var i=0; i < obj.length; i++) {
					if(txt) {txt += ",";}
					var o = obj[i];
					if (o[prop] instanceof Object) {
						txt += o[prop].value;  // user prop
					} else {
						txt += o[prop];   // string
					}
				}
			} else {
				if (typeof obj[prop] != "undefined") {
					if (obj[prop] instanceof Object) {
						txt += obj[prop].value;  // user prop
					} else {
						txt += obj[prop];   // string
					}
				} else {
					txt += "(UNDEFINED - str '" + str + "' obj '" + obj + "')";
				}
			}
			return txt;
		});
};

ZmZimletContext.prototype.makeURL = function(actionUrl, obj) {
	var url = actionUrl.target;
	var param = [];
	if (actionUrl.param) {
		var a = actionUrl.param;
		for (var i = 0; i < a.length; ++i) {
			// trim whitespace as it's almost certain that the
			// developer didn't intend it.
			var val = AjxStringUtil.trim(a[i]._content);
			if (obj) {
				val = this.processString(val, obj);
			}
			val = this.replaceObj(ZmZimletContext.RE_SCAN_PROP, val, this._propsById);
			param.push([ AjxStringUtil.urlEncode(a[i].name),
				     "=",
				     AjxStringUtil.urlEncode(val) ].join(""));
		}
		var startChar = actionUrl.paramStart || '?';
		var joinChar = actionUrl.paramJoin || '&';
		url = [ url, startChar, param.join(joinChar) ].join("");
	}
	return url;
};

/**
* if there already is a paintable canvas to use, as in the case of tooltip,
* pass it to 'div' parameter.  otherwise a canvas (window, popup, dialog) will be created
* to display the contents from the url.
*/
ZmZimletContext.prototype.handleActionUrl = function(actionUrl, canvas, obj, div) {
	var url = this.makeURL(actionUrl, obj);
	var xslt = null;

	if (actionUrl.xslt) {
		xslt = this.getXslt(actionUrl.xslt);
	}
	
	// need to use callback if the paintable canvas already exists, or if it needs xslt transformation.
	if (div || xslt) {
		if (!div) {
			canvas = this.handlerObject.makeCanvas(canvas[0], null);
			div = document.getElementById("zimletCanvasDiv");
		}
		url = ZmZimletBase.PROXY + AjxStringUtil.urlEncode(url);
		AjxRpc.invoke(null, url, null, new AjxCallback(this, this._rpcCallback, [xslt, div]), true);
	} else {
		this.handlerObject.makeCanvas(canvas[0], url);
	}
};

ZmZimletContext._translateZMObject = function(obj) {
	// XXX Assumes all dragged objects are of the same type
	var type = obj[0] ? obj[0].toString() : obj.toString();
	if (ZmZimletContext._zmObjectTransformers[type]) {
		return ZmZimletContext._zmObjectTransformers[type](obj);
	} else {
		return obj;
	}
};

ZmZimletContext._zmObjectTransformers = {

	"ZmMailMsg" : function(o) {
		var all = [];
		o = (o instanceof Array) ? o : [o];
		for(var i=0; i< o.length; i++) {
			var ret = { TYPE: "ZmMailMsg" };
			var oi = o[i];
			ret.id           = oi.getId();
			ret.convId       = oi.getConvId();
			ret.from         = oi.getAddresses(ZmEmailAddress.FROM).getArray();
			ret.to           = oi.getAddresses(ZmEmailAddress.TO).getArray();
			ret.cc           = oi.getAddresses(ZmEmailAddress.CC).getArray();
			ret.subject      = oi.getSubject();
			ret.date         = oi.getDate();
			ret.size         = oi.getSize();
			ret.fragment     = oi.fragment;
			ret.tags         = oi.tags;
			// ret.flagged      = oi.getFlagged();
			ret.unread       = oi.isUnread;
			ret.attachment   = oi._attachments.length > 0;
			// ret.forwarded      = oi.isForwarded();
			ret.sent         = oi.isSent;
			ret.replied      = oi.isReplied;
			ret.draft        = oi.isDraft;
			ret.body		 = ZmZimletContext._getMsgBody(oi);
			all[i] = ret;
		}
		if(all.length == 1) {
			return all[0];
		} else {
			all["TYPE"] = "ZmMailMsg";
			return all;
		}
	},

	"ZmConv" : function(o) {
		var all = [];
		o = (o instanceof Array) ? o : [o];
		for(var i=0; i< o.length; i++) {
			var oi = o[i];
			var ret = { TYPE: "ZmConv" };
			ret.id           = oi.id;
			ret.subject      = oi.getSubject();
			ret.date         = oi.date;
			ret.fragment     = oi.fragment;
			ret.participants = oi.participants.getArray();
			ret.numMsgs      = oi.numMsgs;
			ret.tags         = oi.tags;
			// ret.flagged      = oi.getFlagged();
			ret.unread       = oi.isUnread;
			// ret.attachment   = oi._attachments ?;
			// ret.sent         = oi.isSent;
			
			// Use first message... maybe should be getHotMsg()?
			ret.body         = ZmZimletContext._getMsgBody(oi.getFirstMsg());
			all[i] = ret;
		}
		if(all.length == 1) {
			return all[0];
		} else {
			all["TYPE"] = "ZmConv";
			return all;
		}
	},

	ZmContact_fields : function() {
		return [
			ZmContact.F_assistantPhone,
			ZmContact.F_callbackPhone,
			ZmContact.F_carPhone,
			ZmContact.F_company,
			ZmContact.F_companyPhone,
			ZmContact.F_email,
			ZmContact.F_email2,
			ZmContact.F_email3,
			ZmContact.F_fileAs,
			ZmContact.F_firstName,
			ZmContact.F_homeCity,
			ZmContact.F_homeCountry,
			ZmContact.F_homeFax,
			ZmContact.F_homePhone,
			ZmContact.F_homePhone2,
			ZmContact.F_homePostalCode,
			ZmContact.F_homeState,
			ZmContact.F_homeStreet,
			ZmContact.F_homeURL,
			ZmContact.F_jobTitle,
			ZmContact.F_lastName,
			ZmContact.F_middleName,
			ZmContact.F_mobilePhone,
			ZmContact.F_namePrefix,
			ZmContact.F_nameSuffix,
			ZmContact.F_notes,
			ZmContact.F_otherCity,
			ZmContact.F_otherCountry,
			ZmContact.F_otherFax,
			ZmContact.F_otherPhone,
			ZmContact.F_otherPostalCode,
			ZmContact.F_otherState,
			ZmContact.F_otherStreet,
			ZmContact.F_otherURL,
			ZmContact.F_pager,
			ZmContact.F_workCity,
			ZmContact.F_workCountry,
			ZmContact.F_workFax,
			ZmContact.F_workPhone,
			ZmContact.F_workPhone2,
			ZmContact.F_workPostalCode,
			ZmContact.F_workState,
			ZmContact.F_workStreet,
			ZmContact.F_workURL
			];
	},

	"ZmContact" : function(o) {
		var all = new Array();
		for(var i=0; i< o.length; i++) {
			var ret = { TYPE: "ZmContact" };
			var a = this.ZmContact_fields;
			if (typeof a == "function") {
				a = this.ZmContact_fields = a();
			}
			var attr = o[i].getAttrs();
			for (var j = 0; j < a.length; ++j) {
				ret[a[j]] = attr[a[j]];
			}
			ret.id = o[i].id;
			all[i] = ret;
		}
		if(all.length == 1) {
			return all[0];
		} else {
			all["TYPE"] = "ZmContact";
			return all;
		}
	},
	
	"ZmFolder" : function(o) {
		var oi = o[0] ? o[0] : o;
		var ret = { TYPE: "ZmFolder" };
		ret.id           = oi.id;
		ret.name         = oi.getName();
		ret.path         = oi.getPath();
		ret.isInTrash    = oi.isInTrash();
		ret.unread       = oi.numUnread;
		ret.total        = oi.numTotal;
		ret.url          = oi.url;
		return ret;
	},

	"ZmAppt" : function(o) {
		var oi = o[0] ? o[0] : o;
		oi.getDetails();
		var ret = { TYPE: "ZmAppt" };
		ret.id             = oi.getId();
		ret.uid            = oi.getUid();
		ret.type           = oi.getType();
		ret.subject        = oi.getName();
		ret.startDate      = oi.getStartDate();
		ret.endDate        = oi.getEndDate();
		ret.allDayEvent    = oi.isAllDayEvent();
		ret.exception      = oi.isException();
		ret.alarm          = oi.hasAlarm();
		ret.otherAttendees = oi.hasOtherAttendees();
		ret.attendees      = oi.getAttendees();
		ret.location       = oi.getLocation();
		ret.notes          = oi.getNotesPart();
		ret.isRecurring    = oi.isRecurring();
		ret.timeZone       = oi.getTimezone();
		return ret;
	}

};

ZmZimletContext.prototype.getXslt =
function(url) {
	if (!this._xslt) {
		this._xslt = {};
	}
	var realurl = this.getUrl() + url;
	if (!this._xslt[realurl]) {
		this._xslt[realurl] = AjxXslt.createFromUrl(realurl);
	}
	return this._xslt[realurl];
};

ZmZimletContext.prototype._rpcCallback =
function(xslt, canvas, result) {
	var html, resp = result.xml;
	if (!resp) {
		var doc = AjxXmlDoc.createFromXml(result.text);
		resp = doc.getDoc();
	}
	// TODO:  instead of changing innerHTML, maybe append
	// the dom tree to the canvas.
	if (xslt) {
		html = xslt.transformToString(resp);
	} else {
		html = resp.innerHTML;
	}
	canvas.innerHTML = html;
};

ZmZimletContext._getMsgBody = function(o) {
	var body = o.getTextPart();
	if (!body && o.getBodyPart(ZmMimeTable.TEXT_HTML)) {	
		var div = document.createElement("div");
		div.innerHTML = o.getBodyPart(ZmMimeTable.TEXT_HTML).content;
		body = AjxStringUtil.convertHtml2Text(div);
	}
	return body;
};
