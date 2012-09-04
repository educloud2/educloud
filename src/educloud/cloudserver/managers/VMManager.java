package  educloud.cloudserver.managers;

import org.apache.log4j.Logger;

import  educloud.cloudserver.database.dao.TaskDao;
import  educloud.cloudserver.database.dao.VirtualMachineDao;
import  educloud.cloudserver.rs.VMRest;
import  educloud.cloudserver.scheduler.tasks.AbstractTask;
import  educloud.cloudserver.scheduler.tasks.ChangeVmTask;
import  educloud.cloudserver.scheduler.tasks.CloudTask.Status;
import  educloud.cloudserver.scheduler.tasks.CreateVmTask;
import  educloud.cloudserver.scheduler.tasks.RemoveVmTask;
import  educloud.cloudserver.scheduler.tasks.StartVmTask;
import  educloud.cloudserver.scheduler.tasks.StopVMTask;
import  educloud.cloudserver.selector.NodeSelectorManager;
import  educloud.internal.entities.Node;
import  educloud.internal.entities.Template;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

public class VMManager {

	private static Logger LOG = Logger.getLogger(VMRest.class);
	
	public void CreateVM(VirtualMachine vm, Template template) {

		vm.setState(VMState.PENDING);

		VirtualMachineDao.getInstance().insert(vm, template);

		//if(vm.isWithLoadBalancer()){
			//System.out.println("Cheguei na ultima camada, que cria a TASK com o withLB " + vm.isWithLoadBalancer());
		//} else {
			AbstractTask createVmTask = new CreateVmTask();
			createVmTask.setStatus(Status.PENDING);
			createVmTask.setParameter(CreateVmTask.PARAM_MACHINE_ID, String.valueOf(vm.getId()));
			createVmTask.setParameter(CreateVmTask.PARAM_TEMPLATE_ID, String.valueOf(template.getId()));
			
			TaskDao.getInstance().insert(createVmTask);
		//}
	}
	
	public void cloneVM(VirtualMachine vm) {
		vm.setState(VMState.PENDING);
		
		Node node = NodeSelectorManager.getSelector().getNext(vm);
		vm.setNodeId(node.getId());
		VirtualMachineDao.getInstance().insert(vm);
		LOG.debug("VM " + vm.getName() + " schedelued for clonning with node id = " + node.getId());
		

		AbstractTask createVmTask = new CreateVmTask();
		createVmTask.setStatus(Status.PENDING);
		createVmTask.setParameter(CreateVmTask.PARAM_MACHINE_ID, String.valueOf(vm.getId()));
		
		TaskDao.getInstance().insert(createVmTask);
	}

	public VirtualMachine scheduleStartVM(VirtualMachine vm) {

		vm.setState(VMState.PENDING);

		AbstractTask startVmTask = new StartVmTask();
		startVmTask.setStatus(Status.PENDING);
		startVmTask.setParameter(StartVmTask.PARAM_MACHINE_ID, String.valueOf(vm.getId()));

		VirtualMachineDao.getInstance().changeState(vm);

		TaskDao.getInstance().insert(startVmTask);

		return vm;
	}

	public VirtualMachine scheduleStopVM(VirtualMachine vm) {

		vm.setState(VMState.SHUTDOWN);

		AbstractTask stopVmTask = new StopVMTask();
		stopVmTask.setStatus(Status.PENDING);
		stopVmTask.setParameter(StopVMTask.VM_ID, String.valueOf(vm.getId()));

		VirtualMachineDao.getInstance().changeState(vm);

		TaskDao.getInstance().insert(stopVmTask);

		return vm;
	}
	
	public VirtualMachine scheduleChangeVM(VirtualMachine vm) {

		vm.setState(VMState.PENDING);
		LOG.debug("Agendando a task para a vm " + vm.getName() + " AGORA!");
		ChangeVmTask changeVmTask = new ChangeVmTask();
		changeVmTask.setStatus(Status.PENDING);
		changeVmTask.setParameter(changeVmTask.VM_ID, String.valueOf(vm.getId()));

		VirtualMachineDao.getInstance().update(vm);

		TaskDao.getInstance().insert(changeVmTask);
		LOG.debug("Consegui agendar com sucesso " + vm.getName());
		return vm;
	}
	

	//Escalona uma tarefa de remocao de maquina virtual.
	public void scheduleRemoveVM(VirtualMachine vm) {

		vm.setState(VMState.PENDING);
		VirtualMachineDao.getInstance().changeState(vm);
		
		AbstractTask removeVmTask = new RemoveVmTask();
		removeVmTask.setStatus(Status.PENDING);
		removeVmTask.setParameter(RemoveVmTask.VM_ID, String.valueOf(vm.getId()));

		TaskDao.getInstance().insert(removeVmTask);
	}
}