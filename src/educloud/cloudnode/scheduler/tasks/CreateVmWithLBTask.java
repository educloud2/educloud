package  educloud.cloudnode.scheduler.tasks;

import org.apache.log4j.Logger;
import  educloud.cloudnode.serverclient.VirtualMachineClient;
import  educloud.internal.entities.Template;
import  educloud.internal.entities.VirtualMachine;
import  educloud.internal.entities.VirtualMachine.VMState;

public class CreateVmWithLBTask extends AbstractTask {
	private static Logger LOG = Logger.getLogger(CreateVmTask.class);
	private VirtualMachine vm;
	private Template template;
	private String diskName;
	private String diskLBName;

	public void setVirtualMachine(VirtualMachine vm) {
		this.vm = vm;
	}
	

	public void run() {
		long start = System.nanoTime();

		LOG.debug("Running create virtual machine task");

		try {
			createVMISCSI();
		} catch (Exception e) {
			e.printStackTrace();
		}

		vm.setState(VMState.DONE);

		new VirtualMachineClient().changeState(vm);

		long end = System.nanoTime();
		double elapsedTime = ((end - start)) / 1000000000.0;
		LOG.debug("Elapsed time to create a new machine: '" + elapsedTime + "'");
		
	}
	
	public void createVMISCSI(){
		this.diskName = "vl_vm_" + vm.getId();
		this.diskLBName = "vl_lb_" + vm.getId();
		createLogicalVolume();
		cloneTemplateISCSI();		
	}
	
	private void createLogicalVolume(){		
		 educloud.cloudnode.util.Conexao conection = new  educloud.cloudnode.util.Conexao();		
		String command = "lvcreate -L 2GB -n " + diskName + " vg_educloud";		
		conection.execute(command, "created");
		
		String command2 = "lvcreate -L 2GB -n " + diskLBName + " vg_educloud";		
		conection.execute(command2, "created");
	}
	
	private void cloneTemplateISCSI(){
		 educloud.cloudnode.util.Conexao conection = new  educloud.cloudnode.util.Conexao();
		String command = "dd if=" + template.getDevice() + " of=/dev/vg_educloud/" + diskName;
		conection.execute(command, null);
		
		String command2 = "dd if=/dev/sdc of=/dev/vg_educloud/" + diskLBName;
		conection.execute(command2, null);
	}
	
	public void setTemplate(Template template) {
		this.template = template;
	}

}

