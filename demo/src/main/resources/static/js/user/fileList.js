window.onload = function() {
    searchFiles(1, 10); // 设置默认的page和size值
};

const sourceTypeImages = {
    'Digitized eBook': '../static/images/book.png',
    'Academic Journal': '../static/images/book.png',
};

const API_BASE_URL = "localhost:8088";
let currentPage = 1;
const pageSize = 10;
let filters = {};

function toggleSearch() {
    const searchBar = document.getElementById('search-bar');
    if (searchBar.style.display === 'none' || searchBar.style.display === '') {
        searchBar.style.display = 'flex';
    } else {
        searchBar.style.display = 'none';
    }
}

function executeSearch() {
    currentPage = 1;
    searchFiles(currentPage, pageSize);
}

function searchFiles(page, size) {
    console.log('searchFiles function called');
    console.log(filters);

    const keyword = document.getElementById('keyword').value;
    const series = filters['series'] || [];
    const publisher = filters['publisher'] || [];
    const subject = filters['subject'] || [];
    const database = filters['database'] || [];
    const publishedFrom = filters['publishedFrom'] || null;
    const publishedTo = filters['publishedTo'] || null;
    const publishedYear = filters['publishedYear'] || null;

    const filterData = {
        keyword: keyword,
        series: series,
        publisher: publisher,
        subject: subject,
        database: database,
        publishedFrom: publishedFrom,
        publishedTo: publishedTo,
        publishedYear: publishedYear,
        page: page,
        size: size
    };

    console.log('Filter data being sent:', filterData);

    fetch(`http://${API_BASE_URL}/search`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(filterData)
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log(data);
            const itemList = document.getElementById('item-list');
            itemList.innerHTML = ''; // 清空之前的结果

            if (data.content.length > 0) {
                data.content.forEach(file => {
                    console.log(file); // 显示每个 file 的内容
                    const item = document.createElement('div');
                    item.className = 'item';
                    const sourceImage = sourceTypeImages[file.sourceType] || '../static/images/book.png';
                    const viewButton = file.view !== "Disable" ? `<button onclick="viewPDF('${file.id}', ${file.display})" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>View Online</button>` : '';
                    const downloadButton = file.download !== "Disable" ? `<button onclick="toggleDownloadOptions('${file.id}', this)" ${file.download === 'Disable' ? 'style="display: none;"' : ''}>Download</button>` : '';

                    // 默认的 loanPeriod 文本
                    let loanPeriod = file.loanLabel === 'Borrowed' ? `Borrowed (Return by ${file.returnDate || 'Loading...'})` : file.loanLabel || 'N/A';

                    item.innerHTML = `
                    <div class="item-details">
                        <img src="${sourceImage}" alt="${file.sourceType}" class="source-type-image">
                        <div class="item-content">
                            <h3><strong>Title:</strong> <a href="/bookDetail?id=${file.id}" target="_blank">${file.title}</a></h3>
                            <p><strong>Alternate Title:</strong> ${file.alternativeTitle || 'N/A'}</p>
                            <p><strong>Source Type:</strong> ${file.sourceType || 'N/A'}</p>
                            <p><strong>Authors:</strong> ${file.authors || 'N/A'}</p>
                            <p><strong>ISBN:</strong> ${file.isbn || 'N/A'}</p>
                            <p><strong>Publisher:</strong> ${file.publisher || 'N/A'}</p>
                            <p><strong>Published:</strong> ${file.published || 'N/A'}</p>
                            <p><strong>Status:</strong> ${file.status || 'N/A'}</p>
                            <p id="loan-period-${file.id}"><strong>Loan Period:</strong> ${loanPeriod}</p>
                            <p>${file.description || ''}</p>
                            <div class="item-meta">
                                <p><strong>Subjects:</strong> ${file.subjects || 'N/A'}</p>
                                <a href="${file.url || '#'}" target="_blank" style="display: none;">URL: ${file.url || 'N/A'}</a>
                                <div class="button-container">
                                     ${file.view !== 'Disable' ? viewButton : ''}
                                    ${file.download !== 'Disable' ? downloadButton : ''}
                                    <button onclick="borrowBook('${file.id}')" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>Borrow</button>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                    itemList.appendChild(item);

                    // 如果 loanLabel 为 Borrowed，则发送另一个请求以获取归还日期
                    if (file.loanLabel === 'Borrowed') {
                        console.log("Borrowed")
                        fetch(`http://${API_BASE_URL}/borrow/${file.id}`, {
                            method: 'GET',
                            headers: {
                                'Content-Type': 'application/json'
                            }
                        })
                            .then(response => {
                                if (!response.ok) {
                                    throw new Error('Network response was not ok');
                                }
                                return response.json();
                            })
                            .then(borrowData => {
                                console.log(borrowData)
                                const returnDate = borrowData.loanEndTime  || 'N/A';
                                document.getElementById(`loan-period-${file.id}`).innerHTML = `<strong>Loan Period:</strong> Borrowed (Return by ${returnDate})`;
                            })
                            .catch(error => {
                                console.error('Error fetching borrow details:', error);
                            });
                    }
                });

                document.getElementById('no-files').style.display = 'none';

                // 更新结果信息
                const resultsCount = document.getElementById('results-count');
                resultsCount.innerText = `You are looking at ${(page-1) * size + 1} - ${Math.min((page-1) * size + size, data.totalElements)} of ${data.totalElements} items`;

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


