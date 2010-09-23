//helper class for retrieving properties
package framework.util;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ZimbraSeleniumProperties {
	private static final Logger logger = LogManager.getLogger(ZimbraSeleniumProperties.class);
	
	// Use these strings as arguments for some standard properties, e.g. ZimbraSeleniumProperties.getStringProperty(PropZimbraServer, "default");
	public static final String PropZimbraVersion = "zimbraserverversion"; 
	
	private static ZimbraSeleniumProperties instance = null;
	
	/**
	 * Set configPropFile before initializing ZimbraSeleniumProperties to hard code
	 * the path to your config file
	 */
	public static File configPropFile = null;
	
	private final String configPropName = "config.properties";
	private File dir;
	private String workingDir = ".";
	private PropertiesConfiguration configProp;

	public static String getStringProperty(String key, String defaultValue) {
		return (ZimbraSeleniumProperties.getInstance().getConfigProp()
				.getString(key, defaultValue));
	}

	public static String getStringProperty(String key) {
		return (getStringProperty(key, null));
	}
	
	public static int getIntProperty(String key) {
		return (getIntProperty(key, 0));
	}

	public static int getIntProperty(String key, int defaultValue) {
		String value = ZimbraSeleniumProperties.getInstance().getConfigProp().getString(key, null);
		if ( value == null )
			return (defaultValue);
		return (Integer.parseInt(value));
	}

	private static int counter = 0;
	public static String getUniqueString() {
		return ("" + System.currentTimeMillis() + (++counter));
	}

	public static ResourceBundle getResourceBundleProperty(String key) {
		return ((ResourceBundle) ZimbraSeleniumProperties.getInstance()
				.getConfigProp().getProperty(key));
	}

	public static PropertiesConfiguration getConfigProperties() {
		return ZimbraSeleniumProperties.getInstance().getConfigProp();
	}

	private PropertiesConfiguration getConfigProp() {
		return configProp;
	}

	private static ZimbraSeleniumProperties getInstance() {
		if ( instance == null ) {
			synchronized(ZimbraSeleniumProperties.class) {
				if ( instance == null ) {
					instance = new ZimbraSeleniumProperties();
				}
			}
		}
		return instance;
	}

	private ZimbraSeleniumProperties() {
		logger.debug("new ZimbraSeleniumProperties");
		init();
	}

	private void init() {
		
		if ( configPropFile != null ) {
			
			try {
				
				logger.info("config.properties is "+ configPropFile.getAbsolutePath());
				configProp = new PropertiesConfiguration(configPropFile);

			} catch (ConfigurationException e) {
				ZimbraSeleniumLogger.mLog.error("Unable to open config file: " + configPropFile.getAbsolutePath(), e);
				logger.info("config.properties is default");
				configProp = createDefaultProperties();
			}
			
		} else {
			
			// Use defaults to find the config.properties file
			String wd = PathFinder.findWorkingDir();
	
			if (wd != null)
				workingDir = wd;
	
			dir = new File(workingDir);
	
			try {
				File file = new File(dir.getCanonicalPath() + File.separator
						+ "conf" + File.separator + configPropName);
	
				if (!file.exists()) {
					File[] files = PathFinder.listFilesAsArray(dir, configPropName,
							true);
					if (files != null && files.length > 0) {
						file = files[0];
					}
					if (!file.exists() || !file.getName().contains(configPropName)) {
						ZimbraSeleniumLogger.mLog.error(configPropName
								+ " does not exist!");
						logger.info("config.properties is default");
						configProp = createDefaultProperties();
					}
				}
	
				if (null == configProp) {
					logger.info("config.properties is " + file);
					configProp = new PropertiesConfiguration(file);
				}
				
			} catch (Exception ex) {
				ZimbraSeleniumLogger.mLog.error("Exception : " + ex);
			}
		}

		String locale = configProp.getString("locale");

		configProp.setProperty("zmMsg", ResourceBundle.getBundle("ZmMsg", new Locale(locale)));

		configProp.setProperty("zhMsg", ResourceBundle.getBundle("ZhMsg", new Locale(locale)));

		configProp.setProperty("ajxMsg", ResourceBundle.getBundle("AjxMsg", new Locale(locale)));

		configProp.setProperty("i18Msg", ResourceBundle.getBundle("I18nMsg", new Locale(locale)));

		configProp.setProperty("zsMsg", ResourceBundle.getBundle("ZsMsg", new Locale(locale)));

	}

	private PropertiesConfiguration createDefaultProperties() {
		PropertiesConfiguration defaultProp = new PropertiesConfiguration();

		defaultProp.setProperty("browser", "FF3");

		defaultProp.setProperty("runMode", "DEBUG");

		defaultProp.setProperty("product", "zcs");

		defaultProp.setProperty("locale", "en_US");

		defaultProp.setProperty("intl", "us");

		defaultProp.setProperty("testdomain", "testdomain.com");

		defaultProp.setProperty("multiWindow", "true");

		defaultProp.setProperty("objectDataFile",
				"projects/zcs/data/objectdata.xml");

		defaultProp.setProperty("testDataFile",
				"projects/zcs/data/testdata.xml");

		defaultProp.setProperty("serverMachineName", "localhost");

		defaultProp.setProperty("serverport", "4444");

		defaultProp.setProperty("mode", "http");

		defaultProp.setProperty("server", "qa60.lab.zimbra.com");

		defaultProp.setProperty("ZimbraLogRoot", "test-output");

		defaultProp.setProperty("adminName", "admin");

		defaultProp.setProperty("adminPwd", "test123");

		defaultProp.setProperty("small_wait", "1000");

		defaultProp.setProperty("medium_wait", "2000");

		defaultProp.setProperty("long_wait", "4000");

		defaultProp.setProperty("very_long_wait", "10000");

		defaultProp.setProperty("zmMsg", ResourceBundle.getBundle(
				"framework.locale.ZhMsg", new Locale("en_US")));

		defaultProp.setProperty("zhMsg", ResourceBundle.getBundle(
				"framework.locale.ZhMsg", new Locale("en_US")));

		defaultProp.setProperty("ajxMsg", ResourceBundle.getBundle(
				"framework.locale.AjxMsg", new Locale("en_US")));

		defaultProp.setProperty("i18Msg", ResourceBundle.getBundle(
				"framework.locale.I18nMsg", new Locale("en_US")));

		defaultProp.setProperty("zsMsg", ResourceBundle.getBundle(
				"framework.locale.ZsMsg", new Locale("en_US")));

		return defaultProp;
	}

	private static class CurClassGetter extends SecurityManager {
		private Class<?> getCurrentClass() {
			return getClassContext()[1];
		}
	}

	/**
	 * App type
	 */
	public enum AppType {
		AJAX, HTML, MOBILE, DESKTOP, ADMIN, APPLIANCE
	}
	
	private static AppType appType = AppType.AJAX;
	public static void setAppType(AppType type) {
		appType = type;
	}
	public static AppType getAppType() {
		return (appType);
	}
	
	public static String getBaseURL() {
		
		if ( appType == AppType.DESKTOP )
			return "http://localhost:7633/zimbra/desktop/zmail.jsp";
		
		if ( appType == AppType.AJAX )
			return ZimbraSeleniumProperties.getStringProperty("mode") + "://"
				+ ZimbraSeleniumProperties.getStringProperty("server") + "";

		if ( appType == AppType.HTML )
			return ZimbraSeleniumProperties.getStringProperty("mode") + "://"
					+ ZimbraSeleniumProperties.getStringProperty("server") + "/h/";

		if ( appType == AppType.MOBILE )
			return ZimbraSeleniumProperties.getStringProperty("mode") + "://"
					+ ZimbraSeleniumProperties.getStringProperty("server") + "/m/";

		if(ZimbraSeleniumProperties.getStringProperty("runCodeCoverage", "no").equalsIgnoreCase("yes")) 
			return ZimbraSeleniumProperties.getStringProperty("mode") + "://"
					+ ZimbraSeleniumProperties.getStringProperty("server") + "?dev=1&debug=0";

		if ( appType == AppType.ADMIN )
			return "https://" +
				ZimbraSeleniumProperties.getStringProperty("server") + ":7071";


		// Default
		logger.warn("Using default URL");
		return ZimbraSeleniumProperties.getStringProperty("mode") + "://"
				+ ZimbraSeleniumProperties.getStringProperty("server") + "";
	}

	public static String zimbraGetVersionString() {		
		try {
			ZimbraAdminAccount.GlobalAdmin().soapSend("<GetVersionInfoRequest xmlns='urn:zimbraAdmin'/>");
			String version = ZimbraAdminAccount.GlobalAdmin().soapSelectValue("//admin:info", "version");
			if ( version == null )
				throw new HarnessException("Unable to determine version from GetVersionInfoResponse "+ ZimbraAdminAccount.GlobalAdmin().soapLastResponse());
			
			// The version string looks like 6.0.7_GA_2470.UBUNTU8.NETWORK
			return (version);
			
		} catch (HarnessException e) {
			ZimbraSeleniumLogger.mLog.error("Unable to send GetVersionInfoRequest", e);
			return ("unknown");
		}
	}


	// for unit test need to change access to public
	public static void main(String[] args) {
		ZimbraSeleniumLogger.setmLog(new CurClassGetter().getCurrentClass());

		System
				.setProperty("log4j.configuration",
						"file:///C:/log4j.properties");
		System.out.println(System.getProperty("log4j.configuration"));

		System.out.println(System.getProperty("user.dir"));

		String br = (String) ZimbraSeleniumProperties.getInstance()
				.getConfigProp().getProperty("browser");
		System.out.println(br);
		ZimbraSeleniumLogger.mLog.debug(br);

		ResourceBundle zmMsg = (ResourceBundle) ZimbraSeleniumProperties
				.getInstance().getConfigProp().getProperty("zmMsg");
		System.out.println(zmMsg.getLocale());
		ZimbraSeleniumLogger.mLog.debug(zmMsg.getLocale());
	}

}