let allData = []; // 全局变量存储所有书籍数据
let filters = {
    loanLabel: [],
    database: [],
    borrowedFrom: '',
    borrowedTo: '',
    dueFrom: '',
    dueTo: '',
    keyword: '',
    page: 1,  // 当前页码
    size: 10  // 每页显示的记录数
};

document.addEventListener('DOMContentLoaded', () => {
    fetchData(); // 初次加载时获取所有数据并合并

    // 监听搜索框的输入事件
    const searchInput = document.getElementById('keyword');
    searchInput.addEventListener('input', () => {
        applyFiltersAndDisplay(); // 当用户输入时，调用搜索函数
    });

    // 添加分页按钮的点击事件
    document.getElementById('prev-page').addEventListener('click', () => changePage(-1));
    document.getElementById('next-page').addEventListener('click', () => changePage(1));

    // 添加清空按钮的点击事件
    document.getElementById('clear-borrowed-date').addEventListener('click', () => clearDateFilter('borrowed'));
    document.getElementById('clear-due-date').addEventListener('click', () => clearDateFilter('due'));

    // 添加全选功能的点击事件
    document.getElementById('search-all').addEventListener('click', () => toggleAllSearch());

    // 添加清除搜索条件按钮的点击事件
    document.getElementById('clear-search').addEventListener('click', () => clearSearch());

    // 加载数据库过滤选项
    loadDatabaseOptions();

    // 监听数据库过滤选项的应用
    document.getElementById('apply-database-filter').addEventListener('click', () => applyDatabaseFilter());
});

// 获取并合并数据
async function fetchData() {
    try {
        const borrowResponse = await fetch('/api/borrows', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        const borrowData = await borrowResponse.json();

        const searchResponse = await fetch('/search', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                page: 1,  // 请求所有数据
                size: 1000  // 请求较大数量的数据
            })
        });

        const searchData = await searchResponse.json();
        console.log(searchData);
        // 假设 searchData.content 是数组
        const searchDataArray = searchData.content || [];

        allData = borrowData.map(borrow => {
            const bookInfo = searchDataArray.find(book => book.bookID === borrow.bookID);
            return {
                ...borrow,
                ...bookInfo
            };
        });

        // 在数据加载完成后加载数据库选项
        loadDatabaseOptions();
        applyFiltersAndDisplay(); // 显示数据
    } catch (error) {
        console.error('Error fetching data:', error);
    }
}

// 定义 applySearchFilter 函数
function applySearchFilter() {
    filters.keyword = document.getElementById('keyword').value;
    applyFiltersAndDisplay(); // 使用关键字重新过滤并显示数据
}

// 加载数据库过滤选项
function loadDatabaseOptions() {
    const databaseSelect = document.getElementById('database-select');
    const uniqueDatabases = [...new Set(allData.map(book => book.databaseName))];

    uniqueDatabases.forEach(database => {
        const option = document.createElement('option');
        option.value = database;
        option.textContent = database;
        databaseSelect.appendChild(option);
    });
}

// 切换数据库选择的显示/隐藏
function toggleDatabaseSelect() {
    const databaseSelectContainer = document.getElementById('database-select-container');
    if (databaseSelectContainer.style.display === 'none') {
        databaseSelectContainer.style.display = 'block';
    } else {
        databaseSelectContainer.style.display = 'none';
    }
}

// 应用数据库过滤器
function applyDatabaseFilter() {
    const selectedOptions = Array.from(document.getElementById('database-select').selectedOptions);
    filters.database = selectedOptions.map(option => option.value);
    applyFiltersAndDisplay();
}

