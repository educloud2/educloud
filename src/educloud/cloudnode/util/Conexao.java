package  educloud.cloudnode.util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import  educloud.cloudnode.configuration.NodeConfig;

public class Conexao {
	private String hostname;
	private String user;
	private String password;
	private boolean isAuthenticated;
	
	public Conexao(){
		hostname = NodeConfig.getStorageIp();
		user = NodeConfig.getStorageUser();
		password = NodeConfig.getStoragePass();
		isAuthenticated = false;
	}
	
	public boolean execute(String command, String test){
		boolean result = false;
		try {
			Connection conn = new Connection(hostname);
			conn.connect();
			isAuthenticated = conn.authenticateWithPassword(user, password);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");
			
			Session sess = conn.openSession();
			System.out.println("Wait execution... "+command);
			sess.execCommand(command);			
			
			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
			
			while (true){
				String line = br.readLine();
				if (line == null || line.contains(test)){
					System.out.println("Command Success!");
					result = true;
					break;
				}
				else {
					System.out.println(line);
					result = false;
				}
			}
						
			sess.close();
			conn.close();					
			
		} catch (IOException e) {			
			e.printStackTrace(System.err);
			System.exit(2);
		}
		return result;
	}
}

