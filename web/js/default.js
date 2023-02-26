/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


//obfuscator rotation map
var rotmap = r13init();

/**
* Prepare all.
*/
function prepareAll() {

	prepareGetInTouch();
	prepareTelInTouch();

	var now = new Date();
	var year = now.getFullYear();
	document.getElementById('year').innerHTML += year;
}

/**
 * Prepare all getintouch-links (obfuscated email).
 * ROT-13 with map 'abcdefghijklmnopqrstuvwxyz0123456789'.
 *
 * Example <a href="/getintouch/05x6+scbc46+uz">Email</a>
 */
function prepareGetInTouch() {

	var links = document.getElementsByTagName('a'); // Get all anchors

	function decode(anchor) { // function to recompose the orginal address

		var href = anchor.getAttribute('href');
		var address = href.replace(/.*getintouch\/([a-z0-9._%-]+)\+([a-z0-9._%-]+)\+([a-z.]+)/i, '$1' + '@' + '$2' + '.' + '$3');

		var linktext = anchor.innerHTML; // IE Fix

		if (href != address) {
			var ns = str_r13(address, rotmap);
			ns = ns.toLowerCase();
			anchor.setAttribute('href', 'mailto:' + ns); // Add mailto link	
			anchor.innerHTML = linktext; // IE Fix
		}
	}

	for (var l = 0; l < links.length; l++) { // Loop through the anchors

		var anchor = links[l].getAttribute("href");
		if (anchor != null) {
			var subSection = anchor.substring(0, 10);
			if (subSection == "getintouch") {
				links[l].onclick = function() { // Encode links when clicked
					decode(this);
				}
				links[l].onmouseover = function() { // Display tooltip when links are hovered
					decode(this);
				}
			}
		}
	}

}

/**
 * Prepare all getintouch-links (obfuscated email).
 * ROT-13 with map 'abcdefghijklmnopqrstuvwxyz0123456789'.
 *
 * Example <a href="/telintouch/+41786665544">Phone</a>
 */
function prepareTelInTouch() {

	var links = document.getElementsByTagName('a'); // Get all anchors

	function decode(anchor) { // function to recompose the orginal address

		var href = anchor.getAttribute('href');
		var address = href.replace('telintouch\/', '');

		var linktext = anchor.innerHTML; // IE Fix

		if (href != address) {
			anchor.setAttribute('href', 'tel:' + str_r13(address, rotmap)); // Add tel link	
			anchor.innerHTML = linktext; // IE Fix
		}
	}

	for (var l = 0; l < links.length; l++) { // Loop through the anchors

		var anchor = links[l].getAttribute("href");
		if (anchor != null) {
			var subSection = anchor.substring(0, 10);
			if (subSection == "telintouch") {
				links[l].onclick = function() { // Encode links when clicked
					decode(this);
				}
				links[l].onmouseover = function() { // Display tooltip when links are hovered
					decode(this);
				}
			}
		}
	}

}

/**
 * Decode getintouch-strings.
 * 
 * Parameter link: getintouch-link
 */
function decodeM(link) {

	var address = link.replace(/.*getintouch\/([a-z0-9._%-]+)\+([a-z0-9._%-]+)\+([a-z.]+)/i, '$1' + '@' + '$2' + '.' + '$3');
	var email = str_r13(address, rotmap);
	email = email.toLowerCase();
	document.write(email);
}

/**
 * Decode telintouch-strings.
 * 
 * Parameter link: getintouch-link
 */
function decodeT(link) {

	var address = link.replace('telintouch\/', '');
	var tel = str_r13(address, rotmap);
	document.write(tel);
}

/**
 * Prepare rotation map.
 */
function r13init() {

	var map = new Array();
	var s = "abcdefghijklmnopqrstuvwxyz0123456789";
	for (var i = 0; i < s.length; i++) {
		map[s.charAt(i)] = s.charAt((i + 18) % 36);
	}
	for (var i = 0; i < s.length; i++) {
		map[s.charAt(i).toUpperCase()] = s.charAt((i + 18) % 36).toUpperCase();
	}
	return map;
}

/**
 * Rotate string.
 *
 * Parameter a: string to rotate
 * Parameter map: rotation map
 */
function str_r13(a, map) {

	var s = "";
	for (var i = 0; i < a.length; i++) {
		var b = a.charAt(i);
		s += (b >= 'A' && b <= 'Z' || b >= 'a' && b <= 'z' || b >= '0' && b <= '9' ? map[b] : b);
	}
	return s;
}

function checkUpload(maxSizeMb) {

	const fi = document.getElementById('file');
	
	// Check if any file is selected.
	if (fi.files.length > 0) {
		
		for (i = 0; i <= fi.files.length - 1; i++) {

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
function togglePw(ln) {
	var hide = "Hide Password";
	var show = "Show Password";
	if (ln = 'de') {
		hide = "Passwort ausblenden";
		show = "Passwort einblenden";
	}
	const p1 = document.getElementById('lgpassword1');
	const p2 = document.getElementById('lgpassword2');
	const lb = document.getElementById('lgpwshow');
	var style = window.getComputedStyle(p2);
    if (style.display === 'none') {
    	p2.style.display = "block";
    	p1.style.display = "none";
    	lb.textContent = hide;
    } else {
    	p2.style.display = "none";
    	p1.style.display = "block";
    	lb.textContent = show;
	}
}    
/**
 * Sync PW between fields
 */
function syncPw(id) {
	const p1 = document.getElementById('lgpassword1');
	const p2 = document.getElementById('lgpassword2');
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

	var others = getElementsStartsWithId("dropdown_");
	for (i = 0; i < others.length; i++) {
		var other = others[i];
		if (other.id != "dropdown_" + menuid && other.classList.contains('show')) {
			other.classList.remove('show');
		}
	}
}

/**
 * Get elements startind with an id
 */
function getElementsStartsWithId(id) {

	var children = document.body.getElementsByTagName('*');
	var elements = [], child;
	for (var i = 0, length = children.length; i < length; i++) {
		child = children[i];
		if (child.id.substr(0, id.length) == id)
			elements.push(child);
	}

	return elements;
}

// Close the dropdown menu if the user clicks outside of it
window.onclick = function(event) {

	if (!event.target.matches('.dropbtn')) {

		var dropdowns = document.getElementsByClassName("dropdown-content");
		var i;

		for (i = 0; i < dropdowns.length; i++) {
			var openDropdown = dropdowns[i];
			if (openDropdown.classList.contains('show')) {
				openDropdown.classList.remove('show');
			}
		}
	}
}
