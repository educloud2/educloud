package  educloud.cloudnode.scheduler.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.ws.WebServiceException;

import org.apache.log4j.Logger;
import org.virtualbox.AccessMode;
import org.virtualbox.DeviceType;
import org.virtualbox.StorageBus;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.IMedium;
import org.virtualbox.service.IProgress;
import org.virtualbox.service.ISession;
import org.virtualbox.service.IVirtualBox;
import org.virtualbox.service.IWebsessionManager;

import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.serverclient.VirtualMachineClient;
import  educloud.cloudnode.util.OsUtil;
import  educloud.cloudnode.virtualbox.VirtualBoxConnector;
import  educloud.internal.entities.Template;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

public class CreateVmTask extends AbstractTask {

	private static Logger LOG = Logger.getLogger(CreateVmTask.class);

	/**
	 * keep information about lock of template file
	 */
	private static ConcurrentHashMap<Integer, ReentrantReadWriteLock> lockMap = new ConcurrentHashMap<Integer, ReentrantReadWriteLock>();

	/**
	 * keep information about lock's number by template
	 */
	private static ConcurrentHashMap<Integer, Integer> templateUsageLock = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * control concurrency of {CreateVmTask#lockMap} and {CreateVmTask#templateUsageLock} information
	 */
	private static ReentrantLock usageUpdateLock = new ReentrantLock();

	private VirtualMachine vm;

	private Template template;
	
	//new Implementation
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

		LOG.debug("Running create virtual machine task");

		IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig.getVirtualBoxWebservicesUrl());

		try {
			createVMISCSI(vbox);
		} catch (Exception e) {
			e.printStackTrace();
		}

		vm.setState(VMState.DONE);

