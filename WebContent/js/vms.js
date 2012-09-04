function startVM(id) {
	$.get("controlVirtualMachine?action=start&id=" + id, function() {
		alert("success");
		window.location = 'vms.jsp';
	}).error(function() {
		alert("error");
	});
}

function stopVM(id) {
	$.get("controlVirtualMachine?action=stop&id=" + id, function() {
		alert("success");
		window.location = 'vms.jsp';
	}).error(function() {
		alert("error");
	});
}
function changeVM(id, memory, processors, capProcessors) {
	$.get("controlVirtualMachine?action=change&id=" + id + '&memory=' + memory + '&processors=' + processors + '&capProcessors=' + capProcessors, 
		function() {
			alert("success");
			window.location = 'vms.jsp';
		}
	).error(function() {
		alert("error on post");
	});
}

function cloneVM(id) {
	$.get("controlVirtualMachine?action=clone&id=" + id, function() {
		alert("success! Clone is starting!");
		window.location = 'vms.jsp';
	}).error(function() {
		alert("error");
	});
}

$(function(){
	$('#modalOpenVm').dialog({
			autoOpen: false,
			resizable: false,
			width: 750,
			height: 400,
			modal: true,
			position: ['center',100],
			title: 'ScaleUp - Settings',
			buttons: {
				'Save': function() {
						if ($('#trVmId_' + $('#idVm').val() + ' td:eq(8)').text() == 'RUNNING'){
							changeVM($('#idVm').val(), $('#memoryVm').val(), $('#numberProcessors').val(), $('#processorCapacity').val());
						} else {
							alert('The virtual machine must be in the running state.');
							$(this).dialog('close');
						}
						
						//
				},
				'Close': function() {
					$(this).dialog('close');
				}
			}
		});
});

function openModalVm(id){
	$('#modalOpenVm').dialog("open");
	$('#idVm').val(id);
	$('#tableModalVm tr:eq(1) td:eq(1)').html($('#trVmId_' + id + ' td:eq(1)').text());
	$('#tableModalVm tr:eq(2) td:eq(1)').html($('#trVmId_' + id + ' td:eq(2)').text());
	$('#tableModalVm tr:eq(3) td:eq(1)').html($('#trVmId_' + id + ' td:eq(3)').text());
	//$('#tableModalVm tr:eq(4) td:eq(1)').html($('#trVmId_' + id + ' td:eq(4)').text());
	$('#memoryVm').val($('#trVmId_' + id + ' td:eq(4)').text().split(" ")[0]);
	$('#numberProcessors').val($('#trVmId_' + id + ' td:eq(5)').text());
	$('#processorCapacity').val($('#trVmId_' + id + ' td:eq(6)').text());
	
}