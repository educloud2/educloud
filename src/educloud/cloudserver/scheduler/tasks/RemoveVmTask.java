package  educloud.cloudserver.scheduler.tasks;

import org.apache.log4j.Logger;

import  educloud.cloudserver.database.dao.VirtualMachineDao;
import  educloud.cloudserver.nodecllient.ClientFactory;
import  educloud.cloudserver.nodecllient.NodeComunicationException;
import  educloud.cloudserver.nodecllient.VMNodeClient;
import  educloud.cloudserver.selector.INodeSelector;
import  educloud.cloudserver.selector.NodeSelectorManager;
import  educloud.internal.entities.Node;
import  educloud.internal.entities.VirtualMachine;

public class RemoveVmTask extends AbstractTask {

	public static final String VM_ID = "VM_ID";

	private static Logger LOG = Logger.getLogger(RemoveVmTask.class);


	public void run() {

		markAsRunning();

		// 1) load virtual machine from database
		String vmId = getParameter(VM_ID);
		LOG.debug("VMID que vou deletar = " + vmId);
		VirtualMachine vm = VirtualMachineDao.getInstance().findById(
				Integer.parseInt(vmId));
		LOG.debug("Maquina carregada =  = " + vm.getName());
		INodeSelector selector = NodeSelectorManager.getSelector(); 
		
		Node node = selector.getNext(vm);

		if (null != node) {
			VMNodeClient nodeClient = ClientFactory.createVMNodeClient(node);
			try {
				nodeClient.removeVM(vm);
			} catch (NodeComunicationException e) {
				LOG.error("An error when remove virtual machine: #" + vm.getId(), e);
			}
		}

		VirtualMachineDao.getInstance().remove(vm);

		markAsCompleted();

	}

	
	public String getType() {
		// TODO Auto-generated method stub
		return "REMOVEVM";
	}

}
