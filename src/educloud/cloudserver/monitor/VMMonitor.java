package  educloud.cloudserver.monitor;

import java.util.List;

import org.apache.log4j.Logger;

import  educloud.cloudserver.database.dao.VirtualMachineDao;
import  educloud.cloudserver.nodecllient.ClientFactory;
import  educloud.cloudserver.nodecllient.NodeClient;
import  educloud.cloudserver.nodecllient.NodeComunicationException;
import  educloud.internal.entities.Node;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

public class VMMonitor implements Runnable {

	private static Logger LOG = Logger.getLogger(VMMonitor.class);

	
	public void run() {

		while(true){

			// create webservice client
			NodeClient nodeClient = null;

			// get all virtual machines
			List<VirtualMachine> machines = VirtualMachineDao.getInstance().getAll();

			for ( VirtualMachine vm : machines ) {

				if (vm.getState() == VMState.DONE) {
					continue; // skip machine on DONE state
				}

				int nodeId = vm.getNodeId();
				Node node = new Node();
				node.setId(nodeId);

				// check only machines allocated in a node
				if (nodeId > 0 && vm.getUUID() != null) {

					LOG.debug("cloud server will try contact machine #" + vm.getId() + " on node: " + nodeId);

					// 	Create webservice client
					nodeClient = ClientFactory.createNodeClient(node);

					// Call node service
					try {
						LOG.debug("will call machine #" + 
								vm.getId()+ " name: " + vm.getName() + 
								" state = " + vm.getState());
						//LOG.debug("will call machine #"+ vm.getId()+ " name: " + vm.getName());
						
						//if(!vm.getState().equals(VirtualMachine.VMState.PENDING))
						vm = nodeClient.checkVMStatus(vm);
						
					} catch (NodeComunicationException e) {
						LOG.error("Error on try contact node #"+ node.getId()+". server will change state of VM to 'UNKNOWN'.");

						//Coloca a maquina no estado UNKNOWN.
						vm.setState(VMState.UNKNOWN);
					}

					//Atualiza o estado da maquina virtual.
					VirtualMachineDao.getInstance().changeState(vm);

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// 	TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			try {
				Thread.sleep(500000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
