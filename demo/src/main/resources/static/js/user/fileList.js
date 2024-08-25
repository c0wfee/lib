window.onload = function () {
    const user = JSON.parse(localStorage.getItem('user'));
    if (!user || !user.username) {
        alert('User not logged in');
        window.location.href = '/login';
        return;
    }

    loadFilters();
    searchFiles(1, 10);
};

function loadFilters() {
    fetch('/getPublisher')
        .then(response => response.json())
        .then(data => populateFilterOptions('publisher-filter', data))
        .catch(error => console.error('Error fetching publishers:', error));

    fetch('/getSeries')
        .then(response => response.json())
        .then(data => populateFilterOptions('series-filter', data))
        .catch(error => console.error('Error fetching series:', error));

    fetch('/getYear')
        .then(response => response.json())
        .then(data => {
            const minYear = Math.min(...data);
            const maxYear = Math.max(...data);
            document.getElementById('publishedFrom').placeholder = minYear;
            document.getElementById('publishedTo').placeholder = maxYear;
            document.getElementById('yearRange').textContent = `Year range: ${minYear} - ${maxYear}`;
        })
        .catch(error => console.error('Error fetching years:', error));

    fetch('/getDatabases')
        .then(response => response.json())
        .then(data => populateFilterOptions('database-filter', data))
        .catch(error => console.error('Error fetching databases:', error));
}

let filterOptionsCache = {}; // 缓存所有的过滤选项

