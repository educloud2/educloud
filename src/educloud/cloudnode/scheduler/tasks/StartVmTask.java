package  educloud.cloudnode.scheduler.tasks;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.virtualbox.AccessMode;
import org.virtualbox.AuthType;
import org.virtualbox.CPUPropertyType;
import org.virtualbox.CleanupMode;
import org.virtualbox.DeviceType;
import org.virtualbox.IVRDEServerInfo;
import org.virtualbox.LockType;
import org.virtualbox.NetworkAdapterType;
import org.virtualbox.StorageBus;
import org.virtualbox.StorageControllerType;
import org.virtualbox.service.IAudioAdapter;
import org.virtualbox.service.IBIOSSettings;
import org.virtualbox.service.IConsole;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.IMedium;
import org.virtualbox.service.INetworkAdapter;
import org.virtualbox.service.IProgress;
import org.virtualbox.service.ISession;
import org.virtualbox.service.IStorageController;
import org.virtualbox.service.ISystemProperties;
import org.virtualbox.service.IUSBController;
import org.virtualbox.service.IVRDEServer;
import org.virtualbox.service.IVirtualBox;
import org.virtualbox.service.IWebsessionManager;

import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.serverclient.VirtualMachineClient;
import  educloud.cloudnode.util.OsUtil;
import  educloud.cloudnode.virtualbox.SHAUtils;
import  educloud.cloudnode.virtualbox.VirtualBoxConnector;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

import  educloud.cloudnode.util.Conexao;

public class StartVmTask extends AbstractTask {

	private static Logger LOG = Logger.getLogger(StartVmTask.class);

	private VirtualMachine vm;
	
	private IWebsessionManager manager;
	private ISession session;
	private IMachine machine;
	private String diskName;
	private String targetName;

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
		
