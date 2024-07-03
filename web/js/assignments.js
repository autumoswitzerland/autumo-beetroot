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


document.addEventListener('DOMContentLoaded', (event) => {
	
    const assigned = document.getElementById('assigned');
    const available = document.getElementById('available');
    const assignButton = document.getElementById('assignButton');
    const removeButton = document.getElementById('removeButton');
    const assignedIds = document.getElementById('assignedIds');

    let selectedAvailable = null;
    let selectedAssigned = null;

    available.addEventListener('click', (event) => {
        if (selectedAvailable) {
            selectedAvailable.classList.remove('active');
        }
        selectedAvailable = event.target;
        selectedAvailable.classList.add('active');
    });

    assigned.addEventListener('click', (event) => {
        if (selectedAssigned) {
            selectedAssigned.classList.remove('active');
        }
        selectedAssigned = event.target;
        selectedAssigned.classList.add('active');
    });

    assignButton.addEventListener('click', () => {
        if (selectedAvailable) {
            assigned.appendChild(selectedAvailable);
            selectedAvailable.classList.remove('active');
            selectedAvailable = null;
            updateAssignedIds();
        }
    });

    removeButton.addEventListener('click', () => {
        if (selectedAssigned) {
            available.appendChild(selectedAssigned);
            selectedAssigned.classList.remove('active');
            selectedAssigned = null;
            updateAssignedIds();
        }
    });

    function updateAssignedIds() {
        const ids = [];
        assigned.querySelectorAll('.list-group-item').forEach(item => {
            ids.push(item.getAttribute('data-id'));
        });
        assignedIds.value = ids.join(','); // 1,3
    }
	
});
