package  educloud.cloudserver.scheduler.tasks;

import java.util.Calendar;

import org.apache.log4j.Logger;

import  educloud.cloudserver.database.dao.NodeDao;
import  educloud.cloudserver.database.dao.VirtualMachineDao;
import  educloud.cloudserver.nodecllient.ClientFactory;
import  educloud.cloudserver.nodecllient.NodeComunicationException;
import  educloud.cloudserver.nodecllient.VMNodeClient;
import  educloud.internal.entities.Node;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

/**
 * Classe para representar uma tarefa de parada de maquina virtual.
 *
 */
public class StopVMTask extends AbstractTask{

	public static final String VM_ID = "VM_ID";

	private static Logger LOG = Logger.getLogger(StopVMTask.class);


	public void run() {

		markAsRunning();

		// 1) load virtual machine from database
		String vmId = getParameter(VM_ID);
		VirtualMachine vm = VirtualMachineDao.getInstance().findById(Integer.parseInt(vmId));

		// 2) select the node of respective VM
		Node node = NodeDao.getInstance().findNodeById(vm.getNodeId());
		LOG.debug("Selected node of the virtual machine: #" + node.getId());

		// 3) send requisition for host
		VMNodeClient nodeClient = ClientFactory.createVMNodeClient(node);
		try {
			nodeClient.stopVM(vm);
			node.setAvailMemory(node.getAvailMemory()+vm.getMemorySize());
			node.setLastPing(Calendar.getInstance().getTime());
			NodeDao.getInstance().updateLastPing(node);
		} catch (NodeComunicationException e) {
			LOG.error("An error when stop virtual machine: #" + vm.getId(), e);
		}

		vm.setState(VMState.DONE);
		vm.setVRDEPassword(null);
		vm.setVRDEPort(0);
		vm.setVRDEUsername(null);

		VirtualMachineDao.getInstance().changeState(vm, true);

		markAsCompleted();
	}


	public String getType() {
		return "STOPVM";
	}
}