		// 2) change virtual machine state
		new VirtualMachineClient().changeState(vm);

		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to create a new machine: '" + elapsedTime + "'");
	}
	
	public void createVMISCSI(IVirtualBox vbox){
		String nameVM = vm.getName();		
		
		this.diskName = "vl_vm_" + vm.getId();
		this.targetName = "iqn.educloud:" + diskName;
		
		/*
		// Criando Maquina virtual
		manager = new IWebsessionManager(vbox.port);
		session = manager.getSessionObject(vbox);
		IMachine machine = vbox.createMachine(null, nameVM, template.getOsType(), null, true);
		machine.setMemorySize(template.getMemorySize());
		machine.setCPUCount(template.getNumberProcessors());
		machine.setCPUExecutionCap(30);
		machine.addStorageController("scsi", StorageBus.SCSI);
		vbox.registerMachine(machine);
		*/
		createLogicalVolume();
		
		//Chamando metodo para clonar o template para no novo LV
		cloneTemplateISCSI();		
	}
	
	private void createLogicalVolume(){		
		 educloud.cloudnode.util.Conexao conection = new  educloud.cloudnode.util.Conexao();		
		String command = "lvcreate -L 2GB -n " + diskName + " vg_educloud";		
		conection.execute(command, "created");
	}
	
	private void cloneTemplateISCSI(){
		 educloud.cloudnode.util.Conexao conection = new  educloud.cloudnode.util.Conexao();
		String command;
		if(vm.getMachineParent() > 0){
			command = "dd if=/dev/vg_educloud/vl_vm_" + vm.getMachineParent() + " of=/dev/vg_educloud/" + diskName;
		} else {
			command = "dd if=" + template.getDevice() + " of=/dev/vg_educloud/" + diskName;
		}
		conection.execute(command, null);
	}

	private void cloneTemplate(IVirtualBox vbox) throws FileNotFoundException {

		int idVm = vm.getId();
		int idTemplate = template.getId();

		String property = System.getProperty("file.separator");
		String fileName = "disk-machine-" + idVm + "-template-" + idTemplate + ".vdi";
		String templateSrc = NodeConfig.getTemplateDir() + property + template.getFilename();
		String localStorage = NodeConfig.getLocalStorageDir();
		String tplSrcOnLocalStorage = localStorage + property + template.getFilename();

		addTemplateUsage();

		lockMap.get(template.getId()).readLock().lock();

		if (!new File(templateSrc).exists()) {
			lockMap.get(template.getId()).readLock().unlock();
			throw new FileNotFoundException("Invalid template location '" + templateSrc + "'.");
		}

		// check (and copy as need) if template is on local storage
		checkLocalTemplate();

		// make clone of medium inside local storage
		processLocalClone(vbox, fileName, tplSrcOnLocalStorage, localStorage + property + fileName);

		lockMap.get(template.getId()).readLock().unlock();

		if (lockMap.get(template.getId()).writeLock().tryLock()) {
			removeLocalTemplateFile(property, fileName, localStorage, tplSrcOnLocalStorage);
			lockMap.get(template.getId()).writeLock().unlock();
		}

		removeTemplateUsage();

		copyLocalToStorage(property, localStorage, fileName);
	}

	private void addTemplateUsage() {

		usageUpdateLock.lock();

		if (!lockMap.containsKey(template.getId())) {
			ReentrantReadWriteLock templateLock = new ReentrantReadWriteLock();
			lockMap.put(template.getId(), templateLock);
		}

		if (!templateUsageLock.containsKey(template.getId())) {
			templateUsageLock.put(template.getId(), 0);
		}

		Integer integer = templateUsageLock.get(template.getId());
		templateUsageLock.put(template.getId(), integer + 1);

		usageUpdateLock.unlock();

	}

	private void removeTemplateUsage() {

		usageUpdateLock.lock();

		Integer integer = templateUsageLock.get(template.getId());
		integer = integer - 1;
		if (integer < 1) {
			templateUsageLock.remove(template.getId());
			lockMap.remove(template.getId());
		} else {
			templateUsageLock.remove(integer);
		}

		usageUpdateLock.unlock();

	}

	private void removeLocalTemplateFile(String property, String fileName, String localStorage, String templateSrcOnLocalStorage) {
		// remove template from local storage
		try {
			new File(templateSrcOnLocalStorage).delete();
			LOG.debug("Local template '" + templateSrcOnLocalStorage + "' removed");
		} catch (SecurityException e) {
			LOG.warn("Impossible delete '" + templateSrcOnLocalStorage + "'");
		}
	}

	private void copyLocalToStorage(String property, String localStorage, String fileName) {
		// copy template to local storage
		long start = System.nanoTime();
		try {
			String command = NodeConfig.getLocalToStorageScript() + " " + fileName;
			LOG.debug("will run command: '" + command + "'");
			OsUtil.runScript(command);
		} catch (IOException e) {
			LOG.error("Error on copy local to storage", e);
		} catch (InterruptedException e) {
			LOG.error("Error on copy local to storage", e);
		}

		try {
			new File(localStorage + property + fileName).delete();
			LOG.debug("Local template '" + localStorage + property + fileName + "' removed");
		} catch (SecurityException e) {
			LOG.warn("Impossible delete '" + localStorage + property + fileName + "'", e);
		}
		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to copy from localstorage to storage: '" + elapsedTime + "'");
	}

	private void processLocalClone(IVirtualBox vbox, String fileName,
			String templateSrc, String storageTarget) {
		vm.setBootableMedium(fileName);

		LOG.debug("waiting template clone...");
		long start = System.nanoTime();

		IMedium target = vbox.createHardDisk("VDI", storageTarget);
		/* support only standard variant type */
		IProgress progess = null;
		boolean completed = false;

		IMedium src = getSrcMedium(vbox, templateSrc);
		templateSrc = src.getLocation();

		LOG.debug("will clone: " + templateSrc + " to " + storageTarget);

		progess = src.cloneTo(target, 0, null);

		completed = false;
		do {
			completed = progess.getCompleted();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				LOG.warn(e);
			}
		} while (!completed);
		long end = System.nanoTime();

		LOG.debug("end template clone. Elapsed time: " + (end - start)
				/ 1000000000.0);

		// close used disks
		target.close();


		try {
			src.close();
		} catch (WebServiceException e) {
		} catch (Error e) {
		}

		// release vbox objects
		progess.release();
		src.release();
		target.release();
	}

	private void checkLocalTemplate() {
		String localStorage = NodeConfig.getLocalStorageDir();
		String property = System.getProperty("file.separator");
		String templateSrcOnLocalStorage = localStorage + property + template.getFilename();

		LOG.debug("check file: '" + templateSrcOnLocalStorage + "'");

		if (!new File(templateSrcOnLocalStorage).exists()) {
			lockMap.get(template.getId()).readLock().unlock();
			// copy template to local storage
			lockMap.get(template.getId()).writeLock().lock();
			if (!new File(templateSrcOnLocalStorage).exists()) {
				try {
					long start = System.nanoTime();
					String command = NodeConfig.getTemplateToLocalScript() + " " + template.getFilename();
					LOG.debug("will run command: '" + command + "'");
					OsUtil.runScript(command);
					long end = System.nanoTime();
					double elapsedTime = ((end - start)) / 1000000000.0;
					LOG.debug("Elapsed time copy from template to storage: '" + elapsedTime + "'");
				} catch (IOException e) {
					LOG.error("Error on copy template to local", e);
				} catch (InterruptedException e) {
					LOG.error("Error on copy template to local", e);
				}
			}
			lockMap.get(template.getId()).readLock().lock();
			lockMap.get(template.getId()).writeLock().unlock();
		}
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

			String registeredName = new File(location).getName();
			String srcName = new File(locationSrc).getName();

			if (registeredName.equals(srcName)) {
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

	public void setTemplate(Template template) {
		this.template = template;
	}

}
