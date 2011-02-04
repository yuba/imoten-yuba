package immf.google.contact;

import immf.ImodeAddress;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gdata.data.DateTime;

public class GoogleContact extends ImodeAddress {

	public static final String SEPARETOR = ":";

	private DateTime updateTime;

	private final List<String> groupNameList;

	public GoogleContact(String etag) {
		super();
		groupNameList = new ArrayList<String>();
		this.setId(etag);
	}

	public DateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(DateTime updateTime) {
		this.updateTime = updateTime;
	}

	public void addGroup(String groupName) {
		this.groupNameList.add(groupName);
	}

	public void clearGroup() {
		this.groupNameList.clear();
	}

	public List<String> getGroupNameList()
	{
		return this.groupNameList;
	}

	public String getGroupNames() {
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> iterator = this.groupNameList.iterator(); iterator.hasNext();) {
			builder.append(iterator.next());
			if (iterator.hasNext())
				builder.append(SEPARETOR);
		}
		return builder.toString();
	}

}
