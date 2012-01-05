package cz.jabbim.android.tools;

import java.util.ArrayList;

public class ContactGroup implements Comparable<ContactGroup> {

	private String name;
	private boolean virtual;
	private ArrayList<String> entriesOnline;
	private ArrayList<ContactEntry> entries;

	public ContactGroup(String name, boolean virtual) {
		this.name = name;
		this.virtual = virtual;
		this.entriesOnline = new ArrayList<String>();
		this.entries = new ArrayList<ContactEntry>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isVirtual() {
		return virtual;
	}

	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	public int getEntriesOnline() {
		return entriesOnline.size();
	}

	public void addOnlineEntry(String jid) {
		if (!entriesOnline.contains(jid)) {
			entriesOnline.add(jid);
		}
	}

	public void removeOnlineEntry(String jid) {
		if (entriesOnline.contains(jid)) {
			entriesOnline.remove(jid);
		}
	}

	public void addEntry(ContactEntry contact) {
		for (ContactEntry entry : entries) {
			if (entry.getJid().equals(contact.getJid())) {
				entry.setName(contact.getName());
				entry.setStatus(contact.getStatus());
				entry.setGroups(contact.getGroups());
				return;
			}
		}

		entries.add(contact);
	}

	public ContactEntry getEntry(String jid) {
		for (ContactEntry entry : entries) {
			if (entry.getJid().equals(jid)) { return entry; }
		}

		return null;
	}

	public ArrayList<ContactEntry> getEntries() {
		return this.entries;
	}

	public void clearEntries() {
		entries.clear();
		entriesOnline.clear();
	}

	@Override
	public int compareTo(ContactGroup another) {
		return getName().compareToIgnoreCase(another.getName());
	}

	@Override
	public String toString() {
		return getName();
	}
}
