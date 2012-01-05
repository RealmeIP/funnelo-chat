package cz.jabbim.android.service;

interface ConnectionServiceCall {
	boolean isLoggedIn();
	void setStatus(String state, String type, String mode);
	void login();
	void logOff();
	void connect(String state, String type, String mode);
	void disconnect();
	void sendMessage(String user, String message);
	void getAvatar(String user);
	void getRoster();
	void addEntry(String user, String name, in List<String> groups);
	List<String> getLastStatusMessages();
	void insertAndUseMessage(String message);
}