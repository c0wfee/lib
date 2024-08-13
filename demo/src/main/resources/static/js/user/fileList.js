window.onload = function() {
    searchFiles(1, 10); // 设置默认的page和size值
};

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

// 显示和隐藏筛选列表
function toggleList(headerElement) {
    const list = headerElement.nextElementSibling;
    if (list.style.display === 'none' || list.style.display === '') {
        list.style.display = 'block';
    } else {
        list.style.display = 'none';
    }
}

// 执行搜索并应用筛选条件
function executeSearch() {
    currentPage = 1;
    searchFiles(currentPage, pageSize);
}

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
        subject: filters['subject'] || [],
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
                    console.log(file); // 打印每个文件对象到控制台
                    const item = document.createElement('div');
                    item.className = 'item';
                    const sourceImage = sourceTypeImages[file.sourceType] || '../static/images/book.png';
                    const viewButton = file.view !== "Disable" ? `<button onclick="viewPDF('${file.id}', ${file.display})" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>View Online</button>` : '';
                    const downloadButton = file.download !== "Disable" ? `<button onclick="toggleDownloadOptions('${file.id}', this)" ${file.download === 'Disable' ? 'style="display: none;"' : ''}>Download</button>` : '';

                    // 默认的借阅信息
                    let loanInfo = file.loanLabel === 'Borrowed' ? `On Loan, unavailable until ${file.returnDate || 'Loading...'}` : '';

                    item.innerHTML = `
                    <div class="item-details">
                        <img src="${sourceImage}" alt="${file.sourceType}" class="source-type-image">
                        <div class="item-content">
                            <h3><strong>Title:</strong> <a href="/bookDetail?id=${file.id}" target="_blank">${file.title}</a></h3>
                            <p><strong>Alternate Title:</strong> ${file.alternativeTitle || 'N/A'}</p>
                            <p><strong>Source Type:</strong> ${file.sourceType || 'N/A'}</p>
                            ${file.authors ? `<p><strong>Authors:</strong> ${file.authors}</p>` : `<p><strong>Editors:</strong> ${file.editors || 'N/A'}</p>`}
                            <p><strong>ISBN:</strong> ${file.isbn || 'N/A'}</p>
                            <p><strong>Publisher:</strong> ${file.publisher || 'N/A'}</p>
                            <p><strong>Published:</strong> ${file.published || 'N/A'}</p>
                            <p><strong>Status:</strong> ${file.status || 'N/A'}</p>
                            <p id="loan-period-${file.id}"><strong>Loan Period:</strong> ${loanInfo}</p>
                            <p>${file.description || ''}</p>
                            <div class="item-meta">
                                <p><strong>Subjects:</strong> ${file.subjects || 'N/A'}</p>
                                <a href="${file.url || '#'}" target="_blank" style="display: none;">URL: ${file.url || 'N/A'}</a>
                                <div class="button-container">
                                    ${file.view !== 'Disable' ? viewButton : ''}
                                    ${file.download !== 'Disable' ? downloadButton : ''}
                                    <button id="borrow-button-${file.id}" onclick="borrowBook('${file.id}')" ${file.loanLabel === 'Borrowed' ? 'disabled style="background-color: grey; color: white;"' : ''}>Borrow</button>
                                    <span id="loan-info-${file.id}" style="color: grey; margin-left: 10px;"></span>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                    itemList.appendChild(item);

                    if (file.loanLabel === 'Borrowed') {
                        fetch(`http://localhost:8088/borrow/${file.id}`, {
                            method: 'GET',
                            headers: {
                                'Content-Type': 'application/json'
                            }
                        })
                            .then(response => response.json())
                            .then(borrowData => {
                                const returnDate = borrowData.loanEndTime || 'N/A';
                                document.getElementById(`loan-info-${file.id}`).textContent = `On Loan, unavailable until ${returnDate}`;
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

document.addEventListener('DOMContentLoaded', function () {
    document.getElementById('search-button').addEventListener('click', () => {
        currentPage = 1;
        searchFiles(currentPage, pageSize);
    });
});