		/*
		String bootableMedium = vm.getBootableMedium();

		String mediumLocation;

		// 2) copy bootable medium to machine dir
		String property = System.getProperty("file.separator");

		long startCopyToMachines = System.nanoTime();
		File arquivoDestino = copyMediumToMachinesDir(bootableMedium, property);
		long endCopyToMachines = System.nanoTime();
		double elapsedTimeCopyToMachines = ((endCopyToMachines - startCopyToMachines)) / 1000000000.0;
		LOG.info("elapsedTimeCopyToMachines DONE, elapsed time: " + elapsedTimeCopyToMachines);

		mediumLocation = arquivoDestino.getAbsolutePath();

		// 3) create a virtual machine from vbox
		IMachine machine = createMachine(vbox, mediumLocation);
		machine.release();

		// 4) start virtual machine
		long startVirtualBoxProcess = System.nanoTime();
		ISession sessionObject = new IWebsessionManager(vbox.port).getSessionObject(vbox);
		machine = vbox.findMachine(vm.getName());
		IProgress process = machine.launchVMProcess(sessionObject, NodeConfig.getVboxFrontendType(), "");
		long endVirtualBoxProcess = System.nanoTime();
		double elapsedTimeVirtualBoxProcess = ((endVirtualBoxProcess - startVirtualBoxProcess)) / 1000000000.0;
		LOG.info("elapsedTimeVirtualBoxProcess DONE, elapsed time: " + elapsedTimeVirtualBoxProcess);

		boolean completed = false;
		do {
			completed = process.getCompleted();
		} while (!completed);

		IConsole console = sessionObject.getConsole();
		IVRDEServerInfo vrdeServerInfo = console.getVRDEServerInfo();

		int port = vrdeServerInfo.getPort();

		// 5) notify server that machine was started
		String vmUUID = machine.getId().toString();

		LOG.debug("VM UUID: " + vmUUID);
		LOG.debug("VBox sessionid: " + sessionObject._this);
		LOG.debug("VRDE port: " + port);

		vm.setUUID(vmUUID);
		vm.setVboxSession(sessionObject._this);
		vm.setState(VMState.RUNNING);
		vm.setVRDEPort(port);

		// free resources
		console.release();
		machine.release();
		sessionObject.release();
		process.release();
		vbox.release();

		new VirtualMachineClient().changeState(vm);

		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to start a machine: '" + elapsedTime + "'");
		*/
	}
	
	public void startVMISCSI(IVirtualBox vbox){
		//manager = new IWebsessionManager(vbox.port);
		//session = manager.getSessionObject(vbox);
		//machine = vbox.findMachine(vm.getName());
		
		this.diskName = "vl_vm_" + vm.getId();
		this.targetName = "iqn.educloud:" + diskName;
		
		
		// Criando Maquina virtual
		manager = new IWebsessionManager(vbox.port);
		session = manager.getSessionObject(vbox);
		IMachine machine = vbox.createMachine(null, vm.getName(), vm.getOsType(), null, true);
		machine.setMemorySize(vm.getMemorySize());
		machine.setCPUCount(vm.getNumberProcessors());
		machine.setCPUExecutionCap(30);
		machine.addStorageController("scsi", StorageBus.SCSI);
		vbox.registerMachine(machine);
		
		createTargetVM(vm.getId());
		
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
		progress.waitForCompletion(10000);
		if (progress.getResultCode() != 0)
			System.out.println("Cannot launch VM!");
	}
	
	private void attachToBridge(String machineName) {
		String osName = System.getProperty("os.name"); 
		String interfaceHost = "en0: Ethernet";
		String path = null;
		if(osName.equals("Mac OS X")){
			path = "/Applications/VirtualBox.app/Contents/MacOS/";
		} else if (osName.equals("Windows")) {
			path = "C:\\Program Files\\Oracle\\VirtualBox\\";
		} else if (osName.equals("Linux")){
			path = "/var/oracle/virtualbox/";
		}
		
		String[] command = new String[2]; 
		command[0] = path+"VBoxManage modifyvm "+machineName+" --nic1 bridged";
		command[1] = path+"VBoxManage modifyvm "+machineName+" --bridgeadapter1 \""+interfaceHost+"\"";
		
		System.out.println("Comando => " + command[1]);
		
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(command[0]);
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		
//		try {
//			Process p2 = null;
//			p2 = Runtime.getRuntime().exec(path+"VBoxManage modifyvm "+machineName+" --bridgeadapter1 \""+interfaceHost+"\"");
//			p2.waitFor();
//		} catch (IOException e) {
//			System.out.println("EXC =>" + e.getStackTrace());
//		} catch (InterruptedException e) {
//			System.out.println("EXC 2 =>" + e.getStackTrace());
//		}
//		
//		try {
//			System.out.println("Esperando por mais 10 segundos");
//			Thread.sleep(10000);
//			System.out.println("Conclui a espera de novo");
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
		
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

	private File copyMediumToMachinesDir(String bootableMedium, String property) {

		String machineMedium = NodeConfig.getMachinesDir() + property + bootableMedium;

		try {
			String command = NodeConfig.getStorageToMachineScript() + " " + bootableMedium;
			LOG.debug("Will run command: " + command);
			OsUtil.runScript(command);
		} catch (IOException e) {
			LOG.error("Error on copy template to local");
		} catch (InterruptedException e) {
			LOG.error("Error on copy template to local");
		}

		return new File(machineMedium);
	}

	private IMachine createMachine(IVirtualBox vbox, String mediumLocation) {
		IMachine machine;

		List<IMachine> machines = vbox.getMachines();

		for (IMachine iMachine : machines) {
			String name = iMachine.getName();
			if (name.equals(vm.getName())) {
				LOG.warn("Virtual machine '" + vm.getName()
						+ "' already exists, cloudnode will remove");

				iMachine.unregister(CleanupMode.FULL);
				iMachine.delete(new ArrayList<IMedium>());
			}
			iMachine.release();
		}

		machine = vbox.createMachine(null, vm.getName(), vm.getOsType(),
				UUID.randomUUID(), false);

		// 3) attach new cloned disk to new virtual machine
		ISession sessionObject = new IWebsessionManager(vbox.port)
				.getSessionObject(vbox);

		String name = "SATA Controller";
		IStorageController sc = machine.addStorageController(name,
				StorageBus.SATA);
		sc.setControllerType(StorageControllerType.INTEL_AHCI);
		sc.setUseHostIOCache(false);
		sc.setPortCount(1);
		sc.release();

		ISystemProperties systemProperties = vbox.getSystemProperties();
		String defaultVRDEExtPack = systemProperties.getDefaultVRDEExtPack();
		/* add support for vrde server */
		IVRDEServer vrdeServer = machine.getVRDEServer();
		vrdeServer.setEnabled(true);
		vrdeServer.setAuthTimeout(5000);
		vrdeServer.setVRDEProperty("TCP/Ports", String.valueOf(vm.getVRDEPort()));
		vrdeServer.setVRDEProperty("TCP/Address", NodeConfig.getNodeAddress());
		vrdeServer.setAllowMultiConnection(true);
		vrdeServer.setVRDEExtPack(null);

		try {
			String hash = null;
			hash = SHAUtils.generateHash(vm.getVRDEPassword());
			machine.setExtraData("VBoxAuthSimple/users/" + vm.getVRDEUsername(), hash);

			vrdeServer.setVRDEExtPack(defaultVRDEExtPack);
			vrdeServer.setAuthType(AuthType.EXTERNAL);
			vrdeServer.setAuthLibrary("VBoxAuthSimple");
		} catch (NoSuchAlgorithmException e) {
			LOG.warn(e);
		}

		vrdeServer.release();

		IBIOSSettings biosSettings = machine.getBIOSSettings();
		biosSettings.setACPIEnabled(true);
		biosSettings.release();
		/*
		INetworkAdapter networkAdapter = machine.getNetworkAdapter(0);
		networkAdapter.setMACAddress(networkAdapter.getMACAddress());
		networkAdapter.setHostInterface(NodeConfig.getHostInterface());
		networkAdapter.setAdapterType(NetworkAdapterType.I_82540_EM);
		networkAdapter.setCableConnected(true);
		networkAdapter.setEnabled(true);

		networkAdapter.attachToBridgedInterface();
		networkAdapter.release();
		*/
		machine.saveSettings();
		vbox.registerMachine(machine);
		machine.lockMachine(sessionObject, LockType.SHARED);
		machine.release();

		machine = sessionObject.getMachine();

		IAudioAdapter audioAdapter = machine.getAudioAdapter();
		audioAdapter.setEnabled(true);
		audioAdapter.release();
		machine.setRTCUseUTC(true);
		IUSBController usbController = machine.getUSBController();
		usbController.setEnabled(true);
		usbController.release();

		machine.setCPUProperty(CPUPropertyType.PAE, false);
		
		machine.setMemorySize(vm.getMemorySize());
		machine.setCPUCount(vm.getNumberProcessors());
		machine.setCPUExecutionCap(30);
		
		IMedium medium = getSrcMedium(vbox, mediumLocation);
		machine.attachDevice(name, 0, 0, DeviceType.HARD_DISK, medium);
		medium.release();

		machine.saveSettings();
		sessionObject.unlockMachine();

		return machine;
	}

	/**
	 * Gets src medium registered or not
	 *
	 * @param vbox
	 * @param locationSrc
	 * @return
	 */
	private IMedium getSrcMedium(IVirtualBox vbox, String locationSrc) {
		List<IMedium> hardDisks = vbox.getHardDisks();

		String uuid = null;
		for (IMedium iMedium : hardDisks) {
			String location = iMedium.getLocation();

			if (location.equals(locationSrc)) {
				uuid = iMedium.getId().toString();
			}

			iMedium.release();
		}

		IMedium medium = null;
		if (uuid != null) {
			medium = vbox.findMedium(uuid, DeviceType.HARD_DISK);
		} else {
			medium = vbox.openMedium(locationSrc, DeviceType.HARD_DISK,
					AccessMode.READ_ONLY);
		}

		return medium;
	}

}
