package  educloud.internal.entities;

public class VirtualMachine {

	public enum VMState {PENDING, BOOT, RUNNING, SHUTDOWN, DONE, UNKNOWN};

	private int id;

	private int userId;

	private int nodeId;

	private VMState state;

	private String name;

	private String bootableMedium;

	private String uuid;

	private String vbox;

	private String osType;

	private String description;

	private String vrdePassword;

	private String vrdeUsername;

	private int vrdePort;

	private long memorySize;
	
	private int numberProcessors;
	
	private int capProcessor = 30;
	
	private int machineParent = 0;
	
	private String loadBalancer = null;
	
	private String ipLoadBalancer = null;
	
	private boolean withLoadBalancer = false;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public VMState getState() {
		return state;
	}

	public void setState(VMState state) {
		this.state = state;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBootableMedium() {
		return bootableMedium;
	}

	public void setBootableMedium(String bootableMedium) {
		this.bootableMedium = bootableMedium;
	}

	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	public String getVboxSession() {
		return vbox;
	}

	public void setVboxSession(String vbox) {
		this.vbox = vbox;
	}

	public boolean equals(Object obj){
		if( obj == null )
			return false;
		else if( !(obj instanceof VirtualMachine) )
			return false;
		else
			return ((VirtualMachine)obj).getId() == this.getId();
	}

	public String getOsType() {
		return osType;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getVRDEPassword() {
		return vrdePassword;
	}

	public void setVRDEPassword(String vrdePassword) {
		this.vrdePassword = vrdePassword;
	}

	public String getVRDEUsername() {
		return vrdeUsername;
	}

	public void setVRDEUsername(String vrdeUsername) {
		this.vrdeUsername = vrdeUsername;
	}

	public void setVRDEPort(int port) {
		this.vrdePort = port;
	}

	public int getVRDEPort() {
		return vrdePort;
	}

	public void setMemorySize(long memorySize) {
		this.memorySize= memorySize;
	}

	public long getMemorySize() {
		return memorySize;
	}

	public int getNumberProcessors() {
		return numberProcessors;
	}

	public void setNumberProcessors(int numberProcessors) {
		this.numberProcessors = numberProcessors;
	}

	public int getCapProcessor() {
		return capProcessor;
	}

	public void setCapProcessor(int capProcessor) {
		this.capProcessor = capProcessor;
	}

	public int getMachineParent() {
		return machineParent;
	}

	public void setMachineParent(int machineParent) {
		this.machineParent = machineParent;
	}

	public String getLoadBalancer() {
		return loadBalancer;
	}

	public void setLoadBalancer(String lbId) {
		this.loadBalancer = lbId;
	}

	public String getIpLoadBalancer() {
		return ipLoadBalancer;
	}

	public void setIpLoadBalancer(String ipLoadBalancer) {
		this.ipLoadBalancer = ipLoadBalancer;
	}

	public boolean isWithLoadBalancer() {
		return withLoadBalancer;
	}

	public void setWithLoadBalancer(boolean withLoadBalancer) {
		this.withLoadBalancer = withLoadBalancer;
	}

	
}
