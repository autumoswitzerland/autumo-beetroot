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


function allowDrop(event) {
    event.preventDefault();
}

function drag(event) {
    event.dataTransfer.setData("text", event.target.getAttribute('data-id'));
}

function drop(event) {
    event.preventDefault();
    const data = event.dataTransfer.getData("text");
    const draggedElement = document.querySelector(`[data-id='${data}']`);
    const targetList = event.target.closest('.list-group');

    if (targetList && !targetList.contains(draggedElement)) {
        targetList.appendChild(draggedElement);
        updateAssignedIds();
    }
}

function assignSelected() {
    const selected = document.querySelector('#available .list-group-item.active');
    if (selected) {
        document.getElementById('assigned').appendChild(selected);
        selected.classList.remove('active');
        updateAssignedIds();
    }
}

function removeSelected() {
    const selected = document.querySelector('#assigned .list-group-item.active');
    if (selected) {
        document.getElementById('available').appendChild(selected);
        selected.classList.remove('active');
        updateAssignedIds();
    }
}

function updateAssignedIds() {
    const assignedIds = [];
	const availabledIds = [];
    document.getElementById('assigned').querySelectorAll('.list-group-item').forEach(item => {
        assignedIds.push(item.getAttribute('data-id'));
    });
	document.getElementById('available').querySelectorAll('.list-group-item').forEach(item => {
	    availabledIds.push(item.getAttribute('data-id'));
	});
    document.getElementById('assignedIds').value = assignedIds.join(',');
	document.getElementById('availableIds').value = availabledIds.join(',');
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.list-group-item').forEach(item => {
        item.addEventListener('click', (event) => {
            document.querySelectorAll('.list-group-item').forEach(el => el.classList.remove('active'));
            item.classList.add('active');
        });
    });
});
