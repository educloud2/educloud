package  educloud.gui.beans;

import java.util.List;

import javax.servlet.http.HttpSession;

import  educloud.api.EduCloudAuthorization;
import  educloud.api.EduCloudFactory;
import  educloud.api.clients.EduCloudTemplateClient;
import  educloud.api.clients.EduCloudVMClient;
import  educloud.api.entities.Template;
import  educloud.api.entities.VirtualMachine;
import  educloud.api.entities.exceptions.EduCloudServerException;

public class VirtualMachineBean {

	private String name;

	private String description;

	private String template;
	
	public String scaleout;

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setTemplate(String template) {
		this.template= template;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getTemplate() {
		return template;
	}
	
	public String getScaleout() {
		return scaleout;
	}

	public void setScaleout(String scaleout) {
		this.scaleout = scaleout;
	}

	public void createVirtualMachine(HttpSession session) throws EduCloudServerException {
		EduCloudAuthorization auth = (EduCloudAuthorization)session.getAttribute("CLOUD_AUTHENTICATION");

		EduCloudVMClient vmClient = EduCloudFactory.createVMClient(auth);
		EduCloudTemplateClient templateClient = EduCloudFactory.createTemplateClient(auth);

		int tplId = Integer.parseInt(this.template);
		Template loadedTemplate = templateClient.getTemplate(tplId);

		VirtualMachine virtualMachine = new VirtualMachine();
		virtualMachine.setName(name);
		virtualMachine.setDescription(description);
		virtualMachine.setUserId(auth.getUser().getId());
		
		if(scaleout.equals("true")){
			virtualMachine.setWithLoadBalancer(true);
			System.out.println("=> Veio para criar com scale Out");
		}
		vmClient.createVM(virtualMachine, loadedTemplate);
	}
	
	public void changeVirtualMachine(HttpSession session) throws EduCloudServerException {
		
	}

	public List<VirtualMachine> getVirtualMachines(HttpSession session) throws EduCloudServerException {
		EduCloudAuthorization auth = (EduCloudAuthorization)session.getAttribute("CLOUD_AUTHENTICATION");
		EduCloudVMClient createVMClient = EduCloudFactory.createVMClient(auth);
		return createVMClient.describeInstances();
	}
}
