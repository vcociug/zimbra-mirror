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
 * The Original Code is: Zimbra Collaboration Suite Web Client
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
* Creates an empty contact list controller.
* @constructor
* @class
*
* @param appCtxt		app context
* @param container		containing shell
* @param contactsApp	containing app
*/
function ZmChatListController(appCtxt, container, imApp) {
    if (arguments.length == 0) return;

	ZmController.call(this, appCtxt, container, imApp);

	this._toolbar = new Object;		// ZmButtonToolbar (one per view)
	this._listView = new Object;	// ZmListView (one per view)
	this._list = null;				// ZmList (the data)
	
    	this._listeners = new Object();
	this._listeners[ZmOperation.VIEW] = new AjxListener(this, this._viewButtonListener);
	this._listeners[ZmOperation.NEW_MENU] = new AjxListener(this, this._newListener);
			
	this._viewFactory = new Object();
	this._viewFactory[ZmController.IM_CHAT_TAB_VIEW] = ZmChatTabbedView;
	this._viewFactory[ZmController.IM_CHAT_MULTI_WINDOW_VIEW] = ZmChatMultiWindowView;
	
	this._appCtxt.getSettings().addChangeListener(new AjxListener(this, this._changeListener));
	this._parentView = new Object();
}

ZmChatListController.prototype = new ZmController;
ZmChatListController.prototype.constructor = ZmChatListController;

ZmChatListController.ICON = new Object();
ZmChatListController.ICON[ZmController.IM_CHAT_TAB_VIEW]		= "SinglePane"; // TODO: get real icon
ZmChatListController.ICON[ZmController.IM_CHAT_MULTI_WINDOW_VIEW]	= "OpenInNewWindow"; // TODO: get real icon

ZmChatListController.MSG_KEY = new Object();
ZmChatListController.MSG_KEY[ZmController.IM_CHAT_TAB_VIEW]	= "imChatTabbed";
ZmChatListController.MSG_KEY[ZmController.IM_CHAT_MULTI_WINDOW_VIEW]= "imChatMultiWindow";

ZmChatListController.VIEWS = [ZmController.IM_CHAT_TAB_VIEW, ZmController.IM_CHAT_MULTI_WINDOW_VIEW];

ZmChatListController.prototype.toString = 
function() {
	return "ZmChatListController";
}

// Public methods

ZmChatListController.prototype.show =
function() {
	var view = this._currentView || this._defaultView();
	this.switchView(view, true);
}

ZmChatListController.prototype.switchView = 
function(view, force) {
	if (view != this._currentView || force) {
		this._setup(view);
		var elements = new Object();
		elements[ZmAppViewMgr.C_TOOLBAR_TOP] = this._toolbar[view];
		elements[ZmAppViewMgr.C_APP_CONTENT] = this._parentView[view];
		var ok = this._setView(view, elements, true);
		this._currentView = view;
		if (ok)
			this._setViewMenu(view);
	}
}


// Set the view menu's icon, and make sure the appropriate list item is checked
ZmChatListController.prototype._setViewMenu =
function(view) {
	var appToolbar = this._appCtxt.getCurrentAppToolbar();
	appToolbar.showViewMenu(view);
    var menu = appToolbar.getViewButton().getMenu();
    if (menu) {
	    var mi = menu.getItemById(ZmOperation.MENUITEM_ID, view);
		if (mi)
			mi.setChecked(true, true);
	}
}

ZmChatListController.prototype._preShowCallback =
function(view) {
	return true;
}

// Private and protected methods

ZmChatListController.prototype._standardToolBarOps =
function() {
	var list = [ZmOperation.NEW_MENU];
	/*
	if (this._appCtxt.get(ZmSetting.TAGGING_ENABLED))
		list.push(ZmOperation.TAG_MENU);
	list.push(ZmOperation.SEP);
	if (this._appCtxt.get(ZmSetting.PRINT_ENABLED))
		list.push(ZmOperation.PRINT_MENU);
	list.push(ZmOperation.DELETE, ZmOperation.MOVE);
	*/
	return list;
}

ZmChatListController.prototype._getToolBarOps =
function() {
	var list = this._standardToolBarOps();
	list.push(ZmOperation.SEP);
	//list.push(ZmOperation.EDIT);
	return list;
}

ZmChatListController.prototype._getActionMenuOps =
function() {
    return [];
    /*
//	var list = this._contactOps();
	list.push(ZmOperation.SEP);
	list = list.concat(this._standardActionMenuOps());
	return list;
	*/
}

