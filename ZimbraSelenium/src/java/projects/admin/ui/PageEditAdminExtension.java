/**
 * 
 */
package projects.admin.ui;

import framework.util.HarnessException;

/**
 * @author Matt Rhoades
 *
 */
public class PageEditAdminExtension extends AbsPage {

	public PageEditAdminExtension(AbsApplication application) {
		super(application);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsPage#isActive()
	 */
	@Override
	public boolean isActive() throws HarnessException {
		throw new HarnessException("implement me");
	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsPage#myPageName()
	 */
	@Override
	public String myPageName() {
		return (this.getClass().getName());
	}

	/* (non-Javadoc)
	 * @see projects.admin.ui.AbsPage#navigateTo()
	 */
	@Override
	public void navigateTo() throws HarnessException {
		throw new HarnessException("implement me");
	}

}
