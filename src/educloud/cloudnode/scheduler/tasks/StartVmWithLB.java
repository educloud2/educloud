package  educloud.cloudnode.scheduler.tasks;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.virtualbox.DeviceType;
import org.virtualbox.LockType;
import org.virtualbox.StorageBus;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.IMedium;
import org.virtualbox.service.IProgress;
import org.virtualbox.service.ISession;
import org.virtualbox.service.IVirtualBox;
import org.virtualbox.service.IWebsessionManager;

import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.scheduler.Scheduler;
import  educloud.cloudnode.serverclient.VirtualMachineClient;
import  educloud.cloudnode.util.Conexao;
import  educloud.cloudnode.virtualbox.VirtualBoxConnector;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

public class StartVmWithLB extends AbstractTask {

	
	private static Logger LOG = Logger.getLogger(StartVmTask.class);

	private VirtualMachine vm;
	
	private IWebsessionManager manager;
	private ISession session;
	private IMachine machine;
	private String diskName;
	private String targetName;
	private String diskLBName;

	private String targetNameLB;

	public void setVirtualMachine(VirtualMachine vm) {
		this.vm = vm;
	}
	
	
	public void run() {
		long start = System.nanoTime();

		LOG.debug("Running start virtual machine task");

		// 1) create new session from vbox
		IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig.getVirtualBoxWebservicesUrl());

