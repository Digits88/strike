package au.edu.unimelb.tcp.client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

public class MessageReceiveThread implements Runnable {

	private SSLSocket socket;
	private State state;
	private boolean debug;
	private Client client;

	private BufferedReader in;

	private JSONParser parser = new JSONParser();

	private boolean run = true;
	
	private MessageSendThread messageSendThread;

	public MessageReceiveThread(SSLSocket socket, State state, MessageSendThread messageSendThread, Client client, boolean debug) throws IOException {
		this.socket = socket;
		this.state = state;
		this.messageSendThread = messageSendThread;
		this.client = client;
		this.debug = debug;
	}

	@Override
	public void run() {

		try {
			this.in = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "UTF-8"));
			JSONObject message;
			while (run) {
				message = (JSONObject) parser.parse(in.readLine());
				if (debug) {
					System.out.println("Receiving: " + message.toJSONString());
					System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
				}
				MessageReceive(socket, message);
			}
			System.exit(0);
			in.close();
			socket.close();
		} catch (ParseException e) {
			System.out.println("Message Error: " + e.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Communication Error: " + e.getMessage());
			System.exit(1);
		}

	}

	public void MessageReceive(SSLSocket socket, JSONObject message)
			throws IOException, ParseException {
		String type = (String) message.get("type");

		System.out.println(message.toJSONString());

		// server reply of #newidentity
		if (type.equals("newidentity")) {
			boolean approved = Boolean.parseBoolean((String) message.get("approved"));
			
			// terminate program if failed
			if (!approved) {
				System.out.println(state.getIdentity() + " already in use.");
				//socket.close();
				client.userWasDenied();
				// System.exit(1);
			}
			else {
				client.userWasApproved();
			}

			return;
		}
		
		// server reply of #list
		if (type.equals("roomlist")) {
			JSONArray array = (JSONArray) message.get("rooms");
			// print all the rooms
			System.out.print("List of chat rooms:");
			for (int i = 0; i < array.size(); i++) {
				System.out.print(" " + array.get(i));
			}
			System.out.println();
			System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			return;
		}

		// server sends roomchange
		if (type.equals("roomchange")) {

			// identify whether the user has quit!
			if (message.get("roomid").equals("")) {

				// quit initiated by the current client
				if (message.get("identity").equals(state.getIdentity())) {
					String userid = (String) message.get("identity");
					System.out.println(message.get("identity") + " has quit!");
					this.client.userDidQuit(userid);
					in.close();
					System.exit(1);
				} else {
					String userid = (String) message.get("identity");
					this.client.userDidQuit(userid);
					System.out.println(message.get("identity") + " has quit!");
					System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
				}

				// identify whether the client is new or not
			} else if (message.get("former").equals("")) {

				// change state if it's the current client
				if (message.get("identity").equals(state.getIdentity())) {
					String from = state.getRoomId();
					String to = (String) message.get("roomid");

					this.client.didChangeRoom(from, to);
					state.setRoomId(to);
				}
				else {
					String userid = (String) message.get("identity");
					this.client.userDidJoin(userid);
				}

				System.out.println(message.get("identity") + " moves to "
						+ (String) message.get("roomid"));
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");

				// identify whether roomchange actually happens
			} else if (message.get("former").equals(message.get("roomid"))) {
				System.out.println("room unchanged");
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}

			// print the normal roomchange message
			else {

				// change state if it's the current client
				if (message.get("identity").equals(state.getIdentity())) {

					String from = state.getRoomId();
					String to = (String) message.get("roomid");

					this.client.didChangeRoom(from, to);
					state.setRoomId(to);
				}
				else {
					String from = (String) message.get("former");
					String to = (String) message.get("roomid");
					String userid = (String) message.get("identity");

					// The user is leaving.
					if(from.equalsIgnoreCase(state.getRoomId())) {
						this.client.userDidLeave(userid);
					}
					else {
						this.client.userDidJoin(userid);
					}
				}

				System.out.println(message.get("identity") + " moves from " + message.get("former") + " to "
						+ message.get("roomid"));
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			return;
		}
		
		// server reply of #who
		if (type.equals("roomcontents")) {

			HashSet<String> clients = new HashSet<>();

			JSONArray array = (JSONArray) message.get("identities");
			System.out.print(message.get("roomid") + " contains");
			for (int i = 0; i < array.size(); i++) {

				System.out.print(" " + array.get(i));

				clients.add((String)array.get(i));

					if (message.get("owner").equals(array.get(i))) {
					System.out.print("*");
				}
			}

			this.client.didReceiveInitialClientList(clients);
			System.out.println();
			System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			return;
		}
		
		// server forwards message
		if (type.equals("message")) {
			String identity = (String)message.get("identity");
			String content = (String)message.get("content");

			System.out.println(identity + ": "
					+ content);
			System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			this.client.didReceiveMessage(identity, content);
			return;
		}
		
		
		// server reply of #createroom
		if (type.equals("createroom")) {
			boolean approved = Boolean.parseBoolean((String)message.get("approved"));
			String temp_room = (String)message.get("roomid");
			if (!approved) {
				System.out.println("Create room " + temp_room + " failed.");
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			else {
				System.out.println("Room " + temp_room + " is created.");
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			return;
		}
		
		// server reply of # deleteroom
		if (type.equals("deleteroom")) {
			boolean approved = Boolean.parseBoolean((String)message.get("approved"));
			String temp_room = (String)message.get("roomid");
			if (!approved) {
				System.out.println("Delete room " + temp_room + " failed.");
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			else {
				System.out.println("Room " + temp_room + " is deleted.");
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			return;
		}
		
		// server directs the client to another server
		if (type.equals("route")) {

			String temp_room = (String)message.get("roomid");
			String host = (String)message.get("host");
			int port = Integer.parseInt((String)message.get("port"));
			String username = (String)message.get("username");
			String sessionid = (String)message.get("sessionid");
			String password = (String)message.get("password");
			
			// connect to the new server
			if (debug) {
				System.out.println("Connecting to server " + host + ":" + port);
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}

			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket temp_socket = (SSLSocket) sslsocketfactory.createSocket(host, port);
			
			// send #movejoin
			DataOutputStream out = new DataOutputStream(temp_socket.getOutputStream());
			JSONObject request = ClientMessages.getMoveJoinRequest(state.getIdentity(), state.getRoomId(), temp_room, username, sessionid, password);
			if (debug) {
				System.out.println("Sending: " + request.toJSONString());
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			send(out, request);
			
			// wait to receive serverchange
			BufferedReader temp_in = new BufferedReader(new InputStreamReader(temp_socket.getInputStream()));
			JSONObject obj = (JSONObject) parser.parse(temp_in.readLine());
			
			if (debug) {
				System.out.println("Receiving: " + obj.toJSONString());
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			
			// serverchange received and switch server
			if (obj.get("type").equals("serverchange") && obj.get("approved").equals("true")) {
				messageSendThread.switchServer(temp_socket, out);
				switchServer(temp_socket, temp_in);
				String serverid = (String)obj.get("serverid");
				System.out.println(state.getIdentity() + " switches to server " + serverid);
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			// receive invalid message
			else {
				temp_in.close();
				out.close();
				temp_socket.close();
				System.out.println("Server change failed");
				System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
			}
			return;
		}
		
		if (debug) {
			System.out.println("Unknown Message: " + message);
			System.out.print("[" + state.getRoomId() + "] " + state.getIdentity() + "> ");
		}
	}
	
	public void switchServer(SSLSocket temp_socket, BufferedReader temp_in) throws IOException {
		in.close();
		in = temp_in;
		socket.close();
		socket = temp_socket;
	}

	private void send(DataOutputStream out, JSONObject obj) throws IOException {
		out.write((obj.toJSONString() + "\n").getBytes("UTF-8"));
		out.flush();
	}
}
