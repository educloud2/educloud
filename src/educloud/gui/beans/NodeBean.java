package  educloud.gui.beans;

import java.util.List;

import javax.servlet.http.HttpSession;

import educloud.api.EduCloudAuthorization;
import educloud.api.EduCloudFactory;
import  educloud.api.clients.EduCloudNodeClient;
import  educloud.api.entities.Node;
import  educloud.api.entities.exceptions.EduCloudServerException;

public class NodeBean {

	public List<Node> getNodes(HttpSession session) throws EduCloudServerException {
		EduCloudAuthorization auth = (EduCloudAuthorization)session.getAttribute("CLOUD_AUTHENTICATION");
		EduCloudNodeClient nodeClient = EduCloudFactory.createNodeClient(auth);

		return nodeClient.decribeNodes();
	}
}
