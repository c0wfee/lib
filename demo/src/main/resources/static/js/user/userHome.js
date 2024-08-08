async function fetchBooks(username) {
    try {
        const response = await fetch(`/api/borrows/${username}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const books = await response.json();
        const currentBorrowing = books.filter(book => !book.returnedDate);
        const borrowingHistory = books.filter(book => book.returnedDate);
        displayBooks(currentBorrowing, 'current-borrowing-container');
        displayBooks(borrowingHistory, 'borrowing-history-container', true);
    } catch (error) {
        console.error('Error fetching books:', error);
    }
}

function displayBooks(books, containerId, isHistory = false) {
    const container = document.getElementById(containerId);
    container.innerHTML = ''; // Clear the existing content
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
            returnedDate.textContent = 'Returned Date: ' + book.returnedDate;
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
    const display = null; // Adjust this based on your needs
    if (display == null) {
        window.location.href = `/pdf?fileId=${encodeURIComponent(bookId)}`;
    } else {
        fetch(`http://8.130.130.240:8088/downloadfiles/${encodeURIComponent(bookId)}`, {responseType: 'blob'})
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
                console.error('Error downloading book:', error);
            });
    }
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
            const username = 'admin'; // Replace this with the actual username
            fetchBooks(username);
        })
        .catch((error) => {
            console.error('Error:', error);
            alert('Operation failed');
        });
}

document.addEventListener('DOMContentLoaded', () => {
    const username = 'admin'; // Replace this with the actual username
    fetchBooks(username);
});
