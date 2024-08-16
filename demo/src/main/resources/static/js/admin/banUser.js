const API_BASE_URL = "8.130.130.240:8088";
let currentPage = 1;
const pageSize = 5;
let filters = {};

function searchUsers(page, size) {
    const keyword = document.getElementById('keyword').value;

    if (keyword) {
        console.log(`Searching for keyword: ${keyword}, page: ${page}, size: ${size}`);

        fetch(`http://8.130.130.240:8088/searchUsers?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
            .then(response => {
                console.log('Received response:', response);
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(data => {
                console.log('Received data:', data);
                const itemList = document.getElementById('item-list');
                itemList.innerHTML = ''; // Clear previous results

                if (data.content.length > 0) {
                    data.content.forEach(user => {
                        const item = document.createElement('div');
                        item.className = 'item';

                        const unbanTime = new Date(user.unbanTime);
                        const localTime = unbanTime.toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' });

                        item.innerHTML = `
                        <div class="item-details">
                            <div class="item-content">
                                <h3><strong>Username:</strong> ${user.username}</h3>
                                <p><strong>Password:</strong> ${user.password}</p>
                                <p><strong>User ID:</strong> ${user.id}</p>
                                <p><strong>Unban Time:</strong> ${localTime}</p>
                                <div class="item-meta">
                                    <select class="ban-options" id="ban-options-${user.id}">
                                        <option value="7">7 Days</option>
                                        <option value="30">1 Month</option>
                                        <option value="182">6 Months</option>
                                        <option value="3650">Permanent</option>
                                    </select>
                                    <button onclick="banUser('${user.id}')">Ban User</button>
                                </div>
                            </div>
                        </div>
                    `;
                        itemList.appendChild(item);
                    });
                    document.getElementById('no-files').style.display = 'none';
                    createPagination(data.totalItems, page, size);
                } else {
                    document.getElementById('no-files').style.display = 'block';
                }
            })
            .catch(error => {
                console.error('Error fetching users:', error);
            });
    } else {
        alert('Please enter a search query.');
    }
}

function banUser(id) {
    const selectElement = document.getElementById(`ban-options-${id}`);
    const banDuration = selectElement.value;

    console.log(`Ban user with ID: ${id} for ${banDuration}`);

    fetch(`http://8.130.130.240:8088/banUser`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            userId: id,
            banDuration: banDuration
        })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log('User banned successfully:', data);
            alert('User banned successfully');
            searchUsers(currentPage, pageSize); // Refresh the user list
        })
        .catch(error => {
            console.error('Error banning user:', error);
        });
}


function createPagination(totalElements, currentPage, size) {
    const paginationContainer = document.getElementById('pagination');
    paginationContainer.innerHTML = '';
    const totalPages = Math.ceil(totalElements / size);

    const prevButton = document.createElement('button');
    prevButton.innerText = 'Previous';
    prevButton.disabled = currentPage === 1;
    prevButton.onclick = () => {
        searchUsers(currentPage - 1, size);
        window.scrollTo(0, 0);
    };
    paginationContainer.appendChild(prevButton);

    let startPage, endPage;
    if (currentPage === 1 || currentPage === 2) {
        startPage = 1;
        endPage = Math.min(5, totalPages);
    } else {
        startPage = Math.max(1, currentPage - 2);
        endPage = Math.min(totalPages, currentPage + 2);
    }

    for (let i = startPage; i <= endPage; i++) {
        const pageButton = document.createElement('button');
        pageButton.innerText = i;
        pageButton.className = currentPage === i ? 'active' : '';
        pageButton.onclick = () => {
            searchUsers(i, size);
            window.scrollTo(0, 0);
        };
        paginationContainer.appendChild(pageButton);
    }

    const nextButton = document.createElement('button');
    nextButton.innerText = 'Next';
    nextButton.disabled = currentPage === totalPages;
    nextButton.onclick = () => {
        searchUsers(currentPage + 1, size);
        window.scrollTo(0, 0);
    };
    paginationContainer.appendChild(nextButton);
}

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('search-button').addEventListener('click', () => {
        currentPage = 1;
        searchUsers(currentPage, pageSize);
    });
});
