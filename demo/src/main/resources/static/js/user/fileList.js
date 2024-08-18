window.onload = function () {
    const user = JSON.parse(localStorage.getItem('user'));  // 从 localStorage 获取用户信息
    if (!user || !user.username) {
        alert('User not logged in');
        window.location.href = '/login';  // 如果没有登录，重定向到登录页面
        return;
    }

    loadFilters(); // 加载筛选条件
    searchFiles(1, 10); // 设置默认的page和size值
};

function loadFilters() {
    fetch(`/getPublisher`)
        .then(response => response.json())
        .then(data => populateFilterOptions('publisher-filter', data))
        .catch(error => console.error('Error fetching publishers:', error));

    fetch(`/getSeries`)
        .then(response => response.json())
        .then(data => populateFilterOptions('series-filter', data))
        .catch(error => console.error('Error fetching series:', error));

    fetch(`/getYear`)
        .then(response => response.json())
        .then(data => {
            const minYear = Math.min(...data);
            const maxYear = Math.max(...data);
            document.getElementById('publishedFrom').placeholder = minYear;
            document.getElementById('publishedTo').placeholder = maxYear;
            document.getElementById('yearRange').textContent = `Year range: ${minYear} - ${maxYear}`;
        })
        .catch(error => console.error('Error fetching years:', error));

    fetch(`/getDatabases`)
        .then(response => response.json())
        .then(data => populateFilterOptions('database-filter', data))
        .catch(error => console.error('Error fetching databases:', error));
}

function populateFilterOptions(filterId, options) {
    const filterElement = document.getElementById(filterId);
    if (!filterElement) {
        console.error(`Element with ID '${filterId}' not found.`);
        return;
    }
    filterElement.innerHTML = ''; // 清空现有选项

    const maxItemsToShow = 5; // 初始显示的最大选项数

    options.slice(0, maxItemsToShow).forEach(option => {
        const optionElement = document.createElement('li');
        optionElement.innerHTML = `
            <input type="checkbox" value="${option}" onclick="applyFilter('${filterId.split('-')[0]}', this.value)">
            <label>${option}</label>
        `;
        filterElement.appendChild(optionElement);
    });

    if (options.length > maxItemsToShow) {
        const showMoreElement = document.createElement('li');
        showMoreElement.innerHTML = `
            <button onclick="showMoreOptions('${filterId}', ${maxItemsToShow})">Show More</button>
        `;
        filterElement.appendChild(showMoreElement);
    }
}

// 显示更多选项
function showMoreOptions(filterId, maxItemsToShow) {
    const filterElement = document.getElementById(filterId);
    const options = Array.from(filterElement.children);
    const showMoreButton = options.pop(); // 获取“Show More”按钮并移除

    // 清除当前选项，并显示所有选项
    filterElement.innerHTML = '';
    const allOptions = [...options, ...showMoreButton];

    // 添加所有选项
    allOptions.forEach(option => {
        filterElement.appendChild(option);
    });

    // 再次生成所有选项，并且移除“Show More”按钮
    allOptions.forEach(option => {
        if (option.innerText === 'Show More') {
            filterElement.removeChild(option);
        }
    });
}

const sourceTypeImages = {
    'Digitized eBook': '../static/images/book.png',
    'Academic Journal': '../static/images/book.png',
};

let currentPage = 1;
const pageSize = 10;
let filters = {};

// 显示和隐藏搜索栏
function toggleSearch() {
    const searchBar = document.getElementById('search-bar');
    if (searchBar.style.display === 'none' || searchBar.style.display === '') {
        searchBar.style.display = 'flex';
    } else {
        searchBar.style.display = 'none';
    }
}

window.toggleSearch = toggleSearch;

// 显示和隐藏筛选列表
function toggleList(headerElement) {
    const list = headerElement.nextElementSibling;
    if (list.style.display === 'none' || list.style.display === '') {
        list.style.display = 'block';
    } else {
        list.style.display = 'none';
    }
}

window.toggleList = toggleList;

// 执行搜索并应用筛选条件
function executeSearch() {
    currentPage = 1;
    searchFiles(currentPage, pageSize);
}

// 应用年份筛选条件
function applyPublishedFilter() {
    const yearType = document.querySelector('input[name="yearOption"]:checked').value;
    if (yearType === 'range') {
        const fromYear = document.getElementById('publishedFrom').value || document.getElementById('publishedFrom').placeholder;
        const toYear = document.getElementById('publishedTo').value || document.getElementById('publishedTo').placeholder;
        filters['publishedFrom'] = fromYear;
        filters['publishedTo'] = toYear;
    } else if (yearType === 'single') {
        const year = document.getElementById('publishedYear').value || document.getElementById('publishedYear').placeholder;
        filters['publishedFrom'] = year;
        filters['publishedTo'] = year;
    }
    searchFiles(currentPage, pageSize);
}

