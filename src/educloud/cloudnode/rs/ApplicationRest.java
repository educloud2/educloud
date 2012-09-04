package  educloud.cloudnode.rs;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;

import org.apache.log4j.Logger;
import org.virtualbox.MachineState;
import org.virtualbox.service.IMachine;
import org.virtualbox.service.IVirtualBox;

import  educloud.cloudnode.configuration.NodeConfig;
import  educloud.cloudnode.virtualbox.VirtualBoxConnector;
import  educloud.internal.entities.MachineResourcesInfo;
import  educloud.internal.entities.Node;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;
import  com.google.gson.Gson;
import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Path("/application")
public class ApplicationRest {

	private static Gson gson = new Gson();

	private static Logger LOG = Logger.getLogger(ApplicationRest.class);

	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/check")
	public Response getStatus(String jsonNode) {

		LOG.debug("Returning application status");

		Node node = gson.fromJson(jsonNode, Node.class);

		try {
			IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig.getVirtualBoxWebservicesUrl());
			String version = vbox.getVersion();

			MachineResourcesInfo mri = new MachineResourcesInfo();

			long totalMemory = NodeConfig.getAvailableMemory();
			long usedMemory = countUsedMemory(vbox);

			mri.setTotalMemory(totalMemory);
			mri.setAvailableMemory(totalMemory - usedMemory);

			node.setAvailMemory(totalMemory - usedMemory);
			node.setTotalMemory(totalMemory);
			node.setMachinesReourcesInfo(mri);
			node.setVboxVersion(version);
			node.setConnectedToVBox(true);
			vbox.release();
		} catch (WebServiceException e) {
			LOG.error("Error on connect on vbox services", e);
			node.setVboxVersion(null);
			node.setConnectedToVBox(false);
		} catch (Error e) {
			LOG.error("Error on connect on vbox services", e);
			node.setVboxVersion(null);
			node.setConnectedToVBox(false);
		}

		return Response.ok(gson.toJson(node), MediaType.APPLICATION_JSON)
				.build();
	}

	private long countUsedMemory(IVirtualBox vbox) {
		List<IMachine> machines = vbox.getMachines();

		long usedMemory = 0;

		for (IMachine iMachine : machines) {
			usedMemory = usedMemory + iMachine.getMemorySize();
		}

		return usedMemory;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/checkVm2")
	public Response getStatusVm2(String jsonVm) {

		LOG.debug("Returning application status do nosso teste");
		/*
		VirtualMachine vm = new VirtualMachine();

		try {
			IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig
					.getVirtualBoxWebservicesUrl());

			if (vm.getUUID() != null) {
				IMachine machine = vbox.findMachine(vm.getUUID());

				if (machine != null) {
					if (machine.getState() == MachineState.RUNNING) {
						vm.setState(VMState.RUNNING);
					} else if (machine.getState() == MachineState.STOPPING) {
						vm.setState(VMState.SHUTDOWN);
					} else if (machine.getState() == MachineState.STARTING) {
						vm.setState(VMState.BOOT);
					} else if (machine.getState() == MachineState.POWERED_OFF) {
						vm.setState(VMState.DONE);
					} else {
						vm.setState(VMState.UNKNOWN);
					}

					machine.release();
				}
			}

			vbox.release();
		} catch (WebServiceException e) {
			LOG.error("Error on connect on vbox services to check vm", e);
		} catch (Error e) {
			LOG.error("Error on connect on vbox services to check vm", e);
		}
		*/
		return Response.ok(gson.toJson("{\"notas\" : \"teste\"}"), MediaType.APPLICATION_JSON).build();
	}
	
	
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/checkVm")
	public Response getStatusVm(String jsonVm) {

		LOG.debug("Returning application status");

		VirtualMachine vm = gson.fromJson(jsonVm, VirtualMachine.class);
		IMachine machine;
		IVirtualBox vbox = VirtualBoxConnector.connect(NodeConfig
				.getVirtualBoxWebservicesUrl());
		try {
			if (vm.getUUID() != null) {
				machine = vbox.findMachine(vm.getUUID());
			} else {
				machine = vbox.findMachine(vm.getName());
			}
		} catch (WebServiceException e) {
			LOG.error("Error on connect on vbox services to check vm", e);
			machine = null;
		} catch (Error e) {
			LOG.error("Error on connect on vbox services to check vm", e);
			try {
				machine = vbox.findMachine(vm.getName());
			} catch(Error e2){
				LOG.error("Error on check vm", e2);
				machine = null;
			}
		} finally {
			vbox.release();
		}
		
		if (machine != null) {
			if (machine.getState() == MachineState.RUNNING) {
				vm.setState(VMState.RUNNING);
			} else if (machine.getState() == MachineState.STOPPING) {
				vm.setState(VMState.SHUTDOWN);
			} else if (machine.getState() == MachineState.STARTING) {
				vm.setState(VMState.BOOT);
			} else if (machine.getState() == MachineState.POWERED_OFF) {
				vm.setState(VMState.DONE);
			} else {
				vm.setState(VMState.UNKNOWN);
			}

			machine.release();
		}

		return Response.ok(gson.toJson(vm), MediaType.APPLICATION_JSON).build();
	}

}
