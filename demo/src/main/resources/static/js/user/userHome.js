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
    fetchBooks('admin', false); // 'admin' 是当前用户名，false 表示加载当前借阅的书目
}

function loadBorrowingHistory() {
    fetchBooks('admin', true); // 'admin' 是当前用户名，true 表示加载历史借阅的书目
}

async function fetchBooks(username, isHistory = false) {
    try {
        const response = await fetch(`/api/borrows/${username}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const books = await response.json();
        console.log('Fetched Books:', books);

        const currentDate = new Date();

        let filteredBooks;
        if (isHistory) {
            filteredBooks = books.filter(book => new Date(book.loanEndTime) < currentDate);

        } else {
            filteredBooks = books.filter(book => new Date(book.loanEndTime) >= currentDate);

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

        cardBody.appendChild(author);
        cardBody.appendChild(loanStartTime);

        if (isHistory) {
            const returnedDate = document.createElement('p');
            returnedDate.className = 'card-text';
            returnedDate.textContent = 'Returned Date: ' + book.loanEndTime;
            cardBody.appendChild(returnedDate);
        } else {
            const loanEndTime = document.createElement('p');
            loanEndTime.className = 'card-text';
            loanEndTime.textContent = 'Due Date: ' + book.loanEndTime;

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
                    returnBook(book.bookId);
                }
            });

            cardBody.appendChild(loanEndTime);
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

function returnBook(bookId) {
    let data = new URLSearchParams();
    data.append('bookId', bookId);
    fetch("/returnBook", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: data
    })
        .then(response => response.json())
        .then(data => {
            console.log('Success:', data);
            alert('Operation successful');
            loadCurrentBorrowing();
        })
        .catch((error) => {
            console.error('Error:', error);
            alert('Operation failed');
        });
}
