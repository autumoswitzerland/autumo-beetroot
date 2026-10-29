/**
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


/**
 * Collapsible side menu (the left column of a page).
 *
 * The state is kept in localStorage like the header visibility of header.js,
 * so it belongs to the browser and needs no round trip to the server. The
 * button only appears on pages that really have a side menu; pages without
 * one (e.g. the login page) hide it.
 */
(function () {
	'use strict';

	var STORAGE_KEY = 'sidebar.visibility';
	var COLLAPSED_CLASS = 'sidebar-collapsed';

	function sidebar() {
		return document.getElementById('actions-sidebar')
			|| document.getElementById('service-sidebar')
			|| document.querySelector('.container > nav');
	}

	function collapsed() {
		return document.body.classList.contains(COLLAPSED_CLASS);
	}

	function apply(state, button, nav) {
		document.body.classList.toggle(COLLAPSED_CLASS, state);
		if (!button) {
			return;
		}
		button.setAttribute('aria-expanded', String(!state));
		button.setAttribute('title', state ? button.dataset.labelShow : button.dataset.labelHide);
		button.setAttribute('aria-label', button.getAttribute('title'));
	}

	function toggle(button, nav) {
		var state = !collapsed();
		apply(state, button, nav);
		try {
			localStorage.setItem(STORAGE_KEY, state ? 'false' : 'true');
		} catch (e) {
			// Private mode or a blocked storage: the menu still works, it just forgets.
		}
	}

	function init() {
		markActive();
		var button = document.getElementById('sidebar-toggle');
		var nav = sidebar();
		if (!button) {
			return;
		}
		if (!nav || !nav.querySelector('a, li, input')) {
			// No side menu on this page (e.g. the start page has an empty one,
			// the login page none at all) - no button either.
			button.parentNode.removeChild(button);
			return;
		}
		if (nav.id) {
			button.setAttribute('aria-controls', nav.id);
		}
		button.hidden = false;

		var stored = null;
		try {
			stored = localStorage.getItem(STORAGE_KEY);
		} catch (e) {
			stored = null;
		}
		apply(stored === 'false', button, nav);
		markActive();

		button.addEventListener('click', function (event) {
			// The top bar reacts to a double click (header visibility) - keep the click here.
			event.stopPropagation();
			toggle(button, nav);
		});

		document.addEventListener('keydown', function (event) {
			if ((event.ctrlKey || event.metaKey) && !event.altKey && !event.shiftKey
					&& (event.key === 'b' || event.key === 'B')) {
				event.preventDefault();
				toggle(button, nav);
			}
		});
	}

	/**
	 * Marks the entry of the side menu that belongs to the open page.
	 *
	 * Only links of the side menu are considered (#actions-sidebar /
	 * #service-sidebar). The action links inside a table (View, Edit, Delete)
	 * are never marked. Compared are the normalised path names: the query string
	 * is dropped, trailing slashes are ignored and '/x/index' equals '/x', so
	 * extra page parameters after index/edit/view/add do not matter.
	 */
	function markActive() {
		var current = window.location.pathname.replace(/\/+$/, '');
		var currentAlt = current.replace(/\/index$/, '');
		var links = document.querySelectorAll('#actions-sidebar a.side-nav-item, #service-sidebar a.side-nav-item');
		var best = null;
		var bestLength = -1;

		Array.prototype.forEach.call(links, function (link) {
			// A delete link opens a dialog and is never the current page.
			if (link.classList.contains('dialogLink')) {
				return;
			}
			var path;
			try {
				path = new URL(link.href, window.location.origin).pathname;
			} catch (e) {
				return;
			}
			path = path.replace(/\/+$/, '');
			var pathAlt = path.replace(/\/index$/, '');
			if (path === current || path === currentAlt || pathAlt === current || pathAlt === currentAlt) {
				if (path.length > bestLength) {
					best = link;
					bestLength = path.length;
				}
			}
		});

		if (best) {
			best.setAttribute('aria-current', 'page');
		}
	}

	if (document.readyState === 'loading') {
		document.addEventListener('DOMContentLoaded', init);
	} else {
		init();
	}
}());
