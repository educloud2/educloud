package  educloud.cloudserver.scheduler.tasks;

import java.util.Calendar;

import org.apache.log4j.Logger;

import  educloud.cloudserver.database.dao.NodeDao;
import  educloud.cloudserver.database.dao.UserDao;
import  educloud.cloudserver.database.dao.VirtualMachineDao;
import  educloud.cloudserver.database.dao.VirtualMachineLogDao;
import  educloud.cloudserver.entity.User;
import  educloud.cloudserver.nodecllient.ClientFactory;
import  educloud.cloudserver.nodecllient.NodeComunicationException;
import  educloud.cloudserver.nodecllient.VMNodeClient;
import  educloud.cloudserver.selector.NodeSelectorManager;
import  educloud.internal.entities.Node;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;


public class StartVmTask extends AbstractTask {

	public static final String PARAM_MACHINE_ID = "VM_ID";

	private static Logger LOG = Logger.getLogger(StartVmTask.class);

	
	public void run() {

		markAsRunning();

		// 1) load virtual machine from database
		String vmId = getParameter(PARAM_MACHINE_ID);
		VirtualMachine vm = VirtualMachineDao.getInstance().findById(Integer.parseInt(vmId));

		User user = UserDao.getInstance().findById(vm.getUserId());
		vm.setVRDEPassword(user.getPass());
		vm.setVRDEUsername(user.getLogin());
		vm.setVRDEPort(VirtualMachineDao.getInstance().findNextPort(vm.getId()));

		// 2) select a registered host
		Node node = NodeSelectorManager.getSelector().getNext(vm);

		if (null == node) {
			LOG.warn("The virtual machine #" + vmId + " cannot be started, no instance of cloudnode available");
			VirtualMachineLogDao.getInstance().insert(Integer.parseInt(vmId), "Error on start virtual machine " + vmId + ", no instance of cloudnode is available.");

			// will try start virtual machine after 10 min.
			vm.setState(VMState.PENDING);

			// change virtual machine state to pending
			VirtualMachineDao.getInstance().changeState(vm);

			// reschedule this task
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MINUTE, 2);
			reschedule(calendar.getTime());
			return;
		}

		node.setAvailMemory(node.getTotalMemory()-vm.getMemorySize());
		NodeDao.getInstance().updateLastPing(node);

		vm.setNodeId(node.getId());
		LOG.debug("Selected node for new virtual machine: #" + node.getId());

		// 3) send requisition for host
		VMNodeClient nodeClient = ClientFactory.createVMNodeClient(node);
		try {
			nodeClient.startVM(vm);
		} catch (NodeComunicationException e) {
			LOG.error("An error when start virtual machine: #" + vm.getId(), e);
		}

		VirtualMachineDao.getInstance().updateNode(vm.getId(), vm.getNodeId());

		markAsCompleted();
	}


	public String getType() {
		return "STARTVM";
	}

}
