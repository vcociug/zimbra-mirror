/**
 * 
 */
package com.zimbra.qa.selenium.projects.octopus.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.HttpStatus;
import com.zimbra.qa.selenium.framework.core.ClientSessionFactory;
import com.zimbra.qa.selenium.framework.items.FolderItem;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.octopus.ui.DialogError;
import com.zimbra.qa.selenium.projects.octopus.ui.DialogError.DialogErrorID;
import com.zimbra.soap.mail.type.Folder;

public class PageOctopus extends AbsTab {

	public static class Locators {
		public static final Locators zSignOutButton = new Locators(
				"css=a.(headerLink signOutLink):contains(sign out)");
		public static final Locators zTabMyFiles = new Locators(
				"css=div.octopus-tab-label:contains(My Files)");
		public static final Locators zTabSharing = new Locators(
				"css=div.octopus-tab-label:contains(Sharing)");
		public static final Locators zTabFavorites = new Locators(
				"css=div.octopus-tab-label:contains(Favorites)");
		public static final Locators zTabHistory = new Locators(
				"css=div.octopus-tab-label:contains(History)");
		public static final Locators zTabTrash = new Locators(
				"css=div.octopus-tab-label:contains(Trash)");
		public static final Locators zTabSearch = new Locators(
				"css=div.octopus-tab-label:contains(Search)");
		public static final Locators zMyFilesListViewItems = new Locators(
				"css=div[class*=my-files-list-view]>div.my-files-list-item");
		public static final Locators zRenameInput = new Locators(
				"css=div[class*=edit-pane-panel-shim] input[class=field]");

		public final String locator;

		private Locators(String locator) {
			this.locator = locator;
		}
	}

	public PageOctopus(AbsApplication application) {
		super(application);

		logger.info("new " + PageOctopus.class.getCanonicalName());

	}

	public Toaster zGetToaster() throws HarnessException {
		return (new Toaster(this.MyApplication));
	}

	public DialogError zGetErrorDialog(DialogErrorID octopus) {
		return (new DialogError(octopus, this.MyApplication, this));
	}

	@Override
	public boolean zIsActive() throws HarnessException {
		// Look for the Sign Out button
		boolean present = sIsElementPresent(Locators.zSignOutButton.locator);

		if (!present) {
			logger.debug("zIsActive(): " + present);
			return (false);
		}
		logger.debug("isActive() = " + true);
		return (true);
	}

	@Override
	public String myPageName() {
		return (this.getClass().getName());
	}

	@Override
	public void zNavigateTo() throws HarnessException {

		if (zIsActive()) {
			// This page is already active
			return;
		}

		// Login as the default account
		if (!((AppOctopusClient) MyApplication).zPageLogin.zIsActive()) {
			((AppOctopusClient) MyApplication).zPageLogin.zNavigateTo();
		}
		((AppOctopusClient) MyApplication).zPageLogin.zLogin(ZimbraAccount
				.AccountZWC());
		zWaitForActive();
	}

	/**
	 * Open url with logout attribute
	 * 
	 * @throws HarnessException
	 */
	public void zLogout() throws HarnessException {
		logger.debug("PageOctopus logout()");

		tracer.trace("Logout of the " + MyApplication.myApplicationName());

		zNavigateTo();

		// logout
		String url = this.getLocation();

		// Open url through RestUtil
		Map<String, String> map = new HashMap<String, String>();

		if (url.contains("?") && !url.endsWith("?")) {
			String query = url.split("\\?")[1];

			for (String p : query.split("&")) {
				if (p.contains("=")) {
					map.put(p.split("=")[0], p.split("=")[1].substring(0, 1));
				}
			}
		}

		map.put("loginOp", "logout");

		this.openUrl("", map);

		sWaitForPageToLoad();
		((AppOctopusClient) MyApplication).zPageLogin.zWaitForActive();

		((AppOctopusClient) MyApplication).zSetActiveAcount(null);

	}

	public String getLocation() {
		return ClientSessionFactory.session().selenium().getLocation();
	}

	public String openUrl(String url) throws HarnessException {

		this.sOpen(url);

		return url;
	}

