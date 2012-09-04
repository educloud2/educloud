package  educloud.cloudnode.rs;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import  educloud.cloudnode.scheduler.Scheduler;
import  educloud.cloudnode.scheduler.tasks.ChangeVmTask;
import  educloud.cloudnode.scheduler.tasks.CreateVmTask;
import  educloud.cloudnode.scheduler.tasks.CreateVmWithLBTask;
import  educloud.cloudnode.scheduler.tasks.RemoveVmTask;
import  educloud.cloudnode.scheduler.tasks.RemoveVmWithLBTask;
import  educloud.cloudnode.scheduler.tasks.StartVmTask;
import  educloud.cloudnode.scheduler.tasks.StartVmWithLB;
import  educloud.cloudnode.scheduler.tasks.StopVmTask;
import  educloud.internal.entities.Template;
import  educloud.internal.entities.VirtualMachine;
import  educloud.to.NewVirtualMachineTO;
import  com.google.gson.Gson;
import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Path("/vm")
public class VMRest {

	private static Gson gson = new Gson();

	private static Logger LOG = Logger.getLogger(VMRest.class);

	/**
	 * this method will schedule a new allocation of a virtual machine
	 *
	 * @param machine
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/create")
	public Response createVM(String machine) {

		LOG.debug("Application will start a new VM");
		LOG.debug(machine);

		NewVirtualMachineTO vmTO = gson.fromJson(machine, NewVirtualMachineTO.class);

		VirtualMachine vm = vmTO.getVirtualMachine();
		Template template = vmTO.getTemplate();
		if(vm.getLoadBalancer() != null){
			vm.setWithLoadBalancer(true);
			System.out.println("Criarei a maquina " + vm.getName() + " e o LB = " + vm.getLoadBalancer());
			CreateVmWithLBTask createVmTask = new CreateVmWithLBTask();
			createVmTask.setVirtualMachine(vm);
			createVmTask.setTemplate(template);

			Scheduler.getInstance().addTask(createVmTask);
		} else {
			/* setup task */
			CreateVmTask createVmTask = new CreateVmTask();
			createVmTask.setVirtualMachine(vm);
			createVmTask.setTemplate(template);

			/* add task to scheduler */
			Scheduler.getInstance().addTask(createVmTask);
		}
		// return a new created virtual machine
		return Response.ok(gson.toJson(""), MediaType.APPLICATION_JSON).build();
	}

	/**
	 * this method will schedule a new allocation of a virtual machine
	 *
	 * @param machine
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/start")
	public Response startVM(String machine) {

		LOG.debug("Application will start a new VM");
		LOG.debug(machine);

		VirtualMachine vm = gson.fromJson(machine, VirtualMachine.class);

		if(vm.getLoadBalancer() != null){
			StartVmWithLB startVmTask = new StartVmWithLB();
			startVmTask.setVirtualMachine(vm);
	
			Scheduler.getInstance().addTask(startVmTask);
		} else {
			/* setup task */
			StartVmTask startVmTask = new StartVmTask();
			startVmTask.setVirtualMachine(vm);
	
			/* add task to scheduler */
			Scheduler.getInstance().addTask(startVmTask);
		}

		// return a new created virtual machine
		return Response.ok(gson.toJson(""), MediaType.APPLICATION_JSON).build();
	}

	/**
	 * this method will schedule a task to stop a virtual machine.
	 *
	 * @param machine
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/stop")
	public Response stopVM(String machine) {

		LOG.debug("Application will stop a VM");
		LOG.debug(machine);

		VirtualMachine vm = gson.fromJson(machine, VirtualMachine.class);

		/* setup task */
		StopVmTask stopVmTask = new StopVmTask();
		stopVmTask.setVirtualMachine(vm);

		/* add task to scheduler */
		Scheduler.getInstance().addTask(stopVmTask);

		// return virtual machine
		return Response.ok(gson.toJson(vm), MediaType.APPLICATION_JSON).build();
	}
	
	/**
	 * this method will schedule a task to change a virtual machine.
	 *
	 * @param machine
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/change")
	public Response changeVM(String machine) {

		LOG.debug("Application will change a VM");
		LOG.debug("Machine which i will change: " + machine);

		VirtualMachine vm = gson.fromJson(machine, VirtualMachine.class);

		LOG.debug("Carreguei a maquina: " + vm.getName());
		LOG.debug("Carreguei a maquina: " + vm.getCapProcessor());
		LOG.debug("Carreguei a maquina: " + vm.getMemorySize());
		LOG.debug("Carreguei a maquina: " + vm.getNumberProcessors());
		
		/* setup task */
		ChangeVmTask changeVmTask = new ChangeVmTask();
		changeVmTask.setVirtualMachine(vm);

		/* add task to scheduler */
		Scheduler.getInstance().addTask(changeVmTask);

		// return virtual machine
		return Response.ok(gson.toJson(vm), MediaType.APPLICATION_JSON).build();
	}
	
	/**
	 * this method will schedule a task to change a virtual machine.
	 *
	 * @param machine
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/clone")
	public Response cloneVM(String machine) {

		LOG.debug("Application will clone a VM");
		LOG.debug("Machine which i will clone is: " + machine);
		
		VirtualMachine vm = gson.fromJson(machine, VirtualMachine.class);
		
		LOG.debug("Id Machine which i will clone is: " + vm.getId());
		
		/*
		

		LOG.debug("Carreguei a maquina: " + vm.getName());
		LOG.debug("Carreguei a maquina: " + vm.getCapProcessor());
		LOG.debug("Carreguei a maquina: " + vm.getMemorySize());
		LOG.debug("Carreguei a maquina: " + vm.getNumberProcessors());
		
		ChangeVmTask changeVmTask = new ChangeVmTask();
		changeVmTask.setVirtualMachine(vm);

		Scheduler.getInstance().addTask(changeVmTask);
		*/
		// return virtual machine
		return Response.ok(gson.toJson(vm), MediaType.APPLICATION_JSON).build();
	}

	/**
	 * this method will schedule a task to remove a virtual machine.
	 *
	 * @param machine
	 * @return
	 */
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/remove")
	public Response removeVM(String machine) {

		LOG.debug("Application will remove a VM");
		LOG.debug(machine);

		VirtualMachine vm = gson.fromJson(machine, VirtualMachine.class);

		if(vm.getLoadBalancer() != null){
			RemoveVmWithLBTask removeVmTask = new RemoveVmWithLBTask();
			removeVmTask.setVirtualMachine(vm);
			Scheduler.getInstance().addTask(removeVmTask);
		} else {
			/* setup task */
			RemoveVmTask removeVmTask = new RemoveVmTask();
			removeVmTask.setVirtualMachine(vm);

			/* add task to scheduler */
			Scheduler.getInstance().addTask(removeVmTask);
		}

		return Response.ok(gson.toJson(vm), MediaType.APPLICATION_JSON).build();
	}

}
