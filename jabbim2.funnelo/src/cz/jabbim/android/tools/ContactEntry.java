package cz.jabbim.android.tools;

import java.util.ArrayList;

import cz.jabbim.android.Constants;

public class ContactEntry implements Comparable<ContactEntry> {

	private String jid;
	private String name;
	private String status;
	private ArrayList<ContactResource> resources;
	private ArrayList<String> groups;

	private String avatarHash;
	private boolean avatarChanged;

	// private byte[] avatar;

	public ContactEntry(String jid) {
		this(jid, null, null, new ArrayList<String>());
	}

	public ContactEntry(String jid, String name, String status, ArrayList<String> groups) {
		this.jid = jid;
		this.name = name;
		this.status = status;
		this.groups = groups;

		this.resources = new ArrayList<ContactResource>();
	}

	public void updatePresence(String resourceName, int resourcePriority, int presenceType, int presenceMode,
			String presenceMessage, String avatarHash) {
		if (avatarHash != null) {
			setAvatarHash(avatarHash);
		}

		for (int i = 0; i < resources.size(); i++) {
			if (resources.get(i).getResourceName().equals(resourceName)) {
				resources.get(i).setResourcePriority(resourcePriority);
				resources.get(i).setPresenceType(presenceType);
				resources.get(i).setPresenceMode(presenceMode);
				resources.get(i).setPresenceMessage(presenceMessage);
				return;
			}
		}

		resources.add(new ContactResource(resourceName, resourcePriority, presenceType, presenceMode, presenceMessage));
	}

	private ContactResource getHigherResource() {
		if (resources.size() == 0) { return null; }

		int higer = Integer.MIN_VALUE;
		int index = 0;

		for (int i = 0; i < resources.size(); i++) {
			if (resources.get(i).getResourcePriority() > higer) {
				higer = resources.get(i).getResourcePriority();
				index = i;
			}
		}

		return resources.get(index);
	}

	@Override
	public String toString() {
		if (name == null) { return jid; }
		return name;
	}

	public String getName() {
		return toString();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJid() {
		return jid;
	}

	public int getPresenceMode() {
		ContactResource hResource = getHigherResource();
		if (hResource != null) {
			return hResource.getPresenceMode();
		} else {
			return Constants.PRESENCEMODE_NULL;
		}
	}

	public String getPresenceMessage() {
		ContactResource hResource = getHigherResource();
		if (hResource != null) {
			return hResource.getPresenceMessage();
		} else {
			return "";
		}
	}

	public int getPresenceType() {
		ContactResource hResource = getHigherResource();
		if (hResource != null) {
			return hResource.getPresenceType();
		} else {
			return Constants.PRESENCETYPE_NULL;
		}
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ArrayList<String> getGroups() {
		return this.groups;
	}

	public void setGroups(ArrayList<String> groups) {
		this.groups = groups;
	}

	public String getAvatarHash() {
		return this.avatarHash;
	}

	public void setAvatarHash(String avatarHash) {
		if (!this.avatarHash.equals(avatarHash)) {
			this.avatarHash = avatarHash;
			this.avatarChanged = true;
		}
	}

	public boolean getAvatarChanged() {
		return this.avatarChanged;
	}

	public int countResources() {
		return this.resources.size();
	}

	public ArrayList<ContactResource> getResources() {
		return this.resources;
	}

	@Override
	public int compareTo(ContactEntry another) {
		if (getPresenceType() < another.getPresenceType()) {
			return -1;
		} else if (getPresenceType() > another.getPresenceType()) {
			return 1;
		} else if (getPresenceType() == Constants.PRESENCETYPE_AVAILABLE
				&& another.getPresenceType() == Constants.PRESENCETYPE_AVAILABLE) {
			int statusA = getPresenceMode();
			int statusB = another.getPresenceMode();

			if (statusA == Constants.PRESENCEMODE_CHAT) {
				statusA = -1;
			}
			if (statusB == Constants.PRESENCEMODE_CHAT) {
				statusB = -1;
			}

			if (statusA < statusB) {
				return -1;
			} else if (statusA > statusB) {
				return 1;
			} else {
				return getName().compareToIgnoreCase(another.getName());
			}
		} else {
			return getName().compareToIgnoreCase(another.getName());
		}
	}

}
