let currentPage = 1;
const pageSize = 5;

function searchUsers(page, size) {
    const keyword = document.getElementById('keyword').value.toLowerCase();
    const statusFilter = document.getElementById('status-filter').value;
    const sortFilter = document.getElementById('sort-filter').value;

    fetch(`/searchUsers?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log('Received data:', data);

            return fetch('/borrow/count')
                .then(response => response.json())
                .then(borrowCounts => {
                    const itemList = document.getElementById('item-list');
                    itemList.innerHTML = '';

                    let filteredUsers = data.content;

                    if (statusFilter) {
                        filteredUsers = filteredUsers.filter(user => user.status === statusFilter);
                    }

                    if (keyword) {
                        filteredUsers = filteredUsers.filter(user => user.username.toLowerCase().includes(keyword));
                    }

                    // 将今年的借阅次数添加到每个用户
                    filteredUsers.forEach(user => {
                        user.thisYearBorrowings = borrowCounts[user.username] || 0;
                    });

                    // 根据选择的排序字段进行排序
                    if (sortFilter === 'totalBorrowings') {
                        filteredUsers.sort((a, b) => b.totalBorrowings - a.totalBorrowings);
                    } else if (sortFilter === 'thisYearBorrowings') {
                        filteredUsers.sort((a, b) => b.thisYearBorrowings - a.thisYearBorrowings);
                    }

                    if (filteredUsers.length > 0) {
                        filteredUsers.forEach(user => {
                            const item = document.createElement('div');
                            item.className = 'item';

                            const unbanTime = user.unbanTime ? new Date(user.unbanTime) : null;
                            const banTime = user.banTime ? new Date(user.banTime) : null;
                            const localUnbanTime = unbanTime ? unbanTime.toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : 'N/A';
                            const localBanTime = banTime ? banTime.toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : 'N/A';

                            const isBanned = user.status === 'Banned';
                            const isActive = user.status === 'Active';

                            item.innerHTML = `
                                <div class="item-details">
                                    <div class="item-content">
                                        <h3><strong>Username:</strong> ${user.username}</h3>
                                        <p><strong>User ID:</strong> ${user.id}</p>
                                        <p><strong>Status:</strong> ${user.status || 'N/A'}</p>
                                        <p><strong>Ban Time:</strong> ${localBanTime}</p>
                                        <p><strong>Unban Time:</strong> ${localUnbanTime}</p>
                                        <p><strong>Total Borrowings:</strong> ${user.totalBorrowings}</p>
                                        <p><strong>This Year's Borrowings:</strong> ${user.thisYearBorrowings}</p>
                                        <div class="item-meta">
                                            <select class="ban-options" id="ban-options-${user.id}" ${isBanned ? 'disabled' : ''}>
                                                <option value="7">7 Days</option>
                                                <option value="30">1 Month</option>
                                                <option value="182">6 Months</option>
                                                <option value="3650">Permanent</option>
                                            </select>
                                            <button 
                                                class="${isBanned ? 'disabled-button' : ''}" 
                                                ${isBanned ? 'disabled' : ''}
                                                onclick="banUser('${user.id}')">
                                                Ban User
                                            </button>
                                            <button 
                                                class="${isActive ? 'disabled-button' : ''}" 
                                                ${isActive ? 'disabled' : ''}
                                                onclick="unbanUser('${user.id}')">
                                                Unban User
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            `;
                            itemList.appendChild(item);
                        });
                        document.getElementById('no-files').style.display = 'none';
                        createPagination(filteredUsers.length, page, size);
                    } else {
                        document.getElementById('no-files').style.display = 'block';
                    }
                });
        })
        .catch(error => {
            console.error('Error fetching users:', error);
        });
}


function banUser(id) {
    const selectElement = document.getElementById(`ban-options-${id}`);
    const banDuration = selectElement.value;

    fetch(`/banUser`, {
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
            alert('User banned successfully');
            searchUsers(currentPage, pageSize); // 刷新用户列表
        })
        .catch(error => {
            console.error('Error banning user:', error);
        });
}

function unbanUser(id) {
    fetch(`/removeBan`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            userId: id
        })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            alert('User unbanned successfully');
            searchUsers(currentPage, pageSize); // 刷新用户列表
        })
        .catch(error => {
            console.error('Error unbanning user:', error);
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


// 其他函数保持不变

document.addEventListener('DOMContentLoaded', function () {
    searchUsers(currentPage, pageSize);

    document.getElementById('search-button').addEventListener('click', () => {
        currentPage = 1;
        searchUsers(currentPage, pageSize);
    });

    document.getElementById('status-filter').addEventListener('change', () => {
        currentPage = 1;
        searchUsers(currentPage, pageSize);
    });

    document.getElementById('sort-filter').addEventListener('change', () => {
        currentPage = 1;
        searchUsers(currentPage, pageSize);
    });
});
