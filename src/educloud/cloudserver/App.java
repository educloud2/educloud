package  educloud.cloudserver;

import  educloud.cloudserver.configuration.InvalidConfigurationException;
import  educloud.cloudserver.configuration.ServerConfig;
import  educloud.cloudserver.launchers.JettyLauncher;
import  educloud.cloudserver.launchers.MonitorsLaunchers;
import  educloud.cloudserver.launchers.SchedulerLauncher;

public class App {
	public static void main(String args[]) {

		/* try load configuration file */
		try {
			ServerConfig.setup();
		} catch (InvalidConfigurationException e) {
			System.exit(-1);
		}

		/* try start scheduler */
		SchedulerLauncher.main(args);

		/* try start jetty */
		JettyLauncher.main(args);
		
		/* try start monitors */
		MonitorsLaunchers.main(args);
	}
}
