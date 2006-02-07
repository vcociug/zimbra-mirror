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

function com_zimbra_xslt() {
}

com_zimbra_xslt.prototype = new ZmZimletBase;
com_zimbra_xslt.prototype.constructor = com_zimbra_xslt;

com_zimbra_xslt.registerService =
function(service) {
	if (service && service.xsl && service.getRequest && service.id) {
		if (!com_zimbra_xslt.prototype.services) {
			com_zimbra_xslt.prototype.services = new Object();
		}
		com_zimbra_xslt.prototype.services[service.id] = service;
	}
};

com_zimbra_xslt.prototype.init =
function() {
	var ctxt = this.xmlObj();
	if (!ctxt._panelActionMenu) {
		ctxt._panelActionMenu = new ZmPopupMenu(DwtShell.getShell(window));
	}
	var menu = ctxt._panelActionMenu;
	for (var sid in this.services) {
		var service = this.services[sid];
		var item = menu.createMenuItem(service.id, service.icon, service.label,
								       service.disabledIcon, true);
		item.setData("xmlMenuItem", service);
		item.addSelectionListener(ctxt._handleMenuItemSelected);
		try {
			service.processor = AjxXslt.createFromUrl(this.getResource(service.xsl));
		} catch (ex) {
			DBG.println(AjxDebug.DBG1, ex.dump());
			return;
		}
	}
};

com_zimbra_xslt.prototype.buttonListener =
function(ev) {
	var el = document.getElementById(this._subjectId);
	var q = el.value;
	var canvas = document.getElementById(this._canvasId);

	var service = this.services[this._query];
	var ctxt = new Object();
	this.ctxt = ctxt;
	var ret = service.getRequest(this, q);
	
	var url = ZmZimletBase.PROXY + AjxStringUtil.urlEncode(ret.url);
	if (ret.req) {
		AjxRpc.invoke(ret.req, url, {"Content-Type": "text/xml"}, new AjxCallback(this, this.callback, [ canvas, service ]), false);
	} else {
		AjxRpc.invoke(null, url, null, new AjxCallback(this, this.callback, [ canvas, service ]), true);
	}
};

com_zimbra_xslt.prototype.menuItemSelected =
function(contextMenu, menuItemId, spanElement, contentObjText, canvas) {
	this._query = menuItemId.id;
	var view = new DwtComposite(this.getShell());
	var el = view.getHtmlElement();
	var div = document.createElement("div");
	var subjectId = Dwt.getNextId();
	var canvasId = Dwt.getNextId();
	
	div.innerHTML =
		[ "<table><tbody>",
		  "<tr>",
		  "<td align='right'><label for='", subjectId, "'>Search:</td>",
		  "<td>",
		  "<input autocomplete='off' style='width: 21em' type='text' id='", subjectId, "' value=''/>",
		  "</td>",
		  "</tr>",
		  "<td colspan='2'>",
		  "<div style='width:500px; height:500px; overflow:scroll' id='", canvasId, "'/>",
		  "</td>",
		  "<tr>",
		  "</tr></tbody></table>" ].join("");
	el.appendChild(div);

	var dialog_args = {
		title : menuItemId.label,
		view  : view
	};
	var dlg = this._createDialog(dialog_args);
	dlg.popup();

	el = document.getElementById(subjectId);
	el.select();
	el.focus();

	this._subjectId = subjectId;
	this._canvasId = canvasId;
	
	dlg.setButtonListener(DwtDialog.OK_BUTTON,
		      new AjxListener(this, this.buttonListener));

	dlg.setButtonListener(DwtDialog.CANCEL_BUTTON,
		      new AjxListener(this, function() {
			      dlg.popdown();
			      dlg.dispose();
		      }));
};

com_zimbra_xslt.prototype.getSanitizedDocFromHtml =
function(text) {
	text = text ? text.replace(/&nbsp;/g," ").replace(/&reg;/g,"(R)") : "";
	var doc = AjxXmlDoc.createFromXml(text);
	return doc.getDoc();
};

com_zimbra_xslt.prototype.callback =
function(canvas, service, result) {
	var html, resp;
	var processor = service.processor;
	
	if (!result.success) {
		canvas.innerHTML = "<div><b>Web service returned error.</b></div>"+result.text;
		return;
	}
	
	try {
		if (!result.xml || !result.xml.documentElement) {
			resp = this.getSanitizedDocFromHtml(result.text);
		} else {
			resp = result.xml;
		}
		html = processor.transformToString(resp);
	} catch (ex) {
		DBG.println(AjxDebug.DBG1, ex.dump());
		canvas.innerHTML = "<div><b>Transformation resulted in error.</b></div>";
		return;
	}
	
	//DBG.println(AjxDebug.DBG1, "*********"+AjxStringUtil.htmlEncode(html)+"************");
	html = html ? html.replace(/&gt;/g,">").replace(/&lt;/g,"<").replace(/&quot;/g, '"').replace(/&apos;/g,"'") : "";
	canvas.innerHTML = html;
};

