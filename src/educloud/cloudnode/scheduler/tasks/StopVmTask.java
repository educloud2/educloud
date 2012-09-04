package  educloud.cloudnode.scheduler.tasks;

import  educloud.cloudnode.util.Conexao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.virtualbox.CleanupMode;
import org.virtualbox.DeviceType;
import org.virtualbox.LockType;
import org.virtualbox.service.IConsole;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.IMedium;
import org.virtualbox.service.IProgress;
import org.virtualbox.service.ISession;
import org.virtualbox.service.IVirtualBox;
import org.virtualbox.service.IWebsessionManager;

import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.util.OsUtil;
import  educloud.cloudnode.virtualbox.VirtualBoxConnector;
import  educloud.internal.entities.VirtualMachine;

public class StopVmTask extends AbstractTask {

	private static Logger LOG = Logger.getLogger(StopVmTask.class);

	private VirtualMachine vm;

	private String diskName;

	private String targetName;

	public void setVirtualMachine(VirtualMachine vm) {
		this.vm = vm;
	}

	
	public void run() {
		long start = System.nanoTime();

		LOG.debug("Running stop virtual machine task");

		// 1) Stop virtual machine process
		IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig.getVirtualBoxWebservicesUrl());

		IWebsessionManager manager = new IWebsessionManager(vbox.port);
		ISession session = manager.getSessionObject(vbox);
		
		this.diskName = "vl_vm_" + vm.getId();
		this.targetName = "iqn.educloud:" + diskName;

		long startProcess = System.nanoTime();
		IMachine findMachine = vbox.findMachine(vm.getName());
		findMachine.lockMachine(session, LockType.SHARED);

		IConsole console = session.getConsole();

		IProgress progress = console.powerDown();

		boolean completed = false;
		do {
			completed = progress.getCompleted();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				LOG.error("Virtual machine process was interrupted", e);
			}
		} while (!completed);

		progress.release();
		console.release();

		long endProcess = System.nanoTime();
		double elapsedTimeProcess = ((endProcess - startProcess)) / 1000000000.0;
		LOG.debug("Elapsed time stop vbox process: '" + elapsedTimeProcess + "'");

		// Desatachando target
		findMachine.lockMachine(session, LockType.SHARED);
		IMachine mutable = session.getMachine();		
		mutable.detachDevice("scsi", 0, 0);
		mutable.saveSettings();
		session.unlockMachine();
		
		// Removendo Target VB
		IMedium mm = vbox.findMedium(NodeConfig.getStorageIp()+"|"+targetName+"|1", DeviceType.HARD_DISK);
		mm.close();
		
		// Removendo target do storage. N�o altera o volume l�gico
		Conexao conection = new Conexao();		
		String command = "tgtadm --lld iscsi --op delete --mode target --tid " + vm.getId();
		conection.execute(command, null);
		
		findMachine.unregister(CleanupMode.FULL);
		
		/*
		// 2) Detach virtual machine medium
		IMachine machine = findMachine;
		List<IMedium> unregister = machine.unregister(CleanupMode.FULL);

		// remove medium from storage registry of vbox
		for (IMedium iMedium : unregister) {
			iMedium.close();
		}

		machine.delete(new ArrayList<IMedium>());
		machine.release();

		long startCopy = System.nanoTime();
		// 3) put the virtual machine back to machine storage dir
		copyMachineDiskToStorage();
		long endCopy = System.nanoTime();
		double elapsedTimeCopy = ((endCopy - startCopy)) / 1000000000.0;
		LOG.debug("Elapsed time copy from machines to storage: '" + elapsedTimeCopy + "'");

		String property = System.getProperty("file.separator");
		try {
			new File(NodeConfig.getMachinesDir() + property + vm.getBootableMedium()).delete();
			LOG.debug("Local template '" + NodeConfig.getMachinesDir() + property + vm.getBootableMedium() + "' removed");
		} catch (SecurityException e) {
			LOG.warn("Impossible delete '" + NodeConfig.getMachinesDir() + property + vm.getBootableMedium() + "'", e);
		}

		LOG.debug("virtual machine was stopped");
		*/
		
		
		// 4) notify server that machine was dropped
		session.release();
		vbox.release();

		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to stop a machine: '" + elapsedTime + "'");

	}

	private void copyMachineDiskToStorage() {
		String bootableMedium = vm.getBootableMedium();

		try {
			String command = NodeConfig.getMachineToStorageScript() + " " + bootableMedium;
			LOG.debug("Will run command: " + command);
			OsUtil.runScript(command);
		} catch (IOException e) {
			LOG.error("Error on copy template to local", e);
		} catch (InterruptedException e) {
			LOG.error("Error on copy template to local", e);
		}
	}

}
