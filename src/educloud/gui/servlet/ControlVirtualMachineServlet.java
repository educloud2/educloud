package  educloud.gui.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import  educloud.api.EduCloudAuthorization;
import  educloud.api.EduCloudFactory;
import  educloud.api.clients.EduCloudVMClient;
import  educloud.api.entities.VirtualMachine;
import  educloud.api.entities.exceptions.EduCloudServerException;

public class ControlVirtualMachineServlet extends HttpServlet {

	private static final long serialVersionUID = 2176426477092628127L;

	private static Logger LOG = Logger.getLogger(ControlVirtualMachineServlet.class);

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException {

		String parameterId = request.getParameter("id");
		
		LOG.debug("ID = " + parameterId);
		LOG.debug("Request = " + request.getQueryString());
		
		String parameterAction = request.getParameter("action");
		HttpSession session = request.getSession();
		EduCloudAuthorization auth = (EduCloudAuthorization) session
				.getAttribute("CLOUD_AUTHENTICATION");
		int vmId = Integer.parseInt(parameterId);

		EduCloudVMClient vmClient = EduCloudFactory.createVMClient(auth);
		VirtualMachine virtualMachine;
		
		try {
			virtualMachine = vmClient.getVirtualMachine(vmId);
			if ("start".equals(parameterAction)) {
				vmClient.startVM(virtualMachine);
			} else if ("stop".equals(parameterAction)) {
				vmClient.stopVM(virtualMachine);
			} else if ("change".equals(parameterAction)) {
				LOG.debug("Cheguei no CHANGE");
				
				String newMemory = request.getParameter("memory");
				String newProcessor = request.getParameter("processors");
				String newCapProcessor = request.getParameter("capProcessors");
				virtualMachine.setMemorySize(Integer.valueOf(newMemory));
				virtualMachine.setNumberProcessors(Integer.valueOf(newProcessor));
				virtualMachine.setCapProcessor(Integer.valueOf(newCapProcessor));
				
				vmClient.changeVM(virtualMachine);
			} else if ("clone".equals(parameterAction)) {
				LOG.debug("Starting clone VM!");
				virtualMachine.setUserId(auth.getUser().getId());
				vmClient.cloneVM(virtualMachine);
			} else {
				LOG.debug("");
			}
		} catch (EduCloudServerException e1) {
			LOG.error("on control virtual machine", e1);
		}
	}

}
