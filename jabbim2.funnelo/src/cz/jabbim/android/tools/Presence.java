package cz.jabbim.android.tools;

public class Presence {

	private String jid;
	private String resourceName;
	private int resourcePriority;
	private int presenceType;
	private int presenceMode;
	private String presenceMessage;
	private String avatarHash;

	public Presence(String jid, String resourceName, int resourcePriority, int presenceType, int presenceMode,
			String presenceMessage, String avatarHash) {
		this.jid = jid;
		this.resourceName = resourceName;
		this.resourcePriority = resourcePriority;
		this.presenceType = presenceType;
		this.presenceMode = presenceMode;
		this.presenceMessage = presenceMessage;
		this.avatarHash = avatarHash;
	}

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
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

	public String getAvatarHash() {
		return avatarHash;
	}

	public void setAvatarHash(String avatarHash) {
		this.avatarHash = avatarHash;
	}
}