ZmChatListController.prototype._getViewType = 
function() {
	return this._currentView;
}

ZmChatListController.prototype._defaultView =
function() {
	return (this._appCtxt.get(ZmSetting.IM_VIEW) == "tabbed") ? ZmController.IM_CHAT_TAB_VIEW : ZmController.IM_CHAT_MULTI_WINDOW_VIEW;
}

/*
ZmChatListController.prototype._getTagMenuMsg = 
function(num) {
	return (num == 1) ? ZmMsg.AB_TAG_CONTACT : ZmMsg.AB_TAG_CONTACTS;
}

ZmChatListController.prototype._getMoveDialogTitle = 
function(num) {
	return (num == 1) ? ZmMsg.AB_MOVE_CONTACT : ZmMsg.AB_MOVE_CONTACTS;
}
*/

ZmChatListController.prototype._initializeToolBar = 
function(view) {
	if (this._toolbar[view]) return;

	var buttons = this._getToolBarOps();
	if (!buttons) return;
	this._toolbar[view] = new ZmButtonToolBar(this._container, buttons, null, Dwt.ABSOLUTE_STYLE, "ZmAppToolBar");
	// remove text for Print, Delete, and Move buttons
	var list = [ZmOperation.PRINT, ZmOperation.DELETE, ZmOperation.MOVE];
	for (var i = 0; i < list.length; i++) {
		var button = this._toolbar[view].getButton(list[i]);
		if (button)
			button.setText(null);
	}
	for (var i = 0; i < buttons.length; i++)
		if (buttons[i] > 0 && this._listeners[buttons[i]])
			this._toolbar[view].addSelectionListener(buttons[i], this._listeners[buttons[i]]);
	this._propagateMenuListeners(this._toolbar[view], ZmOperation.NEW_MENU);
	this._setupViewMenu(view);
	
	this._setNewButtonProps(view, ZmMsg.compose, "NewMessage", "NewMessageDis", ZmOperation.NEW_MESSAGE);
}

ZmChatListController.prototype._initializeActionMenu = 
function(view) {
	if (this._actionMenu) return;

	var menuItems = this._getActionMenuOps();
	if (!menuItems) return;
	this._actionMenu = new ZmActionMenu(this._shell, menuItems);
	for (var i = 0; i < menuItems.length; i++)
		if (menuItems[i] > 0)
			this._actionMenu.addSelectionListener(menuItems[i], this._listeners[menuItems[i]]);
	this._actionMenu.addPopdownListener(this._popdownListener);

}

// Load chats into the given view and perform layout.
ZmChatListController.prototype._setViewContents =
function(view) {
	this._listView[view].set(this._list);
}


ZmChatListController.prototype._createNewView = 
function(view) {
	var view = this._parentView[view] = new this._viewFactory[view](this._container, null, Dwt.ABSOLUTE_STYLE, this, this._dropTgt);
	//view.setDragSource(this._dragSrc);
	return view;
}

// Creates basic elements and sets the toolbar and action menu
ZmChatListController.prototype._setup =
function(view) {
	if (this._listView[view]) return;
	
	this._initializeToolBar(view);
	this._listView[view] = this._createNewView(view);
//	this._listView[view].addSelectionListener(new AjxListener(this, this._listSelectionListener));
//	this._listView[view].addActionListener(new AjxListener(this, this._listActionListener));	
	this._initializeActionMenu(view);
	this._resetOperations(this._toolbar[view], 0);
	this._resetOperations(this._actionMenu, 0);
}

// Resets the available options on a toolbar or action menu.
ZmChatListController.prototype._resetOperations = 
function(parent, num) {
	if (!parent) return;
	if (num == 0) {
		parent.enableAll(false);
		parent.enable(ZmOperation.NEW_MENU, true);
	} else if (num == 1) {
		parent.enableAll(true);
	} else if (num > 1) {
		// enable only the tag and delete operations
		parent.enableAll(false);
		parent.enable([ZmOperation.NEW_MENU], true);
	}
}

// Switch to selected view.
ZmChatListController.prototype._viewButtonListener =
function(ev) {
	this.switchView(ev.item.getData(ZmOperation.MENUITEM_ID));
}

