package  educloud.cloudnode.scheduler.tasks;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.virtualbox.CleanupMode;
import org.virtualbox.DeviceType;
import org.virtualbox.LockType;
import org.virtualbox.MachineState;
import org.virtualbox.service.IConsole;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.IMedium;
import org.virtualbox.service.IProgress;
import org.virtualbox.service.ISession;
import org.virtualbox.service.IVirtualBox;
import org.virtualbox.service.IWebsessionManager;

import  educloud.cloudnode.virtualbox.VirtualBoxConnector;

import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.util.OsUtil;
import  educloud.cloudnode.util.Conexao;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

public class RemoveVmTask extends AbstractTask {

	private static Logger LOG = Logger.getLogger(RemoveVmTask.class);

	private VirtualMachine vm;

	private String diskName;

	private String targetName;

	public void setVirtualMachine(VirtualMachine vm) {
		this.vm = vm;
	}

	
	public void run() {

		LOG.debug("Running remove virtual machine task");

		IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig.getVirtualBoxWebservicesUrl());
		
		//String medium = vm.getBootableMedium();

		// remove machine file
		try {
//			String command = NodeConfig.getRemoveFromStorageScript() + " " + medium;
//			LOG.debug("will run command: '" + command + "'");
//			OsUtil.runScript(command);
			removeVM(vbox);
			
		} catch (Exception e) {
			LOG.error("Error on remove DISK from storage", e);
		}
	}
	
	public void removeVM(IVirtualBox vbox){
		if(vm.getState().equals(VMState.RUNNING)){
			// removendo VM do VB
			IWebsessionManager manager = new IWebsessionManager(vbox.port);
			ISession session = manager.getSessionObject(vbox);
			IMachine machine = vbox.findMachine(vm.getName());
			if(machine.getState().equals(MachineState.RUNNING)){
				stopMachine();
			}
			machine.unregister(CleanupMode.FULL);
		}
		
		// Removendo volume logico (disco) do storage
		Conexao conection = new Conexao();		
		String command = "lvremove -f /dev/vg_educloud/vl_vm_" + vm.getId();
		conection.execute(command, "removed");
	}
	
	private void stopMachine(){
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
		
		session.release();
		vbox.release();

		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to stop a machine: '" + elapsedTime + "'");
	}
	
}