// 绑定函数到全局对象
window.applyPublishedFilter = applyPublishedFilter;

// 清除年份筛选条件
function clearPublishedFilter() {
    document.getElementById('publishedFrom').value = '';
    document.getElementById('publishedTo').value = '';
    document.getElementById('publishedYear').value = '';

    delete filters['publishedFrom'];
    delete filters['publishedTo'];

    searchFiles(currentPage, pageSize);
}

// 绑定函数到全局对象
window.clearPublishedFilter = clearPublishedFilter;

// 应用筛选条件并更新filters对象
function applyFilter(filterType, value) {
    if (!filters[filterType]) {
        filters[filterType] = [];
    }
    const index = filters[filterType].indexOf(value);
    if (index > -1) {
        filters[filterType].splice(index, 1);
    } else {
        filters[filterType].push(value);
    }
    searchFiles(currentPage, pageSize);
}

window.applyFilter = applyFilter;

// 搜索文件并应用所有筛选条件
function searchFiles(page, size) {
    const keyword = document.getElementById('keyword').value;

    const filterData = {
        keyword: keyword,
        page: page,
        size: size,
        series: filters['series'] || [],
        publisher: filters['publisher'] || [],
        yearRange: {
            from: filters['publishedFrom'] || '',
            to: filters['publishedTo'] || ''
        },
        database: filters['database'] || []
    };

    fetch(`/search`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(filterData)
    })
        .then(response => response.json())
        .then(data => {

            const itemList = document.getElementById('item-list');
            itemList.innerHTML = '';

            if (data.content.length > 0) {
                data.content.forEach(file => {
                    const item = document.createElement('div');
                    item.className = 'item';
                    const sourceImage = sourceTypeImages[file.sourceType] || '../static/images/book.png';
                    const viewButton = file.view !== "Disable" ? `<button onclick="viewPDF('${file.id}', ${file.display})" ${file.loanLabel === 'Borrowed'}>View Online</button>` : '';
                    const downloadButton = file.download !== "Disable" ? `<button onclick="toggleDownloadOptions('${file.id}', this)">Download</button>` : '';

                    // 默认的借阅信息
                    let loanInfo = file.loanLabel === 'Borrowed' ? `On Loan, unavailable until ${file.returnDate || 'Loading...'}` : file.loanLabel || 'Available';

                    // 生成借书按钮的逻辑，借书期为0时隐藏按钮
                    const borrowButton = file.borrowPeriod <= 0
                        ? ''  // 如果借书期为0，则不生成按钮
                        : `<button id="borrow-button-${file.id}" onclick="borrowBook('${file.id}')" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>Borrow</button>`;

                    // 生成 loan-period 元素的逻辑，借书期为0时隐藏该元素
                    const loanPeriodElement = file.borrowPeriod > 0
                        ? `<p id="loan-period-${file.id}"><strong>Loan Period:</strong> ${file.borrowPeriod} Days</p>`
                        : '';

                    // 生成 loan-info 元素的逻辑，只有在借书期大于0且状态为Borrowed时才会显示该元素
                    let loanInfoElement = '';
                    if (file.borrowPeriod > 0 && file.loanLabel === 'Borrowed') {
                        loanInfoElement = `<span id="loan-info-${file.id}" style="color: grey; margin-left: 10px;"></span>`;
                    }

                    item.innerHTML = `
                    <div class="item-details">
                        <img src="${sourceImage}" alt="${file.sourceType}" class="source-type-image">
                        <div class="item-content">
                            <h3><strong>Title:</strong> <a href="/bookDetail?id=${file.id}" target="_blank">${file.title}</a></h3>
                            ${file.authors ? `<p><strong>Authors:</strong> ${file.authors}</p>` : `<p><strong>Editors:</strong> ${file.editors || 'N/A'}</p>`}
                            <p><strong>ISBN:</strong> ${file.isbn || 'N/A'}</p>
                            <p><strong>Publisher:</strong> ${file.publisher || 'N/A'}</p>
                            <p><strong>Published:</strong> ${file.published || 'N/A'}</p>
                            ${loanPeriodElement}
                            <p>${file.description || ''}</p>
                            <div class="item-meta">
                                <p><strong>Subjects:</strong> ${file.subjects || 'N/A'}</p>
                                <a href="${file.url || '#'}" target="_blank" style="display: none;">URL: ${file.url || 'N/A'}</a>
                                <div class="button-container">
                                    ${viewButton}
                                    ${downloadButton}
                                    ${borrowButton}
                                    ${loanInfoElement}
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                    itemList.appendChild(item);

                    // 如果满足条件，获取还书日期并更新 loan-info 元素
                    if (file.borrowPeriod > 0 && file.loanLabel === 'Borrowed') {
                        fetch(`/borrow/${file.id}`, {
                            method: 'GET',
                            headers: {
                                'Content-Type': 'application/json'
                            }
                        })
                            .then(response => response.text())  // 先使用 .text() 而不是 .json()
                            .then(data => {
                                console.log('Received data:', data);  // 打印响应数据
                                try {
                                    const parsedData = JSON.parse(data);  // 手动解析 JSON
                                    console.log('Parsed data:', parsedData);
                                    const loanInfoElem = document.getElementById(`loan-info-${file.id}`);
                                    if (parsedData.loanEndTime) {
                                        loanInfoElem.textContent = `On Loan, unavailable until ${parsedData.loanEndTime}`;
                                    } else {
                                        loanInfoElem.style.display = 'none';
                                    }
                                } catch (e) {
                                    console.error('Error parsing JSON:', e);
                                }
                            })
                            .catch(error => {
                                console.error('Error fetching borrow details:', error);
                            });

                    }
                });

                document.getElementById('no-files').style.display = 'none';
                const resultsCount = document.getElementById('results-count');
                resultsCount.innerText = `You are looking at ${(page - 1) * size + 1} - ${Math.min((page - 1) * size + size, data.totalElements)} of ${data.totalElements} items`;

                const activeFilters = document.getElementById('active-filters');
                activeFilters.innerHTML = '';
                Object.entries(data.filters).forEach(([key, value]) => {
                    if (value) {
                        const filterBadge = document.createElement('div');
                        filterBadge.className = 'filter-badge';
                        filterBadge.innerHTML = `${key}: ${value} <button onclick="removeFilter('${key}')">&minus;</button>`;
                        activeFilters.appendChild(filterBadge);
                    }
                });

                createPagination(data.totalElements, page, size);
            } else {
                document.getElementById('no-files').style.display = 'block';
            }
        })
        .catch(error => {
            console.error('Error fetching files:', error);
        });
}

// 创建分页
function createPagination(totalElements, currentPage, size) {
    const paginationContainer = document.getElementById('pagination');
    paginationContainer.innerHTML = '';
    const totalPages = Math.ceil(totalElements / size);

    const prevButton = document.createElement('button');
    prevButton.innerText = 'Previous';
    prevButton.disabled = currentPage === 1;
    prevButton.onclick = () => {
        searchFiles(currentPage - 1, size);
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
            searchFiles(i, size);
            window.scrollTo(0, 0);
        };
        paginationContainer.appendChild(pageButton);
    }

    const nextButton = document.createElement('button');
    nextButton.innerText = 'Next';
    nextButton.disabled = currentPage === totalPages;
    nextButton.onclick = () => {
        searchFiles(currentPage + 1, size);
        window.scrollTo(0, 0);
    };
    paginationContainer.appendChild(nextButton);
}

// 移除筛选条件
function removeFilter(filterType) {
    delete filters[filterType];
    searchFiles(currentPage, pageSize);
}

// 切换年份输入框的显示
function toggleYearInputs(yearType) {
    const rangeInputs = document.getElementById('rangeInputs');
    const singleInput = document.getElementById('singleInput');

    if (yearType === 'range') {
        rangeInputs.style.display = 'block';
        singleInput.style.display = 'none';
    } else if (yearType === 'single') {
        rangeInputs.style.display = 'none';
        singleInput.style.display = 'block';
    }
}

// 绑定函数到全局对象
window.toggleYearInputs = toggleYearInputs;

function viewPDF(id, view) {
    if (view == null) {
        window.location.href = `/pdf?fileId=${encodeURIComponent(id)}`;
    } else {
        fetch(`/downloadfiles/${encodeURIComponent(id)}`, {responseType: 'blob'})
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.blob();
            })
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                window.open(url);
            })
            .catch(error => {
                console.error('Error downloading PDF:', error);
            });
    }
}

// 将 viewPDF 函数挂载到 window 对象上，使其在全局范围内可用
window.viewPDF = viewPDF;

function downloadBook(id) {
    const downloadOptions = `
            <div id="download-options-${id}">
                <button onclick="downloadFile('${id}', 'pdf')">Download PDF</button>
                <button onclick="downloadFile('${id}', 'epub')">Download EPUB</button>
                <div id="progress-container-${id}" style="margin-top: 10px; display: none;">
                    <progress id="progress-${id}" value="0" max="100"></progress>
                    <span id="progress-text-${id}">0%</span>
                    <span id="speed-${id}"></span>
                </div>
            </div>
        `;
    const item = document.querySelector(`.item-meta button[onclick="downloadBook('${id}')"]`).parentNode;
    item.innerHTML += downloadOptions;
}

function downloadFile(id, format) {
    const url = `/downloadpdfs/${encodeURIComponent(id)}?format=${format}`;
    const xhr = new XMLHttpRequest();
    const progressContainer = document.getElementById(`progress-container-${id}`);
    const progressBar = document.getElementById(`progress-${id}`);
    const progressText = document.getElementById(`progress-text-${id}`);
    const speedText = document.getElementById(`speed-${id}`);

    xhr.open('GET', url, true);
    xhr.responseType = 'blob';

    let previousLoaded = 0;
    let startTime = Date.now();

    xhr.onprogress = function (event) {
        if (event.lengthComputable) {
            const percentComplete = (event.loaded / event.total) * 100;
            progressBar.value = percentComplete;
            progressText.innerText = `${percentComplete.toFixed(2)}%`;

            const currentTime = Date.now();
            const elapsedTime = (currentTime - startTime) / 1000; // in seconds
            const speed = ((event.loaded - previousLoaded) / 1024) / elapsedTime; // KB/s
            speedText.innerText = `Speed: ${speed.toFixed(2)} KB/s`;

            previousLoaded = event.loaded;
            startTime = currentTime;
        }
    };

    xhr.onloadstart = function () {
        const progressContainer = document.getElementById(`progress-container-${id}`);
        if (progressContainer) {
            progressContainer.style.display = 'block';
        }
    };

    xhr.onload = function () {
        if (xhr.status === 200) {
            const blob = xhr.response;
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = downloadUrl;
            a.download = `${id}.${format}`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(downloadUrl);
            document.body.removeChild(a);
            progressContainer.style.display = 'none';
        } else {
            console.error('Download failed:', xhr.statusText);
        }
    };

    xhr.onerror = function () {
        console.error('Download error:', xhr.statusText);
        progressContainer.style.display = 'none';
    };

    xhr.send();
}

// 将函数挂载到window对象上，使其在全局范围内可用
window.downloadFile = downloadFile;

function toggleDownloadOptions(id, button) {
    const existingOptions = document.querySelector('.download-options');
    if (existingOptions) {
        existingOptions.remove();
    }
    const downloadOptions = document.createElement('div');
    downloadOptions.className = 'download-options';
    downloadOptions.innerHTML = `
        <button onclick="downloadFile('${id}', 'pdf')">Download PDF</button>
        <button onclick="downloadFile('${id}', 'epub')">Download EPUB</button>
    `;
    button.parentNode.appendChild(downloadOptions);
}

// 如果你希望在模块中定义，但仍然让这个函数在全局可访问，可以使用以下方式
window.toggleDownloadOptions = toggleDownloadOptions;

async function borrowBook(id) {
    try {
        let periodUrl = `/getBookPeriod?bookID=${id}`;
        let periodResponse = await fetch(periodUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!periodResponse.ok) {
            throw new Error('Failed to fetch borrow period');
        }

        let borrowPeriodText = await periodResponse.text();
        let borrowPeriod = parseInt(borrowPeriodText, 10);

        let data = new URLSearchParams();
        data.append('bookID', id);
        data.append('borrow_period', borrowPeriod);

        let borrowResponse = await fetch("/borrowBook", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: data
        });

        if (!borrowResponse.ok) {
            throw new Error('Failed to borrow book');
        }

        let borrowData = await borrowResponse.json();
        alert('Operation successful');

        // 刷新页面以反映借书操作
        window.location.reload();
    } catch (error) {
        console.error('Error:', error);
        alert('Operation failed');
    }
}

// 将 borrowBook 函数挂载到 window 对象上，使其在全局范围内可用
window.borrowBook = borrowBook;

document.addEventListener('DOMContentLoaded', () => {
    loadCurrentBorrowing(); // 页面加载时默认加载当前借阅的书目

    // Tab切换时加载相应的数据
    document.getElementById('current-borrowing-tab').addEventListener('click', () => {
        loadCurrentBorrowing();
    });

    document.getElementById('borrowing-history-tab').addEventListener('click', () => {
        loadBorrowingHistory();
    });
});

function loadCurrentBorrowing() {
    const user = JSON.parse(localStorage.getItem('user')); // 从 localStorage 获取用户信息
    if (!user || !user.username) {
        alert('User not logged in');
        window.location.href = '/login';  // 如果没有登录，重定向到登录页面
        return;
    }
    fetchBooks(user.username, false); // 使用从 localStorage 获取的用户名
}

function loadBorrowingHistory() {
    const user = JSON.parse(localStorage.getItem('user')); // 从 localStorage 获取用户信息
    if (!user || !user.username) {
        alert('User not logged in');
        window.location.href = '/login';  // 如果没有登录，重定向到登录页面
        return;
    }
    fetchBooks(user.username, true); // 使用从 localStorage 获取的用户名
}

async function fetchBooks(username, isHistory = false) {
    try {
        const response = await fetch(`/api/borrows/${username}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const books = await response.json();
        console.log('Fetched Books:', books);

        let filteredBooks;
        if (isHistory) {
            // 筛选历史借阅书籍（status 为 "Returned"）
            filteredBooks = books.filter(book => book.status === "Returned");
        } else {
            // 筛选当前借阅书籍（status 不为 "Returned"）
            filteredBooks = books.filter(book => book.status !== "Returned");
        }

        displayBooks(filteredBooks, isHistory ? 'borrowing-history-container' : 'current-borrowing-container', isHistory);
    } catch (error) {
        console.error('Error fetching books:', error);
    }
}

function displayBooks(books, containerId, isHistory = false) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';
    if (books.length === 0) {
        container.innerHTML = '<p class="text-center">No books borrowed.</p>';
        return;
    }

    books.forEach(book => {
        const card = document.createElement('div');
        card.className = 'card mb-3';
        card.setAttribute('data-borrow-id', book.borrowId); // 将 borrow_id 存储在 data 属性中

        const cardHeader = document.createElement('div');
        cardHeader.className = 'card-header';
        cardHeader.textContent = book.bookTitle;

        const cardBody = document.createElement('div');
        cardBody.className = 'card-body';

        const author = document.createElement('p');
        author.className = 'card-text';
        author.textContent = 'Author: ' + (book.authors || 'N/A');

        const loanStartTime = document.createElement('p');
        loanStartTime.className = 'card-text';
        loanStartTime.textContent = 'Borrowed Date: ' + book.loanStartTime;

        const loanEndTime = document.createElement('p');
        loanEndTime.className = 'card-text';
        loanEndTime.textContent = 'Due Date: ' + book.loanEndTime;

        cardBody.appendChild(author);
        cardBody.appendChild(loanStartTime);
        cardBody.appendChild(loanEndTime);

        if (isHistory) {
            const returnedDate = document.createElement('p');
            returnedDate.className = 'card-text';
            returnedDate.textContent = 'Returned Date: ' + (book.returnedDate || 'N/A');
            cardBody.appendChild(returnedDate);
        } else {
            const viewButton = document.createElement('button');
            viewButton.className = 'btn btn-primary view-button mr-2';
            viewButton.textContent = 'View Online';
            viewButton.addEventListener('click', () => {
                viewBook(book.bookId);
            });

            const returnButton = document.createElement('button');
            returnButton.className = 'btn btn-secondary return-button';
            returnButton.textContent = 'Return';
            returnButton.addEventListener('click', () => {
                if (confirm('Are you sure you want to return this book?')) {
                    const borrowId = card.getAttribute('data-borrow-id'); // 获取存储的 borrow_id
                    returnBook(borrowId);
                }
            });

            cardBody.appendChild(viewButton);
            cardBody.appendChild(returnButton);
        }

        card.appendChild(cardHeader);
        card.appendChild(cardBody);
        container.appendChild(card);
    });
}

function viewBook(bookId) {
    console.log('View book with ID:', bookId);
    window.location.href = `/pdf?fileId=${encodeURIComponent(bookId)}`;
}

function returnBook(borrowId) {
    let data = new URLSearchParams();
    data.append('borrow_id', borrowId); // 这里使用新的参数名 'borrow_id'

    fetch("/returnBook", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: data
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.text(); // 后端返回的是字符串，不是 JSON
        })
        .then(data => {
            console.log('Success:', data);
            alert(data); // 提示成功信息
            loadCurrentBorrowing(); // 重新加载当前借阅的书目
        })
        .catch((error) => {
            console.error('Error:', error);
            alert('Operation failed');
        });
}
