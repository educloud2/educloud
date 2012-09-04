package  educloud.gui.beans;

import java.util.List;

import javax.servlet.http.HttpSession;

import  educloud.api.EduCloudAuthorization;
import  educloud.api.EduCloudFactory;
import  educloud.api.clients.EduCloudTemplateClient;
import  educloud.api.entities.Template;
import  educloud.api.entities.exceptions.EduCloudServerException;

public class TemplateBean {

	private String name;

	private String description;

	private String filename;

	private String ostype;

	private String memorySize;
	
	private int numberProcessors;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getOstype() {
		return ostype;
	}

	public void setOstype(String ostype) {
		this.ostype = ostype;
	}

	public String getMemorySize() {
		return memorySize;
	}

	public void setMemorySize(String memorySize) {
		this.memorySize = memorySize;
	}

	public int getNumberProcessors() {
		return numberProcessors;
	}

	public void setNumberProcessors(int numberProcessors) {
		this.numberProcessors = numberProcessors;
	}

	public List<Template> getTemplates(HttpSession session) throws EduCloudServerException {
		EduCloudAuthorization auth = (EduCloudAuthorization)session.getAttribute("CLOUD_AUTHENTICATION");
		EduCloudTemplateClient templateClient = EduCloudFactory.createTemplateClient(auth);
		return templateClient.describeTemplates();
	}

	public void createTemplate(HttpSession session) throws EduCloudServerException {
		EduCloudAuthorization auth = (EduCloudAuthorization)session.getAttribute("CLOUD_AUTHENTICATION");
		EduCloudTemplateClient templateClient = EduCloudFactory.createTemplateClient(auth);
		
		Template template = new Template();
		template.setName(name);
		template.setOsType(ostype);
		template.setFilename(filename);
		template.setMemorySize(Long.valueOf(memorySize));
		template.setNumberProcessors(Integer.valueOf(numberProcessors));
		template.setDescription(description);

		templateClient.createTemplate(template);
	}

}
