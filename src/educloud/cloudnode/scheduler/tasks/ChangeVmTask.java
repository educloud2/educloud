package  educloud.cloudnode.scheduler.tasks;

import org.apache.log4j.Logger;
import org.virtualbox.LockType;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.ISession;
import org.virtualbox.service.IVirtualBox;
import org.virtualbox.service.IWebsessionManager;

import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.virtualbox.VirtualBoxConnector;
import  educloud.internal.entities.VirtualMachine;

public class ChangeVmTask extends AbstractTask {

	private static Logger LOG = Logger.getLogger(StopVmTask.class);
	
	private VirtualMachine vm;
	
	public void setVirtualMachine(VirtualMachine vm) {
		this.vm = vm;
	}


	public void run() {
		long start = System.nanoTime();

		LOG.debug("Running change virtual machine task");

		// 1) Change memory virtual machine process
		IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig.getVirtualBoxWebservicesUrl());
		IWebsessionManager manager = new IWebsessionManager(vbox.port);
		ISession session = manager.getSessionObject(vbox);
		
		long startProcess = System.nanoTime();
		long newMemory = vm.getMemorySize(); 
		long newProcessor = vm.getNumberProcessors(); 
		long newCap = vm.getCapProcessor(); 
		LOG.debug("virtual machine NAME = " + vm.getName());
		IMachine machine = vbox.findMachine(vm.getName());
		machine.lockMachine(session, LockType.SHARED);							
		IMachine mutable = session.getMachine();
		
		mutable.setMemorySize(newMemory);
		mutable.setCPUCount(newProcessor);
		mutable.setCPUExecutionCap(newCap);
		mutable.saveSettings();
		session.unlockMachine();
		
		long endProcess = System.nanoTime();
		double elapsedTimeProcess = ((endProcess - startProcess)) / 1000000000.0;
		LOG.debug("Elapsed time change memory vbox process: '" + elapsedTimeProcess + "'");		
		LOG.debug("virtual machine was changed");

		// 2) notify server that machine was changed
		session.release();
		vbox.release();

		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to change a machine: '" + elapsedTime + "'");		
	}
}