// 前端过滤和显示数据
function applyFiltersAndDisplay() {
    let filteredData = allData;

    // 过滤 Loan Status
    if (filters.loanLabel.length > 0) {
        filteredData = filteredData.filter(book => filters.loanLabel.includes(book.loanLabel));
    }

    // 过滤 Borrowed Date
    if (filters.borrowedFrom) {
        filteredData = filteredData.filter(book => new Date(book.loanStartTime) >= new Date(filters.borrowedFrom));
    }
    if (filters.borrowedTo) {
        filteredData = filteredData.filter(book => new Date(book.loanStartTime) <= new Date(filters.borrowedTo));
    }

    // 过滤 Due Date
    if (filters.dueFrom) {
        filteredData = filteredData.filter(book => new Date(book.loanEndTime) >= new Date(filters.dueFrom));
    }
    if (filters.dueTo) {
        filteredData = filteredData.filter(book => new Date(book.loanEndTime) <= new Date(filters.dueTo));
    }

    // 过滤 Database
    if (filters.database.length > 0) {
        filteredData = filteredData.filter(book => filters.database.includes(book.databaseName));
    }

    // 过滤 Keyword，基于选中的复选框选项
    if (filters.keyword) {
        const searchAll = document.getElementById('search-all').checked;
        const searchTitle = document.getElementById('search-title').checked;
        const searchIsbn = document.getElementById('search-isbn').checked;
        const searchBorrower = document.getElementById('search-borrower').checked;

        filteredData = filteredData.filter(book => {
            let matches = false;
            if (searchAll || (searchTitle && book.bookTitle.toLowerCase().includes(filters.keyword.toLowerCase()))) {
                matches = true;
            }
            if (searchAll || (searchIsbn && book.isbn.toLowerCase().includes(filters.keyword.toLowerCase()))) {
                matches = true;
            }
            if (searchAll || (searchBorrower && book.username && book.username.toLowerCase().includes(filters.keyword.toLowerCase()))) {
                matches = true;
            }
            return matches;
        });
    }

    // 分页处理
    const startIndex = (filters.page - 1) * filters.size;
    const paginatedData = filteredData.slice(startIndex, startIndex + filters.size);

    displayBooks(paginatedData);
    updatePagination(filteredData.length);
}

// 显示数据
function displayBooks(books) {
    const tableBody = document.getElementById('books-table-body');
    tableBody.innerHTML = ''; // 清除现有内容
    books.forEach(book => {
        const row = document.createElement('tr');

        // 显示 Borrow ID
        const borrowIdCell = document.createElement('td');
        borrowIdCell.textContent = book.borrowId;
        row.appendChild(borrowIdCell);

        // 显示 ISBN
        const isbnCell = document.createElement('td');
        isbnCell.textContent = book.isbn || 'N/A';
        row.appendChild(isbnCell);

        // 显示 Book Title
        const bookTitleCell = document.createElement('td');
        bookTitleCell.textContent = book.bookTitle || 'N/A';
        row.appendChild(bookTitleCell);

        // 显示 Database
        const databaseCell = document.createElement('td');
        databaseCell.textContent = book.databaseName || 'N/A'; // 使用 databaseName
        row.appendChild(databaseCell);

        // 显示 Loan Status (转换 Borrowed 为 On Loan)
        const loanStatusCell = document.createElement('td');
        let loanStatus = book.loanLabel;
        if (loanStatus === 'Borrowed') {
            loanStatus = 'On Loan';
        }
        loanStatusCell.textContent = loanStatus || 'N/A';
        row.appendChild(loanStatusCell);

        // 显示 Borrowed Date (Loan Start Time)
        const loanStartTimeCell = document.createElement('td');
        loanStartTimeCell.textContent = book.loanStartTime || 'N/A';
        row.appendChild(loanStartTimeCell);

        // 显示 Due Date (Loan End Time)
        const loanEndTimeCell = document.createElement('td');
        loanEndTimeCell.textContent = book.loanEndTime || 'N/A';
        row.appendChild(loanEndTimeCell);

        // 显示 Returned Date
        const returnedDateCell = document.createElement('td');
        returnedDateCell.textContent = book.returnedDate ? book.returnedDate : 'Not Returned';
        row.appendChild(returnedDateCell);

        // 显示 Borrower (Username)
        const usernameCell = document.createElement('td');
        usernameCell.textContent = book.username || 'N/A';
        row.appendChild(usernameCell);

        // 添加 Actions 列，包含 Return 和 Delete 按钮
        const actionsCell = document.createElement('td');
        const returnButton = document.createElement('button');
        returnButton.className = 'btn btn-success btn-sm';
        returnButton.textContent = 'Return';
        returnButton.onclick = () => returnBook(book.borrowId);

        const deleteButton = document.createElement('button');
        deleteButton.className = 'btn btn-danger btn-sm ml-2';
        deleteButton.textContent = 'Delete';
        deleteButton.onclick = () => deleteBorrow(book.borrowId);

        actionsCell.appendChild(returnButton);
        actionsCell.appendChild(deleteButton);
        row.appendChild(actionsCell);

        tableBody.appendChild(row);
    });
}

// 修改当前页码并获取数据
function changePage(delta) {
    filters.page += delta;
    applyFiltersAndDisplay();
}

