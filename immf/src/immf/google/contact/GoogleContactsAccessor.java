package immf.google.contact;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gdata.client.Query;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.contacts.ContactGroupEntry;
import com.google.gdata.data.contacts.GroupMembershipInfo;
import com.google.gdata.data.extensions.Email;
import com.google.gdata.data.extensions.Name;
import com.google.gdata.util.ServiceException;

public class GoogleContactsAccessor {

	public static final String CLASS_NAME = GoogleContactsAccessor.class.getName();

	private static final Log log = LogFactory.getLog(GoogleContactsAccessor.class);

	public static final String FEED_URL = "https://www.google.com/m8/feeds/contacts/default/full";

	public static final int MAX_RESULT = 65535;

	private ContactsService myService;

	private static GoogleContactsAccessor instance = null;

	private Map<String, GoogleContact> contactMap = null;

	private Map<String, String> groupNameMap = null;

	public static GoogleContactsAccessor getInstance() {
		return instance;
	}

	public static boolean isInitialized() {
		return instance != null;
	}

	public static GoogleContactsAccessor initialize(String gmail, String password) {
		if (instance == null) {
			try {

				if (gmail == null || password == null) {
					log.warn("Doing initialize failed. gmailId or password is null");
					return null;
				}

				instance = new GoogleContactsAccessor();

				ContactsService service = new ContactsService(CLASS_NAME);
				service.setUserCredentials(gmail, password);

				log.info("Doing initialize is success.");

				instance.myService = service;

			} catch (Exception e) {
				log.warn("Doing initialize failed. gmail:" + gmail, e);
				instance = null;
			}
		}

		return instance;
	}

	private GoogleContactsAccessor() {
		contactMap = new HashMap<String, GoogleContact>();
		groupNameMap = new HashMap<String, String>();
	}

	public GoogleContact getGoogleContact(String targetEmailAddress) {

		boolean mustUpdate = true;

		GoogleContact contact = null;

		if (this.contactMap.containsKey(targetEmailAddress)) {
			contact = this.contactMap.get(targetEmailAddress);
			ContactFeed feed = getContactFeed(new DateTime(contact.getUpdateTime().getValue() + 1));
			if (feed.getEntries().size() != 0) {
				for (ContactEntry entry : feed.getEntries()) {
					if (entry.getEtag().equals(contact.getId()))
						mustUpdate = true;
					break;
				}
			}
			mustUpdate = false;
		}

		if (mustUpdate) {
			ContactEntry contactEntry = getContactEntry(targetEmailAddress, null);
			if (contactEntry != null) {
				contact = new GoogleContact(contactEntry.getEtag());
				Name name = contactEntry.getName();
				if (name.hasFullName()) {
					contact.setName(name.getFullName().getValue());
				} else {
					StringBuilder nameBuilder = new StringBuilder();
					if (name.hasFamilyName()) {
						nameBuilder.append(name.getFamilyName().getValue());
					}
					if (name.hasGivenName()) {
						nameBuilder.append(name.getGivenName().getValue());
					}
					contact.setName(nameBuilder.toString());
				}
				contact.setMailAddress(targetEmailAddress);
				contact.setUpdateTime(contactEntry.getUpdated());

				List<String> groupNames = getGroupNames(targetEmailAddress);
				for (String groupName : groupNames) {
					contact.addGroup(groupName);
				}

				this.contactMap.put(targetEmailAddress, contact);

				log.info("Getting address by using Google Contacts API is success.");
			}
		}
		return contact;
	}

	private List<String> getGroupNames(String targetEmailAddress) {
		ContactEntry contact = getContactEntry(targetEmailAddress, null);

		List<String> groupNameList = new ArrayList<String>();

		if (contact != null) {
			for (GroupMembershipInfo gInfo : contact.getGroupMembershipInfos()) {
				String name = groupHrefToName(gInfo.getHref());
				if (name != null && name.length() != 0) {
					groupNameList.add(name);
				}
			}
		}

		return groupNameList;
	}

	private ContactFeed getContactFeed(DateTime dateTime) {
		try {
			Query myQuery = new Query(new URL(FEED_URL));
			myQuery.setMaxResults(MAX_RESULT);
			if (dateTime != null)
				myQuery.setUpdatedMin(dateTime);
			return myService.query(myQuery, ContactFeed.class);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServiceException e) {
			e.printStackTrace();
		}

		return null;
	}

	private ContactEntry getContactEntry(String targetEmailAddress, DateTime dateTime) {
		ContactFeed feed = getContactFeed(dateTime);

		if (feed != null) {
			for (ContactEntry entry : feed.getEntries()) {
				for (Email email : entry.getEmailAddresses()) {
					if (email.getAddress().equals(targetEmailAddress)) {
						return entry;
					}
				}
			}
		}

		return null;

	}

	private String groupHrefToName(String contactHref) {

		if (groupNameMap.containsKey(contactHref)) {
			return groupNameMap.get(contactHref);
		} else {
			String name = null;
			try {
				ContactGroupEntry entry = myService.getEntry(new URL(contactHref), ContactGroupEntry.class);
				if (entry.hasSystemGroup()) {
					name = entry.getPlainTextContent().replace("System Group: ", "");
					if("My Contacts".equals(name)) name = null;
				} else {
					name = entry.getPlainTextContent();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ServiceException e) {
				e.printStackTrace();
			}
			if (name != null) {
				groupNameMap.put(contactHref, name);
			}
			return name;
		}
	}

}
