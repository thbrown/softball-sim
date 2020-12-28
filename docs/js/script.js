// Get the modal
let modal = document.getElementById("optimizer-modal");

// Get the <span> element that closes the modal
let closeElement = document.getElementsByClassName("close")[0];

// Get the modal body so we can populate it's content
let modalBody = document.getElementById("modal-body");

// When the user clicks on <span> (x), close the modal
closeElement.onclick = function () {
  modal.classList.add("hidden");
};

// When the user clicks anywhere outside of the modal, close it
window.onclick = function (event) {
  if (event.target == modal) {
    modal.classList.add("hidden");
  }
};

function optimizerClick(id, nameId, imgUrlId, descriptionId) {
  modal.style.display = "block";

  let name = document.getElementById(nameId).innerHTML;
  let img = document.getElementById(imgUrlId).innerHTML;
  let description = document.getElementById(descriptionId).innerHTML;
  
  // Set all the add button text based on whether or not the button is selected (TODO: DRY)
  let addButtonHtml = "";
  let numericId = parseInt(id)
  console.log(numericId, optimizerSelection, optimizerSelection.has(numericId), optimizerSelection && optimizerSelection.has(numericId));
  if(optimizerSelection && optimizerSelection.has(numericId)) {
  	addButtonHtml = `<div class="add-button" onclick="selectToggleClick('${id}',event)">- Remove</div>`
  } else if(optimizerSelection) {
  	addButtonHtml = `<div class="add-button" onclick="selectToggleClick('${id}',event)">+ Add</div>`
  }

  let modalHtml =
    '<div class="modal-img"> <img src=' +
    img +
    ' width="100%" height=auto></div> <div class="modal-name">' +
    name +
    '</div> <div class="modal-description markdown-body"">' +
    description +
    '</div>' + addButtonHtml;
  modalBody.innerHTML = modalHtml;
  modal.classList.remove("hidden");
}

// The auto render katex script wasn't working, so this manually searches for divs w/ the "katex" class and renders them all after the katex script loads.
function renderKatex() {
  let katexElems = document.getElementsByClassName("katex");
  let katexElemsArr = Array.prototype.slice.call(katexElems, 0);
  for (let i = 0; i < katexElemsArr.length; i++) {
    var html = katex.renderToString(katexElemsArr[i].innerText, {
      throwOnError: false,
    });
    katexElemsArr[i].innerHTML = html;
  }
}

function inIframe() {
  try {
    return window.self !== window.top;
  } catch (e) {
    return true;
  }
}

let optimizerSelection = new Set(); 
function onReceivedMessage(evt) {
  optimizerSelection = new Set(evt.data); 
  
  let buttons = document.getElementsByClassName("add-button");
  for(let i = 0; i < buttons.length; i++) {
  	  let numericId = parseInt(buttons[i].id.split("-")[1])
  	  
  	  // Set all the add button text based on whether or not the button is selected (TODO: DRY)
      if(optimizerSelection.has(numericId)) {
	  	buttons[i].textContent = "- Remove";
	  	console.log("rem", optimizerSelection, numericId, optimizerSelection.has(numericId));
	  } else {
	    buttons[i].textContent = "+ Add";
	    console.log("add", optimizerSelection, numericId, optimizerSelection.has(numericId));
	  }
	  
	  // Un-hide all buttons
	  buttons[i].classList.remove("hidden");
  }
}

function selectToggleClick(id, event) {
  event.stopPropagation();
  console.log("Posting message from iframe", id);
  
  // Toggle the optimizer
  let numericId = parseInt(id);
  if(optimizerSelection.has(numericId)) {
  	optimizerSelection.delete(numericId);
  	event.target.textContent = "+ Add";
  } else {
    optimizerSelection.add(numericId);
    event.target.textContent = "- Remove";
  }
  
  parent.postMessage(Array.from(optimizerSelection), "*");
}

window.addEventListener("message", onReceivedMessage, false);

if (inIframe()) {
  // Hide the header if the content is in an iframe
  document.getElementById("header").classList.add("hidden");
}

// Mermaid is not defined sometimes depending on loading. Not sure this is even necessary?
//mermaid.initialize({ startOnLoad: true });
