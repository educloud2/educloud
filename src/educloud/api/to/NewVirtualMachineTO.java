package  educloud.api.to;

import  educloud.api.entities.Template;
import  educloud.api.entities.VirtualMachine;

public class NewVirtualMachineTO {

	private VirtualMachine virtualMachine;

	private Template template;

	public VirtualMachine getVirtualMachine() {
		return virtualMachine;
	}

	public void setVirtualMachine(VirtualMachine virtualMachine) {
		this.virtualMachine = virtualMachine;
	}

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

}
