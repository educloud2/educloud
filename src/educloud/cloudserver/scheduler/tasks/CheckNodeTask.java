package  educloud.cloudserver.scheduler.tasks;

import java.util.Calendar;

import org.apache.log4j.Logger;

import  educloud.cloudserver.database.dao.NodeDao;
import  educloud.cloudserver.nodecllient.ClientFactory;
import  educloud.cloudserver.nodecllient.NodeClient;
import  educloud.cloudserver.nodecllient.NodeComunicationException;
import  educloud.cloudserver.selector.NodeSelectorManager;
import  educloud.internal.entities.Node;


public class CheckNodeTask extends AbstractTask {

	private static Logger LOG = Logger.getLogger(CheckNodeTask.class);

	public static final String PARAM_NODE_ID = "PARAM_NODE_ID";


	public String getType() {
		return "CHECKNODE";
	}

	
	public void run() {

		markAsRunning();

		// load node details from database
		String parameter = getParameter(PARAM_NODE_ID);
		Node node = NodeDao.getInstance().findNodeById(Integer.parseInt(parameter));

		LOG.debug("cloud server will try contact node #" + parameter);

		// create webservice client
		NodeClient nodeClient = ClientFactory.createNodeClient(node);

		// 1) Call node service
		try {
			if (null != node) {
				LOG.debug("will call node #"+ node.getId()+ ", hostname '" +node.getHostname()+ ':' +node.getPort()+"'");
				node = nodeClient.checkNodeStatus(node);
				NodeSelectorManager.getSelector().updateNode(node);
			} else {
				markAsCompleted();
				NodeSelectorManager.getSelector().unregisterNode(node);
				LOG.error("Error on try contact node #"+ parameter);
				return;
			}
		} catch (NodeComunicationException e) {
			markAsCompleted();
			NodeSelectorManager.getSelector().unregisterNode(node);
			LOG.error("Error on try contact node #"+ parameter);
			return;
		}

		// 2) Update last ping if node was successful return
		node.setLastPing(Calendar.getInstance().getTime());
		NodeDao.getInstance().updateLastPing(node);

		// 3) reschedule task
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, 50);
		LOG.debug("will reschedule task to " + calendar.getTime());
		reschedule(calendar.getTime());
	}

}
