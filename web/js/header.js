/**
 * 
 * Copyright (c) 2026 autumo Ltd. Switzerland, Michael Gasche
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


const app = document.currentScript.dataset.app || "beetRoot";

$(document).ready(function() {
	// Header
	var headerShow = localStorage.getItem(app + ".header.visibility");
	if (headerShow != null) {
		if (headerShow == "false") {
			document.getElementById('header').style.display = "none";
		}
	}
});
function hideHeader() {
	$('header').fadeOut(300, function(){ document.getElementById('header').style.display = "none";});
	localStorage.setItem(app + ".header.visibility", "false");
}
function showHeader() {
	document.getElementById('header').style.display = "flex";
	localStorage.setItem(app + ".header.visibility", "true");
}
