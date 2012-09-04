<%@page import="com.google.educloud.api.entities.RDPConfig" %>
<%@page import="com.google.educloud.api.entities.VirtualMachine" %>
<%@page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="com.google.educloud.api.entities.Template"%>
<%@page import="java.util.List"%>
<jsp:useBean id="virtualMachineBean" class="com.google.educloud.gui.beans.VirtualMachineBean" />
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"></meta>
<title>EduCloud</title>
<jsp:include page="scripts.jsp" />
<script type="text/javascript" src="js/vms.js"></script>
<style type="text/css">
table {
	display: table;
	border-collapse: separate;
	border-spacing: 2px 2px;
	border-color: gray;
}

.cloudgrid {
	width: 100%;
	text-align: left;
	border-collapse: collapse;
}

.cloudgrid th {
	background: #d6d6d6;
	display: table-cell;
	vertical-align: inherit;
	padding: 8px;
}

.cloudgrid td {
	display: table-cell;
	vertical-align: inherit;
	border-bottom: 1px solid white;
	border-top: 1px solid transparent;
	padding: 8px;
	background: #ebebeb;
}

input {
	width: auto;
	display: block;
	padding: auto;
	margin: 0 0 0 0;
	font-size: 18px;
	color: #3a3a3a;
	
}
select {
	width: auto;
	display: block;
	padding: auto;
	margin: 0 0 0 0;
	font-size: 12px;
	color: #3a3a3a;
	padding: 0px;
}
</style>
<script type="text/javascript">
function checkAll(selection) {
	var b = selection == 'select' ? true : false;
	$('input[name^="vm_"]').each(function (index) {
		$(this).attr('checked', b);
	});
}

function executeAction(o) {
	if ($(o).val() == 'delete') {
		var selected = [], i=0;
		$('input[name^="vm_"]').each(function (index) {
			if ($(this).is(':checked')) {
				selected[i++] = $(this).val();
			}
		});
		
		if (selected.length > 0) {
			$.post("deleteVirtualMachines", {'vms':selected.join(';')}, function(data) {
				window.location = "vms.jsp";
			}, 'json')
			.error(function() {
				alert("error");
			});
		}
	}
}
</script>
</head>
<body>
<div id="container"><jsp:include page="header.jsp" />
<div id="content">

<%
	if (request.getMethod().equals("POST")) {
%>
<h2>Postei as paradas</h2>
		
<%		
		//virtualMachineBean.createVirtualMachine(session);
		//response.sendRedirect("vms.jsp");
	}
%>

<div style="margin:10px">
<div>
<div style="background:#ebebeb;padding:5px;">
Select: <a href="#" onclick="checkAll('select')">All</a> | <a href="#" onclick="checkAll('unselect')">None</a>
<select style="display:inline;" onchange="executeAction(this)">
	<option value="">Actions...</option>
	<option value="delete">Delete</option>
</select>
</div>
<table id="templatesTable" class="cloudgrid">
	<tr>
		<th>-</th>
		<th>ID</th>
		<th>Name</th>
		<th>Description</th>
		<th>Memory Size</th>
		<th>Processors</th>
		<th>Processor Capacity</th>
		<th>OS Type</th>
		<th>LoadBalancer</th>
		<th>IP LB</th>
		<th>State</th>
		<th width="200px">Actions</th>
	</tr>
	<%
	
		List<VirtualMachine> vms = virtualMachineBean.getVirtualMachines(session);

		for (VirtualMachine vm : vms) {
	%>
	<tr id="trVmId_<%=vm.getId()%>">
		<td width="10px">
			<input type="checkbox" name="vm_<%=vm.getId()%>" id="vm_<%=vm.getId()%>" value="<%=vm.getId()%>" />
		</td>
		<td><%=vm.getId()%></td>
		<td><a href="javascript:void(0);" onClick="openModalVm(<%=vm.getId()%>)"><%=vm.getName()%></a></td>
		<td><%=vm.getDescription()%></td>
		<td><%=vm.getMemorySize()%> MB</td>
		<td><%=vm.getNumberProcessors()%></td>
		<td><%=vm.getCapProcessor() %></td>
		<td><%=vm.getOsType()%></td>
		<td>
		<% if (!vm.isWithLoadBalancer()) { %>
			<i>No</i>
		<% } else { %>
			<i><b>Yes</b></i>
		<% } %>
		</td>
		<td>
		<% if (!vm.isWithLoadBalancer()) { %>
			<i>-</i>
		<% } else { %>
			<i><%=vm.getIpLoadBalancer() %></i>
		<% } %>
		</td>
		<td><%=vm.getState()%></td>
		<td width="60px">
			<%
				if (vm.isStartable()) {
			%>
				<a onclick="startVM(<%=vm.getId()%>)" href="#start_<%=vm.getId()%>">Start</a> 
			<%
				} else {
			%>
					Start
			<%
			}
			%>
			 |
			<%
				if (vm.isStoppable()) {
			%>
				<a onclick="stopVM(<%=vm.getId()%>)" href="#stop_<%=vm.getId()%>">Stop</a>
				
			<%
				} else {
			%>
					Stop
			<%
				}
			%>
			<%
				if (vm.isWithLoadBalancer()) {
			%>	
				| <a onclick="cloneVM(<%=vm.getId()%>)" href="#clone_<%=vm.getId()%>">Scale Out</a>
			<%
				}
			%>
			
		</td>
	</tr>
	<%
		}
	%>
</table>
</div>
</div>
</div>
<jsp:include page="footer.jsp" />
</div>
<div id="modalOpenVm">
<form id="formScaleUp" action="vms.jsp" method="post">
  <p>&nbsp;</p>
  <p>&nbsp;</p>
  <table class="cloudgrid" id="tableModalVm">
    <tr>
      <th colspan="2" style="text-align:center;">Virtual Machine Info</th>
    </tr>
    <tr>
      <td width="50%">id <input type="hidden" id="idVm" name="idVm" size="10" value=""></td>
      <td width="50%"></td>
    </tr>
    <tr>
      <td>Name</td>
      <td></td>
    </tr>
    <tr>
      <td>Description</td>
      <td></td>
    </tr>
    <tr>
      <td>Memory</td>
      <td><input type="text" id="memoryVm" name="memoryVm" size="3" value="" style="float:left;">MB</td>
    </tr>
    <tr>
      <td>Processor</td>
      <td><input type="text" id="numberProcessors" name="numberProcessors" size="2" value="" style="float:left;"> Cores</td>
    </tr>
    
    <tr>
      <td>Processor Capacity</td>
      <td><input type="text" id="processorCapacity" name="processorCapacity" size="4" value="" style="float:left;">%</td>
    </tr>
    
    <tr>
      <td>&nbsp;</td>
      <td></td>
    </tr>
  </table>
  <p>&nbsp;</p>
  <p>&nbsp;</p>
  <p>&nbsp; </p>
</form>
</div>
</body>
</html>