function populateFilterOptions(filterId, options) {
    const filterElement = document.getElementById(filterId);
    if (!filterElement) {
        console.error(`Element with ID '${filterId}' not found.`);
        return;
    }

    filterElement.innerHTML = '';
    filterOptionsCache[filterId] = options; // 缓存所有选项

    const maxItemsToShow = 5;

    // 显示初始的maxItemsToShow项
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

function showMoreOptions(filterId, maxItemsToShow) {
    const filterElement = document.getElementById(filterId);
    const options = filterOptionsCache[filterId];
    const currentItems = filterElement.querySelectorAll('li'); // 获取当前显示的所有列表项
    const showMoreButton = filterElement.querySelector('button'); // 获取“Show More”按钮

    if (showMoreButton && showMoreButton.textContent === "Show More") {
        // 显示更多选项
        const currentLength = currentItems.length - 1; // 减去“Show More”按钮

        // 显示下一批选项
        options.slice(currentLength, currentLength + maxItemsToShow).forEach(option => {
            const optionElement = document.createElement('li');
            optionElement.innerHTML = `
                <input type="checkbox" value="${option}" onclick="applyFilter('${filterId.split('-')[0]}', this.value)">
                <label>${option}</label>
            `;
            if (showMoreButton && showMoreButton.parentNode === filterElement) {
                filterElement.insertBefore(optionElement, showMoreButton); // 确保 showMoreButton 是当前 filterElement 的子节点
            } else {
                filterElement.appendChild(optionElement); // 直接插入元素
            }
        });

        // 如果没有更多选项，将“Show More”按钮文本改为“Show Less”
        if (currentLength + maxItemsToShow >= options.length) {
            showMoreButton.textContent = "Show Less";
        }

    } else if (showMoreButton && showMoreButton.textContent === "Show Less") {
        // 隐藏显示的额外选项
        const itemsToHide = Array.from(currentItems).slice(maxItemsToShow); // 获取要隐藏的列表项

        itemsToHide.forEach(item => {
            if (item !== showMoreButton) {
                filterElement.removeChild(item); // 删除要隐藏的列表项
            }
        });

        // 将按钮文本改为“Show More”
        showMoreButton.textContent = "Show More";

        // 将“Show More”按钮移到列表末尾
        filterElement.appendChild(showMoreButton);
    }
}

const sourceTypeImages = {
    'Digitized eBook': '../static/images/book.png',
    'Academic Journal': '../static/images/book.png',
};

let currentPage = 1;
const pageSize = 10;
let filters = {};

function toggleSearch() {
    const searchBar = document.getElementById('search-bar');
    searchBar.style.display = searchBar.style.display === 'none' || searchBar.style.display === '' ? 'flex' : 'none';
}

window.toggleSearch = toggleSearch;

function toggleList(headerElement) {
    const list = headerElement.nextElementSibling;
    list.style.display = list.style.display === 'none' || list.style.display === '' ? 'block' : 'none';
}

window.toggleList = toggleList;

function executeSearch() {
    currentPage = 1;
    searchFiles(currentPage, pageSize);
}

function applyPublishedFilter() {
    const yearType = document.querySelector('input[name="yearOption"]:checked').value;
    if (yearType === 'range') {
        const fromYear = document.getElementById('publishedFrom').value || document.getElementById('publishedFrom').placeholder;
        const toYear = document.getElementById('publishedTo').value || document.getElementById('publishedTo').placeholder;
        filters['publishedFrom'] = fromYear;
        filters['publishedTo'] = toYear;
        filters['publishedYear'] = null;  // 清空单一年份的过滤条件
    } else if (yearType === 'single') {
        const year = document.getElementById('publishedYear').value || document.getElementById('publishedYear').placeholder;
        filters['publishedYear'] = year;
        filters['publishedFrom'] = null;  // 清空年份范围的过滤条件
        filters['publishedTo'] = null;
    }
    searchFiles(currentPage, pageSize);
}


window.applyPublishedFilter = applyPublishedFilter;

function clearPublishedFilter() {
    document.getElementById('publishedFrom').value = '';
    document.getElementById('publishedTo').value = '';
    document.getElementById('publishedYear').value = '';

    delete filters['publishedFrom'];
    delete filters['publishedTo'];

    searchFiles(currentPage, pageSize);
}

window.clearPublishedFilter = clearPublishedFilter;

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
        database: filters['database'] || [],
        publishedYear: filters['publishedYear'] || null  // 确保publishedYear字段被包含在请求数据中
    };

    fetch('/search', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(filterData)
    })
        .then(response => response.json())
        .then(data => {
            console.log(data);document.addEventListener('DOMContentLoaded', () => {
                const user = JSON.parse(localStorage.getItem('user'));  // 从 localStorage 获取用户信息
                if (!user || !user.username) {
                    alert('User not logged in');
                    window.location.href = '/login';  // 如果没有登录，重定向到登录页面
                    return;
                }

                const username = user.username;

                fetchAndStoreBooks(username).then(() => {
                    loadCurrentBorrowing(); // 页面加载时默认加载当前借阅的书目
                });

                // Tab切换时加载相应的数据
                document.getElementById('current-borrowing-tab').addEventListener('click', () => {
                    loadCurrentBorrowing();
                });

                document.getElementById('borrowing-history-tab').addEventListener('click', () => {
                    loadBorrowingHistory();
                });
            });

            const itemList = document.getElementById('item-list');
            itemList.innerHTML = '';

            if (data.content.length > 0) {
                data.content.forEach(file => {
                    const item = document.createElement('div');
                    item.className = 'item';
                    const sourceImage = sourceTypeImages[file.sourceType] || '../static/images/book.png';
                    const viewButton = file.view !== "Disable" ? `<button onclick="viewPDF('${file.id}', ${file.display})">View Online</button>` : '';
                    let downloadButton = '';
                    if (file.download !== "Disable") {
                        downloadButton = `<button onclick="toggleDownloadOptions('${file.id}', '${file.downloadLink}', '${file.epubPath}', this)">Download</button>`;
                    }

                    const borrowRecord = data.borrowInfo.find(record => record.bookId === file.id);

                    let loanInfo = borrowRecord && borrowRecord.loanEndTime
                        ? `On Loan, unavailable until ${borrowRecord.loanEndTime}`
                        : file.loanLabel || 'Available';


                    const borrowButton = file.borrowPeriod > 0
                        ? `<button id="borrow-button-${file.id}" onclick="confirmBorrow('${file.id}')" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>Borrow</button>`
                        : '';

                    const loanPeriodElement = file.borrowPeriod > 0 ? `<p id="loan-period-${file.id}"><strong>Loan Period:</strong> ${file.borrowPeriod} Days</p>` : '';
                    let loanInfoElement = '';
                    if (file.borrowPeriod > 0 && file.loanLabel === 'Borrowed') {
                        loanInfoElement = `<span id="loan-info-${file.id}" style="color: grey; margin-left: 10px;">${loanInfo}</span>`;
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
                                    ${borrowButton}
                                    ${loanInfoElement}
                                    ${downloadButton}
                                </div>
                            </div>
                        </div>
                    </div>`;
                    itemList.appendChild(item);
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

function removeFilter(filterType) {
    delete filters[filterType];
    searchFiles(currentPage, pageSize);
}

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
            </div>`;
    const item = document.querySelector(`.item-meta button[onclick="downloadBook('${id}')"]`).parentNode;
    item.innerHTML += downloadOptions;
}

function toggleDownloadOptions(id, downloadLink, epubPath, button) {
    const existingOptions = document.querySelector('.download-options');
    if (existingOptions) {
        existingOptions.remove();
    }

    const downloadOptions = document.createElement('div');
    downloadOptions.className = 'download-options';

    let selectElement = `<select onchange="handleDownloadSelect('${id}', this.value)">`;
    selectElement += `<option value="">Select format</option>`;  // 默认提示

    // 检查 downloadLink 是否为 "NULL"
    if (downloadLink && downloadLink !== "NULL") {
        selectElement += `<option value="pdf,${downloadLink}">Download PDF</option>`;
    }

    // 检查 epubPath 是否为 "NULL"
    if (epubPath && epubPath !== "NULL") {
        selectElement += `<option value="epub,${epubPath}">Download EPUB</option>`;
    }

    selectElement += `</select>`;
    downloadOptions.innerHTML = selectElement;

    button.parentNode.appendChild(downloadOptions);
}

function handleDownloadSelect(id, value) {
    if (value) {
        const [format, link] = value.split(','); // 注意 link 在此场景不再需要，因为会直接从服务器获取文件
        downloadFile(id, format);
    }
}

function downloadFile(id, format) {
    const xhr = new XMLHttpRequest();
    const progressContainer = document.getElementById(`progress-container-${id}`);
    const progressBar = document.getElementById(`progress-${id}`);
    const progressText = document.getElementById(`progress-text-${id}`);
    const speedText = document.getElementById(`speed-${id}`);

    xhr.open('GET', `/downloadpdfs/${encodeURIComponent(id)}?format=${encodeURIComponent(format)}`, true);
    xhr.responseType = 'blob';

    let previousLoaded = 0;
    let startTime = Date.now();

    xhr.onprogress = function (event) {
        if (event.lengthComputable) {
            const percentComplete = (event.loaded / event.total) * 100;
            progressBar.value = percentComplete;
            progressText.innerText = `${percentComplete.toFixed(2)}%`;

            const currentTime = Date.now();
            const elapsedTime = (currentTime - startTime) / 1000;
            const speed = ((event.loaded - previousLoaded) / 1024) / elapsedTime;
            speedText.innerText = `Speed: ${speed.toFixed(2)} KB/s`;

            previousLoaded = event.loaded;
            startTime = currentTime;
        }
    };

    xhr.onloadstart = function () {
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


window.toggleDownloadOptions = toggleDownloadOptions;
window.downloadFile = downloadFile;


function confirmBorrow(id) {
    if (confirm('Are you sure you want to borrow this book?')) {
        borrowBook(id);
    }
}

async function borrowBook(id) {
    try {
        const periodResponse = await fetch(`/getBookPeriod?bookID=${id}`);
        if (!periodResponse.ok) {
            throw new Error('Failed to fetch borrow period');
        }

        const borrowPeriodText = await periodResponse.text();
        const borrowPeriod = parseInt(borrowPeriodText, 10);

        const data = new URLSearchParams();
        data.append('bookID', id);
        data.append('borrow_period', borrowPeriod);

        const borrowResponse = await fetch('/borrowBook', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: data,
        });

        if (!borrowResponse.ok) {
            throw new Error('Failed to borrow book');
        }

        alert('Operation successful');
        window.location.reload();
    } catch (error) {
        console.error('Error:', error);
        alert('Operation failed');
    }
}


window.borrowBook = borrowBook;

function displayBooks(books, containerId, isHistory = false) {
    const container = document.getElementById(containerId);
    if (!container) {
        console.error(`Container with ID '${containerId}' not found.`);
        return;
    }

    container.innerHTML = '';
    if (books.length === 0) {
        container.innerHTML = '<p class="text-center">No books borrowed.</p>';
        return;
    }

    books.forEach(book => {
        const card = document.createElement('div');
        card.className = 'card mb-3';
        card.setAttribute('data-borrow-id', book.borrowId);

        const cardHeader = document.createElement('div');
        cardHeader.className = 'card-header';
        cardHeader.textContent = book.bookTitle;

        const cardBody = document.createElement('div');
        cardBody.className = 'card-body';

        const author = document.createElement('p');
        author.className = 'card-text';
        author.textContent = `Author: ${book.authors || 'N/A'}`;

        const loanStartTime = document.createElement('p');
        loanStartTime.className = 'card-text';
        loanStartTime.textContent = `Borrowed Date: ${book.loanStartTime}`;

        const loanEndTime = document.createElement('p');
        loanEndTime.className = 'card-text';
        loanEndTime.textContent = `Due Date: ${book.loanEndTime}`;

        cardBody.appendChild(author);
        cardBody.appendChild(loanStartTime);
        cardBody.appendChild(loanEndTime);

        if (isHistory) {
            const returnedDate = document.createElement('p');
            returnedDate.className = 'card-text';
            returnedDate.textContent = `Returned Date: ${book.returnedDate || 'N/A'}`;
            cardBody.appendChild(returnedDate);
        } else {
            const viewButton = document.createElement('button');
            viewButton.className = 'btn btn-primary view-button mr-2';
            viewButton.textContent = 'View Online';
            viewButton.addEventListener('click', () => viewBook(book.bookId));

            const returnButton = document.createElement('button');
            returnButton.className = 'btn btn-secondary return-button';
            returnButton.textContent = 'Return';
            returnButton.addEventListener('click', () => {
                if (confirm('Are you sure you want to return this book?')) {
                    const borrowId = card.getAttribute('data-borrow-id');
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
    window.location.href = `/pdf?fileId=${encodeURIComponent(bookId)}`;
}