// 修改部分：确保图片字段列表默认显示所有字段，避免空白页
function displayBookInfo(file) {
    const item = document.createElement('div');
    item.className = 'item';
    const sourceImage = sourceTypeImages[file.sourceType] || '../static/images/book.png';
    const viewButton = file.view !== "Disable" ? `<button onclick="viewPDF('${file.id}', ${file.display})" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>View Online</button>` : '';
    const downloadButton = file.download !== "Disable" ? `<button onclick="toggleDownloadOptions('${file.id}', this)" ${file.download === 'Disable' ? 'style="display: none;"' : ''}>Download</button>` : '';

    let loanPeriod = file.loanLabel === 'Borrowed' ? `Borrowed (Return by ${file.returnDate || 'Loading...'})` : file.loanLabel || 'N/A';

    item.innerHTML = `
        <div class="item-details">
            <img src="${sourceImage}" alt="${file.sourceType}" class="source-type-image">
            <div class="item-content">
                <h3><strong>Title:</strong> <a href="/bookDetail?id=${file.id}" target="_blank">${file.title}</a></h3>
                <p><strong>Alternate Title:</strong> ${file.alternativeTitle || 'N/A'}</p>
                <p><strong>Source Type:</strong> ${file.sourceType || 'N/A'}</p>
                <p><strong>Authors:</strong> ${file.authors || 'N/A'}</p>
                <p><strong>ISBN:</strong> ${file.isbn || 'N/A'}</p>
                <p><strong>Publisher:</strong> ${file.publisher || 'N/A'}</p>
                <p><strong>Published:</strong> ${file.published || 'N/A'}</p>
                <p><strong>Status:</strong> ${file.status || 'N/A'}</p>
                <p id="loan-period-${file.id}"><strong>Loan Period:</strong> ${loanPeriod}</p>
                <p>${file.description || ''}</p>
                <div class="item-meta">
                    <p><strong>Subjects:</strong> ${file.subjects || 'N/A'}</p>
                    <a href="${file.url || '#'}" target="_blank">URL: ${file.url || 'N/A'}</a>
                    <div class="button-container">
                         ${viewButton}
                         ${downloadButton}
                        <button onclick="borrowBook('${file.id}')" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>Borrow</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    return item;
}

// 修改部分：筛选器字段参照实际数据动态生成
function createFilterOptions(filterType, options) {
    const filterList = document.querySelector(`#${filterType}-list`);
    filterList.innerHTML = '';
    options.forEach(option => {
        const li = document.createElement('li');
        li.innerHTML = `<input type="checkbox" id="${filterType}-${option}" value="${option}" onclick="applyFilter('${filterType}', this.value)">
                        <label for="${filterType}-${option}">${option}</label>`;
        filterList.appendChild(li);
    });
}

// 在搜索时动态生成筛选器选项
function updateFilters(data) {
    createFilterOptions('series', data.seriesOptions);
    createFilterOptions('publisher', data.publisherOptions);
    createFilterOptions('subject', data.subjectOptions);
    createFilterOptions('database', data.databaseOptions);
}

