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


function initModalDialog() {
		
	const modal = document.getElementById('dialog');
	    if (!modal) return;		
		
    const okBtn = document.querySelector('.modal-ok');
    const cancelBtn = document.querySelector('.modal-cancel');
    const modalMessage = document.getElementById('modal-message');
	
	let currentFormName = '';
	let href = '';
	
	function showModal(link) {
	    const confirmMessage = link.dataset.confirmMessage;
	    currentFormName = link.dataset.formName;
		href = link.href;
	    modalMessage.textContent = confirmMessage;
	    modal.style.display = 'block';
		document.addEventListener('keydown', handleKeyDown);
	}
	
	function closeModal() {
	    modal.style.display = 'none';
	    document.removeEventListener('keydown', handleKeyDown);
	}
	
	function handleKeyDown(event) {
	    if (event.key === 'Escape') {
	        closeModal();
	    } else if (event.key === 'Enter') {
			event.preventDefault();
	        okBtn.click();
	    }
	}
				
	document.querySelectorAll('.dialogLink').forEach(link => {
	    link.addEventListener('click', function(event) {
	        event.preventDefault();
	        showModal(this);
	    });
	});
	
	okBtn.addEventListener('click', function() {
	    closeModal();
	    if (currentFormName) {
	        document.forms[currentFormName].submit();
	    } else {
			window.location=href;
		}
	});
	
	cancelBtn.addEventListener('click', function() {
	    closeModal();
	});
		
    window.onclick = function(event) {
        if (event.target === modal) {
            closeModal();
        }
    };
	
	document.querySelector('.modal-close').addEventListener('click', closeModal);
}

initModalDialog();
