package projects.ajax.ui;


/**
 * Defines all the button IDs
 * @author Matt Rhoades
 *
 */
public class Buttons {

	// General buttons and pulldown options
	public static final Button B_NEW = new Button("B_NEW");
	public static final Button B_DELETE = new Button("B_DELETE");
	public static final Button B_MOVE = new Button("B_MOVE");
	public static final Button B_PRINT = new Button("B_PRINT");
	public static final Button B_TAG = new Button("B_TAG");
	public static final Button B_SAVE = new Button("B_SAVE");
	
	public static final Button O_NEW_MESSAGE = new Button("O_NEW_MESSAGE");
	public static final Button O_NEW_CONTACT = new Button("O_NEW_CONTACT");
	public static final Button O_NEW_CONTACTGROUP = new Button("O_NEW_CONTACTGROUP");
	public static final Button O_NEW_APPOINTMENT = new Button("O_NEW_APPOINTMENT");
	public static final Button O_NEW_TASK = new Button("O_NEW_TASK");
	public static final Button O_NEW_DOCUMENT = new Button("O_NEW_DOCUMENT");
	public static final Button O_UPLOAD_FILE = new Button("O_UPLOAD_FILE");
	public static final Button O_NEW_FOLDER = new Button("O_NEW_FOLDER");
	public static final Button O_NEW_TAG = new Button("O_NEW_TAG");
	public static final Button O_NEW_ADDRESSBOOK = new Button("O_NEW_ADDRESSBOOK");
	public static final Button O_NEW_CALENDAR = new Button("O_NEW_CALENDAR");
	public static final Button O_NEW_TASKFOLDER = new Button("O_NEW_TASKFOLDER");
	public static final Button O_NEW_BRIEFCASE = new Button("O_NEW_BRIEFCASE");
	public static final Button O_TAG_NEWTAG = new Button("O_TAG_NEWTAG");
	public static final Button O_TAG_REMOVETAG = new Button("O_TAG_REMOVETAG");

	// General dialog buttons
	public static final Button B_YES = new Button("B_YES");
	public static final Button B_NO = new Button("B_NO");
	public static final Button B_CANCEL = new Button("B_CANCEL");

	// MailPage buttons and pulldown options
	public static final Button B_GETMAIL = new Button("B_GETMAIL");
	public static final Button B_REPLY = new Button("B_REPLY");
	public static final Button B_REPLYALL = new Button("B_REPLYALL");
	public static final Button B_FORWARD = new Button("B_FORWARD");
	public static final Button B_RESPORTSPAM = new Button("B_RESPORTSPAM");
	public static final Button B_NEWWINDOW = new Button("B_NEWWINDOW");
	public static final Button B_LISTVIEW = new Button("B_LISTVIEW");

	public static final Button O_LISTVIEW_BYCONVERSATION = new Button("O_LSITVIEW_BYCONVERSATION");
	public static final Button O_LISTVIEW_BYMESSAGE = new Button("O_LSITVIEW_BYMESSAGE");
	public static final Button O_LISTVIEW_READINGPANEBOTTOM = new Button("O_LSITVIEW_READINGPANEBOTTOM");
	public static final Button O_LISTVIEW_READINGPANERIGHT = new Button("O_LSITVIEW_READINGPANERIGHT");
	public static final Button O_LISTVIEW_READINGPANEOFF = new Button("O_LSITVIEW_READINGPANEOFF");

	// SearchPage buttons and pulldown options
	public static final Button B_SEARCHTYPE = new Button("B_SEARCHTYPE");
	public static final Button B_SEARCH = new Button("B_SEARCH");
	public static final Button B_SEARCHSAVE = new Button("B_SEARCHSAVE");
	public static final Button B_SEARCHADVANCED = new Button("B_SEARCHADVANCED");
	
	public static final Button O_SEARCHTYPE_ALL = new Button("O_SEARCHTYPE_ALL");
	public static final Button O_SEARCHTYPE_EMAIL = new Button("O_SEARCHTYPE_EMAIL");
	public static final Button O_SEARCHTYPE_CONTACTS = new Button("O_SEARCHTYPE_CONTACTS");
	public static final Button O_SEARCHTYPE_GAL = new Button("O_SEARCHTYPE_GAL");
	public static final Button O_SEARCHTYPE_APPOINTMENTS = new Button("O_SEARCHTYPE_APPOINTMENTS");
	public static final Button O_SEARCHTYPE_TASKS = new Button("O_SEARCHTYPE_TASKS");
	public static final Button O_SEARCHTYPE_FILES = new Button("O_SEARCHTYPE_FILES");
	public static final Button O_SEARCHTYPE_INCLUDESHARED = new Button("O_SEARCHTYPE_INCLUDESHARED");

	public static class Button {
		
		private final String ID;
		
		protected Button(String id) {
			this.ID = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ID == null) ? 0 : ID.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			
			if (this == obj)
				return true;
			
			if (obj == null)
				return false;
			
			if (getClass() != obj.getClass())
				return false;
			
			Button other = (Button) obj;
			if (ID == null) {
				if (other.ID != null)
					return false;
			} else if (!ID.equals(other.ID))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return ID;
		}


	}
}
