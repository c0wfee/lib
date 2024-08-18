document.addEventListener('DOMContentLoaded', function() {
    const bookId = document.getElementById('fileId').value;
    console.log('Book ID:', bookId);
    if (bookId !== 'default_id') {
        fetch(`/book/${bookId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                document.getElementById('source-image').src = '../static/images/book.png';
                updateField('book-title', data.title);
                updateField('authors', data.authors, 'authors-container');
                updateField('editors', data.editors, 'editors-container');
                updateField('series', data.series, 'series-container');
                updateField('isbn', data.isbn, 'isbn-container');
                updateField('publisher', data.publisher, 'publisher-container');
                updateField('published', data.published, 'published-container');
                updateField('edition', data.edition, 'edition-container');
                updateField('copyright-declaration', data.copyright_declaration, 'copyright-declaration-container');
                updateField('pages', data.pages, 'pages-container');
                updateField('subjects', data.subjects || 'N/A', 'subjects-container');
                updateField('description', data.description || '', 'description-container');

                const loanPeriodElement = document.getElementById('loan-period-container');
                const borrowButton = document.getElementById('borrow-button');
                const loanInfoElement = document.getElementById('loan-info');
                const buttonContainer = document.querySelector('.button-container');

                if (data.borrowPeriod > 0) {
                    document.getElementById('loan-period').textContent = `${data.borrowPeriod} Days`;

                    if (data.loanLabel === 'Borrowed') {
                        borrowButton.disabled = true;
                        borrowButton.textContent = 'Borrowed';
                        borrowButton.classList.add('disabled');
                    } else {
                        borrowButton.textContent = 'Borrow';
                        borrowButton.disabled = false;
                        borrowButton.onclick = function() {
                            borrowBook(bookId);
                        };
                    }

                    // Fetch loan info if the book is borrowed
                    if (data.loanLabel === 'Borrowed') {
                        fetch(`/borrow/${bookId}`)
                            .then(response => response.json())
                            .then(borrowData => {
                                if (borrowData.loanEndTime) {
                                    loanInfoElement.textContent = `On Loan, unavailable until ${borrowData.loanEndTime}`;
                                    loanInfoElement.style.display = 'block';
                                } else {
                                    loanInfoElement.style.display = 'none';
                                }
                            })
                            .catch(error => {
                                console.error('Error fetching borrow details:', error);
                                loanInfoElement.style.display = 'none';
                            });
                    } else {
                        loanInfoElement.style.display = 'none';
                    }
                } else {
                    // Hide borrow button and loan period info if borrowPeriod is 0
                    loanPeriodElement.style.display = 'none';
                    borrowButton.style.display = 'none';
                    loanInfoElement.style.display = 'none';
                }

                // 设置 viewPDF 和 downloadFile 按钮的显示规则
                if (data.view !== "Disable") {
                    const viewButton = document.createElement('button');
                    viewButton.textContent = 'View Online';
                    viewButton.onclick = function() {
                        viewPDF(bookId, data.display);
                    };
                    buttonContainer.appendChild(viewButton);
                }

                if (data.download !== "Disable") {
                    const downloadButton = document.createElement('button');
                    downloadButton.textContent = 'Download';
                    downloadButton.onclick = function() {
                        toggleDownloadOptions(bookId, downloadButton);
                    };
                    buttonContainer.appendChild(downloadButton);
                }

            })
            .catch(error => {
                console.error('Error fetching book details:', error);
                alert(`Error fetching book details: ${error.message}`);
            });
    } else {
        console.error('No book ID provided in URL');
    }
});

function updateField(elementId, value, containerId = null) {
    const element = document.getElementById(elementId);
    if (value) {
        element.textContent = value;
    } else if (containerId) {
        document.getElementById(containerId).style.display = 'none';
    }
}

// 下载和查看PDF函数

function viewPDF(id, view) {
    if (view == null) {
        window.location.href = `/pdf?fileId=${encodeURIComponent(id)}`;
    } else {
        fetch(`/downloadfiles/${encodeURIComponent(id)}`, { responseType: 'blob' })
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
        <div id="progress-container-${id}" style="margin-top: 10px; display: none;">
            <progress id="progress-${id}" value="0" max="100"></progress>
            <span id="progress-text-${id}">0%</span>
            <span id="speed-${id}"></span>
        </div>
    `;
    button.parentNode.appendChild(downloadOptions);
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
        progressContainer.style.display = 'block';
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
window.viewPDF = viewPDF;
window.downloadFile = downloadFile;
window.downloadBook = downloadBook;

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
        console.log('Success:', borrowData);
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