	public String openUrl(String path, Map<String, String> params)
			throws HarnessException {
		ZimbraAccount account = ((AppOctopusClient) MyApplication)
				.zGetActiveAccount();
		if (null == account)
			account = ZimbraAccount.AccountZWC();

		RestUtil util = new RestUtil();

		util.setAuthentication(account);

		if (null != path && !path.isEmpty())
			util.setPath("/" + path + "/");
		else
			util.setPath("/");

		if (null != params && !params.isEmpty()) {
			for (Map.Entry<String, String> query : params.entrySet()) {
				util.setQueryParameter(query.getKey(), query.getValue());
			}
		}

		if (util.doGet() != HttpStatus.SC_OK)
			throw new HarnessException("Unable to open " + util.getLastURI());

		String url = util.getLastURI().toString();

		if (url.endsWith("?"))
			url = url.substring(0, url.length() - 1);

		this.sOpen(url);

		return url;
	}

	@Override
	public AbsPage zToolbarPressButton(Button button) throws HarnessException {
		logger.info(myPageName() + " zToolbarPressButton(" + button + ")");

		tracer.trace("Press the " + button + " button");

		if (button == null)
			throw new HarnessException("Button cannot be null!");

		// Default behavior variables
		//
		String locator = null; // If set, this will be clicked
		AbsPage page = null; // If set, this page will be returned

		// Based on the button specified, take the appropriate action(s)
		//

		if (button == Button.B_TAB_MY_FILES) {
			locator = Locators.zTabMyFiles.locator;
			page = new PageMyFiles(MyApplication);
		} else if (button == Button.B_TAB_SHARING) {
			locator = Locators.zTabSharing.locator;
			page = new PageSharing(MyApplication);
		} else if (button == Button.B_TAB_FAVORITES) {
			locator = Locators.zTabFavorites.locator;
			page = new PageFavorites(MyApplication);
		} else if (button == Button.B_TAB_HISTORY) {
			locator = Locators.zTabHistory.locator;
			page = new PageHistory(MyApplication);
		} else if (button == Button.B_TAB_TRASH) {
			locator = Locators.zTabTrash.locator;
			page = new PageTrash(MyApplication);
		} else if (button == Button.B_TAB_SEARCH) {
			locator = Locators.zTabSearch.locator;
			page = new PageSearch(MyApplication);
		} else {
			throw new HarnessException("no logic defined for button " + button);
		}

		if (locator == null) {
			throw new HarnessException("locator was null for button " + button);
		}

		// Default behavior, process the locator by clicking on it
		//

		// Make sure the button exists
		if (!this.sIsElementPresent(locator))
			throw new HarnessException("Button is not present locator="
					+ locator + " button=" + button);

		// Click it
		this.zClick(locator);

		// If the app is busy, wait for it to become active
		zWaitForBusyOverlay();

		return (page);
	}

	public boolean zIsFolderParent(FolderItem folderItem, String childFolderName)
			throws HarnessException {
		if (folderItem == null || childFolderName == null)
			throw new HarnessException("folder or item cannot be null");

		boolean found = false;
		FolderItem childFolderItem;

		for (int i = 0; i < 5; i++) {
			childFolderItem = FolderItem.importFromSOAP(
					MyApplication.zGetActiveAccount(), childFolderName);
			if (childFolderItem != null
					&& folderItem.getId().contentEquals(
							childFolderItem.getParentId()))
				return true;
			SleepUtil.sleepVerySmall();
		}
		return found;
	}

	public boolean zIsFolderChild(FolderItem folderItem, String parentFolderName)
			throws HarnessException {
		if (folderItem == null || parentFolderName == null)
			throw new HarnessException("folder or item cannot be null");

		boolean found = false;
		FolderItem parentFolderItem;

		for (int i = 0; i < 5; i++) {
			parentFolderItem = FolderItem.importFromSOAP(
					MyApplication.zGetActiveAccount(), parentFolderName);
			if (parentFolderItem != null) {
				List<Folder> subfolders = parentFolderItem.getSubfolders();
				String name = folderItem.getName();
				for (Folder folder : subfolders)
					if (folder.getName().contains(name)) {
						return true;
					}
			}
			SleepUtil.sleepVerySmall();
		}
		return found;
	}

