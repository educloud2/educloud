package  educloud.api.entities;


public class VirtualMachine {

	public enum VMState {PENDING, BOOT, RUNNING, SHUTDOWN, DONE, UNKNOWN};

	private int id;

	private int userId;

	private VMState state;

	private String name;

	private String osType;

	private String description;

	private RDPConfig rdpConfig;

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

	public int getUserId(){
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public boolean isStartable() {
		return state == VMState.DONE;
	}

	public boolean isStoppable() {
		return state == VMState.RUNNING;
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

	public RDPConfig getRDPConfig() {
		return rdpConfig;
	}

	public void setRDPConfig(RDPConfig rdpConfig) {
		this.rdpConfig = rdpConfig;
	}

	public void setMemorySize(long memorySize) {
		this.memorySize = memorySize;
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