// Create menu for View button and add listeners.
ZmChatListController.prototype._setupViewMenu =
function(view) {
	var appToolbar = this._appCtxt.getCurrentAppToolbar();
	var menu = appToolbar.getViewMenu(view);
	if (!menu) {
		var menu = new ZmPopupMenu(appToolbar.getViewButton());
		for (var i = 0; i < ZmChatListController.VIEWS.length; i++) {
			var id = ZmChatListController.VIEWS[i];
			var mi = menu.createMenuItem(id, ZmChatListController.ICON[id], ZmMsg[ZmChatListController.MSG_KEY[id]], null, true, DwtMenuItem.RADIO_STYLE);
			mi.setData(ZmOperation.MENUITEM_ID, id);
			mi.addSelectionListener(this._listeners[ZmOperation.VIEW]);
			if (id == view)
				mi.setChecked(true, true);
		}
		appToolbar.setViewMenu(view, menu);
	}
	return menu;
}

ZmChatListController.prototype._setView =
function(view, elements, isAppView, clear, pushOnly) {

	// create the view (if we haven't yet)
	if (!this._appViews[view]) {
		// view management callbacks
		var callbacks = new Object();
		callbacks[ZmAppViewMgr.CB_PRE_HIDE] =
			this._preHideCallback ? new AjxCallback(this, this._preHideCallback) : null;
		callbacks[ZmAppViewMgr.CB_POST_HIDE] =
			this._postHideCallback ? new AjxCallback(this, this._postHideCallback) : null;
		callbacks[ZmAppViewMgr.CB_PRE_SHOW] =
			this._preShowCallback ? new AjxCallback(this, this._preShowCallback) : null;
		callbacks[ZmAppViewMgr.CB_POST_SHOW] =
			this._postShowCallback ? new AjxCallback(this, this._postShowCallback) : null;
	
		this._app.createView(view, elements, callbacks, isAppView);
		this._appViews[view] = 1;
	}

	// populate the view
	if (!pushOnly)
		this._setViewContents(view);

	// push the view
	 return (clear ? this._app.setView(view) : this._app.pushView(view));
}

// Create some new thing, via a dialog. If just the button has been pressed (rather than
// a menu item), the action taken depends on the app.
ZmChatListController.prototype._newListener = 
function(ev) {
	var id = ev.item.getData(ZmOperation.KEY_ID);
	if (!id || id == ZmOperation.NEW_MENU)
		id = this._defaultNewId;
	if (id == ZmOperation.NEW_MESSAGE) {
		var inNewWindow = this._appCtxt.get(ZmSetting.NEW_WINDOW_COMPOSE) || ev.shiftKey;
		this._appCtxt.getApp(ZmZimbraMail.MAIL_APP).getComposeController().doAction(ZmOperation.NEW_MESSAGE, inNewWindow);
	} else if (id == ZmOperation.NEW_CONTACT) {
		var contact = new ZmContact(this._appCtxt);
		this._appCtxt.getApp(ZmZimbraMail.CONTACTS_APP).getContactController().show(contact);
	} else if (id == ZmOperation.NEW_APPT) {
		var cc = this._appCtxt.getApp(ZmZimbraMail.CALENDAR_APP).getCalController();
		cc.newAppointment();
	} else if (id == ZmOperation.NEW_FOLDER) {
		this._showDialog(this._appCtxt.getNewFolderDialog(), this._newFolderCallback);
	} else if (id == ZmOperation.NEW_TAG) {
		this._showDialog(this._appCtxt.getNewTagDialog(), this._newTagCallback, null, null, false);
	} else if (id == ZmOperation.NEW_CALENDAR) {
		var overviewController = this._appCtxt.getOverviewController();
		var treeData = overviewController.getTreeData(ZmOrganizer.CALENDAR);
		var folder = treeData.root;
	
		var newCalDialog = this._appCtxt.getNewCalendarDialog();
		newCalDialog.setParentFolder(folder);
		newCalDialog.popup();
	}
}

// Adds the same listener to all of a menu's items
ZmChatListController.prototype._propagateMenuListeners =
function(parent, op, listener) {
	if (!parent) return;
	listener = listener || this._listeners[op];
	var opWidget = parent.getOp(op);
	if (opWidget) {
		var menu = opWidget.getMenu();
	    var items = menu.getItems();
		var cnt = menu.getItemCount();
		for (var i = 0; i < cnt; i++)
			items[i].addSelectionListener(listener);
	}
}

// Set up the New button based on the current app.
ZmChatListController.prototype._setNewButtonProps =
function(view, toolTip, enabledIconId, disabledIconId, defaultId) {
	var newButton = this._toolbar[view].getButton(ZmOperation.NEW_MENU);
	if (newButton) {
		newButton.setToolTipContent(toolTip);
		newButton.setImage(enabledIconId);
		newButton.setDisabledImage(disabledIconId);
		this._defaultNewId = defaultId;
	}
}