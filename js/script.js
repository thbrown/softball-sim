// Get the modal
let modal = document.getElementById("optimizer-modal");

// Get the <span> element that closes the modal
let span = document.getElementsByClassName("close")[0];

// Get the modal body so we can populate it's content
let modalBody = document.getElementById("modal-body");

// When the user clicks on <span> (x), close the modal
span.onclick = function() {
  modal.style.display = "none";
}

// When the user clicks anywhere outside of the modal, close it
window.onclick = function(event) {
  if (event.target == modal) {
    modal.style.display = "none";
  }
}

function optimizerClick(id, nameId, imgUrlId, descriptionId) {
	modal.style.display = "block";
			
	let name = document.getElementById(nameId).innerHTML;
	let img = document.getElementById(imgUrlId).innerHTML;
	let description = document.getElementById(descriptionId).innerHTML;
		
	let modalHtml = '<div class="modal-img"> <img src=' + img + ' width="100%" height=auto></div> <div class="modal-name">' + name + '</div> <div class="modal-description markdown-body"">' + description + '</div><div class="add-button" onclick="addButtonClick(' + id + ',event)">+ Use</div>';
	modalBody.innerHTML = modalHtml;
}

function addButtonClick(id, event) {
	event.stopPropagation();
  	let win = window.open("https://softball.app/account/add-optimizer/"+id, '_blank');
  	win.focus();
}

mermaid.initialize({startOnLoad:true});