// 更新分页按钮状态
function updatePagination(totalElements) {
    const totalPages = Math.ceil(totalElements / filters.size);
    document.getElementById('prev-page').disabled = filters.page <= 1;
    document.getElementById('next-page').disabled = filters.page >= totalPages;
}

// 应用日期和状态过滤器
function applyPublishedFilter() {
    filters.borrowedFrom = document.getElementById('borrowedFrom').value;
    filters.borrowedTo = document.getElementById('borrowedTo').value;
    filters.dueFrom = document.getElementById('dueFrom').value;
    filters.dueTo = document.getElementById('dueTo').value;

    applyFiltersAndDisplay(); // 重新过滤并显示数据
}

// 清空日期过滤器
function clearDateFilter(type) {
    if (type === 'borrowed') {
        filters.borrowedFrom = '';
        filters.borrowedTo = '';
        document.getElementById('borrowedFrom').value = '';
        document.getElementById('borrowedTo').value = '';
    } else if (type === 'due') {
        filters.dueFrom = '';
        filters.dueTo = '';
        document.getElementById('dueFrom').value = '';
        document.getElementById('dueTo').value = '';
    }

    applyFiltersAndDisplay(); // 重新过滤并显示数据
}

// 应用其他过滤条件（如状态、数据库等）
function applyFilter(type, value) {
    if (!filters.hasOwnProperty(type)) {
        console.error(`Filter type '${type}' is not defined in filters object.`);
        return;
    }

    if (!Array.isArray(filters[type])) {
        console.error(`Filter type '${type}' should be an array in filters object.`);
        return;
    }

    // 将 On Loan 转换为 Borrowed 进行过滤
    if (value === 'On Loan') {
        value = 'Borrowed';
    }

    if (!filters[type].includes(value)) {
        filters[type].push(value);
    } else {
        filters[type] = filters[type].filter(item => item !== value);
    }

    applyFiltersAndDisplay(); // 重新过滤并显示数据
}

// 全选功能
function toggleAllSearch() {
    const titleCheckbox = document.getElementById('search-title');
    const isbnCheckbox = document.getElementById('search-isbn');
    const borrowerCheckbox = document.getElementById('search-borrower');

    const allChecked = document.getElementById('search-all').checked;

    titleCheckbox.checked = allChecked;
    isbnCheckbox.checked = allChecked;
    borrowerCheckbox.checked = allChecked;
}

// 清空搜索条件并刷新页面
function clearSearch() {
    document.getElementById('keyword').value = '';
    document.getElementById('search-all').checked = false;
    document.getElementById('search-title').checked = false;
    document.getElementById('search-isbn').checked = false;
    document.getElementById('search-borrower').checked = false;

    // 清空过滤条件
    filters.keyword = '';
    filters.loanLabel = [];
    filters.database = [];
    filters.borrowedFrom = '';
    filters.borrowedTo = '';
    filters.dueFrom = '';
    filters.dueTo = '';
    filters.page = 1;

    // 重新加载数据和显示
    applyFiltersAndDisplay();
}

// 归还书籍
async function returnBook(borrowId) {
    try {
        const params = new URLSearchParams();
        params.append('borrow_id', borrowId);

        const response = await fetch('/returnBook', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'  // 使用表单格式发送
            },
            body: params.toString()
        });
        if (response.ok) {
            alert('Successfully returned book.');
            fetchData(); // 刷新数据
        } else {
            alert('Failed to return book.');
        }
    } catch (error) {
        console.error('Error returning book:', error);
    }
}

// 删除借阅记录
async function deleteBorrow(borrowId) {
    try {
        const params = new URLSearchParams();
        params.append('borrow_id', borrowId);

        const response = await fetch('/deleteBorrowInfo', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: params.toString()
        });

        if (response.ok) {
            alert('Successfully deleted borrow record.');
            fetchData(); // 刷新数据
        } else {
            alert('Failed to delete borrow record.');
        }
    } catch (error) {
        console.error('Error deleting borrow record:', error);
    }
}


// 将函数绑定到全局对象上
window.applyPublishedFilter = applyPublishedFilter;
window.clearDateFilter = clearDateFilter;
window.applyFilter = applyFilter;
window.applySearchFilter = applySearchFilter;
window.changePage = changePage;
window.toggleAllSearch = toggleAllSearch;
window.clearSearch = clearSearch;
window.applyDatabaseFilter = applyDatabaseFilter;
window.toggleDatabaseSelect = toggleDatabaseSelect;
window.returnBook = returnBook;
window.deleteBorrow = deleteBorrow;