		startVMISCSI(vbox);
		vm.setState(VMState.RUNNING);
		vbox.release();
		new VirtualMachineClient().changeState(vm);
		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to start a machine: '" + elapsedTime + "'");
	}
	
	public void startVMISCSI(IVirtualBox vbox){
		this.diskName = "vl_vm_" + vm.getId();
		this.targetName = "iqn.educloud:" + diskName;
		
		this.diskLBName = "vl_lb_" + vm.getId();
		this.targetNameLB = "iqn.educloud:" + diskLBName;
		
		if(!vm.getState().equals(VMState.RUNNING)){
			// Criando Maquina virtual
			manager = new IWebsessionManager(vbox.port);
			session = manager.getSessionObject(vbox);
			IMachine machine = vbox.createMachine(null, vm.getName(), vm.getOsType(), null, true);
			machine.setMemorySize(vm.getMemorySize());
			machine.setCPUCount(vm.getNumberProcessors());
			machine.setCPUExecutionCap(30);
			machine.addStorageController("scsi", StorageBus.SCSI);
			vbox.registerMachine(machine);
			//Cria o target da VM
			createTargetVM(vm.getId());
			//seta o disco com o target da VM
			machine.lockMachine(session, LockType.SHARED);
			IMachine mutable = session.getMachine();
			IMedium mm = vbox.createHardDisk("ISCSI", NodeConfig.getStorageIp() + "|" + targetName + "|1");
			mm.setProperty("TargetAddress", NodeConfig.getStorageIp());
			mm.setProperty("TargetName", this.targetName);
			mm.setProperty("LUN", "1");				
			mutable.attachDevice("scsi", 0, 0, DeviceType.HARD_DISK, mm);
			mutable.saveSettings();
			session.unlockMachine();
			//this.attachToBridge(vm.getName());
			IProgress progress = machine.launchVMProcess(session, "gui", "");
			boolean completed = false;
			do {
				completed = progress.getCompleted();
			} while (!completed);
			
			if (progress.getResultCode() != 0)
				System.out.println("Cannot launch VM!");
			progress.release();
			session.getConsole().release();
			
			this.attachToBridge(vm.getName());
			
			StartVmWithLB startVmTask = new StartVmWithLB();
			startVmTask.setVirtualMachine(vm);
	
			Scheduler.getInstance().addTask(startVmTask);
		} else {
			// Criando o Load Balancer
			manager = new IWebsessionManager(vbox.port);
			ISession session2 = manager.getSessionObject(vbox);
			IMachine loadBalancer = vbox.createMachine(null, "LB_"+vm.getId(), "Linux", null, true);
			loadBalancer.setMemorySize(256);
			loadBalancer.setCPUCount(1);
			loadBalancer.setCPUExecutionCap(30);
			loadBalancer.addStorageController("scsi", StorageBus.SCSI);
			vbox.registerMachine(loadBalancer);
			
			//Cria o target do LB
			createTargetLB("LB_"+vm.getId());
			
			//seta o disco com o target da VM
			loadBalancer.lockMachine(session2, LockType.SHARED);
			IMachine mutable2 = session2.getMachine();
			IMedium mm2 = vbox.createHardDisk("ISCSI", NodeConfig.getStorageIp() + "|" + this.targetNameLB + "|1");
			mm2.setProperty("TargetAddress", NodeConfig.getStorageIp());
			mm2.setProperty("TargetName", this.targetNameLB);
			mm2.setProperty("LUN", "1");
			mutable2.attachDevice("scsi", 0, 0, DeviceType.HARD_DISK, mm2);
			mutable2.saveSettings();
			session2.unlockMachine();
			
			//this.attachToBridge(vm.getName());
			IProgress progress2 = loadBalancer.launchVMProcess(session2, "gui", "");
			boolean completed = false;
			do {
				completed = progress2.getCompleted();
			} while (!completed);
			if (progress2.getResultCode() != 0)
				System.out.println("Cannot launch the LB!");
			progress2.release();
			session2.getConsole().release();
			this.attachToBridge("LB_"+vm.getId());
		}
	}
	
	private void attachToBridge(String machineName) {
		String osName = System.getProperty("os.name"); 
		String interfaceHost = NodeConfig.getHostInterface();
		//String interfaceHost = "Realtek PCIe GBE Family Controller";
		String path = null;
		String[] command = new String[2];
		command[0] = path+"VBoxManage modifyvm "+machineName+" --nic1 bridged";
		command[1] = path+"VBoxManage modifyvm "+machineName+" --bridgeadapter1 \""+interfaceHost+"\"";
		
		if(osName.equals("Mac OS X")){
			path = "/Applications/VirtualBox.app/Contents/MacOS/";
		} else if (osName.contains("Windows")) {
			path = "\"C:\\Program Files\\Oracle\\VirtualBox\\";
			
			//No caso do Windows eh preciso escapar o caminho com espacos
			command[0] = path+"VBoxManage\" modifyvm "+machineName+" --nic1 bridged";
			command[1] = path+"VBoxManage modifyvm "+machineName+" --bridgeadapter1 \""+interfaceHost+"\"";
		} else if (osName.equals("Linux")){
			path = "/var/oracle/virtualbox/";
		}
		
		System.out.println("Vou rodar o comando 1 = " + command[0]);
		System.out.println("Vou rodar o comando 2 = " + command[1]);
		
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(command[0]);
			p.waitFor();
			p = Runtime.getRuntime().exec(command[1]);
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}
	
	private void createTargetVM(int idVm){		
		Conexao conection = new Conexao();
		String command = "tgtadm --lld iscsi --op new --mode target --tid " + idVm 
				+ " -T " + this.targetName;
		conection.execute(command, null);
		String command2 = "tgtadm --lld iscsi --op new --mode logicalunit --tid " + idVm 
				+ " --lun 1 -b /dev/vg_educloud/" + this.diskName;
		conection.execute(command2, null);
		String command3 = "tgtadm --lld iscsi --op bind --mode target --tid " + idVm + " -I ALL";
		conection.execute(command3, null);
	}
	
	private void createTargetLB(String idVm){		
		Conexao conection = new Conexao();
		String command = "tgtadm --lld iscsi --op new --mode target --tid " + 20 
				+ " -T " + this.targetNameLB;
		conection.execute(command, null);
		String command2 = "tgtadm --lld iscsi --op new --mode logicalunit --tid " + 20 
				+ " --lun 1 -b /dev/vg_educloud/" + this.diskLBName;
		conection.execute(command2, null);
		String command3 = "tgtadm --lld iscsi --op bind --mode target --tid " + 20 + " -I ALL";
		conection.execute(command3, null);
	}

}