fetch(`http://${API_BASE_URL}/search`, {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(filterData)
})
    .then(response => response.json())
    .then(data => {
        updateFilters(data);
        displayResults(data);
    })
    .catch(error => console.error('Error fetching files:', error));

// 修改部分：确保Published字段选择时间范围
function applyPublishedFilter() {
    const yearType = document.querySelector('input[name="yearType"]:checked').value;

    if (yearType === 'range') {
        const fromYear = document.getElementById('publishedFrom').value;
        const toYear = document.getElementById('publishedTo').value;

        if (fromYear >= 1893 && toYear <= 2024 && fromYear <= toYear) {
            filters['publishedFrom'] = fromYear;
            filters['publishedTo'] = toYear;
            filters['publishedYear'] = null; // 清空单年值
        } else {
            alert('Please enter a valid year range: From year >= 1893, To year <= 2024, and From year <= To year.');
            return;
        }
    } else if (yearType === 'single') {
        const year = document.getElementById('publishedYear').value;

        if (year >= 1893 && year <= 2024) {
            filters['publishedYear'] = year;
            filters['publishedFrom'] = null; // 清空范围年值
            filters['publishedTo'] = null;
        } else {
            alert('Please enter a valid year: between 1893 and 2024.');
            return;
        }
    }
    searchFiles(1, 10);
}

function removeFilter(filterType) {
    delete filters[filterType];
    searchFiles(currentPage, pageSize);
}

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('search-button').addEventListener('click', () => {
        currentPage = 1;
        searchFiles(currentPage, pageSize);
    });
});

function viewPDF(id, view) {
    if (view == null) {
        window.location.href = `/pdf?fileId=${encodeURIComponent(id)}`;
    } else {
        fetch(`http://${API_BASE_URL}/downloadfiles/${encodeURIComponent(id)}`, { responseType: 'blob' })
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
    const url = `http://${API_BASE_URL}/downloadpdfs/${encodeURIComponent(id)}?format=${format}`;
    const xhr = new XMLHttpRequest();
    const progressContainer = document.getElementById(`progress-container-${id}`);
    const progressBar = document.getElementById(`progress-${id}`);
    const progressText = document.getElementById(`progress-text-${id}`);
    const speedText = document.getElementById(`speed-${id}`);

    xhr.open('GET', url, true);
    xhr.responseType = 'blob';

    let previousLoaded = 0;
    let startTime = Date.now();

    xhr.onprogress = function(event) {
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

    xhr.onloadstart = function() {
        progressContainer.style.display = 'block';
    };

    xhr.onload = function() {
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

    xhr.onerror = function() {
        console.error('Download error:', xhr.statusText);
        progressContainer.style.display = 'none';
    };

    xhr.send();
}

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

async function borrowBook(id) {
    try {
        // Construct the URL with the book ID parameter
        let periodUrl = `/getBookPeriod?bookID=${id}`;

        // Fetch the borrow period
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
        let borrowPeriod = parseInt(borrowPeriodText, 10); // 将文本转换为整数

        // Prepare the data for borrowing the book
        let data = new URLSearchParams();
        data.append('bookID', id);
        data.append('borrow_period', borrowPeriod); // 使用获取的借阅时间

        // Borrow the book
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
        console.log('Success:', borrowData);
        alert('Operation successful');
    } catch (error) {
        console.error('Error:', error);
        alert('Operation failed');
    }
}

function toggleList(header) {
    const list = header.nextElementSibling;
    if (list.style.display === '' || list.style.display === 'none') {
        list.style.display = 'block';
    } else {
        list.style.display = 'none';
    }
}

function toggleYearInputs(value) {
    if (value === 'range') {
        document.getElementById('rangeInputs').style.display = 'block';
        document.getElementById('singleInput').style.display = 'none';
    } else {
        document.getElementById('rangeInputs').style.display = 'none';
        document.getElementById('singleInput').style.display = 'block';
    }
}

function clearPublishedFilter() {
    delete filters['publishedFrom'];
    delete filters['publishedTo'];
    delete filters['publishedYear'];
    document.getElementById('publishedFrom').value = '';
    document.getElementById('publishedTo').value = '';
    document.getElementById('publishedYear').value = '';
    searchFiles(1, 10);
}

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
    searchFiles(currentPage, 10);
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

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('search-button').addEventListener('click', () => {
        currentPage = 1;
        searchFiles(currentPage, pageSize);
    });
});
