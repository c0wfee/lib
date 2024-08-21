document.addEventListener('DOMContentLoaded', () => {
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

let allBooks = []; // 用于存储所有的借阅记录

async function fetchAndStoreBooks(username) {
    try {
        const response = await fetch(`/api/borrows/${encodeURIComponent(username)}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        allBooks = await response.json(); // 存储到全局变量
        console.log('Fetched Books:', allBooks);
    } catch (error) {
        console.error('Error fetching books:', error);
    }
}

function loadCurrentBorrowing() {
    const currentBooks = allBooks.filter(book => book.status !== "Returned"); // 筛选当前借阅书籍
    displayBooks(currentBooks, 'current-borrowing-container', false);
}

function loadBorrowingHistory() {
    const historyBooks = allBooks.filter(book => book.status === "Returned"); // 筛选历史借阅书籍
    displayBooks(historyBooks, 'borrowing-history-container', true);
}

function displayBooks(books, containerId, isHistory) {
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
            // 更新本地存储的 allBooks 数组
            allBooks = allBooks.map(book =>
                book.borrowId === parseInt(borrowId) ? { ...book, status: 'Returned' } : book
            );
            fetchAndStoreBooks(JSON.parse(localStorage.getItem('user')).username).then(() => {
                loadCurrentBorrowing(); // 重新加载当前借阅的书目
                loadBorrowingHistory(); // 更新历史借阅的书目
            });
        })
        .catch((error) => {
            console.error('Error:', error);
            alert('Operation failed');
        });
}
