package  educloud.cloudserver.selector;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import  educloud.internal.entities.Node;
import  educloud.internal.entities.VirtualMachine;

/**
 * Classe para implementar o algoritmo de worstfit.
 *
 */
public class WorstFitNodeSelector implements INodeSelector {

	private static Logger LOG = Logger.getLogger(WorstFitNodeSelector.class);

	private List<Node> nodes = new ArrayList<Node>();

	 
	public void updateNode(Node node) {
		if (nodes.contains(node)) {
			nodes.set(nodes.indexOf(node), node);
		}
	}

	 
	public void registerNode(Node node) {
		if (!nodes.contains(node)) {
			// Adiciona o novo nodo.
			nodes.add(node);
		}
	}

	 
	public void unregisterNode(Node node) {
		if (nodes.contains(node)) {
			// Remove o nodo.
			nodes.remove(node);
		}
	}

	 
	public List<Node> getRegisteredNodes() {
		return nodes;
	}

	 
	public Node getNext(VirtualMachine machine) {

		// Para retorno.
		Node nodoSelecionado = null;

		for (Node n : nodes) {

			if (!n.isConnectedToVBox()) {
				continue;
			}

			if (n.getAvailMemory() < machine.getMemorySize()) {
				LOG.debug("Node #'" + n.getId() + "' hasn't sufficient memory to allocate machine #'" + machine.getId() + "'");
				continue;
			}

			if (nodoSelecionado == null)
				nodoSelecionado = n;
			else if (nodoSelecionado.getAvailMemory() > n.getAvailMemory())
				nodoSelecionado = n;
		}

		return nodoSelecionado;
	}
}
