package cse3040fp2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
	static Scanner sc = new Scanner(System.in);
	static class ClientSender extends Thread {
		Socket socket;
		DataOutputStream out;
		String id;
		ClientSender(Socket socket, String id) {
			this.socket = socket;
			try {
				out = new DataOutputStream(socket.getOutputStream());
				this.id = id;
			} catch(Exception e) {}
		}
		public void run() {
			System.out.print(id+">> ");	
			try {
				if (out != null) {
					out.writeUTF(id);
				}
				while(out != null) {
					String str = "";
					String command = sc.nextLine();
					String title, author;
						
					if(command.equals("add")) {
						System.out.print("add-title> ");
						title = sc.nextLine();
						if(title.equals("\n") || title.equals("")) {
							System.out.print(id+">> ");
							continue;
						}
						else {
							System.out.print("add-author> ");
							author = sc.nextLine();
							if(author.equals("\n") || author.equals("")) {
								System.out.print(id+">> ");
								continue;
							}
							else {
								str = command + "\t" + title + "\t" + author;
								out.writeUTF(str);
							}
						}
					}
					else if (command.equals("borrow")) {
						System.out.print("borrow-title> ");
						title = sc.nextLine();
						if(title.equals("\n") || title.equals("")) {
							System.out.print(id+">> ");
							continue;
						} else {
							str = command + "\t" + id +"\t" + title;
							out.writeUTF(str);
						}
					} 
					else if (command.equals("return")) {
						System.out.print("return-title> ");
						title = sc.nextLine();
						if(title.equals("\n") || title.equals("")) {
							System.out.print(id+">> ");
							continue;
						} else {
							str = command + "\t" + id + "\t" + title;
							out.writeUTF(str);
						}
					}
					else if (command.equals("info")) {
						str = command + "\t" + id;
						out.writeUTF(str);
					}
					else if (command.equals("search")) {
						while(true) {
							System.out.print("search-string> ");
							title = sc.nextLine();
							if(title.equals("\n") || title.equals("")) {
								System.out.print(id+">> ");
								break;
							}
							if(title.length() <= 2) {
								System.out.println("Search string must be longer than 2 characters.");
								continue;
							}
							else {
								str = command + "\t" + title;
								out.writeUTF(str);
								break;
							}
						}
					}
					else {
						System.out.println("[available commands]");
						System.out.println("add: add a new book to the list of books.");
						System.out.println("borrow: borrow a book from the library.");
						System.out.println("return: return a book to the library.");
						System.out.println("info: show list of books I am currently borrowing.");
						System.out.println("search: search for books.");
						System.out.print(id+">> ");
						continue;
					}
				}
			} catch(IOException e) {}
			
		}
	}
	static class ClientReceiver extends Thread {
		Socket socket;
		DataInputStream in;
		String id;
		ClientReceiver(Socket socket, String id) {
			this.socket = socket;
			this.id = id;
			try {
				in = new DataInputStream(socket.getInputStream());
			} catch (IOException e) {}
		}
		public void run() {
			while(in != null) {
				try {
					System.out.println(in.readUTF());
					System.out.print(id + ">> ");
				} catch (IOException e) {}
			}
		}
	}
	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Please give the IP address and port number as arguments.");
			System.exit(0);
		}
		
		Socket socket = null;
		String serverIP = args[0];
		String id;
		int serverPort = Integer.parseInt(args[1]);
		try {
			socket = new Socket(serverIP, serverPort);
		} catch (Exception x) {
			System.out.println("Connection establishment failed.");
			System.exit(0);
		}
		while (true) {
			System.out.print("Enter userID>> ");
			id = sc.nextLine();
			Pattern pattern = Pattern.compile("[^a-z0-9]");
			Matcher matcher = pattern.matcher(id);
			String[] tokens = id.split(" ");
			if(tokens.length>1 || id.equals("") || id.equals("\n")  || matcher.find()) {
				System.out.println("UserID must be a single word with lowercase alphabets and numbers.");
			}
			else {
				System.out.println("Hello " + id +"!!");
				break;
			}
		}
		Thread sender = new Thread(new ClientSender(socket, id));
		Thread receiver = new Thread(new ClientReceiver(socket, id));
		sender.start();
		receiver.start();
	}

}
