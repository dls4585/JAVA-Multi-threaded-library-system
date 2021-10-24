package cse3040fp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;


public class Server {
	class CaseInsensitive implements Comparator<String> {
		@Override
		public int compare(String arg0, String arg1) {
			// TODO Auto-generated method stub
			String[] tokena = arg0.split("\t");
			String[] tokenb = arg1.split("\t");
			return tokena[0].compareToIgnoreCase(tokenb[0]);
		} // 나중에 Collections.sort(books, new CaseInsensitive) 사용
	}
	
	File file;
	ArrayList<String> books = null;
	HashMap<String, DataOutputStream> clients;
	PrintWriter booksOut = null;
	BufferedReader booksIn = null;
	Server() {
		clients = new HashMap<>();
		Collections.synchronizedMap(clients);
		books = new ArrayList<>();
		try {
			file = new File("books.txt");
			booksIn = new BufferedReader(new FileReader(file));
			while(true) {
				String line = booksIn.readLine();
				if(line == null) break;
				//System.out.println(line);
				books.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeToFile() {
		try {
			booksOut = new PrintWriter(new FileWriter(file, false));
			for(String book : books) {
				booksOut.println(book);
			}
			booksOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void start(String port) {
		ServerSocket serverSocket = null;
		Socket socket = null;
		try {
			serverSocket = new ServerSocket(Integer.parseInt(port));
			System.out.println("Server has started.");
			
			while(true) {
				socket = serverSocket.accept();
				System.out.println("a new connection from ["+socket.getInetAddress()+":"+socket.getPort()+"]");
				ServerReceiver thread = new ServerReceiver(socket);
				thread.start();
			}
		} catch(Exception e) {e.printStackTrace();}
	}
	
	////// Command-related Method /////////
	public synchronized int findBook(String title) {
		int index = 0;
		for(String book : books) {
			String[] tokens = book.split("\t");
			if(tokens[0].toLowerCase().equals(title.toLowerCase())) {
				return index;
			}
			index++;
		}
		return -1;
	}
	public synchronized boolean isAvailable(String title) {
		for(String book : books) {
			String[] tokens = book.split("\t");
			if(tokens[0].toLowerCase().equals(title.toLowerCase())) {
				if(tokens[2].equals("-")) {
					return true;
				}
				else return false;
			}
		}
		return false;
	}
	
	public synchronized boolean addBook(String newTitle, String newAuthor) {
		if(findBook(newTitle) != -1) {
			return false;
		}
		else {
			String str = newTitle +"\t"+newAuthor+"\t"+"-";
			books.add(str);
			Collections.sort(books, new CaseInsensitive());
			//System.out.println(books);
			writeToFile();
			return true;
		}
	}
	
	public synchronized String borrowBook(String title, String id) {
		String[] bookInfo;
		String updated;
		int index;
		if(isAvailable(title)) {
			index = findBook(title);
			if(index == -1) return null;
			else {
				bookInfo = books.get(index).split("\t");
				updated = bookInfo[0] + "\t" + bookInfo[1] + "\t" + id;
				books.set(index, updated);
				writeToFile();
				return bookInfo[0];
			}
		}
		else return null;
	}
	
	public synchronized String returnBook(String title, String id) {
		String[] bookInfo;
		String updated;
		int index;
		if(!isAvailable(title)) {
			index = findBook(title);
			if (index == -1) return null;
			else {
				bookInfo = books.get(index).split("\t");
				if(bookInfo[2].equals(id)) {
					updated = bookInfo[0] + "\t" + bookInfo[1] + "\t-";
					books.set(index, updated);
					writeToFile();
					return bookInfo[0];
				}
				else return null;
			}
			
		}
		else return null;
	}
	
	public ArrayList<String> infoBook(String id) {
		int i = 1;
		String[] info;
		ArrayList<String> infoResult = new ArrayList<>();
		for(String book : books) {
			info = book.split("\t");
			if(info[2].equals(id)) {
				infoResult.add(i+". " + info[0] + ", " + info[1]);
				i++;
			}
		}
		return infoResult;
	}
	
	public synchronized ArrayList<String> searchBook(String search) {
		int i = 1;
		String[] info;
		ArrayList<String> searchResult = new ArrayList<>();
		for(String book : books) {
			info = book.split("\t");
			if((info[0].toLowerCase().indexOf(search.toLowerCase())!= -1) || info[1].toLowerCase().indexOf(search.toLowerCase())!= -1) {
				searchResult.add(i+". " + info[0] + ", " + info[1]);
				i++;
			}
		}
		return searchResult;
	}
	///////////////////////////////////////////
	
	public void sendToOne(String msg, DataOutputStream sender) {
		Iterator<String> it = clients.keySet().iterator();
		while(it.hasNext()) {
			try {
				String key = it.next();
				DataOutputStream out = (DataOutputStream)clients.get(key);
				if(out.equals(sender)) {
					out.writeUTF(msg);
					break;
				}
			} catch(IOException e) {}
		}
	}
	
	class ServerReceiver extends Thread {
		Socket socket;
		DataInputStream in;
		DataOutputStream out;
		public ServerReceiver(Socket socket) {
			this.socket = socket;
			try {
				in = new DataInputStream(socket.getInputStream());
				out = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {}
		}
		public void run() {
			String id = "";
			String msg;
			try {
				id = in.readUTF();
				clients.put(id, out);
				System.out.println("current number of users: "+clients.size());
				while(in != null) {
					String line = in.readUTF();
					//System.out.println(line);
					String[] tokens = line.split("\t");
					String title;
					switch (tokens[0]) {
					case "add":
						if(addBook(tokens[1], tokens[2])) {
							msg = "A new book added to the list.";
							sendToOne(msg, out);
						} else {
							msg = "The book already exists in the list.";
							sendToOne(msg, out);
						}
						break;
					case "borrow":
						title = borrowBook(tokens[2], tokens[1]);
						if(title != null) {
							msg = "You borrowed a book. - " + title;
							sendToOne(msg, out);
						}
						else {
							msg = "The book is not available.";
							sendToOne(msg, out);
						}
						break;
					case "return":
						title = returnBook(tokens[2], tokens[1]);
						if(title != null) {
							msg = "You returned a book. - " + title;
							sendToOne(msg, out);
						}
						else {
							msg = "You did not borrow the book.";
							sendToOne(msg, out);
						}
						break;
					case "info":
						ArrayList<String> infos = infoBook(tokens[1]);
						msg = String.format("You are currently borrowing %d books:", infos.size());
						if(infos.size() != 0) {
							for(String info : infos) {
								msg += "\n" +info;
							}
						}
						sendToOne(msg, out);
						break;
					case "search":
						ArrayList<String> result = searchBook(tokens[1]);
						msg = String.format("Your search matched %d results.", result.size());
						if(result.size() != 0) {
							for(String info : result) {
								msg += "\n" +info;
							}
						}
						sendToOne(msg, out);
						break;
					default:
						break;
					}
				}
			} catch(IOException e) {
				
			}finally {
				clients.remove(id);
				System.out.println(id+ " [" + socket.getInetAddress() + ":" + socket.getPort() + "] has disconnected.");
				System.out.println("current number of users: "+clients.size());
			}
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length != 1) {
			System.out.println("Please give the port number as an argument.");
			System.exit(0);
		}
		new Server().start(args[0]);
	}

}
