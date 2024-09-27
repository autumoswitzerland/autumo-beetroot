/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */


/**
 * Prepare all.
 */
function prepareAll() {
	prepareYear();
}

/**
 * Prepare year.
 */
function prepareYear() {
	let now = new Date();
	let year = now.getFullYear();
	document.getElementById('footeryear').innerHTML += year;
}

function checkUpload(maxSizeMb) {
	const fi = document.getElementById('file');
	// Check if any file is selected.
	if (fi.files.length > 0) {
		for (let i = 0; i <= fi.files.length - 1; i++) {
			const fsize = fi.files.item(i).size; //
			const fileMb = Math.round((fsize / 1024 / 1024));
			//document.getElementById('showSize').innerHTML = 'File size: <b>' + fileMb + '</b> MB or <b>' + fsize + '</b> Bytes';
			// The size of the file.
			if (fileMb >= maxSizeMb) {
				alert("File too Big, please select a file less than " + maxSizeMb + " MB!\n\nFile size: " + fileMb + " MB (" + fsize + " Bytes).");
				location.reload();
			}
		}
	}
}
    
/**
 * Toggle PW.
 */
function togglePw(hideText, showText) {
	let p1 = document.getElementById('lgpassword1');
	let p2 = document.getElementById('lgpassword2');
	let lb = document.getElementById('lgpwshow');
	let style = window.getComputedStyle(p2);
    if (style.display === 'none') {
    	p2.style.display = "block";
    	p1.style.display = "none";
    	lb.textContent = hideText;
    } else {
    	p2.style.display = "none";
    	p1.style.display = "block";
    	lb.textContent = showText;
	}
}    
/**
 * Sync PW between fields
 */
function syncPw(id) {
	let p1 = document.getElementById('lgpassword1');
	let p2 = document.getElementById('lgpassword2');
	if (id == 'lgpassword1')
		p2.value = p1.value;
	if (id == 'lgpassword2')
		p1.value = p2.value;
}
   
/**
 * Dropdown.
 */
function dropdown(menuid) {
	document.getElementById("dropdown_" + menuid).classList.toggle("show");
	let others = getElementsStartsWithId("dropdown_");
	for (let i = 0; i < others.length; i++) {
		let other = others[i];
		if (other.id != "dropdown_" + menuid && other.classList.contains('show')) {
			other.classList.remove('show');
		}
	}
}

/**
 * Get elements startind with an id
 */
function getElementsStartsWithId(id) {
	let children = document.body.getElementsByTagName('*');
	let elements = [], child;
	for (let i = 0, length = children.length; i < length; i++) {
		child = children[i];
		if (child.id.substr(0, id.length) == id)
			elements.push(child);
	}
	return elements;
}

// Close the dropdown menu if the user clicks outside of it
window.onclick = function(event) {
	if (!event.target.matches('.dropbtn')) {
		let dropdowns = document.getElementsByClassName("dropdown-content");
		let i;
		for (i = 0; i < dropdowns.length; i++) {
			let openDropdown = dropdowns[i];
			if (openDropdown.classList.contains('show')) {
				openDropdown.classList.remove('show');
			}
		}
	}
}

/**
 * Copy terminal to cliboard.
 */
function copyTerm() {
    let term = document.getElementById("terminal");
    navigator.clipboard.writeText(term.textContent);
	let flashMessage = document.getElementById('flashMessage');
	flashMessage.classList.remove('hidden');
	setTimeout(function() {
	    flashMessage.classList.add('hidden');
	}, 1000);
}