	public boolean zIsItemInCurentListView(String itemName)
			throws HarnessException {
		if (itemName == null)
			throw new HarnessException("item cannot be null");

		boolean found = false;
		for (int i = 0; i < 5; i++) {
			List<String> itemNames = zGetListViewItems();

			for (String str : itemNames)
				if (str.contains(itemName)) {
					return true;
				}
			SleepUtil.sleepVerySmall();
		}
		return found;
	}

	public List<String> zGetListViewItems() throws HarnessException {
		List<String> items = new ArrayList<String>();
		String locator = Locators.zMyFilesListViewItems.locator;

		int count = sGetCssCount(locator);
		String str;

		for (int i = 1; i <= count; i++) {
			str = this.sGetText(locator + ":nth-child(" + i
					+ ") span.my-files-list-item-name");

			items.add(str);
		}
		return items;
	}

	public String searchFile(String fileName) throws HarnessException {
		ZimbraAccount account = MyApplication.zGetActiveAccount();
		account.soapSend("<SearchRequest xmlns='urn:zimbraMail' types='document'>"
				+ "<query>" + fileName + "</query>" + "</SearchRequest>");

		String id = account.soapSelectValue("//mail:SearchResponse//mail:doc",
				"id");

		return id;
	}

	public String searchFileIn(String fileName, String folderName)
			throws HarnessException {
		ZimbraAccount account = MyApplication.zGetActiveAccount();
		account.soapSend("<SearchRequest xmlns='urn:zimbraMail' types='document'>"
				+ "<query>in:"
				+ folderName
				+ " "
				+ fileName
				+ "</query>"
				+ "</SearchRequest>");

		String id = account.soapSelectValue("//mail:SearchResponse//mail:doc",
				"id");

		return id;
	}

	public void trashItemUsingSOAP(String itemId, ZimbraAccount account)
			throws HarnessException {
		account.soapSend("<ItemActionRequest xmlns='urn:zimbraMail'>"
				+ "<action id='" + itemId + "' op='trash'/>"
				+ "</ItemActionRequest>");
	}

	public void deleteItemUsingSOAP(String itemId, ZimbraAccount account)
			throws HarnessException {
		account.soapSend("<ItemActionRequest xmlns='urn:zimbraMail'>"
				+ "<action id='" + itemId + "' op='delete'/>"
				+ "</ItemActionRequest>");
	}

	
	public void moveItemUsingSOAP(String itemId, String targetId, ZimbraAccount account)
			throws HarnessException {
		account.soapSend("<ItemActionRequest xmlns='urn:zimbraMail'>"
				+ "<action id='" + itemId + "' l='" + targetId + "' op='move'/>"
				+ "</ItemActionRequest>");
	}

	
	public void rename(String text) throws HarnessException {
		// ClientSessionFactory.session().selenium().getEval("var x = selenium.browserbot.findElementOrNull(\""+Locators.zFrame.locator+"\");if(x!=null)x=x.contentWindow.document.body;if(browserVersion.isChrome){x.textContent='"+text+"';}else if(browserVersion.isIE){x.innerText='"+text+"';}");
		logger.info("renaming to: " + text);

		// sSelectFrame("relative=top");
		if (zWaitForElementPresent(Locators.zRenameInput.locator, "3000"))
			sType(Locators.zRenameInput.locator, text);
		else
			throw new HarnessException(Locators.zRenameInput.locator
					+ " not present");

		zKeyEvent(Locators.zRenameInput.locator, "13", "keydown");
	}

	@Override
	public AbsPage zToolbarPressPulldown(Button pulldown, Button option)
			throws HarnessException {
		throw new HarnessException("Implement me");
	}

	@Override
	public AbsPage zListItem(Action action, String item)
			throws HarnessException {
		throw new HarnessException("Implement me");
	}

	@Override
	public AbsPage zListItem(Action action, Button option, String item)
			throws HarnessException {
		throw new HarnessException("Implement me");
	}

	@Override
	public AbsPage zListItem(Action action, Button option, Button subOption,
			String item) throws HarnessException {
		throw new HarnessException("Implement me");
	}
}
