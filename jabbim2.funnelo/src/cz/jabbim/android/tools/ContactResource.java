package cz.jabbim.android.tools;

public class ContactResource implements Comparable<ContactResource> {

	private String resourceName;
	private int resourcePriority;

	private int presenceType;
	private int presenceMode;
	private String presenceMessage;

	public ContactResource(String resourceName, int resourcePriority, int presenceType, int presenceMode,
			String presenceMessage) {
		this.resourceName = resourceName;
		this.resourcePriority = resourcePriority;
		this.presenceType = presenceType;
		this.presenceMode = presenceMode;
		this.presenceMessage = presenceMessage;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public int getResourcePriority() {
		return resourcePriority;
	}

	public void setResourcePriority(int resourcePriority) {
		this.resourcePriority = resourcePriority;
	}

	public int getPresenceType() {
		return presenceType;
	}

	public void setPresenceType(int presenceType) {
		this.presenceType = presenceType;
	}

	public int getPresenceMode() {
		return presenceMode;
	}

	public void setPresenceMode(int presenceMode) {
		this.presenceMode = presenceMode;
	}

	public String getPresenceMessage() {
		return presenceMessage;
	}

	public void setPresenceMessage(String presenceMessage) {
		this.presenceMessage = presenceMessage;
	}

	@Override
	public int compareTo(ContactResource another) {
		if (getResourcePriority() > another.getResourcePriority()) {
			return 1;
		} else if (getResourcePriority() < another.getResourcePriority()) {
			return -1;
		} else {
			return 0;
		}
	}

}
