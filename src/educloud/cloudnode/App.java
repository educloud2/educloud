package  educloud.cloudnode;

import  educloud.cloudnode.configuration.InvalidConfigurationException;
import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.launchers.JettyLauncher;
import  educloud.cloudnode.launchers.SchedulerLauncher;


public class App {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			NodeConfig.setup();
		} catch (InvalidConfigurationException e) {
			System.exit(-1);
		}

		/* try start jetty */
		JettyLauncher.main(args);

		/* try start scheduler */
		SchedulerLauncher.main(args);
	}

}
