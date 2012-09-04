package  educloud.cloudserver.managers;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import  educloud.cloudserver.configuration.ServerConfig;
import  educloud.cloudserver.rs.TemplateRest;
import  educloud.cloudserver.virtualbox.VirtualBoxConnector;
import  educloud.internal.entities.Template;


import org.virtualbox.AccessMode;
import org.virtualbox.DeviceType;
import org.virtualbox.IMediumAttachment;
import org.virtualbox.LockType;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.IMedium;
import org.virtualbox.service.IProgress;
import org.virtualbox.service.ISession;
import org.virtualbox.service.IStorageController;
import org.virtualbox.service.IVirtualBox;
import org.virtualbox.service.IWebsessionManager;


public class StorageManager {
	
	private String urlVirtualBox = ServerConfig.getUrlVirtualBox().toString();
	private String storageName = ServerConfig.getStorageName().toString();
	private String pathTemplates = ServerConfig.getPathTemplates().toString();
	
	private ArrayList<String> devices;
	private ArrayList<Integer> ports;
	
	private static Logger LOG = Logger.getLogger(TemplateRest.class);
	
	public StorageManager(){
		devices = new ArrayList<String>();
		devices.add(0, "/dev/sdd");
		devices.add(1, "/dev/sde");
		devices.add(2, "/dev/sdf");
		devices.add(3, "/dev/sdg");
		devices.add(4, "/dev/sdh");
		ports = new ArrayList<Integer>();
		ports.add(0, 3);
		ports.add(1, 4);
		ports.add(2, 5);
		ports.add(3, 6);
		ports.add(4, 7);
		ports.add(5, 8);
	}
	
	public boolean atachTemplateToStorage(List<Template> listaTemplates, 
			Template tpl){
		long start = System.nanoTime();
		LOG.debug("Running attach template in to Storage");
		
		try {
			IVirtualBox vbox = VirtualBoxConnector.connect(urlVirtualBox);
			IWebsessionManager manager = new IWebsessionManager(vbox.port);
			ISession session = manager.getSessionObject(vbox);
				
			IMachine machine = vbox.findMachine(storageName);
			
			machine.lockMachine(session, LockType.SHARED);							
			IMachine mutable = session.getMachine();
			
			IMedium medium = vbox.openMedium(pathTemplates + tpl.getFilename(), 
					DeviceType.HARD_DISK, AccessMode.READ_ONLY); 
	
			mutable.attachDevice(this.getStorageController(), this.getNextPort(listaTemplates), 0, 
					DeviceType.HARD_DISK, medium);
			
			mutable.saveSettings();
			session.unlockMachine();
			session.release();
			vbox.release();
			
			tpl.setPort(this.getNextPort(listaTemplates));
			tpl.setDevice(this.getNextDevice(listaTemplates));
		} catch (Exception e){
			LOG.warn("Exception atacching Template to Storage. " + e);
			return false;
		}
		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to attach in to storage: '" + elapsedTime + "'");
		
		return true;
	}
	
	public boolean detachTemplateFromStorage(Template tpl){
		try {
			IVirtualBox vbox = VirtualBoxConnector.connect(urlVirtualBox);
			IWebsessionManager manager = new IWebsessionManager(vbox.port);
			ISession session = manager.getSessionObject(vbox);
			IMachine machine = vbox.findMachine(storageName);
			machine.lockMachine(session, LockType.SHARED);							
			IMachine mutable = session.getMachine();
			mutable.detachDevice(this.getStorageController(), tpl.getPort(), 0);
			mutable.saveSettings();
			session.unlockMachine();
			session.release();
			vbox.release();
		} catch (Exception e){
			LOG.warn("Exception detacching Template to Storage. " + e);
			return false;
		}
		
		return true;
	}
	
	private String getNextDevice(List<Template> listaTemplates){
		return devices.get(listaTemplates.size());
	}
	
	private int getNextPort(List<Template> listaTemplates){
		return ports.get(listaTemplates.size());
	}
	
	public String getStorageController() throws Exception{
		IVirtualBox vbox = VirtualBoxConnector.connect(urlVirtualBox);
		IMachine machine = vbox.findMachine(storageName);
		String controller = null;
		for(IStorageController stController : machine.getStorageControllers()){
			if(stController.getName().contains("SATA"))
				controller = stController.getName();
		}
		if(controller.equals(null))
			throw new Exception("No CONTROLLER AVAILABLE");
		else
			return controller;
	}
	
	/*
	 * @todo: falta implementar
	 * 
	public boolean getAllDisksInMachine(String machineName){
		IVirtualBox vbox = VirtualBoxConnector.connect(urlVirtualBox);
		IWebsessionManager manager = new IWebsessionManager(vbox.port);
		ISession session = manager.getSessionObject(vbox);
		IMachine machine = vbox.findMachine(machineName);
		
		for(IMedium medium : vbox.getHardDisks()){
			System.out.println("\t => " + medium.getLocation());
		}
		
		return true;
	}
	*/
	
}
